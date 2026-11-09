# sheet.py <pcsx2 prefix> <recomp txt> <hl_frames dir> <out.png> [fight|overlay]: PCSX2 (left) x recomp (right) per shot
# (HLB_SHOT by fight frame, or HLB_OSHOT by vsyncs after an overlay appears)
import sys, os, re, glob
from PIL import Image
pp, rtxt, fr, out = sys.argv[1:5]; fight = int(sys.argv[5]) if len(sys.argv) > 5 else 1
txt = open(rtxt).read()
oshot = re.findall(r"v(\d+) oshot (\d+)", txt)  # HLB_OSHOT: [fight] is the overlay
if oshot:
    shots = [(int(v), int(n)) for v, n in oshot]
else:
    shots = [(int(m.group(1)), int(m.group(3))) for m in re.finditer(r"v(\d+) shot fight (\d+) frame (\d+)", txt) if int(m.group(2)) == fight]
files = sorted((int(re.search(r"frame_(\d+)\.ppm", f).group(1)), f) for f in glob.glob(fr + "/frame_*.ppm") if re.search(r"frame_\d+\.ppm$", f))
rows = []
for v, n in shots:
    p = ("%s_o%u_%03u.png" % (pp, fight, n)) if oshot else ("%s_%02d_%05d.png" % (pp, fight, n))
    r = next((f for fv, f in files if fv >= v), None)
    if os.path.exists(p) and r:
        rows.append((n, Image.open(p).convert("RGB"), Image.open(r).convert("RGB")))
W, H = 320, 224
cols = 2
sheet = Image.new("RGB", (cols * 2 * W + 8, ((len(rows) + cols - 1) // cols) * H), "black")
for i, (n, a, b) in enumerate(rows):
    x = (i % cols) * (2 * W + 8); y = (i // cols) * H
    sheet.paste(a.resize((W, H)), (x, y)); sheet.paste(b.resize((W, H)), (x + W, y))
sheet.save(out); print(len(rows), "pairs ->", out)
