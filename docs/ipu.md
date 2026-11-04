# IPU e filmes: medidas e proposta (item 16)

Data: 2026-11-04. Só medidas e proposta. Nenhum código foi mudado para isto.

## Como os filmes rodam hoje

- **O caminho do jogo:** o jogo toca os `PSS/` (`LOGO_C.PSS`, `NARUTO10.PSS`, `OPENING.PSS`) pela `libmpeg` da Sony
  (`sceMpegDemuxPssRing`, `sceMpegGetPicture`).
- **O runtime não emula a IPU:**
  - `Kernel/Stubs/IPU.cpp` só completa o `sceIpuInit` (escreve as tabelas IQ/VQ no FIFO e devolve 0).
  - As chamadas da `libmpeg` são tratadas em alto nível em `Kernel/Stubs/MPEG.cpp`. O MPEG-2 é decodificado pelo FFmpeg
    (`AV_CODEC_ID_MPEG2VIDEO`, `thread_count = 1`), na própria GameThread, dentro do `sceMpegGetPicture`.
  - O YUV vira RGBA com o `sws_scale` (`SWS_BILINEAR`), e o resultado é copiado para a RAM do jogo, onde ele mesmo o
    manda ao GS como textura.
- **No Android**, o FFmpeg vem estático (`recomp/android/CMakeLists.txt`, `FFMPEG`, `PS2X_HAS_FFMPEG=1`).

## Medidas

**Janela `filme` (v2000–2950, `NARUTO10.PSS` e `OPENING.PSS`), rodada 3, combinação c (GS do ARMSX2), `perfwin.py`:**

| aparelho | fps (pior) | EE ms/quadro | VU1 | GS | ocioso |
|---|---|---|---|---|---|
| RP5 (`sd480p`) | 50,0 (48,6) | 4,8 | 1,2 | 3,7 | 12,7 |
| Odin 3 | 50,0 (49,9) | 1,5 | 0,7 | 2,3 | 17,4 |
| Cube | 50,0 (48,9) | 3,4 | 1,2 | 6,3 | 14,9 |

**Perfil, Cube:**

- **Gravação:** `simpleperf` de 15 s a partir de v2255, em `build/lab/rec/prof_cube_film`, com o APK `0e8e877`. As
  porcentagens são de todas as amostras do processo. A GameThread tem 35,8%.

| o quê | % |
|---|---|
| decodificação no FFmpeg (`ff_simple_idct_put_int16_8bit`, `ff_mpv_*`, …) | 2,4 |
| `sceMpegGetPicture` (a cópia do quadro para a RAM do jogo, fora o FFmpeg) | 1,8 |
| `yuv2rgb_c_32` (YUV para RGBA, o **caminho em C** do swscale) | 0,8 |
| demux, fila de callbacks, `feedElementaryStream` | 0,2 |
| **total do filme** | **~5,2** (~15% da GameThread) |

**Leitura:**

- Os filmes não são gargalo em nenhum dos três aparelhos: 50 fps com 12–17 ms ociosos por quadro.
- Na GameThread, o maior custo é o mesmo do resto do jogo: o interpretador do IOP (4,1% do processo) e o `spu2Mix`
  (4,0%).
- O `yuv2rgb_c_32` mostra que o swscale do Android usa a conversão em C, não a de NEON.

## Proposta (por ordem, nenhuma é necessária para os 50 fps)

1. **YUV para RGBA em NEON.** Pode ser o swscale com o NEON do aarch64 ligado na build do FFmpeg, se a versão usada
   tiver esse caminho, ou uma conversão própria em NEON (4:2:0 para RGBA, sem escala, porque a origem e o destino têm o
   mesmo tamanho). Ganho esperado: a maior parte do 0,8% e uma parte da cópia. Risco baixo, e dá para comparar quadro a
   quadro com a conversão de hoje.
2. **Decodificar fora da GameThread.** O FFmpeg com `thread_count > 1` (fatias), ou uma thread própria que decodifica o
   quadro seguinte enquanto o jogo usa o atual. O `sceMpegGetPicture` passaria a só copiar. Ganho: ~2,4% do processo
   sai da GameThread. Risco médio: o jogo acorda a thread de vídeo antes de cada demux (`MPEG.cpp`, comentário perto da
   linha 1413) e depende dessa ordem. Mudar isso pede a rota dos filmes com som e imagem conferidos nos 3 aparelhos.
3. **Escrever o RGBA direto onde o jogo lê**, sem o vetor intermediário (`MpegDecodedFrame::rgba`) e a cópia. Ganho:
   parte do 1,8% do `sceMpegGetPicture`.
4. **Emular a IPU de verdade:** não. O jogo só usa a IPU pela `libmpeg`, e o HLE com FFmpeg já cobre os três filmes.
   Uma IPU de baixo nível (VDEC, IDEC, CSC) custaria mais CPU e mais código, sem nada a ganhar neste jogo.

O que mudaria a ordem: um mod que traga filmes em resolução maior, ou um aparelho mais lento que o Cube na janela dos
filmes. Nenhum dos dois foi visto.
