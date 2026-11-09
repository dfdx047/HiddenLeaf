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
| `vu1cmp.py <a_vu1.cap> <b_vu1.cap> [n]` | os inícios da VU1 (`HLB_VU1CAP=F,N`) em ordem: pc, e o que difere nos 16 KB de dados, VF e VI |
| `vu1trace.py <a_vu1.cap> <a0> <b_vu1.cap> <b0> [n]` | o trace por instrução (`HLB_VU1TRACE=K0,K1`, pc, VI e VF antes de cada par) dos inícios `a0..` e `b0..`, e o primeiro passo que difere |

**Captura da VU1** (`<prefix>_vu1.cap`, os dois lados no mesmo formato, `{tipo, tamanho}` + dados):
- `0xA1`: um início (MSCAL/MSCNT): pc, os 16 KB de dados, VF (512 B) e VI (16 × u32);
- `0xA3`: um passo do trace: pc, VI (16 × u16), VF;
- `0xA4`: o código (16 KB), uma vez no `K0`; `0xA6`: o FNV-1a do código a cada início; `0xA7`: o código quando o FNV mudou;
- `0xA5`: eventos de 4 palavras. Com a 4ª = 3, um MPG da VIF1 (`0xC0000000`, endereço, palavras); no PCSX2, também a
  entrada do `vu1ExecMicro` (endereço, VPU_STAT, TPC, 0 = antes / 1 = depois do `vu1Finish`), o fim de cada passo
  (`0xA…`: ebit, branch) e o `vu1ResetRegs` (`0xB0000000`);
- `{0xFF, vsync}`: um vsync.

O trace do PCSX2 só sai com a VU1 no interpretador: `[EmuCore/CPU/Recompiler] EnableVU1 = false` no `PCSX2.ini` da
bancada, só nessa execução. A janela de inícios casa pelo índice do vsync comparável: no quadro 199 da `route_v3`, o
PCSX2 grava 1508 inícios no vsync anterior, e o recomp nenhum.
