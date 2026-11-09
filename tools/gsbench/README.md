# gsbench: o GS do recomp contra o do PCSX2 (bancada)

Leitores das capturas da bancada (`docs/registro.md`, 2026-11-08 e 2026-11-09). Capturas: `HLB_GIFCAP`/`HLB_GIFCAPV` no
recomp (formato `HL_GSCAP`: `{path, tamanho}` + pacote, `{0xFF, vsync}` por vsync) e `HLB_GSDUMP`/`HLB_GSDUMPV` no PCSX2
da bancada (dump do GS, `.gs.zst`).

| Script | Uso |
|---|---|
| `dump2cap.py <dump.gs.zst> <out.cap>` | dump do PCSX2 → formato `HL_GSCAP`, e `<out.cap>.state` (a VRAM começa no byte 425) |
| `capcmp.py <a.cap> <b.cap> [tbp...]` | grupos de desenho por estado (FRAME, TEX0, PRIM, ALPHA, TEST, com `CTXT` e `PRMODE`) |
| `xfers.py <cap> [n] [all]` | transferências host→local (BITBLTBUF, TRXPOS, TRXREG) por quadro |
| `imgs.py <a.cap> <b.cap>` | cada transferência com o md5 dos dados, o que um lado tem e o outro não (estado do GIF por path) |
| `vramcmp.py <x.cap.state> <vram_N.bin>` | VRAM por página: 24 bits baixos e 8 altos |
| `frames.py`, `verts.py` | desenhos por FRAME/FBMSK; vértices de um TBP |
| `sheet.py <pcsx2 prefix> <recomp txt> <hl_frames> <out.png>` | capturas lado a lado (PCSX2 à esquerda) nos quadros do `HLB_SHOT` |
