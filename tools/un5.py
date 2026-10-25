#!/usr/bin/env python3
"""
Hidden Leaf: tools for the recompilation of Naruto Shippuden: Ultimate Ninja 5 (PS2, SLES-55605).

Nothing of the game is kept in the repository: these tools read the player's own ISO and write what they make
outside the tree (or into build/, which git ignores).

  un5.py verify   <iso>                  which release or mod an ISO is (by SHA-256)
  un5.py extract  <iso> <dir>            copies the boot files (SYSTEM.CNF, the ELF, PRG/, MODULES/, small DATA/ .BIN)
  un5.py overlays <dir-or-files...>      the CodeWarrior overlays (MWo3) of PRG/: id, load address, sizes
  un5.py diff     <base-iso> <mod-iso> [-o pack.json]
                                         function by function, what a mod changes in the code (ELF and overlays):
                                         the list of functions its code pack has to carry
  un5.py unpack   <elf> <out>            unpacks an ELF packed with SjCRUNCH (the Brazilian version's)
  un5.py prepelf  <elf> <out>            the ELF for the recompiler: its one big section split into code and data
  un5.py ovlelf   <overlay.BIN> <out>    an overlay (MWo3) as an ELF at its load address, code and data apart
  un5.py fnmap    <elf> <recomp output> <map.csv>
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
    """A CodeWarrior overlay: header of 0x40 bytes, then text and data. Returns a dict, or None."""
    if data[:4] != b"MWo3":
        return None
    oid, addr, text, dat, bss, end, bss_end = struct.unpack_from("<7I", data, 4)
    return dict(id=oid, addr=addr, text=text, data=dat, bss=bss, end=end, bss_end=bss_end, body=data[0x40:])


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
                out[p] = (o["addr"], o["body"])
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
        print(f"base: Naruto Shippuden: Ultimate Ninja 5, European release ({BASE_SERIAL}), unmodified")
        return 0
    if sha in KNOWN_MODS:
        print(f"mod: {KNOWN_MODS[sha][1]} ({KNOWN_MODS[sha][0]}) - import the European ISO as the base, this one as a mod")
        return 2
    print("unknown program: another release, a damaged image, or a mod this tool does not know")
    return 3


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
        fn = len(functions(body[:o["text"] + o["data"]], o["addr"]))
        print(f"{os.path.basename(p):14} id {o['id']}  load {o['addr']:08x}  text {o['text']:06x}  data {o['data']:06x}"
              f"  bss {o['bss']:05x}  end {o['end']:08x}  ~{fn} functions")


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


def cmd_ovlelf(a):
    raw = open(a.ovl, "rb").read()
    o = mwo3(raw)
    if not o:
        raise SystemExit("not an MWo3 overlay")
    body = raw[0x40:0x40 + o["text"] + o["data"]]
    # ELF header + one program header, then the body
    hdr = bytearray(52 + 32)
    hdr[0:16] = b"\x7fELF\x01\x01\x01" + b"\0" * 9
    struct.pack_into("<HHIIIIIHHHHHH", hdr, 16, 2, 8, 1, o["addr"], 52, 0, 0x20924001, 52, 32, 1, 40, 0, 0)
    body_off = 0x80
    struct.pack_into("<IIIIIIII", hdr, 52, 1, body_off, o["addr"], o["addr"], len(body), len(body) + o["bss"], 7, 0x10)
    data = bytes(hdr) + b"\0" * (body_off - len(hdr)) + body
    secs = [(".text", 1, SHF_ALLOC | SHF_EXEC, o["addr"], body_off, o["text"])]
    if o["data"]:
        secs.append((".data", 1, SHF_ALLOC | SHF_WRITE, o["addr"] + o["text"], body_off + o["text"], o["data"]))
    if o["bss"]:
        secs.append((".bss", 8, SHF_ALLOC | SHF_WRITE, o["addr"] + o["text"] + o["data"], body_off + len(body), o["bss"]))
    with open(a.out, "wb") as f:
        f.write(write_sections(data, secs))
    print("overlay id %d at %08x: code %#x, data %#x, bss %#x -> %s" % (o["id"], o["addr"], o["text"], o["data"], o["bss"], a.out))


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
        return word(p) == 0x03E00008 or word(p - 4) == 0x03E00008

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


def cmd_stubfix(a):
    text = open(a.config).read()
    moved = []
    for name, handler in STUB_ALIASES.items():
        m = re.search(r'^\s*"%s@(0x[0-9A-Fa-f]+)",?\s*\n' % re.escape(name), text, re.M)
        if not m:
            continue
        text = text[:m.start()] + text[m.end():]
        text = re.sub(r"^(stubs = \[\s*\n)", lambda s: s.group(1) + '  "%s@%s",\n' % (handler, m.group(1)), text, count=1, flags=re.M)
        moved.append("%s -> %s@%s" % (name, handler, m.group(1)))
    open(a.config, "w").write(text)
    print("stubs: " + (", ".join(moved) if moved else "nothing to change"))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("verify"); s.add_argument("iso"); s.set_defaults(fn=cmd_verify)
    s = sub.add_parser("extract"); s.add_argument("iso"); s.add_argument("dir"); s.set_defaults(fn=cmd_extract)
    s = sub.add_parser("overlays"); s.add_argument("paths", nargs="+"); s.set_defaults(fn=cmd_overlays)
    s = sub.add_parser("diff"); s.add_argument("base"); s.add_argument("mod"); s.add_argument("-o"); s.set_defaults(fn=cmd_diff)
    s = sub.add_parser("unpack"); s.add_argument("elf"); s.add_argument("out"); s.set_defaults(fn=cmd_unpack)
    s = sub.add_parser("prepelf"); s.add_argument("elf"); s.add_argument("out"); s.set_defaults(fn=cmd_prepelf)
    s = sub.add_parser("ovlelf"); s.add_argument("ovl"); s.add_argument("out"); s.set_defaults(fn=cmd_ovlelf)
    s = sub.add_parser("stubfix"); s.add_argument("config"); s.set_defaults(fn=cmd_stubfix)
    s = sub.add_parser("fnmap"); s.add_argument("elf"); s.add_argument("output"); s.add_argument("map"); s.set_defaults(fn=cmd_fnmap)
    a = ap.parse_args()
    sys.exit(a.fn(a) or 0)


if __name__ == "__main__":
    main()
