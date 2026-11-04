# Dumps do VIF1 e do GIF (`HL_VIF_DUMP`)

Para a Fase 3 do Nominum (VIF1 + VU1 dentro do Nominum). Um par de arquivos por cena guarda:

- o fluxo VIF1 que o jogo manda;
- tudo o que o GIF recebe dos interpretadores do VIF1 e da VU1 do Hidden Leaf, o "interpretador atual": o `PS2Memory::processVIF1Data` e o `VU1Interpreter`, com o caminho rápido do patch `0043`.

Com isso, um VIF1 + VU1 novo pode ser comparado bit a bit, pacote a pacote, com o que o Hidden Leaf faz hoje.

Os dumps ficam em `~/Downloads/vifdumps/<aparelho>_<cena>.vif.zst` e `.gif.zst`, fora do git, porque são dados do jogo. O leitor e conferidor é `tools/vifdump.py info <base>`.

## Como gravar

- **Chave:** no `hl_env.txt`, `HL_VIF_DUMP=cena,cena,...`. As cenas e os gatilhos são os do `HL_GS_DUMP` (`hl_game.cpp`, `kDumpScenes`): `logos`, `filme`, `titulo`, `menu`, `selecao`, `luta_inicio`, `ougi_jogador`, `ougi_cpu`, `ko`, `winner`, `resultado`, `campanha_vila`, `campanha_cortina`. `cena@vsync` muda o vsync.
- **Duração e nome:** `HL_VIF_DUMP_FRAMES=N` dá o número de vsyncs (300 por padrão). `HL_GS_DUMP_DEVICE` dá o nome do aparelho.
- **Onde fica:** a saída vai para `<pasta do jogo>/vifdumps/`, sem compressão.
- **Rotas:** `build/lab/vifdrun.sh SERIAL APARELHO ENV ENDV`. Ele puxa cada dump quando termina, apaga-o do aparelho e comprime no PC com `zstd -10`. As envs são `hl_env_vif_v3.txt` (rota v3, 11 cenas), `hl_env_vif_cpu2.txt` (`ougi_cpu`) e `hl_env_vif_camp.txt` (as duas da campanha).
- **Código:** `recomp/patches/0058-vif1-gif-dump.patch` (`runtime/hl_vif_dump.{h,cpp}` e os ganchos).
  - O VIF1 é gravado no `processVIF1Data`, o GIF no `submitGifPacket`.
  - Os registradores privilegiados e o vsync vêm do `hl_gs_live.cpp`.
  - A foto inicial é tirada na thread do VIF1/GIF (o `VuWorker` no Android), entre dois comandos dela.
  - Desligado, cada gancho custa uma leitura atômica.

## Formato (little endian)

Cada arquivo tem um cabeçalho de 96 bytes e depois os registros.

**Cabeçalho**

| Campo | Tamanho | Valor |
|---|---|---|
| mágica | 8 | `HLVIF1\0\0` no `.vif`, `HLGIF1\0\0` no `.gif` |
| versão | u32 | 1 |
| tamanho do cabeçalho | u32 | 96 |
| cena | 32 | ASCII com zeros |
| aparelho | 32 | ASCII com zeros |
| vsync da rota no começo | u64 | |
| vsyncs pedidos | u32 | |
| reservado | u32 | 0 |

**Registro:** `u32 tipo`, `u32 tamanho` (do que vem depois dos 16 bytes), `u64 sequência` e a carga. A sequência é **um contador só para os dois arquivos**: intercalados por ela, os dois dão a ordem exata em que o interpretador viu e fez cada coisa. O registro de fim (tipo `0xFF`, carga vazia) tem o mesmo número nos dois.

### `.vif`

| Tipo | Carga |
|---|---|
| `0x54415453` (`STAT`), um só, o primeiro | o estado no começo, ver abaixo |
| `1`, transferência VIF1 | os bytes inteiros de uma chamada a `processVIF1Data`: VIFcodes e dados, como o DMA do canal 1 entregou |
| `2`, vsync | `u64` o número do vsync neste dump (de 1); `u32` CRC32 da micro memória da VU1, `u32` CRC32 da memória de dados, `u32` CRC32 dos registradores do VIF1, todos depois do vsync; `u32` 0 |
| `0xFF` | fim |

**`STAT`:**
1. `u32` tamanho do código (16384) e `u32` tamanho dos dados (16384).
2. A micro memória da VU1, depois a memória de dados.
3. Os registradores do VIF1, 23 `u32`: `stat fbrst err mark cycle mode num mask code itops base ofst tops itop top row[4] col[4]`.
4. O que um DIRECT deixou devendo, 4 `u32`: os QW de DIRECT que continuam na próxima transferência, os QW de IMAGE que o GIF ainda espera, o DIRECTHL desse IMAGE e o PATH3 mascarado.
5. A VU1: `vf[32][4]` (f32), `vi[16]` (i32), `acc[4]` (f32), `q p i` (f32), `r pc mac clip status top itop` (u32), e 4 bytes: `ebit`, `dBitEnabled`, `tBitEnabled`, 0.

Os CRCs por vsync servem para conferir. Se a micro memória ou os dados mudarem sem que o VIF1 do dump explique (uma escrita do EE direto na memória da VU1, por exemplo), o CRC do vsync não bate com o que a sua VU1 tem.

### `.gif`

| Tipo | Carga |
|---|---|
| `1`, pacote GIF | 16 bytes, depois o pacote como o `submitGifPacket` recebeu. Os 16 bytes: `u8` caminho (1 = PATH1 do XGKICK, 2 = PATH2 do DIRECT/DIRECTHL, 3 = PATH3 do canal GIF), `u8` DIRECTHL, `u8` 1 se saiu de dentro de uma transferência VIF1, `u8` 0, `u32` 0, `u64` a sequência dessa transferência VIF1 (0 se fora) |
| `3`, registrador privilegiado | `u32` o registrador (o índice que o `hl_gs_live` usa), `u32` 0, `u64` o valor |
| `2`, vsync | `u64` o número do vsync, depois os registradores privilegiados que o vsync mostrou (o bloco que o `hl_gs_live` manda ao GS) |
| `0xFF` | fim |

Detalhes:
- Um IMAGE que o DIRECT deixou devendo sai como um pacote PATH2 com uma GIFtag IMAGE feita pelo interpretador, como hoje.
- O PATH3 está no `.gif` para a ordem e para refazer as imagens pelo backend. Para o teste da VU1, valem só os caminhos 1 e 2 de dentro das transferências.
- Um PATH1 "fora" de uma transferência seria uma VU1 que o EE ligou direto: o log conta esses casos.

## Limites conhecidos

- **Escritas fora do VIF1:** o EE escrevendo direto na memória da VU1, ou num registrador do VIF1 pelo endereço dele, não é gravado. Os CRCs por vsync mostram se houve.
- **Começo do dump:** a foto é tirada entre dois comandos da thread. Uma VU1 parada no meio (MSCNT) leva o `pc`. O estado interno do pipeline do interpretador (`m_xgkick` pela metade, as latências) não vai.
- **Custo:** cru, ocupa ~0,8 MB por vsync nos logos (filme) e mais na luta. O jogo fica mais lento enquanto grava, mas o conteúdo não muda, porque a rota é por vsync.
