#!/usr/bin/env python3
"""
Hidden Leaf: tools for the recompilation of Naruto Shippuden: Ultimate Ninja 5 (PS2, SLES-55605).

Nothing of the game is kept in the repository: these tools read the player's own ISO and write what they make
outside the tree (or into build/, which git ignores).

  un5.py verify   <iso>                  which release or mod an ISO is (by SHA-256); the base must match Redump (SHA-1)
  un5.py extract  <iso> <dir>            copies the boot files (SYSTEM.CNF, the ELF, PRG/, MODULES/, small DATA/ .BIN)
  un5.py overlays <dir-or-files...>      the CodeWarrior overlays (MWo3) of PRG/: id, load address, sizes
  un5.py diff     <base-iso> <mod-iso> [-o pack.json]
                                         function by function, what a mod changes in the code (ELF and overlays):
                                         the list of functions its code pack has to carry
  un5.py unpack   <elf> <out>            unpacks an ELF packed with SjCRUNCH (the Brazilian version's)
  un5.py prepelf  <elf> <out>            the ELF for the recompiler: its one big section split into code and data
  un5.py ovlelf   <overlay.BIN> <out>    an overlay (MWo3) as an ELF at its load address, code and data apart
  un5.py fnmap    <elf> <recomp output> <map.csv> [--also <other ELF>...]
                                         the function map for a second pass of the recompiler: the functions it
                                         found, plus every function only reached through a pointer in the data
                                         (constructors, vtables, callbacks), in the CSV format of a Ghidra export

Functions are found here by their ends (jr ra + delay slot): an approximation, good enough to size a mod's changes.
The function map the recompiler uses comes from Ghidra (docs/plano.md).
"""
import argparse
import hashlib
import json
import os
import re
import struct
import sys

SECTOR = 2048

BASE_SERIAL = "SLES-55605"
BASE_ELF = "SLES_556.05"
BASE_ELF_SHA256 = "20a43677397731a2a20899336d1165ace5b436906b9b89be90fb10f4558dd19d"
# the whole European ISO as Redump lists it (1926234112 bytes)
BASE_ISO_SHA1 = "50eedd72f0a8ee95b16c6e4e7a6b259743925012"
BASE_OVERLAYS = {
    "PRG/BTL.BIN": "7e8518da7bd4957af18cb0ababe67f0e9b37c42c6551375201b15997f0a3dfe3",
    "PRG/ADV.BIN": "7e2af55362141bb1b055247cd7ef7edae290f3c0095701bc51467f096a2d00b8",
    "PRG/ETC.BIN": "bdb6bda1f9d335047586a263e478486c8e7924b91fa972b6f3e58caec5ea0778",
    "PRG/TEXTENG.BIN": "3e42d2ddffe770b05dd41e2c5937380133e255c9ce32ca2f037e34c65a8e571e",
    "PRG/TEXTFRN.BIN": "485d44d2866e9e985d2d84b4796bd8daa573567d44b8913b9fbccffaafc836b0",
    "PRG/TEXTGER.BIN": "84de40f3def485b80863370723e462a7b23ebb6a4b5e6a088b4897f78005222d",
    "PRG/TEXTITA.BIN": "110a915c3043214a6c2a9bd2ca3cb33284629126f91b76db6e525a9fd7b20b03",
    "PRG/TEXTSPA.BIN": "72ec1bb3aead2764bdc096a1a757c6969d1573c99954df6711e0a0495418e927",
}
KNOWN_MODS = {
    "47c40141a3e1aeb0c96bc28e8dc311938b284d54fd21f4d8ba953c2e16234809": ("un6-a35", "Ultimate Ninja 6 (Alpha 3.5)"),
    "20872b06f4ef8b5ff49291b297a97941bc9131525b79e1cab2b1717156ca0056": ("un6-a11x", "Ultimate Ninja 6 (Alpha 1.1X)"),
    "7c3517aa09c0c3e34742b89ee19a2de5f71753380ee198e8861a8030cfcf8e51": ("un5-br", "Versao Brasileira"),
}

JR_RA = b"\x08\x00\xe0\x03"


# ---------------------------------------------------------------------------------------------------------------
# ISO 9660

class Iso:
    """A read-only ISO 9660 image: files by path ("PRG/BTL.BIN"), without ";1"."""

    def __init__(self, path):
        self.f = open(path, "rb")
        self.files = {}
        pvd = self._read(16 * SECTOR, SECTOR)
        if pvd[0] != 1 or pvd[1:6] != b"CD001":
            raise SystemExit(f"{path}: not an ISO 9660 image")
        lba, size = struct.unpack_from("<I", pvd, 156 + 2)[0], struct.unpack_from("<I", pvd, 156 + 10)[0]
        self._walk(lba, size, "", 0)

    def _read(self, off, n):
        self.f.seek(off)
        return self.f.read(n)

    def _walk(self, lba, size, d, depth):
        if depth > 8:
            return
        data = self._read(lba * SECTOR, size)
        pos = 0
        while pos + 34 <= len(data):
            ln = data[pos]
            if ln == 0:
                pos = (pos // SECTOR + 1) * SECTOR
                continue
            extent, nbytes = struct.unpack_from("<I", data, pos + 2)[0], struct.unpack_from("<I", data, pos + 10)[0]
            flags, nlen = data[pos + 25], data[pos + 32]
            raw = data[pos + 33:pos + 33 + nlen]
            pos += ln
            if nlen == 1 and raw in (b"\x00", b"\x01"):
                continue
            name = raw.decode("latin-1").split(";")[0]
            if name.endswith(".") and not flags & 2:
                name = name[:-1]
            p = f"{d}/{name}" if d else name
            if flags & 2:
                self._walk(extent, nbytes, p, depth + 1)
            elif p.upper() in self.files:
                self.files[p.upper()][1] += nbytes
            else:
                self.files[p.upper()] = [extent * SECTOR, nbytes]

    def read(self, path):
        off, n = self.files[path.upper()]
        return self._read(off, n)

    def sha256(self, path):
        off, n = self.files[path.upper()]
        h = hashlib.sha256()
        self.f.seek(off)
        while n > 0:
            b = self.f.read(min(n, 1 << 20))
            h.update(b)
            n -= len(b)
        return h.hexdigest()

    def boot(self):
        cnf = self.read("SYSTEM.CNF").decode("latin-1")
        m = re.search(r"BOOT2\s*=\s*cdrom0:\\?([^;\r\n]+)", cnf, re.I)
        return m.group(1).strip().split("\\")[-1].upper() if m else None


def is_boot_file(path, size):
    u = path.upper()
    return (u == "SYSTEM.CNF" or re.match(r"^S[LC][A-Z]{2}_\d{3}\.\d{2}$", u) is not None or u.startswith("PRG/")
            or u.startswith("MODULES/") or (u.startswith("DATA/") and u.endswith(".BIN") and size <= 2 << 20))


# ---------------------------------------------------------------------------------------------------------------
# code: ELF segments and MWo3 overlays

def elf_text(data):
    """The ELF's loaded bytes with a file part: [(vaddr, bytes)]."""
    if data[:4] != b"\x7fELF":
        raise SystemExit("not an ELF")
    phoff, = struct.unpack_from("<I", data, 0x1C)
    phentsize, phnum = struct.unpack_from("<HH", data, 0x2A)
    out = []
    for i in range(phnum):
        ptype, off, vaddr, _paddr, filesz = struct.unpack_from("<IIIII", data, phoff + i * phentsize)
        if ptype == 1 and filesz:
            out.append((vaddr, data[off:off + filesz]))
    return out


def mwo3(data):
    """A CodeWarrior overlay: header of 0x40 bytes, then text and data. Returns a dict, or None.
    The game loads the whole file, header included, at addr: the body (code) is at addr + 0x40 in memory ("code");
    ETC.BIN's end is addr + 0x40 + text + data, and the overlays' jal targets are prologues there."""
    if data[:4] != b"MWo3":
        return None
    oid, addr, text, dat, bss, ctors, ctors_end = struct.unpack_from("<7I", data, 4)
    return dict(id=oid, addr=addr, code=addr + 0x40, text=text, data=dat, bss=bss, ctors=ctors, ctors_end=ctors_end,
                body=data[0x40:])


def ctor_islands(data, o):
    """The header's last two words are the overlay's table of static constructors (ctors .. ctors_end), which the game
    calls after loading it. CodeWarrior puts them in the data, outside text (BTL.BIN 0x8ea080, ADV.BIN 0x8aa680 on).
    Returns the code around each one: [(lo, hi)], from the constructor through the functions right after it."""
    lo_data, hi_data = o["code"] + o["text"], o["code"] + o["text"] + o["data"]

    def word(addr):
        return struct.unpack_from("<I", data, addr - o["addr"])[0] if lo_data <= addr < hi_data else None

    islands = []
    for at in range(o["ctors"], o["ctors_end"], 4):
        start = word(at)
        if start is None or not lo_data <= start < hi_data:
            continue
        p, reach = start, start
        while True:
            w = word(p)
            if w is None:
                break
            op = w >> 26
            if op in (1, 4, 5, 6, 7, 0x14, 0x15, 0x16, 0x17) or (op == 0x11 and (w >> 21) & 0x1F == 8):
                imm = w & 0xFFFF
                reach = max(reach, p + 4 + ((imm - 0x10000 if imm & 0x8000 else imm) << 2))
            if w == 0x03E00008 and p + 4 >= reach:
                p += 8
                while word(p) == 0:
                    p += 4
                nxt = word(p)
                # another function follows (addiu sp, sp, -n): the same island
                if nxt is not None and nxt >> 16 == 0x27BD and nxt & 0x8000:
                    reach = p
                    continue
                break
            p += 4
        islands.append((start, p))
    merged = []
    for lo, hi in sorted(islands):
        if merged and lo <= merged[-1][1]:
            merged[-1] = (merged[-1][0], max(merged[-1][1], hi))
        else:
            merged.append((lo, hi))
    return merged


def functions(blob, base):
    """Approximate functions of a code blob: split after every jr ra and its delay slot. {start: bytes}."""
    starts = [0] + [i + 8 for i in range(0, len(blob) - 3, 4) if blob[i:i + 4] == JR_RA]
    return {base + s: blob[s:e] for s, e in zip(starts, starts[1:] + [len(blob)]) if e > s}


def code_regions(iso):
    """{name: (load address, code bytes)} for the boot ELF and every overlay of PRG/."""
    out = {}
    boot = iso.boot()
    for vaddr, seg in elf_text(iso.read(boot)):
        out[f"{boot}@{vaddr:08x}"] = (vaddr, seg)
    for p in sorted(iso.files):
        if p.startswith("PRG/"):
            o = mwo3(iso.read(p))
            if o:
                out[p] = (o["code"], o["body"])
    return out


# ---------------------------------------------------------------------------------------------------------------
# commands

def cmd_verify(a):
    iso = Iso(a.iso)
    boot = iso.boot()
    if not boot or boot not in iso.files:
        raise SystemExit("no PS2 boot program (SYSTEM.CNF)")
    sha = iso.sha256(boot)
    print(f"boot program  {boot}  sha256 {sha}")
    if sha == BASE_ELF_SHA256:
        bad = [p for p, want in BASE_OVERLAYS.items() if p not in iso.files or iso.sha256(p) != want]
        if bad:
            print(f"European release, but overlays differ: {', '.join(bad)} (a mod over the original program?)")
            return 1
        sha1 = file_sha1(a.iso)
        print(f"image         sha1 {sha1}")
        if sha1 != BASE_ISO_SHA1:
            print(f"European program, but the image is not the Redump dump (sha1 {BASE_ISO_SHA1}): some other file on it"
                  " is damaged or patched. Dump the disc again, or use an image that matches Redump.")
            return 4
        print(f"base: Naruto Shippuden: Ultimate Ninja 5, European release ({BASE_SERIAL}), unmodified (Redump)")
        return 0
    if sha in KNOWN_MODS:
        print(f"mod: {KNOWN_MODS[sha][1]} ({KNOWN_MODS[sha][0]}) - import the European ISO as the base, this one as a mod")
        return 2
    print("unknown program: another release, a damaged image, or a mod this tool does not know")
    return 3


def file_sha1(path):
    h = hashlib.sha1()
    with open(path, "rb") as f:
        while b := f.read(1 << 22):
            h.update(b)
    return h.hexdigest()


def cmd_extract(a):
    iso = Iso(a.iso)
    n = 0
    for p, (off, size) in sorted(iso.files.items()):
        if not is_boot_file(p, size):
            continue
        out = os.path.join(a.dir, p)
        os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
        with open(out, "wb") as f:
            f.write(iso.read(p))
        n += 1
        print(f"{size:>10}  {p}")
    print(f"{n} files in {a.dir}")


def cmd_lbn(a):
    # Where each file sits on the disc: "<first sector, hex> <bytes> <path>" per line, by sector. The runtime reads the
    # extracted disc, so without this it gives the files sectors of its own; with it, the game sees the same sector
    # numbers as on the disc (and PCSX2), and the drive's seek times can follow the real layout (docs/registro.md,
    # 2026-11-08).
    iso = Iso(a.iso)
    rows = sorted((off // SECTOR, size, p) for p, (off, size) in iso.files.items())
    with open(a.out, "w") as f:
        for lsn, size, p in rows:
            f.write(f"{lsn:x} {size} {p}\n")
    print(f"{len(rows)} files in {a.out}")


def cmd_overlays(a):
    paths = []
    for x in a.paths:
        paths += [os.path.join(x, f) for f in sorted(os.listdir(x))] if os.path.isdir(x) else [x]
    for p in paths:
        with open(p, "rb") as f:
            o = mwo3(f.read())
        if not o:
            print(f"{os.path.basename(p):14} (not an overlay)")
            continue
        body = o["body"]
        fn = len(functions(body[:o["text"] + o["data"]], o["code"]))
        print(f"{os.path.basename(p):14} id {o['id']}  load {o['addr']:08x}  text {o['text']:06x}  data {o['data']:06x}"
              f"  bss {o['bss']:05x}  ctors {o['ctors']:08x}  ~{fn} functions")


def cmd_diff(a):
    base, mod = Iso(a.base), Iso(a.mod)
    rb, rm = code_regions(base), code_regions(mod)
    # the boot program may be renamed by the mod (SLUS_556.06): match ELF segments by address
    def key(k):
        return k.split("@", 1)[1] if "@" in k else k
    rbk = {key(k): v for k, v in rb.items()}
    report = {"base_elf_sha256": base.sha256(base.boot()), "mod_elf_sha256": mod.sha256(mod.boot()),
              "mod_boot": mod.boot(), "regions": []}
    total_changed = total_new = 0
    for name, (addr, blob) in rm.items():
        b = rbk.get(key(name))
        fm = functions(blob, addr)
        if b is None or b[0] != addr:
            entry = {"region": name, "load": f"{addr:08x}", "status": "new", "functions": len(fm)}
            total_new += len(fm)
        else:
            fb = functions(b[1], addr)
            changed = sorted(s for s, code in fm.items() if fb.get(s) != code)
            entry = {"region": name, "load": f"{addr:08x}", "status": "changed" if changed else "same",
                     "functions": len(fm), "changed": len(changed), "changed_at": [f"{s:08x}" for s in changed]}
            total_changed += len(changed)
        report["regions"].append(entry)
        print(f"{name:28} {entry['status']:8} {entry['functions']:6} functions"
              + (f", {entry['changed']} changed" if "changed" in entry else ""))
    print(f"total: {total_changed} changed, {total_new} new (the code pack's functions)")
    if a.o:
        with open(a.o, "w") as f:
            json.dump(report, f, indent=1)
        print(f"written: {a.o}")


def cmd_unpack(a):
    """SjCRUNCH v0.3 (Sjeep, 2002): the packed program is LZO1X blocks behind the unpacker; header in .data."""
    try:
        import lzo  # python-lzo
    except ImportError:
        raise SystemExit("needs python-lzo (pip install python-lzo; liblzo2-dev to build it)")
    d = open(a.elf, "rb").read()
    if b"SjCRUNCH" not in d:
        raise SystemExit("not packed with SjCRUNCH")
    # .data of the unpacker starts with the signature; the header the unpacker reads is 0xf30 bytes in
    data_off = d.index(b"SjCRUNCH v0.3")
    h = data_off + 0xF30
    dest, entry = struct.unpack_from("<II", d, h + 8)
    p = h + 0x10 + 0x20
    out = bytearray()
    while True:
        ul, cl = struct.unpack_from("<II", d, p)
        if ul == 0:
            break
        blk = d[p + 8:p + 8 + cl]
        out += lzo.decompress(blk, False, ul) if cl < ul else blk[:ul]
        p += 8 + cl
    with open(a.out, "wb") as f:
        f.write(out)
    print(f"unpacked {len(out):#x} bytes, loads at {dest:08x}, entry {entry:08x} -> {a.out}")


# ---------------------------------------------------------------------------------------------------------------
# ELFs for the recompiler

# Where the code of the European ELF is (the linker put it, the data and two islands of code in one section):
#   0x100000-0x3d5760   the game's code
#   0x40f848-0x410d68   kernel-mode code the game copies into the kernel (lui 0x8007...): data for the recompiler
#   0x5e3a00-0x5e7208   the static constructors (their table follows at 0x5e7210)
CODE_RANGES = {
    BASE_ELF_SHA256: [(0x100000, 0x3D5760), (0x5E3A00, 0x5E7208)],
    # UN6 Alpha 3.5 (SLUS_556.06): the European program changed in place (same segment, 1687 words of code), its new code in
    # the overlays MOD.BIN and TEXTBRA.BIN
    "47c40141a3e1aeb0c96bc28e8dc311938b284d54fd21f4d8ba953c2e16234809": [(0x100000, 0x3D5760), (0x5E3A00, 0x5E7208)],
}

SHF_WRITE, SHF_ALLOC, SHF_EXEC = 1, 2, 4


def write_sections(data, sections):
    """A new section table for an ELF32 image, appended at its end: sections = [(name, type, flags, addr, offset, size)]."""
    names = bytearray(b"\0")
    entries = [(0,) * 10]
    for name, stype, flags, addr, off, size in sections:
        entries.append((len(names), stype, flags, addr, off, size, 0, 0, 16 if flags & SHF_EXEC else 4, 0))
        names += name.encode() + b"\0"
    shstr_name = len(names)
    names += b".shstrtab\0"
    out = bytearray(data)
    out += b"\0" * ((4 - len(out) % 4) % 4)
    str_off = len(out)
    out += names
    out += b"\0" * ((4 - len(out) % 4) % 4)
    entries.append((shstr_name, 3, 0, 0, str_off, len(names), 0, 0, 1, 0))
    shoff = len(out)
    for e in entries:
        out += struct.pack("<IIIIIIIIII", *e)
    struct.pack_into("<I", out, 0x20, shoff)
    struct.pack_into("<HHH", out, 0x2E, 40, len(entries), len(entries) - 1)
    return bytes(out)


def cmd_prepelf(a):
    data = open(a.elf, "rb").read()
    sha = hashlib.sha256(data).hexdigest()
    ranges = CODE_RANGES.get(sha)
    if ranges is None:
        raise SystemExit("unknown ELF (sha256 %s): no code ranges for it" % sha)
    phoff, = struct.unpack_from("<I", data, 0x1C)
    _t, off, vaddr, _p, filesz, memsz = struct.unpack_from("<IIIIII", data, phoff)
    secs = []
    cur = vaddr
    n = 0
    for lo, hi in ranges:
        if cur < lo:
            secs.append((".data%d" % n, 1, SHF_ALLOC | SHF_WRITE, cur, off + cur - vaddr, lo - cur)); n += 1
        secs.append((".text%d" % n, 1, SHF_ALLOC | SHF_EXEC, lo, off + lo - vaddr, hi - lo)); n += 1
        cur = hi
    end = vaddr + filesz
    if cur < end:
        secs.append((".data%d" % n, 1, SHF_ALLOC | SHF_WRITE, cur, off + cur - vaddr, end - cur))
    if memsz > filesz:
        secs.append((".bss", 8, SHF_ALLOC | SHF_WRITE, end, off + filesz, memsz - filesz))
    with open(a.out, "wb") as f:
        f.write(write_sections(data, secs))
    for s in secs:
        print("%-8s %08x-%08x %s" % (s[0], s[3], s[3] + s[5], "code" if s[2] & SHF_EXEC else "data"))


def data_code_islands(data, o):
    """Code the overlay keeps in its data: the A35's MOD.BIN and TEXTBRA.BIN have a text of a few words and their
    functions among the data. Found by their prologues (addiu sp, sp, -n) and returns (jr ra), in clusters: [(lo, hi)].
    A cluster that starts with a return (a leaf function, no prologue: MOD.BIN's 0x940194) begins where the data does, or
    where the last cluster ended, if that is near."""
    if o["text"] >= 0x1000:
        return []
    lo_data = o["code"] + o["text"]
    hi_data = min(o["code"] + o["text"] + o["data"], o["addr"] + len(data)) & ~3  # (TEXTBRA.BIN's header counts more)
    # Marks: prologues start code; returns end it; a J or JAL into the game's main code (the mod's hooks call the game's
    # functions and jump back into them, 0x9509a0 has no prologue nor return near it) is code too, and its island
    # starts up to 0x100 bytes before it.
    marks = []
    for v in range(lo_data, hi_data - 3, 4):
        w = struct.unpack_from("<I", data, v - o["addr"])[0]
        game = (w >> 26) in (2, 3) and 0x100000 <= ((w & 0x03FFFFFF) << 2) < 0x3E0000
        if ((w >> 16) == 0x27BD and w & 0x8000) or w == 0x03E00008 or game:
            marks.append((v, "ret" if w == 0x03E00008 else "game" if game else "pro"))
    islands = []
    for v, kind in marks:
        if islands and v - islands[-1][2] <= 0x2000:
            islands[-1][2] = v
            islands[-1][1] = max(islands[-1][1], v + 8)
        elif kind == "pro":
            islands.append([v, v + 4, v])
        else:
            before = islands[-1][1] if islands else lo_data
            start = before if v - before <= 0x1000 else max(before, v - 0x100)
            islands.append([start, v + 8, v])
    # An island goes on to where its own branches land, a little past its last mark: MOD.BIN's 0x9ee3b0 ends with a jal
    # into the game (0x9ee408) and then two branch targets that are j's (0x9ee41c to the mod, 0x9ee424 to BTL.BIN), not
    # marks: cut at 0x9ee410, the A35 died after a fight with missing-target 0x9ee41c (docs/registro.md, 2026-11-05 13:00).
    def word(v):
        return struct.unpack_from("<I", data, v - o["addr"])[0] if lo_data <= v < hi_data else None
    for isl in islands:
        grown = True
        while grown:
            grown = False
            for p in range(isl[0], isl[1], 4):
                w = word(p)
                if w is None:
                    continue
                op = w >> 26
                if not (op in (1, 4, 5, 6, 7, 0x14, 0x15, 0x16, 0x17) or (op == 0x11 and (w >> 21) & 0x1F == 8)):
                    continue
                imm = w & 0xFFFF
                t = p + 4 + ((imm - 0x10000 if imm & 0x8000 else imm) << 2)
                if isl[1] <= t < min(isl[1] + 0x40, hi_data):
                    isl[1] = min(t + 8, hi_data)
                    grown = True
    return [(lo, hi) for lo, hi, _ in islands if hi > lo]


def called_islands(data, o, callers):
    """With --from ELF...: where other ELFs' code jumps or calls into this overlay's data (only for overlays that keep
    their code there, like the A35's MOD.BIN: BTL.BIN jumps to 0x9cb104, right after an island the marks found), code
    from there to the next return (at most 0x1000 bytes, else 0x100): [(lo, hi)]."""
    if o["text"] >= 0x1000 or not callers:
        return []
    lo_data = o["code"] + o["text"]
    hi_data = min(o["code"] + o["text"] + o["data"], o["addr"] + len(data)) & ~3
    out = []
    for path in callers:
        blob = open(path, "rb").read()
        for name, addr, off, size, flags, t in sections_of(blob):
            if t != 1 or not flags & SHF_EXEC:
                continue
            for i in range(0, min(size, len(blob) - off) - 3, 4):
                w, = struct.unpack_from("<I", blob, off + i)
                if (w >> 26) not in (2, 3):
                    continue
                v = (w & 0x03FFFFFF) << 2
                if not lo_data <= v < hi_data:
                    continue
                end = v + 0x100
                for p in range(v, min(v + 0x1000, hi_data - 4), 4):
                    if struct.unpack_from("<I", data, p - o["addr"])[0] == 0x03E00008:
                        end = p + 8
                        break
                out.append((v, min(end, hi_data)))
    return out


def cmd_ovlelf(a):
    raw = open(a.ovl, "rb").read()
    o = mwo3(raw)
    if not o:
        raise SystemExit("not an MWo3 overlay")
    body = raw[0x40:0x40 + o["text"] + o["data"]]
    # ELF header + one program header, then the body
    hdr = bytearray(52 + 32)
    hdr[0:16] = b"\x7fELF\x01\x01\x01" + b"\0" * 9
    struct.pack_into("<HHIIIIIHHHHHH", hdr, 16, 2, 8, 1, o["code"], 52, 0, 0x20924001, 52, 32, 1, 40, 0, 0)
    body_off = 0x80
    struct.pack_into("<IIIIIIII", hdr, 52, 1, body_off, o["code"], o["code"], len(body), len(body) + o["bss"], 7, 0x10)
    data = bytes(hdr) + b"\0" * (body_off - len(hdr)) + body
    secs = [(".text", 1, SHF_ALLOC | SHF_EXEC, o["code"], body_off, o["text"])]
    # the data, with the static constructors' code in it as code sections of its own
    at, n = o["code"] + o["text"], 0
    islands = []
    for lo, hi in sorted(ctor_islands(raw, o) + data_code_islands(raw, o) + called_islands(raw, o, getattr(a, "callers", None))):
        if islands and lo <= islands[-1][1]:
            islands[-1] = (islands[-1][0], max(hi, islands[-1][1]))
        else:
            islands.append((lo, hi))
    # an island right after the text continues it (MOD.BIN's first function starts in its text, at 0x940070, and goes
    # on in the data: as two sections the recompiler ended it at the text's end)
    if islands and islands[0][0] == at:
        secs[0] = (".text", 1, SHF_ALLOC | SHF_EXEC, o["code"], body_off, islands[0][1] - o["code"])
        at = islands.pop(0)[1]
    for lo, hi in islands + [(o["code"] + o["text"] + o["data"],) * 2]:
        if lo > at:
            secs.append((".data%d" % n, 1, SHF_ALLOC | SHF_WRITE, at, body_off + at - o["code"], lo - at))
        if hi > lo:
            secs.append((".init%d" % n, 1, SHF_ALLOC | SHF_EXEC, lo, body_off + lo - o["code"], hi - lo))
        at, n = hi, n + 1
    if o["bss"]:
        secs.append((".bss", 8, SHF_ALLOC | SHF_WRITE, o["code"] + o["text"] + o["data"], body_off + len(body), o["bss"]))
    with open(a.out, "wb") as f:
        f.write(write_sections(data, secs))
    print("overlay id %d at %08x: code %#x, data %#x, bss %#x -> %s" % (o["id"], o["code"], o["text"], o["data"], o["bss"], a.out))


def sections_of(data):
    """(name, addr, offset, size, flags, type) of an ELF32's sections."""
    shoff, = struct.unpack_from("<I", data, 0x20)
    shentsize, shnum, shstrndx = struct.unpack_from("<HHH", data, 0x2E)
    raw = [struct.unpack_from("<IIIIIIIIII", data, shoff + i * shentsize) for i in range(shnum)]
    stroff = raw[shstrndx][4]
    out = []
    for e in raw[1:]:
        name = data[stroff + e[0]:data.index(b"\0", stroff + e[0])].decode()
        out.append((name, e[3], e[4], e[5], e[2], e[1]))
    return out


def cmd_fnmap(a):
    data = open(a.elf, "rb").read()
    secs = sections_of(data)
    code = [(addr, addr + size, off) for name, addr, off, size, flags, t in secs if flags & SHF_EXEC and t == 1]
    blobs = [(addr, off, size) for name, addr, off, size, flags, t in secs if t == 1 and flags & SHF_ALLOC]

    def word(addr):
        for lo, hi, off in code:
            if lo <= addr < hi:
                return struct.unpack_from("<I", data, off + addr - lo)[0]
        return None

    def after_return(v):
        """v follows the end of a function: jr ra, its delay slot, then maybe nop padding."""
        p = v - 4
        for _ in range(8):
            w = word(p)
            if w is None:
                return False
            if w != 0:
                break
            p -= 4
        # p is the last word that is not a nop: the delay slot itself, or the jr ra when the slot is a nop. A jr ra
        # right before v puts v on its delay slot, which belongs to that function (0x12203c, addiu sp, sp, 0x30)
        return word(p - 4) == 0x03E00008 or (word(p) == 0x03E00008 and p < v - 4)

    starts = set()
    for f in os.listdir(a.output):
        m = re.match(r"sub_([0-9A-Fa-f]{8})_0x", f)
        if m:
            starts.add(int(m.group(1), 16))
    found = len(starts)
    # pointers in any allocated part of the ELF to a function prologue (addiu sp, sp, -N) in the code
    added = 0
    for addr, off, size in blobs:
        for i in range(0, size - 3, 4):
            v, = struct.unpack_from("<I", data, off + i)
            if v & 3 or v in starts:
                continue
            w = word(v)
            if w is None or w == 0:
                continue
            if ((w >> 16) == 0x27BD and (w & 0x8000)) or after_return(v):
                starts.add(v)
                added += 1
    # a prologue right after a return is a function even when nothing here names it: the analyzer follows calls only, and
    # left half of BTL out (entered from the main ELF, from pointers made at run time)
    own = 0
    for lo, hi, off in code:
        for v in range(lo, hi - 3, 4):
            if v in starts:
                continue
            w = word(v)
            if not w or not after_return(v):
                continue
            # a prologue, or a leaf at the 16-byte alignment of the compiler's functions (BTL's empty virtual methods,
            # jr ra at 0x6ea220, known only to vtables built at run time). If that is the arm of a function after one
            # of its returns, the branch to it becomes a tail call on the same registers.
            if ((w >> 16) == 0x27BD and (w & 0x8000)) or not v & 15:
                starts.add(v)
                own += 1
    print("%d more after a return in this ELF's code" % own)

    # other ELFs call into this one (the BTL overlay calls 0x3d1f60, which nothing here calls; the main ELF calls
    # 0x72f340, in BTL): JAL and J in their code, and pointers in anything they load. Only code is read as instructions,
    # data that happens to look like a JAL would split functions at any word after a return.
    outside = 0
    mine = [(lo, hi) for lo, hi, _ in code]
    for path in a.also or []:
        blob = open(path, "rb").read()
        # another overlay loaded at the same addresses (ADV and BTL) calls its own code there, not ours: skipped
        theirs = [(addr, addr + size) for name, addr, off, size, flags, t in sections_of(blob) if flags & SHF_EXEC]
        if any(lo < h and l < hi for lo, hi in mine for l, h in theirs):
            continue
        for name, addr, off, size, flags, t in sections_of(blob):
            if t != 1 or not flags & SHF_ALLOC:
                continue
            for i in range(0, min(size, len(blob) - off) - 3, 4):  # (a section may run past the file: the A35's MOD.BIN)
                w, = struct.unpack_from("<I", blob, off + i)
                call = bool(flags & SHF_EXEC) and (w >> 26) in (2, 3)
                v = (w & 0x03FFFFFF) << 2 if call else w
                if v & 3 or v in starts:
                    continue
                first = word(v)
                if first is None or first == 0:
                    continue
                prologue = (first >> 16) == 0x27BD and bool(first & 0x8000)
                # a J from another ELF's code goes back into the middle of a function here: the A35's MOD.BIN hooks the
                # game's code and jumps back after its own (0x941838 -> 0x10bd74). That has to be an entry too.
                back = call and (w >> 26) == 2
                # (and a call to code after padding: the A35's main ELF calls 0x9476c0 in MOD.BIN, a leaf after zeros)
                if prologue or back or (call and (after_return(v) or word(v - 4) == 0)):
                    starts.add(v)
                    outside += 1
    if a.also:
        print("%d more reached from the other ELFs" % outside)
    ordered = sorted(starts)
    with open(a.map, "w") as f:
        f.write("name,start,end,size\n")
        for i, s in enumerate(ordered):
            hi = next((h for lo, h, _ in code if lo <= s < h), s + 4)
            e = min(ordered[i + 1], hi) if i + 1 < len(ordered) else hi
            f.write("sub_%08X,0x%x,0x%x,%d\n" % (s, s, e, e - s))
    print("%d functions found by the recompiler, %d more reached through pointers -> %s" % (found, added, a.map))


# SDK functions the analyzer names, that the runtime handles under another name. Without the handler the game's own code
# runs and asks the IOP (cdvdfsv, never loaded here) over SIF RPC, and waits forever: sceCdLayerSearchFile binds
# 0x80000597 at boot. Each one moves from entry_points to stubs, under the runtime's name.
STUB_ALIASES = {
    "sceCdLayerSearchFile": "sceCdSearchFile",  # (file, name, layer): the runtime reads only file and name
}


# SDK functions of the European ELF the analyzer leaves unnamed (sub_), bound to the runtime's handler by address.
# libmpeg keeps its state for the runtime's stubs, not in the game's memory: the game's own sceMpegIsEnd and
# sceMpegIsRefBuffEmpty read fields nobody fills, so the opening movie never ended (its last frame stayed on screen).
STUB_AT = {
    0x166f40: "sceSifFreeIopHeap",  # (no name in the ELF: unstubbed, it does nothing, and every movie leaks 0x2e810 bytes of IOP heap until the game gets 0 and writes to address 0)
    0x159748: "sceMpegIsEnd",
    0x159758: "sceMpegIsRefBuffEmpty",
}


# SDK functions the analyzer stubs that must run as the game's own code. The libvu0 math is VU0 macro code: recompiled,
# it keeps the PS2's float rules (no Inf or NaN, overflow clamps), which the runtime's C++ versions lose (the 2D camera's
# sceVu0MulMatrix gave the x86 NaN, so every sprite landed at 0,0 and was clipped). Each one moves from stubs to
# entry_points. sceVpu0Reset touches the hardware: it stays a stub.
# The math library too: fabs, sin, cos, floor and atan here are the soft-float double versions (argument in $a0, result
# in $v0) while the runtime's C++ reads $f12 and writes $f0; and its rem_pio2f gives NaN for an angle of 0x7f800000,
# where the game's own gets 0 (x - x), so sin = 0 and cos = 1.
# (Not libsdr yet: sceSdRemoteInit, sceSdRemote and sceSdTransToIOP stay stubs, so the movie player's sound
# (sceSdTransToIOP, then BlockTrans to the SPU2) goes nowhere and the logos and films play silent. Recompiled, they
# bind Sony's sdrdrv (sid 0x80000701, run for real on the IOP) and the films have sound, but the EE later jumps into
# data (missing-target 0xbd9c90, v10300-11700) on the RP5 and the Odin: docs/registro.md, 2026-11-03 11:30.)
# (Since 2026-11-05 libsdr is recompiled by default. The jump into data was the dispatch taking a checkpoint below a call to
# the same target for a return, patch 0068, not libsdr: docs/registro.md, 2026-11-05 02:40.)
# rand (0x17C558): the game's own newlib rand, a 64-bit LCG (0x5851F42D4C957F2D) on its _reent (*(0x412DD8) + 0xA8, seeded by
# its srand at 0x17C540) that gives 31 bits. The runtime's stub gave the host's std::rand() & 0x7FFF and ignored the seed:
# the CPU's choices (BTL 0x6F0E60, 0x6F1260, 0x6F1720) and rand()/2^31 (0x192830) came out of another sequence and range
# (docs/registro.md, 2026-11-08).
UNSTUB = re.compile(r"_?sceVu0\w+|__ieee754_rem_pio2f|__kernel_cosf|__kernel_sinf|atan|cos|fabs|floor|sin|rand")
# HL_SDR=0 at build time leaves the three libsdr calls as stubs (no sound in the logos and films)
if os.environ.get("HL_SDR", "1") != "0":
    UNSTUB = re.compile(UNSTUB.pattern + r"|sceSdRemoteInit|sceSdRemote|sceSdTransToIOP")


def cmd_stubfix(a):
    text = open(a.config).read()
    moved = []
    block = re.search(r"^stubs = \[\s*\n(.*?)^\]", text, re.M | re.S)
    keep, own = [], []
    for line in block.group(1).splitlines(keepends=True):
        m = re.match(r'\s*"(\w+)@(0x[0-9A-Fa-f]+)"', line)
        if m and UNSTUB.fullmatch(m.group(1)):
            own.append('  "%s@%s",\n' % (m.group(1), m.group(2)))
        else:
            keep.append(line)
    if own:
        text = text[:block.start(1)] + "".join(keep) + text[block.end(1):]
        text = re.sub(r"^(entry_points = \[\s*\n)", lambda s: s.group(1) + "".join(own), text, count=1, flags=re.M)
        moved.append("%d functions recompiled" % len(own))
    for name, handler in STUB_ALIASES.items():
        m = re.search(r'^\s*"%s@(0x[0-9A-Fa-f]+)",?\s*\n' % re.escape(name), text, re.M)
        if not m:
            continue
        text = text[:m.start()] + text[m.end():]
        text = re.sub(r"^(stubs = \[\s*\n)", lambda s: s.group(1) + '  "%s@%s",\n' % (handler, m.group(1)), text, count=1, flags=re.M)
        moved.append("%s -> %s@%s" % (name, handler, m.group(1)))
    for addr, handler in STUB_AT.items():
        entry = "%s@0x%08X" % (handler, addr)
        if '"%s"' % entry in text:
            continue
        text = re.sub(r"^(stubs = \[\s*\n)", lambda s: s.group(1) + '  "%s",\n' % entry, text, count=1, flags=re.M)
        moved.append("0x%x -> %s" % (addr, handler))
    open(a.config, "w").write(text)
    print("stubs: " + (", ".join(moved) if moved else "nothing to change"))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("verify"); s.add_argument("iso"); s.set_defaults(fn=cmd_verify)
    s = sub.add_parser("extract"); s.add_argument("iso"); s.add_argument("dir"); s.set_defaults(fn=cmd_extract)
    s = sub.add_parser("lbn"); s.add_argument("iso"); s.add_argument("out"); s.set_defaults(fn=cmd_lbn)
    s = sub.add_parser("overlays"); s.add_argument("paths", nargs="+"); s.set_defaults(fn=cmd_overlays)
    s = sub.add_parser("diff"); s.add_argument("base"); s.add_argument("mod"); s.add_argument("-o"); s.set_defaults(fn=cmd_diff)
    s = sub.add_parser("unpack"); s.add_argument("elf"); s.add_argument("out"); s.set_defaults(fn=cmd_unpack)
    s = sub.add_parser("prepelf"); s.add_argument("elf"); s.add_argument("out"); s.set_defaults(fn=cmd_prepelf)
    s = sub.add_parser("ovlelf"); s.add_argument("ovl"); s.add_argument("out"); s.add_argument("--from", nargs="*", dest="callers"); s.set_defaults(fn=cmd_ovlelf)
    s = sub.add_parser("stubfix"); s.add_argument("config"); s.set_defaults(fn=cmd_stubfix)
    s = sub.add_parser("fnmap"); s.add_argument("elf"); s.add_argument("output"); s.add_argument("map"); s.add_argument("--also", nargs="*"); s.set_defaults(fn=cmd_fnmap)
    a = ap.parse_args()
    sys.exit(a.fn(a) or 0)


if __name__ == "__main__":
    main()
