#!/usr/bin/env python3
"""
Hidden Leaf: the recompiled overlays as modules of the runtime.

PS2Recomp recompiles one ELF into functions named by address (sub_006C6D00_0x6c6d00) and one dense table. The
overlays of PRG/ load at the same addresses (BTL, ADV and ETC all at 0x6c6d00), so each overlay's output is:
  - renamed: every function gets the overlay's prefix (btl_sub_006C6D00_0x6c6d00), in its own header;
  - given its own table (un5_<name>_table) instead of the global one;
  - described in un5_overlay_list.inc: load address, code size, table, and the signature the runtime checks against
    guest memory to know which overlay is resident (bytes of the overlay's code, taken from the player's file here,
    at build time: nothing of the game is in the repository).

  overlays.py <name> <overlay.BIN> <ps2recomp output dir> <destination dir>   (one overlay)
  overlays.py --list <destination dir> <name>...                               (the list, after all of them)
"""
import os
import re
import struct
import sys

SIG_WINDOWS = 3
SIG_LEN = 32


def mwo3(raw):
    if raw[:4] != b"MWo3":
        raise SystemExit("not an MWo3 overlay")
    oid, addr, text, data, bss, end, bss_end = struct.unpack_from("<7I", raw, 4)
    return dict(id=oid, addr=addr, text=text, data=data, body=raw[0x40:])


def signature(o):
    """Windows of the code that tell overlays at the same address apart: near the start, the middle, the end."""
    text = o["text"]
    offs = sorted({min(max(0, off), max(0, text - SIG_LEN)) & ~3 for off in (0x100, text // 2, text - 0x40)})
    return [(off, o["body"][off:off + SIG_LEN]) for off in offs if off + SIG_LEN <= text]


def one(name, binpath, outdir, dest):
    o = mwo3(open(binpath, "rb").read())
    os.makedirs(dest, exist_ok=True)
    sym2 = re.compile(r"\b((?:sub|entry)_[0-9A-Fa-f]+_[0-9A-Fa-fx_]+)\b")
    header = "un5_%s_functions.h" % name
    count = 0
    for f in sorted(os.listdir(outdir)):
        if not f.endswith(".cpp") or f == "register_functions.cpp":
            continue
        src = open(os.path.join(outdir, f)).read()
        src = src.replace("#include <ps2_recompiled_functions.h>", '#include "%s"' % header)
        src = sym2.sub(lambda m: "%s_%s" % (name, m.group(1)), src)
        with open(os.path.join(dest, "%s_%s" % (name, f)), "w") as w:
            w.write(src)
        count += 1
    h = open(os.path.join(outdir, "ps2_recompiled_functions.h")).read()
    h = sym2.sub(lambda m: "%s_%s" % (name, m.group(1)), h)
    h = h.replace("PS2_RECOMPILED_FUNCTIONS_H", "UN5_%s_FUNCTIONS_H" % name.upper())
    with open(os.path.join(dest, header), "w") as w:
        w.write(h)
    # the table: the slots the generated registration fills, for this overlay alone
    reg = open(os.path.join(outdir, "register_functions.cpp")).read()
    base = int(re.search(r"g_ps2RecompiledFunctionTableBase = (0x[0-9a-fA-F]+)u", reg).group(1), 16)
    slots = int(re.search(r"g_ps2RecompiledFunctionTableSlotCount = (\d+)u", reg).group(1))
    entries = re.findall(r"g_ps2RecompiledFunctionTable\[(\d+)\] = (\w+);", reg)
    with open(os.path.join(dest, "un5_%s_table.cpp" % name), "w") as w:
        w.write('#include "ps2_runtime.h"\n#include "%s"\n\n' % header)
        w.write("PS2Runtime::RecompiledFunction un5_%s_table[%du] = {};\n" % (name, slots))
        w.write("extern const uint32_t un5_%s_base = 0x%xu;\nextern const uint32_t un5_%s_slots = %du;\n\n" % (name, base, name, slots))
        w.write("namespace {\nstruct Init_%s {\n    Init_%s() {\n" % (name, name))
        for slot, fn in entries:
            w.write("        un5_%s_table[%s] = %s_%s;\n" % (name, slot, name, fn))
        w.write("    }\n} init_%s;\n}\n" % name)
    sig = signature(o)
    with open(os.path.join(dest, "un5_%s.sig" % name), "w") as w:
        w.write("%d %x %x %x\n" % (o["id"], o["addr"], o["text"], base))
        for off, b in sig:
            w.write("%x %s\n" % (off, b.hex()))
    print("%s: %d functions, %d table entries, base %08x, %d slots" % (name, count, len(entries), base, slots))


def write_list(dest, names):
    with open(os.path.join(dest, "un5_overlay_list.inc"), "w") as w:
        for n in names:
            w.write("extern PS2Runtime::RecompiledFunction un5_%s_table[];\n" % n)
            w.write("extern const uint32_t un5_%s_base;\nextern const uint32_t un5_%s_slots;\n" % (n, n))
        w.write("\nstatic const Un5Overlay kOverlays[] = {\n")
        for n in names:
            lines = open(os.path.join(dest, "un5_%s.sig" % n)).read().split("\n")
            oid, addr, text, _base = lines[0].split()
            sigs = [l.split() for l in lines[1:] if l.strip()]
            w.write('    {"%s", %s, 0x%su, 0x%su, un5_%s_table, &un5_%s_base, &un5_%s_slots, {\n' % (n, oid, addr, text, n, n, n))
            for off, hx in sigs:
                b = bytes.fromhex(hx)
                w.write("        {0x%su, {%s}},\n" % (off, ", ".join("0x%02x" % x for x in b)))
            w.write("    }, %d},\n" % len(sigs))
        w.write("};\n")
    print("list: %s" % ", ".join(names))


if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "--list":
        write_list(sys.argv[2], sys.argv[3:])
    elif len(sys.argv) == 5:
        one(*sys.argv[1:5])
    else:
        print(__doc__)
        sys.exit(1)
