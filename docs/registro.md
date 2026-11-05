# Registro

O que já foi feito, o que funciona, o que está quebrado e onde cada coisa foi mexida. **Ler inteiro antes de começar;
atualizar e commitar junto com o código ao terminar cada item.** Entradas antigas não se apagam: uma correção é uma
entrada nova. Detalhe técnico e medidas longas ficam em `docs/plano.md`; aqui fica o estado e o histórico.

## Estado das funções

Estados: **funciona**, **parcial**, **quebrado**, **não testado**. "Rota" = rota completa gravada (método no fim deste
arquivo); "rota fixa" = a rota antiga de medida (`build/lab/hl_env.txt`, Naruto x Naruto no lamen), que só chega à luta.

| Função | Estado | Último build verificado | Aparelhos | Como foi verificado | Observações |
|---|---|---|---|---|---|
| Boot e logos (imagem) | funciona | `2fee126` (libun5 de 11-03 03:31) | RP5, Cube | rota fixa, prints | |
| Som dos logos e da abertura (filme) | em andamento | `app_sdr9.apk` (lab) | Cube, RP5 | `HL_MPEG_LOG`, `HL_RPC_LOG`, `[snd]` | com `HL_SDR=1`, som do logo ao fim; o travamento de ~v10000 era o `POKE` da rota (2026-11-04 18:40). Falta: `missing-target` no RP5 em v16860 (3ª luta), filmes escuros na gravação, Odin e 20:9 |
| Título e menus | funciona | `2fee126` | RP5, Cube | rota fixa, prints, `[snd]` | som em título, Mode Select, Options, Shop, Collection, seleções, VS |
| Luta | parcial | build de 11-03 12:00 | RP5, Cube, Odin 3 | rota v3 gravada, `ana.py` | 50 fps; piscadas: só falsos positivos (golpes); Ougi do GS com SCANMSK sai escuro e listrado; quadro preto com HUD no Cube depois do Ougi "Linked" da CPU |
| Tela de vitória / resultado | parcial | `f00dd5b` | RP5, Cube, Odin 3 (16:9 e 20:9) | rota v3 gravada | WINNER, resultado e menu nas duas lutas; fantasma da luta 2 corrigido (SCANMSK, `3d79104`). Falta a rota completa (campanha, pausa, sair) e os mods. Vitória da CPU vai direto à seleção (não conferido no PCSX2) |
| Campanha (Master Mode) | parcial | `a6c40f5` | RP5, Cube, Odin 3 (20:9) | rota de campanha gravada (`*_camp*`) | anda, troca de área e menu START: RP5 50 fps, Odin 50, Cube 40–55 (VU1). Em 20:9, a cortina de transição não cobre a largura (~1 s por troca) |
| Widescreen na luta | parcial | `2fee126` | RP5 | gravação de 11-03: 808 → 0 quadros com barra | câmera larga presa à entrada do lutador nos últimos 6 vsyncs (não ao estado da cena). Relato de regressão |
| Widescreen na campanha | não testado | | | | câmera larga ligada pelo `ADV` |
| Widescreen nos menus | parcial | `2fee126` | RP5 | prints | esticados; "Esticar" e "Desenhar em 16:9" não existem |
| Widescreen em 20:9 | parcial | `a6c40f5` | Odin 3 (2400x1080) | rotas v3 e de campanha gravadas, `ana.py` | luta: câmera `x 0.600`, 0 barras; campanha: a cortina de transição não cobre a largura (~1 s por troca); menus esticados |
| Controles por cena | funciona | `9c6c786` | RP5 | prints (11-02) | cena pelo overlay e estado (`TouchBridge.nativeScene`) |
| Especial com um botão (Ougi) | parcial | `a3dbcfc` | RP5, Odin 3, Cube | rota v3 gravada, log `[sp]`, 1x e 2x no Cube (11-04 15:25) | o jogo mostra o pedido e o toque responde; "ERROR" era da COM; 2x dobra as respostas; △△ à mão e disputa não vistos |
| Painel de pausa | funciona | `9c6c786` | RP5 | prints (11-02) | não reconferido depois de 11-02 |
| Contador de fps | funciona | `2225a89` | RP5 | prints (11-02) | |
| 60/120 interpolados | não testado | | | | 120 só aparece em tela ≥ 100 Hz (`34467f3`); Odin 3 travado à noite |
| Resolução 1x–4x | não testado | | | | |
| Save manager (.ps2 in/out) | funciona | `1e8d294` | RP5 | teste de unidade `Ps2CardTest`, import/export à mão | |
| Desbloquear tudo | não testado | `44cf73e` | | | opção do launcher (`0050` poke) |
| Mods de dados | parcial | `264b764` | Cube | A35 com `HL_MODS` | só testado junto com o A35 |
| UN6 A35 (pacote de código) | parcial | `43b39fa` (libun5_un6_a35 de 11-03 05:27) | Cube | rota até a luta | precisa de save copiado à mão em `BASLUS-55606NARUTO6`; resultado não conferido |
| UN6 Alpha 1.1X | parcial | (este commit; `libun5_a11x.so` de 11-04 15:59) | Cube | rota v3 gravada (11-04 16:10) | só dados mudam: a base roda o programa da mod; luta, Ougi, WINNER e resultado vistos; faltam RP5, Odin e 20:9 |
| Packs de textura | quebrado | | | | não implementado no motor |
| Opções do launcher | parcial | `34467f3` | RP5 | à mão | as sem efeito foram escondidas (`cc6696d`) |
| Backend próprio do Nominum (`NOMINUM_BACKEND=nominum`, teste) | quebrado | Nominum `12fb79b` (APK de teste de 11-04 05:45, não instalado agora) | Odin 3 (16:9 e 20:9), Cube; RP5 só com o Turnip | rotas v3 e de campanha gravadas a 1x, 2x, 4x e 6x, `HL_PERF`, memória da GPU, giro e 30 fps pelo painel | RP5 com o driver do sistema: `DEVICE_LOST` no mesmo ponto (rastro anotado); com o Turnip roda. Pré-rotação achata a imagem a 90/270 (certa com `NOMINUM_VK_PRETRANSFORM=0`). Cube roda sem `dualSrcBlend`, mas Ougi a 9,5 fps. Odin: Ougi 30,4 (era 21,5). Detalhe em 11-04 07:50 |
| VIF1 e VU1 do Nominum (`NOMINUM_VU1=1`, teste, desligado por padrão) | parcial | `0e8e877` + Nominum `b2ff28b` (APK de 11-04 14:02) | Odin 3 (16:9 e 20:9), RP5 (sd480p, Qualcomm e Turnip), Cube | rotas v3 e de campanha `rec/n3_*_b`, `HL_PERF`, capturas | VU1 25–30% mais barata; RP5 sd480p a 50 na campanha; no Cube a campanha cai (GS do Nominum sobe), ver 11-04 15:00 |
| Dumps do GS por cena (`HL_GS_DUMP`) | funciona | APK de 11-03 21:26 | RP5, Cube, Odin 3 (16:9 e 20:9) | 52 dumps, `tools/gsdump.py info` e capturas do cabeçalho | ferramenta de teste; o jogo cai a 4–9 fps enquanto grava (zstd na thread de replay). Não abertos no PCSX2 ainda |
| Dumps do VIF1 e do GIF por cena (`HL_VIF_DUMP`) | funciona | `libun5` de 11-04 07:47 (patch `0058`) | Odin 3 (16:9) | 13 pares em `~/Downloads/vifdumps/`, `tools/vifdump.py info` sem erro | ferramenta de teste, para a Fase 3 do Nominum; formato em `docs/vifdump.md`. Só no 16:9 (a câmera larga muda o que vai à VU1) |

## Histórico (mais novo primeiro)

### 2026-11-05 02:25, noite, itens 2a, 2d e 4: o contexto da thread 8 volta a um estado velho; limitador no RP5; rodada 4

**Item 2a:**
- `app_sdr13.apk` (anel de eventos): a `n16_rp5_ring` também falhou. No RP5 em 16:9, a `sdr13` falhou 2 de 2, e a `n17`
  foi parada. No Odin em 20:9 com a `sdr13`, `n15_odin209_v3` e `n16_odin209_v3` foram até o fim (2 de 2).
- **Patch novo, `0065-ee-event-ring-merge.patch`:**
  - os eventos do escalonador também entram no anel de despacho, na ordem real (`0xee00<tag><tipo>`);
  - os dois anéis passam a ter 64 entradas;
  - o anel é impresso 8 por linha, porque o Android cortava a linha longa e o fim se perdia (foi o que aconteceu na `n19`).
- Rodadas no RP5 em 16:9:
  - `app_sdr14.apk`: `n18` passou, `n19` falhou;
  - `app_sdr15.apk`: `n20` passou, `n21` falhou, `n22` passou.
  - Somando a noite com a libsdr e sem a vigia: 4 falhas em 7.
- **A ordem real na `n21`:**
  1. O último despacho é `0x3167c0` (`sp 0xb6fe40`).
  2. Na execução nativa seguinte, o laço da `0x35b310` passa por dois objetos. A `0x3563c0` do primeiro chama a
     `0x1bf9a0` e a `0x1bfeb0`. A do segundo não chama nada.
  3. A interrupção é empilhada (`P0`) com o contexto base já em `{pc 0x1bfeb0, ra 0x31b658, sp 0xb6fb80}`.
  4. Depois dela (`O0`), o despacho roda a `0x1bfeb0` e a `0x31b658`, e a `0x31b4e0` tira da pilha o `ra 0xe3b4c0`.
- **O fato novo:** o rastro de buscas da GameThread não tem a `0x31b4e0` nesta execução, nem quem a chama. Ele é
  `thread_local` e registra tudo o que passa pelo `lookupFunction`. Mesmo assim, o contexto da thread 8 termina no
  estado da `0x31b4e0` parada no próprio `jal 0x1bfeb0`. Ou esse estado é velho e voltou ao contexto, ou foi escrito
  por código que não rodou na GameThread.
- **Lido e descartado:**
  - os ganchos do Hidden Leaf (`cameraMatrices`, `fighterInput`, `sequenceStep`, `padRead`, `promptPressed`) chamam o
    original e depois só mexem na memória e no `v0`, nunca no `pc`;
  - a conclusão do RPC (`finishCall` e `completeClient`) só escreve o `v0` e os buffers, e não copia nenhum contexto de
    volta.
- **Achado à parte, sem prova de que seja a causa:** o `guarded()` não restaura o `m_transferJump` quando o corpo lança.
  Quem lança é o `invokeCurrentSequence`, no `ExitThread` com handlers. O ponteiro fica num `jmp_buf` de um quadro que
  já morreu.
- **Hipótese em teste, patch `0066-ee-foreign-lookup.patch`:** `[ee-foreign]` registra toda busca de função do jogo
  feita fora da thread do host que roda o EE. Vai na `app_sdr16.apk`, rodadas `n23`–`n25_rp5_foreign`. No boot da `n23`,
  nenhuma linha.

**Item 2d (estouro) no RP5:** `n18`, `n20`, `n21` e `n22` (rota v3 com o jogo base, uns 260 s cada) dão pico máximo de
0,980 e nenhuma janela acima de 1. O rms médio fica entre 0,213 e 0,235. Com isto, o item fecha nos 3 aparelhos.

**Item 4 (rodada 4, Nominum `ff2e56d`, `app_nmn4.apk`):**
- **Build:** export para `build/nominum-ff2e56d`, catálogo da VU1 com 53 imagens e 1196 blocos (`build/vu1rec`), GS
  `libbt4gsx_ff2e56d.so`. Script `build/lab/nmn4.sh`.
- **Erro meu de rota:** os envs `hl_env_n3_v3_a/b.txt` ainda têm o `POKE` da rota antiga. Com a libsdr, o `POKE` dá o
  salto conhecido para `0xbd9c90` (`sp 0x20821380`), visto na `n4_cube_v3_a/b` e na `n4_odin209_v3_b` (guardadas como
  `*.poke`). Refeito com `hl_env_r4_v3_a/b.txt`, que é a rota `hl_env_hp_v3.txt` mais `NOMINUM_BACKEND=nominum` (e mais
  `NOMINUM_VU1=1` no b).
- **Cube, campanha (fps médio, com o pior entre parênteses):**

  | combinação | vila | andar | menu START |
  |---|---|---|---|
  | b | 49,7 (39,0) | 47,1 (8,6) | 48,6 (30,6) |
  | b sem blocos (`NOMINUM_VU1_REC=0`) | 48,6 | 45,7 | 40,7 |
  | a | 48,6 | 45,9 | 40,8 |

  - Os blocos recompilados tiram uns 4–5 ms da VU1 (15,4 para 11,5 ms na vila).
  - O GS do Nominum fica em 5–6,5 ms.
  - A imagem está certa (brilho médio 106 no centro do vídeo).
- **Linha "replay thread on the cores of capacity":** 0 em todas as rodadas com o `ff2e56d`; na rodada 3 eram 3 por
  rodada com o backend próprio. Confirmado.
- **Odin: imagem preta.** Nas 5 rodadas do Odin (16:9 a e b, 20:9 v3 e campanha b, giro), a área do jogo sai preta
  (brilho médio 0,7–0,9), com o HUD do app e o contador por cima.
  - O Odin estava em "60 interpolado": o log tem `[interp] asked 60`, e a rodada 3 foi a 50. O `fps_mode` voltou para
    50 pelo toque do giro.
  - Guardadas como `*.i60`. Refeitas a 50, mais duas a 60 (b e c) para separar a causa: `q_r4_odin3.out`.
- **Odin, giro:** são refeitas só as 3 swapchains do giro (`turn 90 → 270 → 90`), mas a imagem estava preta. Repetido
  a 50.

### 2026-11-05 01:40, noite, itens 2a, 2b, 2c, 2d e 3c: o sdrdrv não tem callback; som por tela; sincronia dos filmes

**Item 2a:**
- A vigia com a thread em cada linha (`app_sdrwatch12.apk`) passou 2 de 2 no RP5 em 16:9 (`rec/n11_rp5_wv`, v17423;
  `n12_rp5_wv`, v17431), sem pegar nada. Contando com a `n6`, a build da vigia passou 3 de 3, e a build normal falhou 5
  de 6 em 16:9. A vigia muda o tempo e esconde a falha.
- **Ferramenta nova sem custo de tempo (patch `0063-ee-event-ring.patch`):** cada thread guarda, num anel, os últimos
  32 eventos do escalonador: invocação empilhada (`P`), terminada (`O`, depois do `onComplete`) e thread posta para
  rodar (`R`). Cada evento leva o `pc`, o `ra` e o `sp` do contexto base. O anel é impresso junto com os despachos
  quando uma thread salta para o nada. Vai na `app_sdr13.apk`; 3 rodadas no RP5 em 16:9 (`n15`–`n17_rp5_ring`).
- **Lido e descartado como causa:**
  - o gancho da câmera troca só a entrada `0x10DD00` da tabela (`replaceFunction`), não os pontos de retomada da
    função;
  - as invocações de interrupção, alarme, callback do GS, SIF, RPC, `SyscallOverride` e IPU começam com `sp` 0 ou o da
    pilha de invocação; nenhuma usa a pilha da thread;
  - o `0x3389ac`/`0x338c30` antes da falha não é espera: é um laço de 12 entradas que chama a `0x338890`, cujos desvios
    para `0x3389ac` passam pelo despacho.

**Item 2b (callback do `sdrdrv` depois do BlockTrans), rodada `rec/n13_odin_rpc` (`HL_RPC_LOG=1`, filmes):**
- 1906 chamadas ao `0x80000701`, todas síncronas (flags `0x2`, tratadas pelo IOP, sem `Nowait` e sem callback). O
  `0x8100` (BlockTransStatus) foi chamado 1865 vezes, o `0x80e0` (BlockTrans) 5 vezes e o `0x8010` 26 vezes.
- A função de fim do `sceSdRemote` vem de `*(0x410e50)`, que vale 0 na RAM (`rec/n9_rp5_tl/ram_miss.bin`). O EE não
  registra nenhum servidor de RPC nem comando SIF para o som.
- **Não há callback a entregar:** o jogo não chama o `sceSdRemoteCallbackInit` e acompanha a transferência pelo status.
  Nada a corrigir.

**Item 2c (som por tela), rotas sem `HL_FAST_UNTIL`:**
- Rota de filmes (`n8_odin_film`, `n8_cube_film`) e mapa vsync→tela pelas gravações:
  - **mudos:** a escolha de idioma e os logos parados da Bandai Namco Games e da Bandai, do boot até v1401;
  - **com som:** o filme do logo (`LOGO_C.PSS`, v1401), o `NARUTO10.PSS`, a abertura, o título e os menus;
  - **1 s mudo em v6309–6358:** é o fim da abertura, enquanto o logo do título se forma.
- Rotas v3 e de campanha com o jogo base (`n10_odin_v3`, `n10_odin_camp`, `n10_cube_v3`, `n11_cube_v3`, todas até o
  fim, 0 `missing-target`):
  - depois de v6400 (fim do `HL_FAST_UNTIL`), só ~1 s de silêncio em cada troca: entrada da luta, entre as lutas e
    resultado;
  - seleção, luta, vitória, resultado e campanha têm som.
- **Não comparado com o PCSX2:** o vídeo de referência (`Gravação_de_tela_20261031_234651.webm`) não tem faixa de som.
  Os logos mudos ficam como "a conferir no PCSX2".

**Item 2d (estouro), agora com o jogo base:**
- Odin (`n10_odin_v3` 373 s, `n10_odin_camp` 205 s) e Cube (`n10_cube_v3`, `n11_cube_v3`, 742 s): pico máximo 0,980 e 0
  segundos com pico acima de 1.
- rms médio na v3: 0,197 (Odin), 0,190 e 0,198 (Cube). Antes do limitador era 0,198.
- No RP5 a medida fica para a próxima rodada.

**Item 3c (sincronia dos filmes), `HL_SND_ONSET=1` (novo: `[snd-on]`/`[snd-off]` do SPU2 em janelas de 10 ms, e
`[mpeg-pic]`, patch `0064-film-picture-log.patch`), `rec/n14_odin_sync` e `n14_cube_sync`:**
- Os dois aparelhos dão os mesmos vsyncs: o caminho é determinístico.
- **Imagem presa ao vsync:** na abertura, do quadro 225 (v1937) ao 2400 (v6287), são 2175 quadros em 4350 vsyncs,
  exatos 2 por quadro.
- **Começo:**
  - `LOGO_C.PSS`: primeiro quadro em v1446, o som entra em v1449 (60 ms depois);
  - `NARUTO10.PSS`: primeiro quadro em v1626, som em v1647 (420 ms);
  - abertura: quadro 0 em ~v1887, som forte em v1904 (340 ms). Antes, de v1856 a v1885, entram e saem trechos baixos
    (rms 0,02).
  - Falta saber se o atraso dos dois últimos é do próprio filme (som que começa depois da imagem): o vídeo de referência
    não tem som.
- **Fim:** o último quadro marcado é o 2400 (v6287), e o som sai em v6337, menos de 1 s depois.
- **Atraso fixo do som:** a fila do SDL fica perto do teto de 200 ms (`kMaxBytes`), com mediana de 128 ms no Odin e de
  175 ms no Cube. O som toca uns 130–175 ms atrás da imagem, fora a latência do aparelho. Quando a fila passa do teto,
  o pedaço é descartado (`writeChunk`).
  - **Próximo passo** (não feito à noite, por risco): teto de ~100 ms com correção de deriva (reamostrar ±0,5%) em vez
    de descartar.

### 2026-11-05 01:15, noite, itens 2a, 2d, 3b e 3c: o quadro da `0x31b4e0` é sobrescrito; limitador; YUV em NEON

**Item 2a (o salto da 3ª luta), com a `sdr11` no RP5 em 16:9:**
- A vigia por valor (`rec/n6_rp5_wv`, `app_sdrwatch11.apk`) passou (v17355) e não pegou nada.
- `rec/n9_rp5_tl` (`HL_THREAD_LOG`, `HL_MISS_DUMP`) repetiu a falha em v16883 e gravou a RAM (`ram_miss.bin`, que fica
  em `files/runtime/data/hl_frames/`).
- **O que a RAM mostra:**
  - a pilha da thread 8 (`0xb63ca0`+`0xc400`, só dela: nenhuma outra thread do log tem pilha ali) tem, de `0xb6fb80`
    a `0xb6fbf0`, os quadros de outra cadeia de chamadas: `0x35b310` (laço que chama a vtable+0xc de objetos de 0xa0
    bytes) → `0x3568a0` (sp `0xb6fbd0`, `ra 0x35b3f0`) → `0x356920` (sp `0xb6fbb0`, `ra 0x3568e0`, s0 salvo =
    `0xe3b4c0`) → `0x3563c0` (sp `0xb6fb80`, `ra 0x356944`);
  - a `0x3563c0` tem o mesmo par de chamadas que a `0x31b4e0` (`jal 0x1bf9a0`, depois `jal 0x1bfeb0`) e o quadro dela
    está no mesmo `sp` (`0xb6fb80`);
  - o anel de despacho da thread 8: ela rodava em `0x3167c0` (sp `0xb6fe40`), entra o tratador do timer (`0x169a10`),
    e a thread volta em `0x1bfeb0` com `ra 0x31b658` e `sp 0xb6fb80`. Quem montou esse contexto é a `0x31b4e0`,
    parada numa troca no `jal 0x1bfeb0`. Ao voltar, o epílogo dela lê o `ra` em `sp+0x30` = `0xb6fbb0`, onde está o s0
    que a `0x356920` salvou: `0xe3b4c0`.
- **Leitura:**
  - enquanto a `0x31b4e0` estava parada no `jal`, a cadeia `0x35b310`... rodou na mesma pilha e por cima do quadro
    dela;
  - ou a thread 8 tem dois contextos vivos (o de `0x3167c0` e o da `0x31b4e0`), ou outra execução usa a pilha dela;
  - o código gerado guarda os registradores no contexto, sem cópia local. Quem copia contexto inteiro são as
    invocações do runtime (RPC, SIF, `SyscallOverride`), mas todas trocam o `sp` pela pilha de invocação.
- **Próximo passo:** a vigia `app_sdrwatch12.apk` (`hl_watch.cpp` agora diz a thread e o `pc`/`sp` do último
  despacho em cada linha), 2 rodadas no RP5 em 16:9 (`n11_rp5_wv`, `n12_rp5_wv`).
- Enquanto isso, as rodadas de 11-04 no Odin (`n5_odin_*`) estavam com a mod Alpha 1.1X ligada: o `un5_mods.txt` de
  11-04 ficou no aparelho. Renomeei para `un5_mods.dev.txt`, e as rodadas `n10_odin_*` são com o jogo base.

**Item 2d (estouro):**
- O `hl_spu2.cpp` ganhou um limitador de pico depois do filtro DC: teto de 0,98, envelope com soltura de 100 ms, que
  só age acima do teto (`HL_SPU2_LIMIT=0` desliga).
- Odin, com a `sdr10` (com mod, ver acima): 537 s em `n5_odin_v3` e `n5_odin_camp`, pico máximo 0,980 e 0 segundos com
  pico acima de 1. Antes, eram 681 de 1438 s acima de 1, com máximo de 1,462. O rms médio da rota v3 foi de 0,198 para
  0,193.
- Falta a rota com o jogo base nos 3 aparelhos.

**Item 3b (YUV para RGBA em NEON, patch `0062-film-yuv-neon.patch`):**
- `MPEG.cpp` guarda os planos 4:2:0 do quadro. No `sceMpegGetPicture`, `hl_yuv2rgba.h` converte direto para a imagem
  do jogo (colunas de 16 pixels, alfa 0x80, as mesmas do caminho antigo), sem o vetor RGBA intermediário e sem a
  cópia. Isso junta as propostas 1 e 3 do `docs/ipu.md`.
- A conversão é a do `yuv2rgb_c_32` do swscale refeita: tabelas de 16.16, BT.601 de faixa limitada, croma do vizinho.
  No aarch64 vai em NEON, 16 pixels de duas linhas por passo.
- `HL_MPEG_SWS=1` volta ao swscale. Com `HL_PERF`, o log mostra uma linha `[mpeg]` a cada 250 quadros.
- **Mesma imagem:** 200 quadros do `OPENING.PSS`, comparados com o swscale da build do Android (`libswscale.a` do
  DragonRage): 0 bytes diferentes no PC e no Cube (`tools/yuvtest/test.cpp`).
- **Tempo por quadro**, conversão mais escrita na RAM do jogo, na rota de filmes `hl_env_film.txt`:
  - Odin (`rec/n8_odin_film`): 0,05 + 0,16 = 0,21 ms, contra 0,24 + 0,31 = 0,55 ms com o swscale (`n8_odin_film_sws`);
  - Cube (`n8_cube_film`): 0,17 + 0,52 = 0,69 ms, contra 0,53 + 0,87 = 1,40 ms com o swscale.
- A série inteira de patches se aplica limpa sobre a `e563698`, e o resultado é igual à árvore. O `MPEG.cpp` compila no
  PC (`-fsyntax-only`).

**Item 3c (fluidez), Odin a 120 Hz:**
- `mpdecimate` no centro da gravação, de 30 a 110 s: na abertura são 25 quadros diferentes por segundo, que é a taxa do
  `OPENING.PSS`, com um ou outro segundo de 22 a 24 nos cortes escuros. É igual nos dois caminhos.
- O gravador do Cube grava a 42 fps, então lá a contagem não serve.
- Falta conferir a sincronia do som no começo e no fim do filme.

### 2026-11-05 01:00, noite, item 2a: o salto da 3ª luta só acontece com a câmera larga, no RP5

- **As 3 rodadas da `sdr9` de 11-04, no RP5 em 16:9, falham no mesmo ponto** (`rec/sdr9_rp5_v3`, `sdr9b`, `sdr9c`):
  - a 3ª luta começa em v14880 e, em v16883, a thread 8 "volta" para `0xe3b4c0`. O `pc`, o `ra`, o `a0` e o `s0` valem
    todos `0xe3b4c0`, e a palavra 0 desse objeto vale `0x5e9fb0`, com jeito de vtable;
  - antes do salto roda o tratador do timer do kernel (`0x169a10`, lê o TIMER0, `ra 0` na pilha de invocação
    `0x1fe7ff0`, o dos alarmes). Depois a thread 8 volta em `0x1bfeb0`/`0x31b658`;
  - o retorno seguinte, o `jr ra` de `0x31b4e0` (quadro de 0x80, `ra` em `sp+0x30`, com `sp 0xb6fb80` = slot
    `0xb6fbb0`), lê `0xe3b4c0` em vez do `ra`.
- **Passagens:** à meia-noite, a mesma build e a mesma rota passaram 3 de 3 no RP5 (`n1_rp5_dump`, `n2_rp5_dump`,
  `n3_rp5_pure`, até v17981–18048).
  - **A diferença é a imagem:** o desenvolvedor pôs o RP5 em 4:3 às 00:03 (`un5_settings.txt`, `aspect_milli=1333`,
    copiado em `files/un5_settings.dev-0003.txt`). Os logs confirmam: as rodadas com falha têm 13 linhas `[cam]`
    (câmera larga), e as passagens têm 0.
  - Voltei o RP5 a `aspect_milli=0` (tela cheia, o padrão das rotas). Com isso, a `sdr10` repete a falha
    (`rec/n4_rp5_ret`, v17973, `cam=13`). No fim da noite o RP5 volta a 1333.
- **O que não reproduz:**
  - o Odin em 16:9 (`n1_odin_sdr9`, `n2_odin_pure`, câmera larga, até v18024–18068);
  - o Cube (tela quadrada: não há câmera larga);
  - a build da vigia no RP5 em 16:9 (`w10_rp5_v3`), cuja 2ª luta acaba em outro momento (v12034 contra v13292).
- **Tentativa sem resultado:** `HL_RETCHECK` (um gancho em volta da `0x31b4e0`). A função é interrompida por trocas de
  thread e volta ao despacho no meio (em `0x172390`/`0x170000`), então o gancho só pega falsos positivos. Tirei.
- **Próximo passo:** a vigia (`app_sdrwatch11.apk`) com `HL_WATCH=b6fbb0` e o modo novo `HL_WATCH_VALUE=e3b4c0` (só as
  escritas desse valor, com os 16 eventos antes), no RP5 em 16:9.
- **Item 1, no PC:** o runtime foi recompilado com `-ffp-contract=off` (`build/lab/pc_nofma.log`, 00:28). Em 70 s, os
  dois binários (`ps2EntryRunner` novo e `build/lab/ps2EntryRunner_fma`) dão o mesmo log de boot: 0 erros, só o número
  de linhas `[perf] nice` muda (`pc_nofma_run.log`, `pc_fma_run.log`). Não vi a imagem.

### 2026-11-05 00:30, noite, itens 1 e 3a: `-ffp-contract=off` no PC; os filmes não são escuros

**Item 1 (`-ffp-contract=off`, pedido do `docs/integracao.md` do Nominum, linha 268):**

- **Android:** já valia desde `8fac981` (11-01). `recomp/android/CMakeLists.txt` põe `UN5_NUMERIC` (`-fno-strict-aliasing
  -fwrapv -ffp-contract=off`) em `add_compile_options`. Conferido no `build/android/build.ninja`: 815 de 815 objetos da
  `libun5` (runtime, VU0, VU1, FPU do EE e código gerado) e da `hl_spu2` têm o flag. Nada muda no APK, e não há rota
  a refazer por isto.
- **PC:** o `recomp/build.sh` passava só `-march=native`. Com FMA (`-mfma` ligado neste processador), o GCC funde
  `a*b+c` (o padrão dele é `-ffp-contract=fast`). Agora passa `-march=native -ffp-contract=off` em C e C++.
  - O runtime do PC está sendo recompilado com o flag (`build/lab/pc_nofma.log`). O binário antigo ficou em
    `build/lab/ps2EntryRunner_fma`. A rota curta no PC fica para quando a compilação acabar.

**Item 3a (filmes escuros):** não é defeito. Medido contra o PSS e contra o PCSX2.

- **De onde veio a suspeita:** do contact sheet de 1 quadro a cada 8 s (entrada de 11-04 18:40), que caiu nas
  transições pretas e no *fade-in* do filme. Com 1 quadro a cada 2 s, os filmes aparecem inteiros nas duas builds
  (`sdr9_rp5_v3`, com a libsdr, e `cam_rp5_v3`, com os stubs).
- **Medida:** quadros reduzidos a 128x112, casados pela correlação da luminância.
  - **PCSX2 contra o `OPENING.PSS` decodificado pelo FFmpeg no PC** (vídeo de referência de 10-31, t = 13–17 s):
    correlação 0,96. A cor média do PCSX2 fica a 3–6% da do PSS (por exemplo, RGB 81/59/39 contra 86/63/41).
  - **RP5 (`rec/sdr9_rp5_v3`) contra o mesmo PSS:** correlação 0,90–0,95. A cor média fica a 1–6% (153/130/110 contra
    159/133/110). Os dois quadros com diferença maior (até 50% no vermelho) estão no meio de um corte, onde o
    casamento erra.
- **A conversão está certa:** o PSS diz `color_range=tv`, e o `MPEG.cpp` usa o `sws_scale` padrão (BT.601, faixa
  limitada para RGBA completa). É a mesma conta da CSC da IPU no PCSX2 (`(Y-16)*1,164`, `(Cr-128)*1,596`).
  - Diferença que sobra, sem efeito visível: a IPU dá alfa 0/0x40/0x80 pelos limiares TH0/TH1, e o runtime escreve
    sempre 0x80.
- **Visto de passagem:** o corredor da abertura não está no `OPENING.PSS`. É desenhado em tempo real pelo motor.

### 2026-11-04 18:40, item 8: a falha da libsdr vinha da rota de teste (o `POKE`), não do jogo

**Correção da entrada das 18:00:** os `free` ruins não são uma cauda velha da lista. Quem os causa é a nossa rota.

- **O gancho da cauda** (`HL_LISTCHECK=1`, `app_sdrwatch6.apk`) não achou nenhuma cauda velha nos acréscimos de
  `0x18d410`. O gancho seguinte (`HL_LISTCHECK=2`, `app_sdrwatch7.apk`, `libun5_sdrwatch7.so` sha1 `d5ba96e5`) percorre a
  lista antes do `0x18db30`. Resultado igual no Cube (v10247, `rec/list2_cube_v3`) e no RP5 (v11000, `rec/list2_rp5_v3`):
  - o objeto é `0xd7b730`, com 4 nós (`d7b760 d7b810 d7b8c0 d7b970`);
  - o `+0x9c` do último nó, **`0xd7ba0c`**, vale `0x3c3c3c3c`.
- **`0xd7ba0c` é o endereço do `6450:POKE:d7ba0c:3c:4` da rota v3.** Ele punha 0,0115 na vida da CPU (`lutador+0x6c`,
  lutador em `0xd7b9a0`; entrada de 2026-11-03 09:40) e foi achado com a libsdr nos stubs. Com a libsdr de verdade, o
  heap fica arrumado de outro jeito, e o mesmo endereço cai na última palavra de um nó dessa lista. O `0x10400002`
  seguinte é o que já estava nos bytes de `0x3c3c3c3c` (a RAM dá a volta). A segunda luta fazia o mesmo com `d7ba8c`.
  - O travamento de 11-03 (v10300–11700 no RP5 e no Odin, `missing-target 0xbd9c90`) e a pilha repetida das threads
    131–133 têm esta mesma causa. Todos foram vistos com rotas que têm o `POKE`.
- **Prova:** a mesma rota sem os `POKE` (`hl_env_sdr_nopoke.txt`, `app_sdrwatch7.apk`, `HL_FREECHECK=1` só no log):
  - **Cube** (`rec/nopoke_cube_v3`, até v17929) e **RP5** (`rec/nopoke_rp5_v3`, até v17809): 0 `free` ruins, 0
    `missing-target`, 0 listas quebradas.
  - **Com som do começo ao fim:** rms 0,30 e 0,52 nos logos e filmes (v1595–2718), 0,08–0,50 depois.
  - Sem o golpe de 1 ponto, a CPU não perde: VS, duas lutas e a volta à seleção, sem WINNER (contact sheet conferido).
- **Mudanças:**
  - **`hl_game.cpp`:**
    - `HL_CPU_HP=<vsync>,...` (rotas de teste): na primeira entrada de um lutador da CPU (`+0x60 & 0x1e0`) a partir de
      cada vsync, a vida dele vira 0,0115. Não depende de onde o heap põe o lutador. A rota v3 nova é
      `build/lab/hl_env_hp_v3.txt`: os dois `POKE` trocados por `HL_CPU_HP=6450,11150`.
    - `HL_LISTCHECK=1/2` (só na build da vigia).
- **Regra para as rotas daqui em diante:** nenhum `POKE` de endereço do heap. Endereço fixo só em dado estático do ELF.
- **A build normal com a libsdr** (`app_sdr9.apk`, `libun5_sdr9.so` sha1 `4ae117b3`, código gerado `build/lab/runner_sdr`)
  e a rota `hl_env_hp_v3.txt`:
  - **Onde o lutador fica:** `[route]` acha a CPU da luta 1 em **`0xd7ba20`**. Com os stubs (`app_sdr8.apk`, que por
    engano saiu sem a libsdr: o `rsync -a` manteve as datas e o ninja não recompilou os 4 arquivos) ela fica em
    `0xd7b9a0`. Isso confirma que o heap muda e que o `POKE` antigo caía em outro objeto.
  - **Cube** (`rec/sdr9_cube_v3`, até v18006): 0 `missing-target`, com som. Rms 0,45 e 0,53 nos logos e filmes.
  - **RP5** (`rec/sdr9_rp5_v3`, até v17771): som nos filmes (0,52 e 0,43). Luta 1: WINNER com o personagem e
    resultado (Health 71, 726 ryo). A luta 2 vai até o fim.
  - **RP5, falha nova:** **1 `missing-target`** em v16860, na terceira luta (já fora da rota pensada, que é ✕ a cada 150
    vsyncs depois de v15900). A thread 8 salta para `0xe3b4c0` (dado), depois de um laço `0x3389ac`/`0x338c30`, com sp
    `0xb6fc00`. O som e o perf seguem até o fim.
  - **A conferir:** na gravação do RP5, os quadros dos filmes (v1600–2900) aparecem quase pretos. Na build da vigia
    (`rec/nopoke_rp5_v3`), o logo "10th" e o filme apareciam.
- **Decisão (a mais segura):** a libsdr **continua opcional** (`HL_SDR=1 recomp/build.sh`). Não liguei por padrão, por
  causa da falha do RP5 em v16860 e dos filmes escuros. O próximo passo é a vigia de `free` (`HL_FREECHECK=1`) e a lista
  (`HL_LISTCHECK=2`) na rota `hl_env_hp_v3.txt` até v17500, no RP5.
- **Rodada com a vigia (18:47, RP5, `app_sdrwatch10.apk`, `libun5_sdrwatch10.so`, a mesma rota com `HL_FREECHECK=1`
  e `HL_LISTCHECK=2`, `rec/w10_rp5_v3`):**
  - até v17764: 0 `missing-target`, 0 `free` ruins, 0 listas quebradas;
  - a CPU foi achada em `0xd7ba20` nas duas lutas;
  - som em toda a rota (rms 0,52 e 0,44 nos filmes).

  O salto de v16860 da `sdr9` **não se repetiu: é intermitente**. Ainda não tem causa.
- **Ainda falta para o item 8:** repetir a `sdr9` algumas vezes no RP5 para pegar o salto de v16860, conferir os filmes
  escuros, o callback do `sdrdrv` (o pedido do item) e os picos acima de 1,0 vistos em 11-03, e fazer a rota completa
  nos 3 aparelhos e no 20:9.

### 2026-11-04 18:00, itens 8 e 11: quem libera os ponteiros ruins; câmera larga só volta em câmera viva

**Item 8: a origem do `free` ruim**

- **Build `app_sdrwatch5.apk`** (`libun5_sdrwatch5.so` sha1 `e1028bfa`, `HL_SDR=1`). Com `HL_FREECHECK=1`, o `free` do jogo
  (`0x1181b0`) é conferido, e um cabeçalho que não está ligado nos dois sentidos fica no log, com a thread e os
  endereços de código achados na pilha.
- **Resultado igual nos dois aparelhos** (Cube v10247, `rec/free5_cube_v3`; RP5 v11000, `rec/free5_rp5_v3`). São
  exatamente dois `free` ruins:
  - `free(0x3c3c3c3c)` (o "cabeçalho" dele são instruções MIPS: o endereço dá a volta na máscara da RAM);
  - `free(0x10400002)`.

  Os dois vêm da thread 8, pelo invólucro `0x117570` chamado em `0x18db5c`, com a pilha `0x18db64`, `0x18d2cc`,
  `0x18d324`, `0x198658`, `0x21d104`, `0x21c990`, `0x29f764`.
- **`0x18db30(obj, flag)`** libera uma lista: `p = obj+0x14`; enquanto `p`, `prox = p+0x9c`, `free(p)`; no fim, se
  `flag`, `free(obj)`.
  - O `0x3c3c3c3c` é o `+0x9c` do último nó bom: **a lista não termina em 0**.
  - Os nós de 0xa0 bytes são feitos por `0x18d410`: `0x1176c0(0xa0)`, depois `0x18e340`, que zera o `+0x9c`. Eles
    entram na lista pelo ponteiro de cauda do objeto (`+8`): `nó+0x9c = *cauda; *cauda = nó; cauda = &nó+0x9c`.
  - Se a cauda ficou apontando para um nó já liberado, o acréscimo lê lixo para o "próximo" e escreve o nó novo em
    memória livre.
  - Ainda não sei qual objeto é este nem por que só com a libsdr de verdade. É código do jogo (`0x18d2a0`/`0x18d240`,
    na volta do resultado da luta para a seleção).
- **Teste de que é a causa:** com `HL_FREECHECK=2` (pula os dois `free`, `rec/free2_rp5_v3`, `app_sdrwatch4.apk`), o
  RP5 passa da cena da falha sem `missing-target`. As threads 131–133 recebem pilhas válidas (`0xbec610`,
  `0xc6db10`…), e o som segue até v12627. **Pular não é correção:** a escrita do nó novo em memória livre continua.
- **O som dos filmes com a libsdr:** na mesma rota do RP5, rms 0,45 em v1652 e 0,17 em v2312. Com a build normal
  (`rec/cam_rp5_v3`), 0,0. A libsdr de verdade é o caminho do item 8. O que falta é tirar essa falha.
- **Próximo passo:** achar o objeto da lista (um gancho em `0x18d410` que registre `obj`, a cauda e os nós) e o
  momento em que a cauda fica velha. Depois, corrigir onde o jogo esvazia a lista sem pôr a cauda de volta, ou onde a
  libsdr faz o jogo pular essa volta.

**Item 11: a volta da câmera larga (`hl_game.cpp`)**

- **Mudança:** ao sair do modo largo, uma câmera só volta para 1 se ainda está na lista de câmeras vivas do jogo
  (cabeça `*(gp-0x424)` = `0x617acc`, próxima em `+0x268`, a lista que a `0x10F420` percorre) **e** ainda tem a escala
  larga que escrevemos. Antes bastava ter a escala larga. A lista foi conferida num dump de RAM (5 câmeras encadeadas,
  todas com 1,0).
- **Teste no RP5 16:9** (`app_norm4.apk`, `libun5_norm4.so` sha1 `1fc233e2`):
  - **Rota v3, duas lutas** (`rec/cam_rp5_v3`, até v17912): câmera larga em v6006 e v10663, "2 of 12 cameras back to
    1 (24 cameras live)" e "6 of 9 (29 live)". Também WINNER com o personagem, resultado, seleção e segunda luta. 0
    `missing-target`.
  - **Campanha** (`rec/cam_rp5_camp`): `x 0.750` desde v3523, 47,7–51,5 fps.
- **Não feito:** Odin (bloqueado), 20:9, os menus ("Esticar" e "Desenhar em 16:9") e o recorte.

**Outros:** a `libun5` normal deste commit é a `libun5_norm4.so`: Alpha 1.1X, câmera, e a verificação do `free` só na
build da vigia.

### 2026-11-04 17:15, item 8: a vigia funciona e mostra o `free` de um ponteiro com cabeçalho de lixo

**Builds:** `app_sdrwatch2.apk` (vigia corrigida, `libun5_sdrwatch2.so` sha1 `019ea76e`) e `app_sdrwatch3.apk`
(`HL_WATCH_PTRS=1`: só o que não parece ponteiro de lista, com os 16 eventos anteriores; `2e2f5dbc`). As duas foram
feitas com `HL_SDR=1`.

**A falha se repete:**

- Rota v3 no RP5 (`rec/watch2_rp5_v3`, `watch3_rp5_v3`) e no Cube (`watch2_cube_v3`, `watch3_cube_v3`).
- O `missing-target 0xbd9c90` vem em v11007/v10974 no RP5 e em v10215/v10207 no Cube. **O Cube também tem a falha** (não
  tinha sido rodado com a libsdr).

**Quem escreve o `0x80852021` na lista `0x618a50`:**

- É o próprio alocador do jogo: `sub_118E60` (pc `0x118ffc`, `sw v1, 4(s1)`), quando põe um bloco livre na lista. O
  "bloco" que ele insere é o endereço `0x80852021`. Logo depois vem o `0xf9990000`.
- O mesmo nos dois aparelhos, e o bloco tocado logo antes é sempre `0xd67ed0`.
- O `sub_118E60` é chamado pelo `free` do jogo (`0x1181b0`, ra `0x118428`). Os blocos do heap formam uma lista por
  endereço (cabeçalho: `+0` o bloco anterior, `+4` o seguinte, `+8` o tamanho, `+0xc` a etiqueta). O `free(p)` lê o
  bloco anterior em `*(p-0x10)` e devolve à lista livre o espaço entre o fim desse bloco e o seguinte. O
  `0x80852021` = anterior + tamanho do anterior: **o cabeçalho do ponteiro liberado é lixo**.

**De onde vem o lixo (vigia em `0xd67ed0` e `0xd67ed8`, `rec/w4_cube_d67ed8`, `w5_cube_d67ed0`, `w5_rp5_d67ed0`):**

- Durante os filmes (v1336–2737), a região é o buffer de imagem do filme. A thread de vídeo `0x101c00` (por
  `0x101620`) escreve nela palavras `80RRGGBB`, que são pixels RGBA do PS2 (alfa `0x80`). O `0x80852021` é um pixel
  assim (azul-escuro).
- Depois do último filme, o alocador reaproveita a região e escreve cabeçalhos válidos em `0xd67ed0` (v5880–5904).
  Ninguém mais escreve lá.
- Então o estrago não é uma escrita atrasada nesse bloco. É um **`free` (por volta de v10198–10227) de um ponteiro que
  aponta para uma parte do antigo buffer de imagem nunca entregue pelo alocador**, com o pixel no lugar do cabeçalho.
  Na build normal, a libsdr é stub, e o jogo segue outro caminho no fim dos filmes. O ponteiro velho fica na mão de
  quem o libera depois.

**Próximo passo (build em andamento, `build/lab/watchbuild4.sh`):**

- **Verificação do `free`:** com `HL_FREECHECK=1` (só na build da vigia, `hl_game.cpp`), o `free` do jogo é conferido.
  Quando o cabeçalho não está ligado nos dois sentidos, a build registra o ponteiro, o cabeçalho, o `ra` e o `sp`. Com
  `=2`, além disso, ela pula esse `free`, para ver se o resto da rota segue sem a falha. Pular é um teste, não uma
  correção.
- **Para não perder tempo de build:** `recomp/build.sh` ganha `HL_NO_PC=1` (só o código gerado, sem o runtime do PC
  do passo 7). Os códigos gerados normal e `HL_SDR` ficam guardados em `build/lab/runner_{norm,sdr}`.

### 2026-11-04 16:10, itens 8, 10 e 16: a vigia de escrita não via nada; a Alpha 1.1X roda com a base; custo dos filmes

**Item 8, vigia de escrita (correção da entrada de 15:25):**

- **O que rodou:** a build da vigia (`app_sdrwatch.apk`, `libun5_sdrwatch.so`) na rota v3 do RP5 até v12844
  (`rec/watch_rp5_v3`). A falha voltou igual, em v11588: `missing-target 0xbd9c90`, `sp 0x20821380`, `a2 0x852021`.
  Houve **0 linhas `[watch]`**.
- **No Odin:** a mesma rota não rodou. O aparelho estava na tela de bloqueio (`mDreamingLockscreen=true`, padrão) e fica
  de fora até ser desbloqueado.
- **Causa das 0 linhas, por dois motivos:**
  1. O `hl_watch::g_addr` era lido do `HL_WATCH` num inicializador estático, que roda no `dlopen`, antes de o
     `un5_main.cpp` carregar o `hl_env.txt`. A vigia nunca ligou.
  2. As escritas do código recompilado vão pelos `Ps2FastWrite*` (`ps2_runtime_macros.h`), que não passam pelo
     `ps2TraceGuestWrite`. Mesmo ligada, a vigia só veria o `memcpy`/`memset` do runtime.
- **Patch `0061` refeito:**
  - `hl_watch::setup()` passa a ser chamado logo depois do `loadEnvFile`.
  - Os `Ps2FastWrite8..128` ganham o gancho `HL_WATCH_FAST`, com o endereço de retorno do host
    (`[watch-fast] … from libun5+0x…`, para o `llvm-addr2line`).
  - O `PS2Runtime::dispatchGuestBranch` ganha um `poll`, que compara os 8 bytes vigiados a cada chamada ou salto do
    jogo e registra qualquer mudança, venha de onde vier (DMA, IOP, código do HL), com a origem e o destino do salto
    (`[watch-poll]`).
  - Tudo fica atrás de `-DHL_WATCH_BUILD`, e a build normal não muda. A série inteira de patches aplica limpa no
    `e563698` (conferido numa árvore à parte).
- **Engano meu durante o trabalho:** rodei `git stash` no `build/PS2Recomp` (a árvore com os patches aplicados) e
  desfiz na hora com `git stash pop`. As 72 mudanças voltaram, a pilha ficou vazia, e o `0061` aplicado é igual ao do
  patch.
- **Pista nova, ainda não seguida:** a entrada de 11-03 08:06 mostra que o `cameraScale` do `hl_game.cpp` já
  escreveu em câmeras liberadas e quebrou a lista livre do heap. Os ganchos do HL que escrevem na RAM do jogo (câmera,
  Ougi, controle) são candidatos tão bons quanto a libsdr. O `[watch-poll]` deve mostrar a diferença.

**Item 10, Alpha 1.1X (`un5_main.cpp`):**

- **O programa só muda dados:** o `SLES_556.05` da Alpha tem os mesmos cabeçalhos de programa que o europeu. As 606
  palavras diferentes estão todas fora do código (`0x4452b4`…`0x54c8d8`, tabelas). No `BTL.BIN` só mudam 13
  palavras, todas na parte de dados (depois de `text 0x1e5b40`). `ADV`, `ETC` e os `TEXT*` são iguais (`un5.py diff`,
  `build/mods/a11x_pack.json`). Isso corrige a `analise-elf.md` ("~5,8 KB de código mudado"): o que muda são dados.
- **A falta era outra:** a Alpha não precisa de pacote de código. Ela precisa que a `libun5` base rode o programa da
  mod. Hoje a base sempre roda o `SLES_556.05` europeu e só troca os arquivos do disco.
- **Mudança:** com a mod ligada, se ela tem um `SLES_556.05` com os mesmos cabeçalhos e os mesmos bytes nas faixas de
  código do `CODE_RANGES` (`0x100000–0x3d5760`, `0x5e3a00–0x5e7208`), a base roda esse programa
  (`un5: mod program … (the base's code, its own data)`). Senão, roda o europeu e avisa que a mod precisa do pacote.
  Função `sameCodeAsBase`.
- **Teste no Cube** (`build/lab/app_a11x.apk`, `libun5_a11x.so` sha1 `8ce229f7`): a Alpha já estava importada em
  `modlib/un6-a11x`, e a ligação foi feita à mão (`un5_mods.txt`, `modlib/mods.txt`, restaurados depois). Rota v3 até
  v9260, em `rec/a11x_cube_v3`:
  - o log diz `un5: mod program …/modlib/un6-a11x/disc/SLES_556.05 (the base's code, its own data)`, e o `BTL`
    residente é o de sempre;
  - o vídeo tem o título "Ultimate Ninja 6", o CyberConnect2, o 10th, a abertura, a carga da UN6, a seleção, a luta
    Naruto x Sakura (50 fps de v6535 em diante), o Ougi, o WINNER com o personagem e a tela de resultado, com som (rms
    0,09–0,17 na luta).
- **Não feito:** RP5, Odin (bloqueado) e 20:9. O A35 com a build de hoje também não. O pacote `libun5_un6_a35.so` é de
  11-03 05:27, anterior ao Nominum `b2ff28b` e a todos os patches de hoje.

**Item 16, filmes:** medidas e proposta em `docs/ipu.md` (`04f01ba`). O resumo: o runtime não emula a IPU, a `libmpeg`
é HLE com FFmpeg na GameThread, e os filmes rodam a 50 fps nos três aparelhos, com 12–17 ms ociosos. No Cube, o filme
custa ~5% do processo, e o YUV para RGBA usa o caminho em C do swscale (`yuv2rgb_c_32`). A primeira proposta é essa
conversão em NEON.

### 2026-11-04 15:50, item 13: as funções que mais gastam CPU na campanha e na luta (RP5, sd480p, VU1 do Nominum)

**Método:**

- **Aparelho:** RP5 no perfil `sd480p`, com o driver da Qualcomm. A tabela manda o GS para o ARMSX2, e com
  `NOMINUM_VU1=1` (combinação b da rodada 3) a VU1 e o VIF1 são os do Nominum.
- **APK:** `build/lab/app_norm.apk`, com `libun5_norm0060.so`.
- **Gravação:** `simpleperf record --app` (o app é depurável; `-p` é negado sem root), `cpu-clock` a 4000 Hz por 30 s,
  a partir de ~v6400. As rotas são `hl_env_n3_camp_b.txt` (vila e caminhada, 50 fps) e `hl_env_n3_v3_b.txt` (luta 1 e
  Ougi, 50 fps).
- **Script:** `build/lab/profq.sh`. Os resumos são feitos com `proftop.py` (top N) e `profcat.py` (categorias), que
  usam o `simpleperf` do NDK com `--symfs build/lab/symfs_v3`.
- **Saída:** `build/lab/rec/prof_rp5_{camp,v3,camp_c}/` (`perf.data`, `report*.txt`).

**Threads (% de todas as amostras):**

| cena | GameThread (EE+IOP+SPU2) | VuWorker (VIF1/VU1) | CPU Thread (GS do Nominum/ARMSX2) | GS (ARMSX2, Vulkan) |
|---|---|---|---|---|
| campanha, b | 35,2 | 33,7 | 24,6 | 3,7 |
| luta, b | 36,6 | 22,3 | 10,9 | 27,0 |
| campanha, c (VU1 do runtime) | 31,7 | 40,4 | 21,2 | 3,7 |

**O que a GameThread faz (% da própria thread):**

| categoria | campanha b | luta b | campanha c |
|---|---|---|---|
| código do jogo recompilado (`sub_`, `adv_sub_`, `btl_sub_`) | 34,6 | 37,0 | 34,7 |
| escalonador, despacho, memória (`checkpointDue`, `PS2Memory`, `lookupFunction`) | 18,3 | 15,9 | 17,9 |
| interpretador do IOP (`IopCpuCore::executeInstruction`, `step`, `runCycles`) | 17,4 | 19,8 | 17,6 |
| SPU2 (`spu2Mix`, reverb) | 6,0 | 8,6 | 6,1 |
| VU0/VU1 do runtime na GameThread (`VU1Interpreter::fastExecute`) | 4,9 | 1,7 | 4,6 |
| travas e atômicos | 4,9 | 3,0 | 4,7 |
| núcleo do sistema | 3,8 | 4,8 | 3,8 |

**O que a VuWorker faz:**

- **Com b:** dois terços são a VU1 do Nominum (`recBlock<…>` e `fastExecute`). O resto é o `Vif1::transfer` (9–11%)
  e o `memcpy` (5–6%), que em boa parte é o espelho do `hl_nominum_vu1.cpp` (16 KB de dados entrando e saindo a cada
  transferência, mais a comparação do micro).
- **Com c:** 76% é o `VU1Interpreter::fastExecute` do runtime.

**Funções do jogo que mais pesam na GameThread (% da thread):**

- **Campanha:** `adv_sub_7f0d90` 2,0, `0x194530` 2,0, `0x152dd8` 1,4, `0x195b40` 1,3, `0x3c3370` 1,1, `0x195ee0` 0,8,
  `0x1963b0` 0,7, `0x10fc20` 0,6, `0x19fec0` 0,5, `0x1c42c0` 0,5, `0x194390` 0,5, `0x1933f0` 0,5.
- **Luta:** `0x1d70f0` 2,3, `0x19fec0` 1,9, `0x1d6640` 1,2, `0x1d73d0` 1,2, `0x1d76a0` 1,0, `0x1d14d0` 1,0, `0x194530`
  0,9, `0x194390` 0,8, `0x152dd8` 0,8, `0x1a58d0` 0,7, `0x1abd00` 0,6, `0x10fc20` 0,6.

  Nenhuma passa de 2,3%: o código do jogo está espalhado.

**Top 30 do processo, campanha b (% de todas as amostras):**

| # | % | thread | library | function |
|---|---|---|---|---|
| 1 | 6.46 | CPU Thread | libbt4gsx.so | `nominum::core::kick` |
| 2 | 6.29 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<7539251845881921587ul>` |
| 3 | 3.88 | CPU Thread | libbt4gsx.so | `nominum::core::transfer` |
| 4 | 3.63 | VuWorker | libun5.so | `nominum::core::Vif1::transfer` |
| 5 | 2.87 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::fastExecute` |
| 6 | 2.68 | GameThread | libun5.so | `ps2x::iop::detail::IopCpuCore::executeInstruction` |
| 7 | 2.21 | CPU Thread | libc.so | `__memcpy` |
| 8 | 2.07 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<13807691333433948256ul>` |
| 9 | 2.06 | CPU Thread | libbt4gsx.so | `std::vector<nominum::core::Vertex, std::allocator<nominum::core::Vertex> >::push` |
| 10 | 1.77 | VuWorker | libc.so | `__memcpy` |
| 11 | 1.71 | CPU Thread | libbt4gsx.so | `nominum::core::hashOf` |
| 12 | 1.66 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<12529414650559193755ul>` |
| 13 | 1.55 | GameThread | libun5.so | `ps2x::iop::detail::IopEmulator::Impl::step` |
| 14 | 1.48 | GameThread | libun5.so | `EeScheduler::checkpointDue` |
| 15 | 1.48 | CPU Thread | libbt4gsx.so | `std::vector<unsigned int, std::allocator<unsigned int> >::push_back[abi:nn210000` |
| 16 | 1.41 | VuWorker | libun5.so | `nominum::core::Vif1::transfer` |
| 17 | 1.38 | GameThread | libun5.so | `VU1Interpreter::fastExecute` |
| 18 | 1.37 | GameThread | libun5.so | `isa_native::spu2Mix` |
| 19 | 1.29 | GameThread | libun5.so | `std::__wrap_iter<unsigned char*> std::vector<unsigned char, std::allocator<unsig` |
| 20 | 1.20 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<6222360698763564915ul>` |
| 21 | 1.08 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<5386084775297249833ul>` |
| 22 | 1.00 | CPU Thread | libbt4gsx.so | `nominum::core::addIndices` |
| 23 | 0.95 | CPU Thread | libbt4gsx.so | `live_next` |
| 24 | 0.87 | GameThread | libun5.so | `ps2x::iop::detail::IopEmulator::Impl::runCycles` |
| 25 | 0.82 | CPU Thread | libbt4gsx.so | `nominum::core::writeRegister` |
| 26 | 0.77 | GameThread | [kernel.kallsyms] | `[kernel.kallsyms][+ffffff90b0e836f4]` |
| 27 | 0.72 | GameThread | libun5.so | `adv_sub_007F0D90_0x7f0d90` |
| 28 | 0.71 | GameThread | libun5.so | `sub_00194530_0x194530` |
| 29 | 0.66 | GameThread | libc.so | `__aarch64_swp2_rel` |
| 30 | 0.64 | VuWorker | [kernel.kallsyms] | `[kernel.kallsyms][+ffffff90b0e836f4]` |

**Top 30 do processo, luta b:**

| # | % | thread | library | function |
|---|---|---|---|---|
| 1 | 3.13 | GameThread | libun5.so | `ps2x::iop::detail::IopCpuCore::executeInstruction` |
| 2 | 2.82 | GS | libbt4gsx.so | `GSState::GIFPackedRegHandlerXYZ2<4u, 0u, false>` |
| 3 | 2.79 | GS | libc.so | `__memcpy` |
| 4 | 2.34 | GameThread | libun5.so | `EeScheduler::checkpointDue` |
| 5 | 2.01 | GameThread | libun5.so | `isa_native::spu2Mix` |
| 6 | 1.96 | VuWorker | libun5.so | `nominum::core::Vif1::transfer` |
| 7 | 1.86 | GS | libbt4gsx.so | `GSRendererHW::Draw` |
| 8 | 1.81 | CPU Thread | libc.so | `__memcpy` |
| 9 | 1.76 | GameThread | libun5.so | `ps2x::iop::detail::IopEmulator::Impl::step` |
| 10 | 1.69 | CPU Thread | libbt4gsx.so | `nominum::core::kick` |
| 11 | 1.46 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::fastExecute` |
| 12 | 1.45 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<5386084775297249833ul>` |
| 13 | 1.35 | VuWorker | libc.so | `__memcpy` |
| 14 | 1.32 | GS | libbt4gsx.so | `GSRendererHW::ClearGSLocalMemory` |
| 15 | 1.16 | GS | vulkan.adreno.so | `!!!0000!4c1ccfa0a5caed359b512f9c9e680b!c095b0f6a5b!` |
| 16 | 1.12 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<934311555775441768ul>` |
| 17 | 1.12 | CPU Thread | libbt4gsx.so | `nominum::core::transfer` |
| 18 | 1.01 | GameThread | libun5.so | `ps2x::iop::detail::IopEmulator::Impl::runCycles` |
| 19 | 1.00 | GS | libbt4gsx.so | `GSState::Transfer<3>` |
| 20 | 1.00 | GameThread | libun5.so | `std::__wrap_iter<unsigned char*> std::vector<unsigned char, std::allocator<unsig` |
| 21 | 0.95 | GameThread | [kernel.kallsyms] | `[kernel.kallsyms][+ffffff90b0e836f4]` |
| 22 | 0.86 | CPU Thread | libbt4gsx.so | `nominum::core::hashOf` |
| 23 | 0.82 | GS | libbt4gsx.so | `GSVector4i GSVertexKickKernel::RunChunk<4u,` |
| 24 | 0.82 | GameThread | libun5.so | `sub_001D70F0_0x1d70f0` |
| 25 | 0.81 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<13465663943026878050ul>` |
| 26 | 0.79 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<7539251845881921587ul>` |
| 27 | 0.73 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<14222797883197560457ul>` |
| 28 | 0.72 | VuWorker | libun5.so | `nominum::vu1::VU1Interpreter::recBlock<7509493376599568744ul>` |
| 29 | 0.70 | GameThread | libun5.so | `sub_0019FEC0_0x19fec0` |
| 30 | 0.67 | VuWorker | libun5.so | `nominum::core::Vif1::transfer` |

**O que isso diz para os itens seguintes:**

- **Item 14 (NEON no MMI e no VU0 em modo macro):** as funções vetoriais do runtime (`ps2_vadd`, `ps2_vmul`,
  `ps2_vslow`, `ps2_fmul_slow`…) somam menos de 0,1% da GameThread nas duas cenas, e o NEON não tem onde ganhar ali.
  O que aparece é o `VU1Interpreter::fastExecute` rodando na GameThread (3,9% na campanha). São os micros chamados
  pela GameThread, a verificar se é o VU0 em modo micro (`VCALLMS`).
- **Item 15 (funções do SDK nativas):** nas duas cenas, o maior custo único fora do código do jogo é o
  **interpretador do IOP** (17–20% da GameThread), seguido de `EeScheduler::checkpointDue` (4%), `spu2Mix` (4%) e um
  `vector<unsigned char>::insert` (3,7%, de onde vem a ver). As funções do jogo da lista acima precisam ser
  identificadas (SDK ou jogo) antes de escolher a primeira.
- **VuWorker com b:** o espelho do estado a cada transferência (o `memcpy`, 5–6% da thread) é um custo do HL, não do
  Nominum. Copiar só o que mudou é uma medida a fazer.

### 2026-11-04 15:25, itens 8 e 9: a libsdr corrompe uma lista livre do heap no tempo dos filmes; o Ougi de um botão é mostrado pelo jogo

**Item 8, som dos logos e filmes (em andamento, sem correção):**

- **Build de laboratório:** `HL_SDR=1` (a libsdr do EE recompilada), em `build/lab/app_sdr.apk` (`libun5_sdr.so`).
  Rodou na rota v3 com o GS do ARMSX2 e `HL_RPC_LOG=80000701` (patch `0060`). O travamento de 11-03 volta igual nos
  dois aparelhos (`rec/sdr_rp5_v3`, `rec/sdr_odin209_v3`): o som existe até ~v10300 e zera, e há um `missing-target
  0xbd9c90` com `sp 0x20821380`.
- **O que o `sdrdrv` responde** (1403 chamadas):
  - Início: `0x8000` (init), `0x8050`, `0x8130`/`0x8131` e `0x8070`.
  - Depois, por filme: `0x80e0` (BlockTrans, inicia com 0x3000), `0x8010` (o volume, `0xf80`/`0x1080`) e `0x80d0`
    (VoiceTrans de 0x800). No meio, 1356 vezes, `0x8100`, que é o BlockTransStatus.
  - O status só volta dois valores, `0x00170fc0` e `0x01173fc0`: o início de cada metade de um anel de 0x6000. A conta do
    jogo sobre ele (`0x100ae0`) fica dentro do anel.
  - **Depois do último filme (v2662) não há mais nenhuma chamada ao `sdrdrv`.** O estrago é feito até ali e fica
    dormente até a próxima alocação passar pelo lugar.
- **Threads (`HL_THREAD_LOG`, `rec/sdr_odin209_v3t`):** cada filme tem um par de threads de áudio (33/34, 35/36, 37/38:
  `0x104020` e `0x101c00`), que a thread 8 termina depois do BlockTrans de parada. De v8035 a v10254 nada é criado. Em
  v10254 as tarefas 132–135 recebem a pilha `0x20820011`.
- **RAM no momento da falha (`HL_MISS_DUMP=1`, `rec/sdr_odin209_v3m/ram_miss.bin`):**
  - O heap do jogo tem as listas livres por tamanho em `0x618250` (pares next/prev; vazia aponta para si mesma).
  - A lista em **`0x618a50` contém `80852021 f9990000` em vez de `00618a50 00618a50`**. O mesmo `0x852021` está no `a2`
    da falha e em `0xb6fcc0`.
  - As 4 tarefas (`0xbd8de0`, `0xbd9b70`…, função em +0xc e tamanho em +0x18) têm `0x20820011` como pilha em +0x1c,
    que é o que o alocador `0x117c70` devolveu.
- **Próximo passo, em andamento:** quem escreve em `0x618a50`. Build de laboratório com vigia de escrita:
  - patch `0061` (o `ps2TraceGuestWrite`, que o runtime já chama em toda escrita do jogo, nos `memcpy`/`memset` e nas
    cópias do IOP, ganha um corpo atrás de `-DHL_WATCH_BUILD`);
  - `host/hl_watch.cpp`, ativado com `HL_WATCH=618a50`;
  - `HL_WATCH_BUILD=1 recomp/android/build.sh`, em `build/android_watch`.

  A build normal não muda.

**Item 9, Ougi com um botão (sem código novo):**

- Pela gravação `rec/n3_odin_v3_c`, o próprio jogo mostra o Ougi: "Press!" com a barra entre 1P e COM, a coluna de
  botões de cada lado com a contagem, e no fim **"OK" para o 1P e "ERROR" para a COM**. O "ERROR" anotado em 11-03 é a
  COM perdendo, não o botão único falhando. A tela de estado dizia "responde sozinho em segundo plano". Hoje só há
  resposta quando o botão especial é tocado (`g_padSpecialDown`), e o jogo mostra o pedido.
- **Multiplicador (`mash_percent`, Cube, `rec/sp_cube_x1` e `sp_cube_x2`):** a mesma rota, com o ○ a cada 12 vsyncs,
  dá 88 respostas a 1x e 176 a 2x (cada toque vale duas). O Ougi termina nos dois.
- **Não visto:** o Ougi pela sequência longa feita à mão (△△ e ○ com a opção ligada) e a disputa de dois Ougis ao mesmo
  tempo (`+0xB00`).



### 2026-11-04 15:00, rodada 3 do Nominum (`b2ff28b`), itens 1.2–1.7: a VU1 do Nominum dá 50 fps na campanha do RP5 em sd480p

APK: `libun5.so` `0e8e877` (o gancho `NOMINUM_VU1`) e a `libbt4gsx.so` do Nominum `b2ff28b`. As combinações:

- **a:** `NOMINUM_BACKEND=nominum`;
- **b:** a, mais `NOMINUM_VU1=1`;
- **c:** o GS do ARMSX2, sem as duas.

No RP5 com o driver da Qualcomm, a tabela do Nominum manda a e b para o ARMSX2. Lá, a e c são o mesmo caminho e b é o ARMSX2 com a VU1 do Nominum. **O RP5 ficou em `sd480p` em todas as rodadas**, inclusive as do Turnip (`tools/device-profile.sh limit sd480p`, com `HL_STOP_PERF=1`; continua ligado). Rotas `rec/n3_*`: a v3 (até v12300) e a campanha (até v11900), com `HL_GS_SCALE=1` e capturas.

**1.2 Autoteste (zips em `build/lab/selftest3/`):**

| Aparelho | Escolha | Zip |
|---|---|---|
| Odin 3 (Adreno 830, Qualcomm) | Nominum, sem alternativas | `odin3.zip` |
| Cube (Mali-G57) | Nominum; a tabela liga `no-dual-source` | `cube.zip` |
| RP5, Qualcomm 0746, pela tabela | ARMSX2 (a entrada do `DEVICE_LOST`), como esperado | `rp5_qualcomm_table.zip` |
| RP5, Qualcomm, `NOMINUM_SELFTEST=force` + `IGNORE_TABLE=1` | **passa: Nominum, sem alternativas.** Os 15 casos do teste não pegam o `DEVICE_LOST` da campanha | `rp5_qualcomm_forced.zip` |
| RP5, Turnip v26.3.0-R6 | Nominum, sem alternativas | `rp5_turnip.zip` |

**1.3 fps (média do trecho) e VU1 (ms por vsync) do `HL_PERF`, a / b / c:**

| Trecho | Odin 3 | RP5 sd480p | Cube |
|---|---|---|---|
| Luta 1 | 50 / 50 / 50; VU1 3,3 / 2,5 / 3,3 | 49,8 / 50,2 / 49,9; 7,3 / 5,9 / 7,2 | 49,6 / 48,4 / 50; 7,2 / 5,7 / 7,0 |
| Ougi | 50 / 50 / 50; 4,2 / 3,0 / 4,2 | 50 / 50 / 50; 8,8 / 6,6 / 8,8 | 38,2 / 38,1 / 50; 9,7 / 7,7 / 8,3 |
| Vitória | 50 / 50 / 50; 2,2 / 1,7 / 2,3 | 50 / 50 / 50; 4,9 / 3,6 / 4,4 | 38,7 / 38,6 / 50; 5,2 / 4,3 / 5,3 |
| Resultado | 50 / 50 / 50 | 50 / 50 / 50 | 48,4 / 48,5 / 50 |
| Luta 2 | 50 / 50 / 50; 5,6 / 4,4 / 5,7 | 50 / 50 / 49,6; 13,5 / 10,2 / 12,0 | 47,7 / 41,0 / 50; 12,6 / 9,6 / 12,9 |
| Campanha, vila | 50 / 50 / 50; 6,9 / 5,2 / 6,7 | **43,6 / 50,0 / 43,9; 17,7 / 12,4 / 17,4** | 49,4 / 43,1 / 49,5; 15,3 / 11,7 / 15,1 |
| Campanha, andando | 49,9 / 49,9 / 50 | **44,5 / 50,0 / 44,6** | 45,9 / 40,1 / 49,2 |
| Campanha, menu START | 50 / 50 / 50; 8,4 / 6,5 / 7,9 | **36,7 / 47,8 / 36,6; 23,3 / 16,7 / 23,4** | 42,2 / 35,7 / 42,9 |
| Logos (carregamento) | 42,7 / 42,6 / 41,3 | 28,8 / 28,8 / 28,9 | não medido |
| Filme | 50 / 50 / 50 | 50 / 50 / 50 | não medido |

- **A meta de 50 fps com b na campanha em sd480p foi alcançada** na vila e andando. O menu START fica em 47,8. A VU1
  do Nominum (recompilada pelo catálogo) custa de 25 a 30% menos que o caminho rápido do runtime nos três aparelhos.
- **Cube, campanha, b pior que a, nas duas vezes (`n3_cube_camp_b` e `_b2`: vila 43,1 e 43,4 contra 49,4):** com b, a
  VU1 cai de 15,3 para 11,7 ms, mas a thread do GS do Nominum sobe de 10,9 para 15,6 ms. O fluxo é do mesmo tamanho
  (3,2 MB por quadro nos dois). **São os blocos recompilados:** b com `NOMINUM_VU1_REC=0` (`n3_cube_camp_bnorec`, a VU1 do
  Nominum interpretada) volta a vila 49,5 / andando 46,1 / menu 42,4, com GS 10,8 ms e VU1 14,9 (igual a a). Com os
  blocos, a VU1 fica mais barata, mas a thread do GS do Nominum perde ~5 ms por quadro no Cube. Suspeita: o código
  recompilado (1196 blocos) disputa o cache com a thread do GS. No Odin a mesma cena dá GS 4,0 em a e b, e no RP5
  (ARMSX2) b é mais rápido. Para a sessão do Nominum.
- **Os logos e filmes do Cube e a repetição `n3_cube_camp_a2` não foram feitos.** O Claude Code parou essa fila às
  14:41 por falta de memória no PC: a build da libsdr e a normal compilavam juntas. Ficam para a próxima rodada.
- Os segundos abaixo de 10 fps são os mesmos em a, b e c: o EE leva ~236 ms por quadro no carregamento dos logos (RP5,
  v1573–1603 e v1866–1881).

**1.4 Rota com b:**

- Odin 3 em 16:9 e 20:9 (`wm size 2400x1080`, `n3_odin209_*`): v3 e campanha inteiras a 50 fps, com **0 segundos
  travados** e uma só swapchain (2400x1080 no 20:9).
- RP5 e Cube: as rotas inteiras, com o mesmo número de segundos lentos de a e c (só os do carregamento).
- Imagem: as capturas de a e b nas mesmas cenas (título, seleção de fase, luta, Ougi, Ninja Song/WINNER, seleção de
  personagem) mostram o mesmo conteúdo, sem defeito à vista (`rec/n3_odin_v3_{a,b,c}/shot_*.png`). Não comparei pixel
  a pixel, porque a rota não cai no mesmo vsync.
- `[nvu1]` no log de todas as rodadas b.

**1.5 Odin, tela girada e painel a 30 fps (`n3_odin_rot_b`):**

- Gira para 270 em v2231 e volta para 90 em v4847. São refeitas só as swapchains do giro (turn 90 → 0 → 270 → 90), e a
  imagem fica em pé nas duas (`rot270_v2231.png`, `rot90_v4847.png`).
- No vídeo: a 30 do painel, 26–28 quadros por segundo; a 50, 50–54, igual à rodada 2. As quedas para 3 fps em v6928 e
  v8842 são o jogo pausado com o painel aberto.

**1.6 RP5 com o Turnip (`n3_rp5t_*`, sd480p):**

- Com b (backend do Nominum mais a VU1): v3 41,3 / 43,1 / 36,7 / 45,0 / 50,0 (luta 1 / Ougi / vitória / resultado /
  luta 2); campanha 27,3 / 26,9 / 21,5. O limite é a thread do GS do Nominum (26–35 ms), não a VU1.
- O ARMSX2 com o Turnip (`n3_rp5t_v3_c`) fica a 50 em todos os trechos.
- Com o driver da Qualcomm a tabela vai para o ARMSX2, como esperado. O `files/drivers/selected` foi apagado no fim.

**1.7 Dumps do VIF1 em 4:3:** `HL_ASPECT=1333`, a câmera da luta sem o alargamento (`build/lab/hl_env_vif43_*.txt`,
`vif43q.sh`). As **13 cenas** estão em `~/Downloads/vifdumps/odin3_43_<cena>.{vif,gif}.zst` (1,3 GB), e `tools/vifdump.py info` dá
`ok` em todas.

**Outros:**

- **Corrigido:** o `0058` (`HL_VIF_DUMP`) não compilava no PC. O `#include "runtime/hl_vif_dump.h"` do `ps2_runtime.cpp`
  estava dentro do `#if PS2X_HEADLESS_HOST`. No Android, o unity build escondia o erro. O runner do PC volta a linkar.
- **Novo, `0060-rpc-log-sid.patch`:** `HL_RPC_LOG=<sid em hex>` registra só aquele serviço, com o buffer de resposta
  depois da chamada. É para o item 8.
- A ISO da Alpha 1.1X está em `~/Downloads/Naruto UN5/`, não só no Odin, como dizia o `CLAUDE.md`.

### 2026-11-04 14:20, modo contínuo, item 1.1: `NOMINUM_VU1=1`, o VIF1 e a VU1 do Nominum `b2ff28b` no lugar dos do runtime

**O pedido das 13:47 (modo contínuo, 16 itens em três frentes):** a rodada 3 do Nominum (itens 1–7), a Frente A (8–12) e
o EE (13–16), em ordem, sem parar para perguntar, com relatório às 13:00 e às 19:00. O de 13:00 já tinha passado quando
o pedido chegou; o primeiro será o de 19:00.

- **Decisão (o mais seguro, sem mudar o Nominum):** o `docs/integracao.md` do Nominum manda o VIF1 para o
  `nominum_submit_vif1` dentro da `libbt4gsx.so` por um tipo novo de pacote no anel, "a definir junto". Esse tipo não
  existe nos dois lados, e o cabeçalho do anel tem de ser igual nos dois. Por isso o Hidden Leaf compila o **mesmo código
  do Nominum** (`core/vif1.cpp`, a VU1 em `core/vu1/` e o catálogo de blocos recompilados do `nominum-vurec`) e o roda
  na `VuWorker`, onde rodava o `processVIF1Data` do runtime. Os pacotes GIF que ele solta (PATH1 e PATH2) vão ao anel
  como antes, e a `libbt4gsx.so` não muda. Quando o tipo do anel existir, é trocar o gancho.
- **Código:**
  - `recomp/patches/0059-nominum-vu1-hook.patch`: `runtime/hl_nominum_vu1.h` (um ponteiro de função) e o desvio no
    começo do `processVIF1Data`, depois do gancho do `HL_VIF_DUMP`. Desligado, custa um teste de ponteiro. A série
    inteira (0001–0059) aplica limpa sobre o `e563698`.
  - `recomp/android/host/hl_nominum_vu1.cpp`: o `Vif1` do Nominum. Antes de cada transferência entram a micro memória
    (só se mudou), os dados, os 23 registradores do VIF1, a dívida de DIRECT/IMAGE e a máscara do PATH3. Depois da
    transferência, tudo volta. O custo é de ~32 KB de cópia por transferência (~0,5 transferência por vsync).
  - `recomp/android/CMakeLists.txt` e `build.sh`: a biblioteca `nominum_vu1` vem de `build/nominum-<commit>`, um
    `git archive` do repositório do Nominum, com `NOMINUM_COMMIT=b2ff28b` por padrão. O catálogo vem de
    `build/nominum-<commit>/build/vu1rec`: o `nominum-vurec` gerou-o sobre `~/Downloads/vifdumps`, com 53 imagens de
    código, 1196 blocos e 296 diferentes. É código do jogo e fica fora do git. Sem ele, a VU1 do Nominum roda
    interpretada.
  - No PC, `nominum-vifreplay luta_inicio` deu "igual bit a bit" com 2,475 ms de VU1 por vsync recompilado, contra
    4,133 do caminho rápido.
- **Limites:** os registradores da VU1 (vf/vi/acc) não são espelhados entre as duas VU1. Só importam se o EE ler um
  registrador da VU1 (pelo que se vê, o UN5 não faz isso). O Nominum também não
  reproduz o pipeline de XGKICK pela metade entre duas transferências. Os dois ficam anotados aqui.
- **APK:** `libun5.so` com o gancho e a `libbt4gsx.so` do Nominum `b2ff28b` (`build/lab/gsx_b2ff28b/`, sha1
  `0451129d…`; a do HL de antes em `build/lab/libbt4gsx_hl_bb0ee18.so`). Ela foi instalada nos três aparelhos.
- **Primeiro teste (Odin 3, `n3_odin_v3_cb`, GS do ARMSX2 com `NOMINUM_VU1=1`):** o log mostra `[nvu1] Nominum's VIF1
  and VU1 (core/vif1, recompiled blocks where the catalog has them)`, e até v12800 a contagem sobe sem travar (9000
  transferências, 5,4 milhões de kicks, 12,3 s de VU1). A luta ficou a **50 fps**, com VU1 4,5 ms e idle 13,5 ms no
  `HL_PERF`. Faltam a imagem (gravação a conferir) e a matriz completa (itens 1.2–1.4, em andamento nos três aparelhos).

### 2026-11-04 08:25, `HL_VIF_DUMP`: o VIF1 e o GIF das 13 cenas para a Fase 3 do Nominum (prontos em `~/Downloads/vifdumps/`)

**O pedido colado às 07:45 (três partes: o urgente dos filmes e do Ougi, a Fase 2E e a Fase 3)** é quase todo código do Nominum: o upload em anel, o autoteste de GPU, o `nominum_submit_vif1` e o recompilador da VU1. Esta sessão só lê o repositório do Nominum. O próprio pedido trata a sessão do Hidden Leaf na terceira pessoa ("gravados pela sessão do Hidden Leaf", "leia o registro do Hidden Leaf"). Na dúvida, fiz o mais seguro, como o pedido manda, e anoto aqui:
- **Feito aqui:** o que o pedido dá ao Hidden Leaf, os dumps do item 3.2, e o diagnóstico daqui para o item 1.4.
- **Não mexi** no Nominum, nem nas partes 1, 2 e nos itens 3.1 e 3.3–3.6. Ficam para a sessão do Nominum.

**Para a sessão do Nominum, item 1.4 (o diagnóstico do Hidden Leaf):** as travas de filme, Ougi e vitória com o backend próprio são esperas da thread do GS, não CPU nem VU1. O rastro não estava ligado nessas rodadas, só na `n2_rp5_lost`. Pelo `HL_PERF` da rodada 2 (`rec/n2_*`), "GS ocupado" é a CPU da thread do backend sobre o tempo do vsync, e `waitGS` é o jogo esperando o anel do GS, em ms por vsync:

| Aparelho, trecho (backend próprio, 1x) | fps | ms por vsync | GS ocupado | `waitGS` | VU1 |
|---|---|---|---|---|---|
| Cube, filme de abertura | 23,2 | 43,2 | 12,0 ms (28%) | 41,9 | 4,2 |
| Cube, Ougi | 9,5 | 105,3 | 19,3 ms (18%) | 112,8 | 11,1 |
| Cube, vitória | 23,7 | 42,1 | 12,1 ms (29%) | 55,0 | 5,6 |
| RP5 (Turnip), Ougi | 30,7 | 32,5 | 7,5 ms (23%) | 28,2 | 9,1 |
| RP5 (Turnip), vitória | 38,3 | 26,1 | 9,2 ms (35%) | 17,7 | 6,2 |
| Odin, filme | 46,3 | 21,6 | 6,6 ms (30%) | 19,1 | 1,6 |
| Odin, Ougi | 30,4 | 32,8 | 10,1 ms (31%) | 30,7 | 4,5 |
| RP5 (Turnip), campanha, vila | 29,8 | 33,5 | 26,4 ms (79%) | 19,6 | 20,8 |
| Cube, ARMSX2, Ougi (referência) | 50,0 | 20,0 | 7,7 ms (39%) | 0,0 | 8,3 |

- **No filme, no Ougi e na vitória**, o jogo espera o GS quase o vsync inteiro, e a thread do backend fica 70–80% do tempo parada, sem CPU. Isso bate com a suspeita da parte 1: uma espera da GPU por transferência IMAGE ou por ReadFIFO2. Os filmes são só PATH2 IMAGE: nos dumps, `filme` tem 540 891 pacotes PATH2, ~1 MB por vsync, sem VU1. O Ougi tem 296 741 pacotes (209 030 do PATH1), nenhum de PATH3.
- **A exceção é a campanha no RP5:** ali a thread do GS fica 79% ocupada, então é CPU.
- **O RP5 com o driver do sistema** cai antes disso (`DEVICE_LOST`, entrada de 07:50, item 1).

**Correção da entrada de 07:50:** lá eu disse que, no Cube, "o limite é a CPU (GS 19 ms por vsync)". Está errado. O `HL_PERF` dá CPU por vsync, e a 9,5 fps cada vsync leva 105 ms: a thread do GS fica ocupada 18% e espera o resto (tabela acima). O 30 fps não ajuda no Cube porque a espera é por transferência, não por present.

**Os dumps (item 3.2 do pedido):** são 13 pares `odin3_<cena>.vif.zst` e `.gif.zst`, 1,3 GB comprimidos, em `~/Downloads/vifdumps/`, fora do git.
- **Gravação:** gravados no Odin 3 em 16:9 com a build de 07:47, pelas rotas `hl_env_vif_v3.txt`, `hl_env_vif_cpu2.txt` (`ougi_cpu`, v6619) e `hl_env_vif_camp.txt`, com os mesmos gatilhos e vsyncs dos dumps do GS. Cada um tem 300 vsyncs.
- **Conferência:** o `tools/vifdump.py info` lê todos sem erro. As sequências crescem, não há número nos dois arquivos, e cada par tem um `STAT` e o fim. O resumo está em `build/lab/vifdump_info.txt`.
- **O que eles mostram:**
  - Logos e filme são só DIRECT/DIRECTHL (PATH2) e MPG, sem MSCAL.
  - As outras cenas: o jogo manda uma transferência VIF1 por par de vsyncs (~149 em 300), de 0,3 a 2,9 MB por vsync crus. Por exemplo, a `campanha_vila` tem 113 042 MSCAL, 311 324 MSCNT, 1 935 181 UNPACK, 586 035 STMASK e 170 662 DIRECT.
  - Nenhum PATH3, nenhum MSCALF, e nenhum PATH1/2 fora de uma transferência VIF1: a VU1 só é ligada pelo VIF1.
  - **Os microprogramas:** o jogo os reenvia por MPG o tempo todo, milhares de vezes por dump, mas nas 13 cenas só há **24 pedaços distintos** (tamanho e CRC32). O CRC da micro memória muda de 0 a 10 vezes por dump, entre os vsyncs.
- **Só no 16:9:** a câmera larga muda os números que vão à VU1 na luta e na campanha (x 0,75). Um 4:3 (Cube, ou `HL_ASPECT=1333`) sairia diferente.
- **Custo:** gravando, o jogo fica mais lento, mas a rota é por vsync e as cenas caem nos mesmos pontos dos dumps do GS.

**Código (Hidden Leaf):**
- `recomp/patches/0058-vif1-gif-dump.patch`: o `runtime/hl_vif_dump.{h,cpp}` novo e os ganchos no `processVIF1Data`, no `submitGifPacket`, no `writeRegister` e no `vsyncCommit` do `hl_gs_live`, mais o `bind` no `PS2Runtime`. Ele entra no `CMakeLists.txt` do runtime do PC e no `recomp/android/CMakeLists.txt`. A série inteira (0001–0058) aplica numa árvore limpa do `e563698` e dá os mesmos arquivos da build. No PC, só passou por `g++ -fsyntax-only`; o runtime do PC não foi recompilado.
- `hl_game.cpp`: o `HL_VIF_DUMP` (e o `HL_VIF_DUMP_FRAMES`) usa a lista de cenas do `HL_GS_DUMP`. `un5_main.cpp`: a pasta `vifdumps/`.
- `tools/vifdump.py`: `info`, o leitor e conferidor. `docs/vifdump.md`: o formato.
- Desligado, cada gancho custa uma leitura atômica.
- No laboratório: `build/lab/vifdrun.sh` puxa cada dump quando termina e o apaga do aparelho, porque o Odin tem 12–19 GB livres e um dump cru tem até 1,8 GB.

**Aparelhos:** o APK com este `libun5.so` e o `libbt4gsx.so` do Hidden Leaf (`bb0ee18`) está nos três, sem `hl_env.txt`. A cópia do `libun5.so` anterior está em `build/lab/libun5_7973998.so`.

**Não feito:** as partes 1 e 2 e os itens 3.1 e 3.3–3.6, que são código do Nominum (acima). Também ficaram de fora os dumps em 4:3 e em outros aparelhos, e a recompilação do runtime do PC.

### 2026-11-04 07:50, teste do backend próprio do Nominum no aparelho, rodada 2 (Nominum `12fb79b`, para a sessão do Nominum)

**Para a sessão do Nominum:** o `12fb79b` sobe nos três aparelhos, e o Cube não volta mais ao ARMSX2. A swapchain não se refaz mais a cada vsync, e o Ougi do Odin passou de 21,5 para 30,4 fps. Mas sobram quatro defeitos, por ordem de gravidade. Nada no código do Nominum foi mudado.

1. **RP5 com o driver do sistema: `VK_ERROR_DEVICE_LOST` continua, no mesmo ponto.** Caiu em v3737 (com `NOMINUM_VK_LOST_TRACE=1`) e em v3691 (sem ele), logo depois do NOW LOADING que vem depois de "New Game", como nas três da rodada 1. Não é a swapchain: até ali ela foi feita só 2 vezes.
   - O driver é o Adreno 650 `0746.0`, de 09/27/23. Nas duas quedas, ele escreve 4 vezes `W/Adreno-GSL: <log_gpu_snapshot:471>: panel.gpuSnapshotPath is not set.not generating user snapshot` nos ~130–270 ms antes da falha: o driver do kernel viu uma falha ou trava da GPU.
   - A chamada que falha é de novo o `vkQueueSubmit(m_queue, 1, &si, m_fence) failed (-4)`. A pilha: `fail` ← `NominumBackend::presentToWindow()+4844` ← `pcrtc(...)+2120` ← `vsync(...)+172` ← `nominum_vsync+116` ← `host::runOwn+2984` ← `Bt4Gsx_Run+612`, na thread "CPU Thread".
   - **O rastro (`NOMINUM_VK_LOST_TRACE`, os 64 últimos comandos, do mais velho ao mais novo):** de `1365847` a `1365908`, 62 comandos alternam entre `dispatch clut 1x1, push 13084 0 0 0 0 0 0 0, buffer +0/0` e `dispatch upload 4x1, push 13072 2 20 0 0 32 0 1024, buffer +0/512`. Os dois últimos são `1365909: draw 32484: key 3f0120e count 6 z 1e00/31 frame 0/8/0 tex 0/0/0 1x1 scissor 0,0 512x448` e `1365910: dispatch upload 512x1, push 14848 4 19 0 0 256 0 131072, buffer +0/131072`.
   - Com o rastro, a rota não ficou muito mais lenta até ali (v3737 em 164 s).
   - Os logs inteiros estão em `build/lab/rec/n2_rp5_lost/` (`log.txt`, o `un5_log`; `logcat_all.txt`, o logcat das duas quedas) e em `n2_rp5_v3_s1/`.
   - A tela logo antes mostra uma faixa branca no meio (a imagem achatada do item 2).
   - **Com o Turnip v26.3.0-R6 (adrenotools) não cai.** O log diz `nominum-vk: custom driver libvulkan_freedreno.so (adrenotools)` e `nominum-vk: Turnip Adreno (TM) 650`. Ele fez a rota v3 até v12405, a campanha até v11981 e todas as escalas, sem `DEVICE_LOST`. Ou o defeito é do driver da Qualcomm, ou de algo que só esse driver não tolera. O resto da rodada no RP5 foi com o Turnip.
2. **A pré-rotação achata a imagem (novo no `12fb79b`).** Com o giro de 90 ou 270 (RP5 e Odin em 16:9; a tela física é 1080x1920), o log diz `swapchain 1920x1080, ... turn 90`. A imagem do jogo ocupa só ~31% da altura, centrada e de largura inteira: (1080/1920)² = 0,316. A orientação está certa: em pé a 90 e a 270 no Odin. Pela proporção, a imagem da swapchain não troca largura e altura no giro de 90/270, e o compositor espreme os 1920x1080 na tela girada.
   - Está assim na rota inteira: menus, luta pelo nativo, Ougi, a imagem atrás do painel de pausa. Nas gravações e nas capturas (`rec/n2_odin_rot/*.png`, `rec/n2_rp5_turnip_rot/*.png`).
   - Certo no Odin em 20:9 (`wm size 2400x1080`, `turn 0`) e no Cube (720x720, `turn 90`; quadrado, então não espreme).
   - **Com `NOMINUM_VK_PRETRANSFORM=0` a imagem sai inteira e certa**, a 90 e a 270 (Odin, `rec/n2_odin_pt0`), com 3 swapchains, todas `turn 0`.
3. **Fora do nativo, o backend próprio ainda é mais lento que o ARMSX2, e no Cube e no RP5 bem mais.** Tabelas abaixo.
   - **Odin:** o Ougi subiu de 21,5 para 30,4 fps (GS de 25,9 para 10,1 ms), a vitória de 37,5 para 44,3, e o resultado chega a 50. O ARMSX2 dá 50 nos três.
   - **O gargalo não é mais a CPU:** com o painel em 30 fps, o jogo no Ougi sobe para 39,1 fps e a vitória para 50,0 (RP5: 34,1 e 40,2). Com menos presents, o jogo anda mais rápido. O que segura é o envio à GPU e o present FIFO, não a frente do GS. A thread do GS gasta ~10 ms de CPU por vsync no Ougi com `idle` 0.
   - **Cube (Mali-G57, sem `dualSrcBlend`):** a 1x, luta 46 fps, Ougi 9,5, vitória 23,7, campanha 40–49. O ARMSX2 da rodada 1 dá 50 em tudo na luta e 43–50 na campanha.
   - **RP5 (Turnip):** a 1x, luta 1 a 41,6 fps, luta 2 a 30,2, Ougi 30,7, vitória 38,3, campanha 23–30 (GS 26–35 ms). O **ARMSX2 com o mesmo Turnip** fica a 50,0 em todos os trechos da luta e da campanha. Então é o backend, não o driver.
4. **O upscale funciona, mas o tempo de quadro cai muito no Cube e no RP5.** Tabela abaixo.
   - Odin: a luta e a campanha ficam a 48–50 até 6x. Só o Ougi cai, de 30 a 20.
   - RP5: a luta cai de 42 a 20 fps de 1x a 6x, a campanha de 30 a 17.
   - Cube: a 4x, o Ougi fica a 2,5 fps; a 6x, a 1,4, e a rota não passou de v8333 em 25 min.
   - A memória cresce menos que a do ARMSX2: no Odin a 6x, 583 MiB contra 987 MiB.

**Build:** o `libbt4gsx.so` do Nominum `12fb79b` saiu do `tools/build_android.sh` de lá, com `NOMINUM_BUILD=build/gsx-nominum` (150 passos sobre a pasta da rodada 1). O stripped tem 34 740 016 bytes (sha1 `47f881cb…`); a cópia fica em `build/lab/libbt4gsx_nominum_12fb79b.so`. Ele foi para `jniLibs` num APK de teste, instalado nos três aparelhos; a integração por submódulo continua sem ser feita. Os dois backends são o mesmo binário: o ARMSX2 é o padrão, e `NOMINUM_BACKEND=nominum` no `hl_env.txt` liga o próprio. O `libun5.so` é o mesmo da rodada 1, e nenhum código do Hidden Leaf mudou nesta rodada. No fim, o APK com o `libbt4gsx.so` do Hidden Leaf (`bb0ee18`) voltou aos três. No RP5, o driver voltou ao do sistema (`files/drivers/selected` apagado; antes não havia nenhum escolhido), e a rotação dos dois Adreno voltou ao que era (RP5: `force_landscape=1`, `user_rotation=1`; Odin: `user_rotation=0`, `wm size` resetado).

**Como medi (ferramentas de `build/lab`, ignoradas pelo git):**
- `recfull2.sh` é o `recfull.sh` com três acréscimos:
  - A memória da GPU a cada 10 s. No Adreno, `/sys/class/kgsl/kgsl/proc/<pid>/kernel` e `kernel_max` (o pico; o `user` fica 0). No Mali, `dumpsys gpu --gpumem` (a linha `Proc <pid> total`, amostrada; o pico é a maior amostra).
  - Ações por vsync (`ACTS=V:rot270|rot90|fps30|fps50|shot`): o giro (`wm fixed-to-user-rotation enabled` + `cmd window user-rotation lock 1|3`) e o limite de fps pelo painel (voltar, tocar "30" ou "50 (nativo)", Continuar).
  - A checagem de "jogo morto" com 3 tentativas.
- `q2.sh` roda uma fila de rodadas por aparelho, e `n2sum.sh` faz o resumo de cada uma.
- As envs são `hl_env_n2_{v3,camp}_s{1,2,4,6}.txt`, `hl_env_n2_v3_lost.txt` e `hl_env_n2_v3_pt0.txt`. Do ARMSX2 são `hl_env_nmn_{v3,camp}_arm.txt` e `hl_env_n2_v3_arm_s{4,6}.txt`.
- Os trechos (por vsync) são: luta 1 6450–6900, Ougi 7000–7900, vitória 8000–8950, resultado 8950–9450 (conferido nas folhas por vsync: WINNER até ~8950, "Ninja Song" até ~9450), luta 2 11200–12100, vila 6300–8000, andar 8100–10500 e menu START 10550–11800. As rodadas de escala param em v9100 (a v3) e v11800 (a campanha).
- **Rodadas refeitas por erro meu:** o primeiro `n2_odin_v3_s2` e os primeiros `n2_odin_v3_s4`, `n2_odin_rot`, `n2_odin_pt0` e `n2_odin_v3_s6` rodaram com duas filas ao mesmo tempo no Odin. Uma espera procurou o "done" no arquivo errado, e o `force-stop` de uma rodada matou a outra (o "GAME DIED at v4402" do 2x). Ficaram em `rec/*_invalid_overlap`, fora das tabelas, e todas foram refeitas sozinhas.

**Item 1 do pedido, o RP5 com `NOMINUM_VK_LOST_TRACE=1`:** o item 1 acima.

**Item 2 do pedido, 1x (fps / GS em ms por vsync), rodada 2 contra a rodada 1:**

| Trecho | Odin 16:9 próprio r1 → r2 | Odin 20:9 próprio r1 → r2 | Odin ARMSX2 (r1) | Cube próprio r2 | Cube ARMSX2 (r1) | RP5 próprio, Turnip (r2) | RP5 ARMSX2, Turnip (r2) | RP5 ARMSX2, sistema (r1) |
|---|---|---|---|---|---|---|---|---|
| luta 1, nativo | 50,0 / 3,9 → 50,0 / 2,5 | 50,0 / 2,9 → 50,0 / 2,6 | 50,0 / 2,6 | 46,4 / 10,2 | 50,0 / 6,9 | 41,6 / 16,4 | 50,0 / 8,4 | 50,2 / 7,8 |
| Ougi, pelo GS | 21,5 / 25,9 → **30,4 / 10,1** | 22,5 / 24,8 → **29,6 / 10,3** | 50,0 / 4,3 | **9,5 / 19,3** | 50,0 / 7,6 | **30,7 / 7,5** | 50,0 / 5,0 | 50,0 / 5,2 |
| vitória | 37,5 / 10,2 → **44,3 / 4,2** | 37,2 / 10,2 → **44,4 / 3,8** | 50,0 / 3,1 | **23,7 / 12,1** | 50,0 / 7,4 | **38,3 / 9,2** | 50,0 / 5,9 | 50,0 / 5,9 |
| resultado | 47,8 / 9,0 → 50,0 / 2,0 | 48,6 / 8,5 → 50,0 / 1,9 | 50,0 / 1,8 | 38,8 / 4,5 | 50,0 / 5,2 | 50,0 / 2,0 | 50,0 / 3,4 | 50,0 / 3,3 |
| luta 2 | 50,0 / 5,2 → 50,0 / 3,9 | 50,0 / 4,3 → 50,0 / 4,0 | 50,0 / 4,0 | 46,1 / 13,9 | 50,0 / 10,4 | **30,2 / 26,7** | 50,0 / 12,0 | 50,0 / 10,4 |
| campanha, vila | 49,6 / 6,0 → 50,0 / 3,9 | | 50,0 / 3,9 | 48,7 / 10,8 | 49,7 / 10,6 | **29,8 / 26,4** | 50,0 / 11,4 | 50,0 / 10,8 |
| campanha, andar | 46,8 / 8,2 → 49,6 / 4,3 | | 49,1 / 4,1 | 43,0 / 14,0 | 48,2 / 12,1 | **28,7 / 28,1** | 50,0 / 11,7 | 49,1 / 11,4 |
| campanha, menu START | 49,1 / 7,6 → 50,0 / 5,2 | | 50,0 / 5,1 | 40,2 / 15,4 | 43,1 / 14,1 | **23,4 / 35,4** | 50,0 / 13,7 | 50,0 / 13,2 |

(No Odin r2, a campanha a 1x é a `n2_odin_camp_s1`. Com o backend próprio, a coluna GS é o tempo da thread "CPU Thread" só; com o ARMSX2, é a soma de "GS" e "CPU Thread", como na rodada 1. Rodada 1 em `rec/nmn_*`, rodada 2 em `rec/n2_*`.)

Na rodada 2, a rota v3 do Odin 16:9 com o backend próprio levou ~230 s até v12887, contra 411 s na rodada 1: o `HL_FAST_UNTIL` voltou a correr (a 60–120 fps no vídeo até v6400).

**Item 3 do pedido, upscale (fps no trecho / memória da GPU no pico, MiB; v3 até v9100, campanha até v11800):**

| Aparelho, backend | Escala | luta 1 | Ougi | vitória | campanha (vila / andar / menu START) | memória da GPU no pico |
|---|---|---|---|---|---|---|
| Odin 16:9, próprio | 1x | 50,0 | 30,4 | 44,3 | 50,0 / 49,6 / 50,0 | 237 (v3), 241 (campanha) |
| | 2x | 50,0 | 27,9 | 42,9 | 50,0 / 49,4 / 49,9 | 266, 263 |
| | 4x | 50,0 | 25,2 | 42,1 | 50,0 / 48,9 / 49,6 | 385, 353 |
| | 6x | 49,9 | 19,9 | 37,8 | 49,8 / 48,2 / 49,2 | **583**, 502 |
| Odin 16:9, ARMSX2 | 1x | 50,0 | 50,0 | 50,0 | | 187 |
| | 4x | 50,0 | 50,0 | 50,0 | | 537 |
| | 6x | 50,0 | 50,0 | 50,0 | | **987** |
| RP5, próprio, Turnip | 1x | 41,6 | 30,7 | 38,3 | 29,8 / 28,7 / 23,4 | 220, 217 |
| | 2x | 39,5 | 27,2 | 37,5 | 26,6 / 26,2 / 20,9 | 249, 240 |
| | 4x | 27,9 | 18,1 | 29,9 | 21,4 / 20,6 / 16,8 | 367, 329 |
| | 6x | 20,2 | 12,1 | 23,0 | 17,6 / 16,8 / 14,0 | **564**, 477 |
| RP5, ARMSX2, Turnip | 1x | 50,0 | 50,0 | 50,0 | 50,0 / 50,0 / 50,0 | 276, 289 |
| Cube, próprio | 1x | 46,4 | 9,5 | 23,7 | 48,7 / 43,0 / 40,2 | 317, 327 |
| | 2x | 39,7 | 5,9 | 20,4 | 33,1 / 28,6 / 25,3 | 356, 352 |
| | 4x | 17,9 | 2,5 | 10,1 | 18,1 / 14,7 / 13,7 | 487, 443 |
| | 6x | 9,8 | 1,4 | 9,0 (até v8333, 25 min) | 10,5 / 8,8 / 8,0 | **674**, 619 |
| Cube, ARMSX2 | 1x | 50,0 | 50,0 | 50,0 | | 301 |

(A memória no Adreno é o `kernel_max` do kgsl, do processo do jogo inteiro: o GS, o nativo, a swapchain e o resto do processo. No Mali é a maior amostra de 10 em 10 s do `dumpsys gpu --gpumem`, que pode perder um pico curto. Com o ARMSX2, o "4x" e o "6x" são o `upscale_multiplier` do `.ini`; o log diz `un5: PCSX2's GS, vulkan, 6x`.)

**Item 4 do pedido, a tela girada de verdade e o 30 fps do painel:**
- **Odin, as duas paisagens, no meio da rota (`rec/n2_odin_rot`):** o jogo começa a 90, gira para 270 em v2381 (filme) e volta para 90 em v4850 (seleção). O log tem 4 swapchains na rota toda: 2 a `turn 90` no começo, `turn 270` no giro e `turn 90` na volta. Ou seja, ela é refeita só quando o giro muda. A imagem fica em pé nas duas, mas achatada (item 2). Com `NOMINUM_VK_PRETRANSFORM=0`, as duas paisagens saem certas (`rec/n2_odin_pt0`).
- **RP5: a 270 não dá para testar por software.** A ROM da Retroid prende a tela a 90° (`settings system force_landscape=1`). O `mUserRotation` continua `ROTATION_90` mesmo com `force_landscape=0`, `cmd window user-rotation lock 0|3`, `wm fixed-to-user-rotation enabled` e `cmd window set-ignore-orientation-request true`. E o jogo pede `sensorLandscape`. O Cube é quadrado.
- **30 fps do painel, no meio da luta:**
  - O toque em "30" e depois em "50 (nativo)" funciona no backend próprio (`rec/n2_odin_rot`, `rec/n2_rp5_turnip_rot`, `rec/n2_cube_fps`).
  - Contando os quadros por segundo no vídeo gravado, no modo 30 o Odin fica a 25–27, o RP5 a 22–27 e o Cube a 24–25, onde o jogo acompanha (no Ougi do Cube, 4–6, porque o jogo está a 9,4). No modo 50 ficam a 50–54.
  - Com o limite, o jogo anda mais rápido onde o backend é lento (item 3). Odin: Ougi 30,4 → 39,1 fps, vitória 42,6 → 50,0. RP5: Ougi 30,7 → 34,1, vitória 37,5 → 40,2. No Cube não muda nada (Ougi 9,5 → 9,4): lá o limite é a CPU (GS 19 ms por vsync).

**Não feito:** o tingido do KO não foi medido de novo (não foi pedido nesta rodada). Também ficaram de fora a rota completa de uma vez só (v3 e campanha separadas, como na rodada 1), o A35 e a Alpha 1.1X, o RP5 com o driver do sistema além do ponto da queda, e a 270 no RP5.

### 2026-11-04 00:10, teste do backend próprio do Nominum no aparelho (para a sessão do Nominum)

**Para a sessão do Nominum:** o backend próprio (`2328b2c`) roda a rota inteira no Odin 3, mas cai no RP5 e não sobe no Cube. Três defeitos dele e uma confirmação, por ordem de gravidade. Nada no código do Nominum foi mudado.

1. **RP5 (Adreno 650): `VK_ERROR_DEVICE_LOST` sempre no mesmo ponto.** `nominum-vk: vkQueueSubmit(m_queue, 1, &si, m_fence) failed (-4)`, depois `abort` (`fail` ← `NominumBackend::presentToWindow` +2792 ← `pcrtc` +1748 ← `vsync` ← `nominum_vsync` ← `host::runOwn`, thread "CPU Thread"). Aconteceu 3 vezes em 3: v3717 e v3742 (rota v3) e v2954 (rota de campanha). Nos três casos foi logo depois do NOW LOADING que vem depois de "New Game": a tela fica branca, e o Mode Select nunca aparece. Logos, filme e título rodam antes disso. O Odin (Adreno 830) passa desse ponto nas duas rotas.
2. **Cube (Mali-G57): o aparelho não tem `dualSrcBlend`** (`nominum: no Vulkan device with dualSrcBlend`, `bt4gsx: nominum_create failed (backend 2)`; o `rgcube_vkjson.json` confirma: `dualSrcBlend 0`). Volta ao ARMSX2 como previsto e roda igual a ele. O backend próprio não serve para o Cube enquanto depender do dual-source.
3. **A swapchain é refeita a cada vsync quando a tela gira.** Com o RP5 e o Odin em 16:9 (tela física em retrato, jogo em paisagem), o log tem `nominum-vk: swapchain 1920x1080, ...` uma vez por vsync (16 281 linhas numa rota v3). Pela leitura do código, a causa é esta: o `preTransform` é identidade (`vk/nominum_backend.cpp:1965`), então o Android responde `VK_SUBOPTIMAL_KHR` em todo present, e `:2095` refaz a swapchain a cada `SUBOPTIMAL`. No Odin em 20:9 (`wm size 2400x1080`, sem rotação) a swapchain é feita só 2 vezes. O custo é ~1 ms de GS por vsync na luta (3,9 → 2,9 ms no 16:9 contra o 20:9).
4. **A frente do GS em CPU é lenta onde o nativo não liga.** No Odin, com o nativo ligado, a luta fica a 50 fps. No Ougi, que o GS desenha (nativo desligado de v6911 a v7944), cai a **21,5 fps** (GS 25,9 ms por vsync, no 20:9 22,5 fps e 24,8 ms). Na vitória e no resultado fica em **38 fps** (GS 10 ms, pior segundo 18–20). Nesses mesmos trechos o ARMSX2 dá 50,0 com GS 3–4,5 ms. Na campanha, vila e menu START a 49–50 fps, com pior segundo de 23–40, contra 50 no ARMSX2.
5. **O congelamento do KO (o item 4 do pedido): confirmado no aparelho, o ARMSX2 com o nativo perde o tingido.** Medi o brilho médio do centro da tela, quadro a quadro, no vídeo:
   - **ARMSX2 com o nativo**, nos quatro (RP5, Cube, Odin 16:9 e 20:9): o quadro congelado sai tingido (claro e azulado, Y ~167–174) só por ~0,2 s. Depois fica congelado com as cores normais (Y ~120) por ~0,7 s, até o "KO".
   - **Backend próprio**, no Odin 16:9 e 20:9: o tingido dura o congelamento inteiro, ~1,1 s.
   - O ponto em que o tingido some é a hora em que o nativo liga (`hl_native: scene on at vsync 7944`, 8 vsyncs depois do KO em v7936), em todos. É o mesmo erro que o Nominum achou no PC (`ko`, v68–v103).

**Build:** o `libbt4gsx.so` do Nominum `2328b2c` saiu de `tools/build_android.sh` do Nominum, com `NOMINUM_BUILD=build/gsx-nominum` (1803 passos). Ele leva 9 `Bt4Gsx_*`, 12 `nominum_*` e 4 da libc++, e nenhum `vk*` indefinido; tem 34 671 024 bytes, igual ao registro de lá. A integração por submódulo (`docs/integracao.md`, passos 1–7) **não foi feita**: o teste usou só a biblioteca, copiada para `jniLibs` num APK de teste. Os dois lados da comparação usam o **mesmo binário**: o ARMSX2 é o padrão, e o backend próprio liga com `NOMINUM_BACKEND=nominum` no `hl_env.txt`. As duas rotas rodaram com `HL_GS_SCALE=1` (1x). Esse binário não tem o `HL_GS_DUMP` (o `armsx2_host.cpp` é o `hl_gsx.cpp` de `ccf6a4e`). Logo depois, o APK com o `libbt4gsx.so` do Hidden Leaf voltou aos três aparelhos, e o `wm size` do Odin foi resetado.

**Código (Hidden Leaf, `un5_main.cpp`):** a thread que chama `Bt4Gsx_Run` agora se chama "CPU Thread" desde o começo. Com o ARMSX2 nada muda, porque o `VMManager` já lhe dá esse nome. Com o backend próprio ninguém dava, e o `HL_PERF` (que conta o GS pelas threads "GS" e "CPU Thread") mostrava 0. Com o backend próprio, a coluna GS é o tempo dessa thread só: fluxo, frente do GS e envio à GPU, num núcleo. Com o ARMSX2, é a soma das threads "GS" e "CPU Thread".

**Rotas (`build/lab/recfull.sh`, envs `hl_env_nmn_{v3,camp}_{arm,nmn}.txt`, gravações em `build/lab/rec/nmn_*`):**

| Aparelho | Backend | Rota v3 (até a luta 2 e a seleção) | Campanha (vila, troca de área, menu START, painel de pausa, sair) |
|---|---|---|---|
| RP5 | próprio | **cai** em v3717 e v3742 (`DEVICE_LOST`) | **cai** em v2954 (`DEVICE_LOST`) |
| RP5 | ARMSX2 | até v16411, 0 `missing-target` | até v12167, painel e sair ok |
| Cube | próprio → ARMSX2 (sem `dualSrcBlend`) | até v16172, 0 `missing-target` | até v12092, painel e sair ok |
| Odin 16:9 | próprio | até v16051 (411 s; o ARMSX2 levou 215 s, porque o present FIFO segura o `HL_FAST_UNTIL`) | até v11938, painel e sair ok |
| Odin 16:9 | ARMSX2 | até v16277 | até v11948, painel e sair ok |
| Odin 20:9 | próprio | até v16422 | até v11981, painel e sair ok |
| Odin 20:9 | ARMSX2 | até v16436 | até v12067, painel e sair ok |

Nenhum `missing-target` nem trava fora do `DEVICE_LOST`. As cenas batem entre os dois backends nas folhas por vsync (`build/lab/atv.sh`): título, seleção, luta, Ougi, WINNER, transição branca, seleção 2, luta 2, o quadro preto com HUD do "Vermillion Mode" (igual nos dois, então não é do backend), NOW LOADING, vila, cortina, menu START. Nenhuma cena errada vista a olho; não comparei pixel a pixel.

**Tempo de quadro (`HL_PERF`, média por segundo no trecho; fps / GS em ms por vsync; `build/lab/perfwin.py`):**

| Trecho (vsyncs) | Odin 16:9 próprio | Odin 16:9 ARMSX2 | Odin 20:9 próprio | Odin 20:9 ARMSX2 | RP5 ARMSX2 | Cube ARMSX2 |
|---|---|---|---|---|---|---|
| luta 1, nativo (6450–6900) | 50,0 / 3,9 | 50,0 / 2,6 | 50,0 / 2,9 | 50,0 / 2,6 | 50,2 / 7,8 | 50,0 / 6,9 |
| Ougi, pelo GS (7000–7900) | **21,5 / 25,9** | 50,0 / 4,3 | **22,5 / 24,8** | 50,0 / 4,5 | 50,0 / 5,2 | 50,0 / 7,6 |
| vitória e resultado (8000–9000) | **37,9 / 10,2** | 50,0 / 3,1 | **37,7 / 10,1** | 50,0 / 3,2 | 50,0 / 5,7 | 50,0 / 7,2 |
| luta 2 (11200–12100) | 50,0 / 5,2 | 50,0 / 4,0 | 50,0 / 4,3 | 50,0 / 4,0 | 50,0 / 10,4 | 50,0 / 10,4 |
| campanha, vila (6300–8000) | 49,6 / 6,0 | 50,0 / 3,9 | 49,5 / 5,3 | 50,0 / 4,0 | 50,0 / 10,8 | 49,7 / 10,6 |
| campanha, andar e trocar de área (8100–10500) | 46,8 / 8,2 | 49,1 / 4,1 | 46,7 / 7,3 | 49,1 / 4,4 | 49,1 / 11,4 | 48,2 / 12,1 |
| campanha, menu START (10550–11800) | 49,1 / 7,6 | 50,0 / 5,1 | 49,0 / 6,7 | 50,0 / 5,0 | 50,0 / 13,2 | 43,1 / 14,1 |

(O pior segundo de ~1 fps em "andar e trocar de área" é a carga da área, igual nos dois. No Cube, 43 fps no menu START é o limite da VU1 de sempre, item 2 dos próximos passos.)

**Não feito:** o A35 e a Alpha 1.1X com o backend próprio. A rota completa de uma vez só (rodei a v3 e a campanha separadas, como nas entradas de 11-03). O 4:3 do painel de pausa conferido na imagem (o painel abre e volta, mas não olhei a imagem por trás dele).

### 2026-11-03 22:05, `HL_GS_DUMP`: dumps do GS por cena para o Nominum Render (prontos em `~/Downloads/gsdumps/`)

- **Os dumps estão prontos:** 52 arquivos `<aparelho>_<cena>.gs.zst` (4,0 GB) em `~/Downloads/gsdumps/` no PC, fora do
  git. Aparelhos: `rp5`, `cube`, `odin3` (16:9) e `odin3-20x9` (`wm size 2400x1080`, janela 2400x1080 no log). Cenas, 13
  por aparelho: logos, filme, titulo, menu, selecao, luta_inicio, ougi_jogador, ougi_cpu, ko, winner, resultado,
  campanha_vila, campanha_cortina. Todos com 304 vsyncs, `state version 9`, serial SLES-55605, CRC C071D4C1, captura
  640x480 e o registro de proveniência (`TargetsReadBack`). Conferidos pelo `tools/gsdump.py info` (lê o fluxo inteiro,
  0 erros) e pelas capturas do cabeçalho (`gsdump.py shot`): cada cena é a mesma nos quatro. **Não abri nenhum no PCSX2**
  (falta confirmar que o estado do ARMSX2, versão 9 como a do PCSX2, carrega igual).
- **Como (sem mexer no renderizador nem no Nominum):** `hl_gsx.cpp` (a `libbt4gsx`, leitor do fluxo) abre o dump com o
  código do PCSX2 que vem no ARMSX2, como o `GSRenderer::VSync`: na thread do GS, `ReadbackTextureCache`, `GSfreeze`,
  `SaveSnapshotToMemory` e `GSDumpBase::CreateZstDump` com os registradores privilegiados. Depois grava o fluxo
  **cru** do jogo (transfers, ReadFIFO2, registradores + vsync), antes do nativo. O caminho próprio do `GSRenderer` só
  veria o que chega ao `GSState::Transfer`, e na luta isso é só o que o nativo deixa passar. Antes do `Freeze`, o nativo
  entrega ao GS o que segura (`hl_native::flushAll`, a mesma chamada do ReadFIFO2). Vários dumps podem gravar juntos (KO
  e WINNER se cruzam). Exportado `Bt4Gsx_GsDump(base, frames)`.
- **Gatilhos (`hl_game.cpp`, `dumpTick`):** `HL_GS_DUMP=cena,...`, `HL_GS_DUMP_FRAMES=N` (300), `HL_GS_DUMP_DEVICE` (padrão
  `ro.product.model`), `cena@vsync` muda o vsync de qualquer uma. Uma vez por cena; `ko` só com o BTL e as duas da
  campanha só com o ADV carregados (senão `skipped`). Saída em `<pasta do jogo>/gsdumps/`.
  - **Por detector que já existe:** `luta_inicio` (entrada do lutador, `0x21E660`, a mesma da câmera larga: v6007),
    `ougi_jogador` e `ougi_cpu` (o detector de Ougi do especial, lutador em ação 8 com golpe de Ougi `0xF00000`,
    separado por `humanFighter`: v6537 e v6619).
  - **Por vsync fixo da rota** (sem detector): `logos` 1200, `filme` 2000, `titulo` 3000, `menu` 3800, `selecao` 4700,
    `ko` 7880, `winner` 8540, `resultado` 8950 (rota v3), `campanha_vila` 8150 e `campanha_cortina` 9080 (rota de
    campanha). A vila tem detector (ADV + nativo ligado), mas ele vê primeiro a cena do portão (v4559), não o passeio.
  - Primeira rodada com os vsyncs tirados do vídeo `rp5_v3h`: `titulo` 2900 saiu preto, `menu` 3620 era o NOW LOADING,
    `winner` 8050 ainda a luta depois do KO, `resultado` 8550 a WINNER. Corrigidos para os de cima e refeitos nos quatro
    (capturas: corredor 3D do título, Mode Select, WINNER "Naruto Uzumaki", Ninja Song 85/96/1161/626). `luta_inicio`
    abre preto em todos: é o fade de entrada da luta (146 MB de fluxo).
- **Rotas (em `build/lab`, `gsdrun.sh SERIAL APARELHO ENV ENDV`):** `hl_env_gsd_v3.txt` (rota v3 + as 11 cenas da luta),
  `hl_env_gsd_cpu2.txt` (a v3 sem o ○ e sem o `POKE` da luta 1, `HL_SP_CPU=600`: só `ougi_cpu`), `hl_env_gsd_camp.txt`
  (campanha) e `hl_env_gsd_fix.txt` (as 4 corrigidas, no 16:9).
- **Correção das entradas de 09:40 e 10:05 (o "Ougi Linked da CPU"):** o Ougi da luta 1 da rota v3 é **do jogador**.
  O botão do especial é o ○, e a rota aperta ○ a cada 12 vsyncs, então o "especial com um botão" dá o Ougi
  (`[sp] v6524 fighter d69a00 ougi, chakra 15.0`). Com `HL_SP_CPU`, a CPU (`d7b9a0`) faz o próprio em v6619.
- **Custo:** enquanto um dump grava, o jogo cai para 7–9 fps (KO no RP5) e, com dois juntos, 4–5. A causa é o zstd
  nível 6 do PCSX2 na thread de replay (3 a 6 MB por vsync na luta). O conteúdo não muda: o jogo espera o anel, e os
  vsyncs da rota, o KO e o Ougi caem iguais nos três aparelhos.
- **Build normal (libsdr nos stubs), o item "em andamento" das 13:45:** 18 rodadas, 0 `missing-target`. As v3 foram até
  v12104 (RP5), v11905 (Cube), v12244 (Odin) e v12194 (Odin 20:9), além do v11747–11750 onde a build da libsdr travava.
- **Ferramenta nova:** `tools/gsdump.py` (`info`, `shot`) lê `.gs` e `.gs.zst` (precisa do programa `zstd`).

### 2026-11-03 13:45, travamento da libsdr: o heap do jogo devolve a mesma pilha a quatro threads

- **Build de laboratório:** `HL_SDR=1 recomp/build.sh <iso>` (chave nova em `tools/un5.py`: recompila
  `sceSdRemoteInit`, `sceSdRemote` e `sceSdTransToIOP`; stubfix 40 funções em vez de 37). Logs novos, todos por
  variável de ambiente: `HL_SIFDMA_LOG` (0055, cada cópia do IOP para a RAM do EE), dump das últimas 32 entradas da
  thread no `missing-target` do escalonador (0056) e `HL_THREAD_LOG` (0057: criar, iniciar, sair, terminar, apagar,
  wakeup que falha).
- **RP5, rota v3 (`rp5_sdr1..3`):** o `missing-target 0xbd9c90` volta sempre em v11747–11750. Desta vez o jogo não
  fechou: a thread morreu e o som zerou dali em diante (rms 0,0 de v11893 a v14000). No `rp5_sdr1`, o painel de pausa
  rodou no meio (4:3 → Tela cheia → Continuar, v7256) sem problema.
- **O que os logs mostram:**
  - Descartado: o IOP escreve na RAM do EE só em `0x408c00` e `0x40eac0` (0x880 bytes, ra IOP `0x3a470`), longe da pilha.
  - Descartado: a end function da `sceSdRemote`, porque `sceSdCallBack` nunca é chamada e o ponteiro fica 0.
  - A thread que salta é a 131 (tarefa do jogo, `func 0x1d4d20`, criada pela 8 em v11747). Ela dormia na espera de
    quadro (`0x1d5540`, `ra` salvo em `sp+0x30` = `0x821370`); ao acordar, o `ra` era `0xbd9c90`.
  - As threads 131, 132, 133 e 134 foram **todas criadas com a mesma pilha `0x20820011`**. A pilha da tarefa vem do
    alocador do jogo (`0x117c70`, em `0x1d50dc`). Todas as outras 129 criações têm ponteiros normais do heap
    (`0xb61bc0`, `0xdeb800`…). A 134 recebe o argumento `0xbd9c30`, o mesmo `a1` do travamento: ela rodou sobre a
    pilha da 131.
  - Portanto, entre v8005 (última criação com pilha válida) e v11747, o heap do jogo passa a devolver um endereço
    inválido. Algo da libsdr corrompe o heap, ou o esgota.
- **Em andamento:** a mesma rota com a build normal (libsdr nos stubs) e os mesmos logs, para comparar.

### 2026-11-03 13:00, relatório da manhã publicado; pistas para o som

- Relatório: https://claude.ai/artifact/TBAXXm6YEQhGsuDxmPHkyU (privado).
- **Pistas do travamento da libsdr (sem build novo):** a `sceSdRemote` do jogo (`0x16aee8`) chama `sceSifCallRpc`
  pela convenção de registradores: `mode` = 1 (NOWAIT) quando o 1º argumento é 0, end function = `*(0x410e50)`
  (posta por `sceSdCallBack`, `0x16aec8`), end parameter em `sp+0` = o ponteiro do buffer. No runtime
  (`Kernel/Syscalls/RPC.cpp`), quando o cliente não tem semáforo, `completionSemaphore = endParameter`: com a libsdr,
  isso sinaliza um "semáforo" com o endereço do buffer. E com end function ≠ 0 o runtime chama a função pelo
  escalonador. Uma das duas é a primeira suspeita do `missing-target 0xbd9c90` (thread do escalonador, `a0 1`). O EE
  não registra nenhum servidor de callback do `sdrdrv` (o único `sceSifRegisterRpc` é o de `0x14264c`).
- Próximo passo: recompilar com a libsdr e `HL_RPC_LOG=1` até o travamento, para ver a última chamada a `0x80000701`.

### 2026-11-03 12:50, correção da entrada de 09:40 (bisect)

- Não existe `build/bisect/apk/a3dbcfc.apk`: a pasta só tem `cd8fca7.apk` (08:20), que não roda. O APK de `a3dbcfc`
  ainda precisa ser feito.

### 2026-11-03 12:55, painel de pausa pela rota: ainda não

- No RP5, o overlay de controles é desenhado pelo SDL e o `uiautomator` não vê nada dele (`ui.py` vazio). Um toque
  em (900, 85), onde fica o ☰ no layout do Cube, não abriu o painel; a alça azul da borda direita abre o "Odin Hub" do
  sistema do aparelho, não o nosso. Falta achar a posição do ☰ no RP5 (ele fica sob o HUD de desempenho do sistema)
  para pôr o painel (widescreen liga/desliga) e o "sair" na rota.

### 2026-11-03 12:50, correção da entrada de 12:35 (20:9 na campanha)

- A "imagem em 4:3 com faixa preta" na troca de área em 20:9 é, na verdade, a **cortina de transição** do jogo (o logo do
  UN5 entrando como pincelada), que em 2400x1080 não cobre a largura: fica uma borda preta irregular à esquerda e o
  cenário à direita por ~1 s a cada troca (`odin209_camp`, 105 s). Os 56,8–64,2 s que o `ana.py` acusou são cenário
  escuro na borda (um prédio): falso positivo. Defeito real, mas pequeno (item 3).

### 2026-11-03 12:35, sem código novo: a campanha com a build `a6c40f5` (RP5)

- **Rota de campanha (`build/lab/hl_env_camp.txt`, `rec/rp5_camp3`):** Master Mode → START, Yes, ✕ (pula a cena) →
  ✕ a cada 40 vsyncs no diálogo → "Act 1 Naruto Returns Home" → tutorial de movimento → anda pela vila com o analógico
  (`LRIGHT`, `LUP`, `LDOWN`, `LLEFT`) → **troca de área** (Hidden Leaf Gate, tela de carga) → anda → START do jogo (menu,
  tutorial "Start menu items"). **50,0 fps** de v5998 a v12370 (`[perf]`, sem limite de 480p), sem `missing-target`,
  som presente (rms 0,08–0,16). Sem barras visíveis no 16:9.
- **Falta na rota:** o painel de pausa do launcher (widescreen liga/desliga) e sair.
- **Odin 3 em 2400x1080 (`odin209_camp`) e Cube (`cube_camp`), mesma rota:** os dois andam, trocam de área e abrem o
  menu START, sem `missing-target`. Odin 49,8–50,2 fps; Cube 40,1–54,7 fps (VU1). **Defeito novo no 20:9:** nas
  transições de área (tela de carga, telas claras), a imagem sai em 4:3 com faixa preta à esquerda: `ana.py` acusa
  56,8–64,2 s e trechos curtos em 13–15 s. Na vila, sem barras. Item 3 (widescreen).

### 2026-11-03 12:10, `hl_native.cpp`: o fantasma depois da luta 2 era SCANMSK 2 preso no GS (corrigido na rota v3)

- **Causa (`HL_SCANMSK_LOG=1`, `rp5_v3g`):** em v12280, com a cena nativa desligada (um Ougi que o GS desenha), o jogo
  escreve `SCANMSK 2` (só linhas pares) **uma vez** e nunca escreve 0, nem na luta seguinte. O PMODE não muda a rota
  toda, então não há `sceGsResetGraph` que zere o GS. A partir daí o GS não desenha as linhas ímpares, que guardam a
  última imagem (o Ougi, a WINNER ou o último quadro da luta, conforme quem desenhou antes). O nativo ignora SCANMSK,
  por isso a luta seguinte "limpa" a tela. No Odin e no Cube o jogo não escreveu SCANMSK nessa rodada (o Ougi não
  aconteceu no mesmo ponto): daí o "intermitente".
- **Correção:** ao desligar a cena da luta, se o SCANMSK do jogo for ≠ 0, o nativo manda `SCANMSK 0` ao GS (log
  `fight over with SCANMSK 2: 0 to the GS`). Nada depois da luta é desenhado linha sim, linha não.
- **Rota v3, três aparelhos (`rp5_v3h`, `odin209_v3h` em 2400x1080, `cube_v3h` em 720x720):** RP5, luta 2 vencida pelo
  P1: WINNER "Naruto Uzumaki", resultado (Health 44, Time 92, Bonus 671, 753 → 807, 2149 Ryo), seleção, VS e carga
  **limpos** (antes, listrados por ~50 s). GS em `SCANMSK 0` desde v13367. Odin 20:9: a CPU venceu a luta 2, seleção
  limpa. Cube: a luta 2 foi ao tempo, seleção limpa. Nenhum `missing-target`. Barras: 0 no jogo.
- **Ainda quebrado:** o Ougi que o GS desenha antes do KO (v12280–13097 no RP5) sai escuro e listrado, pelo mesmo
  SCANMSK 2. Fica para o item 4/6. A rota completa (campanha, pausa, sair) e o Odin em 16:9 não foram refeitos com esta
  build: **não conta como corrigido pela regra C**.
- **Odin 3 em 16:9 (`odin_v3h`, `wm size reset`):** luta 1 com WINNER → resultado (85/96/851 → 1161, 1342) →
  seleção; luta 2 vencida pela CPU → seleção limpa. Sem `missing-target`.
- **Fica no código:** `dropTallSceneTarget` (11:30) e `HL_SCANMSK_LOG`; patch `0054-mpeg-stream-log.patch`
  (`HL_MPEG_LOG`). Build sem a libsdr recompilada: sem `missing-target` nos três (`rp5_v3g`, `odin209_v3g`, `cube_v3g`),
  o que confirma que o travamento das 11:30 vinha dela.

### 2026-11-03 11:30, fantasma da luta 2 e som dos filmes: duas tentativas, nenhuma resolvida

- **Fantasma depois da luta 2 (RP5), tentativa 1, `drawFrame` (`hl_native.cpp`):** um Ougi que o GS desenha sozinho
  (fallback) deixa o alvo da página 0 com 2047 linhas, por cima da página 0x70 que a tela lê. Recriar o alvo do nativo
  a cada quadro quando ele está alto (`rp5_v3c`, `odin209_v3c`, `cube_v3c`): **fantasma igual no RP5**, e o alvo alto
  voltava a cada fallback. Desfeito.
- **Tentativa 2, desligar a cena (`dropTallSceneTarget`, pela fila do GS):** o alvo alto sai quando a cena termina
  (log `scene over, the tall target 513x2047 ... goes`, 3 vezes em `rp5_v3e`). **Fantasma continua**, mas mudou: em vez
  da WINNER, fica o último quadro da luta, escuro, em linhas alternadas. Ou seja, metade das linhas da tela não é mais
  redesenhada depois da luta, e o conteúdo dela depende de quem desenhou por último. Mantido no código (não piora nada
  e o log ajuda), sem contar como correção.
- **Desentrelaçamento desligado (`HL_GS_EXTRA=deinterlace_mode=1`, `rp5_v3d`, `odin209_v3d`):** fantasma igual no RP5.
  Não é o desentrelaçador.
- **O que sobra:** PMODE `7f67`, SMODE2 1 (entrelaçado por campo), DISPFB1/2 `1070` iguais (o `[priv]` não muda a rota
  toda). O GS do ARMSX2 conta SCANMSK (`m_scanmask_used`) e as cenas de Ougi desenham linha sim, linha não com SCANMSK:
  se o GS ficar com SCANMSK ≠ 0 depois da luta, metade das linhas para. Pela leitura, `emitRegisters` reenvia SCANMSK
  quando `s_gsInSync` cai. **`HL_SCANMSK_LOG=1`** (novo, desligado por padrão): SCANMSK do jogo, a que o nativo manda e
  a que o GS tem a cada vsync. Rodada com ele (`rp5_v3s`) estragada pela build da libsdr (abaixo); a refazer.
- **Som dos logos e dos filmes: causa achada.** `HL_MPEG_LOG=N` (novo, patch `0054-mpeg-stream-log.patch`) mostra que o
  player registra o callback de áudio (tipo 2, `0x1046a0`) e o recebe nos três filmes. O callback põe o ADPCM num anel
  (`-0x3D8($gp)`), e o envio ao IOP é pela libsdr do EE: `sceSdTransToIOP` (`0x16ae40`) e `sceSdRemote` (`0x16aee8`,
  BlockTrans), que o analisador liga aos **stubs do runtime**, que respondem e não mandam nada. O IOP roda o `sdrdrv`
  4.0.1 da Sony de verdade (em `MODULES.BIN`, sid `0x80000701`).
- **Tentativa: recompilar `sceSdRemoteInit`/`sceSdRemote`/`sceSdTransToIOP` (`un5.py` `UNSTUB`).** No Cube, até o
  título: `BindRpc 0x80000701`, 1762 chamadas, **som a partir de v1502** (rms 0,39–0,58; antes 0 até v2977). Mas na rota
  v3 o EE salta para dado: `missing-target 0xbd9c90` (thread do escalonador, `a1 0xbd9c30`) em ~v11500 no RP5 e ~v10000
  no Odin 20:9; no RP5 o 3D da luta saiu preto. **Desfeito** (o `un5.py` guarda a nota). Suspeita: o callback do
  `sdrdrv` para o EE, ou a regeneração completa do código (primeira desde o fim de 11-02). A build sem a libsdr separa
  as duas.
- **Estouro:** `spu2Output` é igual ao do PCSX2. Picos de 1,07–1,12 e rms 0,4–0,58 nos filmes e na luta. Falta medir
  o nível no PCSX2 para comparar.
- **Ferramenta:** a ISO europeia está em `~/Downloads/Naruto UN5/`, não no caminho do `CLAUDE.md`.

### 2026-11-03 10:05, `un5_main.cpp` (leitor do `hl_env.txt`) e `HL_PRIV_LOG`: vitória nos 3 aparelhos e em 20:9

- **Bug da ferramenta de teste:** `loadEnvFile` lia o `hl_env.txt` com `fgets` em 2048 bytes. A rota v3 (3049 bytes)
  era cortada sem aviso e parava em v11265 (último `[hl_pad] tick`): por isso a luta 2 da primeira v3 "não respondia".
  Agora lê com `getline` (sem limite). Conferido: rota v3 até v15900 nos três aparelhos.
- **`HL_PRIV_LOG=1`** (`hl_game.cpp` `onVsync`): uma linha `[priv]` com PMODE, SMODE2, DISPFB/DISPLAY 1 e 2 e BGCOLOR
  sempre que mudam (chave de depuração, desligada por padrão).
- **Rota v3 (`build/lab/hl_env_v3.txt`):** a v1 até a luta 1, `POKE` da vida da CPU nas duas lutas (`d7ba0c` em v6450,
  `d7ba8c` em v11150: o jogo reinicia a vida no começo do round, ~444 vsyncs depois da câmera da luta; antes disso o
  `POKE` se perde), ○ a cada 12 vsyncs e ✕ a cada 150 depois de cada KO.
- **Vitória da luta 1, os três aparelhos (`rec/odin_v2`, `rp5_v2`, `cube_v2`, 16:9; `odin209_v3b` em 2400x1080):** KO
  v7936 → WINNER "Naruto Uzumaki" com a arte → "Ninja Song" (85/96/1161/1342) → seleção → luta 2. Igual nos três, os
  mesmos lutadores nos mesmos endereços (`d69a00`/`d7b9a0`, luta 2 `d69a80`/`d7ba20`).
- **Vitória da luta 2: quebrada no RP5 (`rp5_v3b`).** WINNER e resultado aparecem com a imagem da WINNER por baixo em
  linhas alternadas, e esse fantasma listrado fica na seleção, no VS e na carga (~50 s) até o START BATTLE. A mesma
  tela listrada e desbotada apareceu na seleção depois da luta 2 no Odin em 20:9 (`odin209_v3`, com a CPU vencendo).
  No Odin em 16:9 (`odin_cur1`) e no RP5 com a CPU vencendo (`rp5_v3`), a seleção saiu limpa: **intermitente**. É o
  relato "fica presa numa imagem parada, o jogo segue por trás". Em investigação (captura `w2.bin` + `[priv]`).
- **Barras laterais (`ana.py`, v3b, 170 s cada):** fora do launcher, 0 quadros com barra de verdade nos três; os
  acusados (Odin 20:9 aos 149 s, Cube aos 27 s) são cenário escuro na borda e tela preta de carga. Câmera larga em 20:9:
  `x 0.600`. O Cube não usa câmera larga (tela 1:1).
- **Piscadas:** o detector acusa 17–19 por vídeo; as que olhei (Odin 111 s, 128 s, 137 s) são flash de golpe e troca
  de pose. Nenhum personagem sumindo num quadro. **Quadro preto com HUD** no Cube logo depois do Ougi "Linked" da CPU
  (`cube_v2`, ~67 s): a anotar no item 4.
- **Som:** picos de 1,04 a 1,25 nos três (`[snd] peak`): estoura. O filtro de DC de `spu2Output` passa de 1,0 depois
  do `clamp_mix`.
- **RP5:** no carregador, 100% às 09:23 (às 08:00 estava em 41% sem carregar).

### 2026-11-03 09:40, sem código novo: a tela de vitória aparece quando o P1 vence (Odin 3, build `21c8a48`)

- **Rota nova para a vitória (`build/lab/hl_env_p1w.txt`):** a rota v1 (o P1 parado) sempre perde, e com a CPU vencendo o
  jogo vai do KO ao branco, ao NOW LOADING e à seleção, sem WINNER nem resultado (v10046 KO → v10220–10300 branco →
  v10306 NOW LOADING). Para o P1 vencer: `6450:POKE:d7ba0c:3c:4` põe 0,0115 na vida da CPU e ○ a cada 12 vsyncs dá o golpe.
  A vida é o float `lutador+0x6c` (1,0 = cheia; lutadores em `0xd69a00` e `0xd7b9a0` nesta rota, pelo `HL_FTRACE`;
  conferido com 4 dumps de RAM: P1 0,945 → 0,542 → 0,142 → 0,009, CPU 1,0).
- **Resultado (Odin 3, 16:9, `rec/odin_p1w2`):** KO em v7936 → WINNER com "Naruto Uzumaki" e **a arte do personagem
  inteira** → resultado "Ninja Song" (Health 85, Time 96, Bonus 1161, 1342 ryo, "Outstanding!") → ✕ → seleção → VS →
  segunda luta (Naruto x Sakura). Também apareceu um Ougi "Linked" da CPU (Sakura), sem erro.
- **O relato "presa numa imagem parada, o jogo segue por trás"** era o travamento do heap depois do KO (`2fee126`,
  corrigido em `21c8a48`): nesta rota não acontece mais.
- **Correção do que eu achei antes (nesta manhã):** as texturas que tomei por "arte WINNER" na captura do GS
  (`tbp 3e80/3c00`, v10280–10430 da rota v1) são do NOW LOADING; o branco depois do KO é o fade do jogo, igual com o GS
  em software (`HL_GS_API=sw`), sem o renderizador nativo e sem câmera larga (`HL_ASPECT=1333`).
- **Não conferido:** se, no PCSX2, a CPU vencendo também pula a WINNER (o vídeo de referência termina no meio da luta;
  o savestate `SLES-55605 (C071D4C1).01.p2s` de 07:50 é o resultado de uma vitória do jogador, "Winner GUY").
- **Ainda nesta rota (Odin 3):** os controles na tela continuam os da luta na WINNER e no resultado; os filmes e logos
  sem som (`[snd] rms 0`); um trecho de dupla exposição aos 188–189 s da luta 1 (`rec/odin_cur1`).
- **Bisect de 11-01:** o build de referência `cd8fca7` não roda com o código gerado de hoje (`[guest-branch:missing-target]
  target=0x100008`, tela preta no RP5); causa não achada em ~25 min. O APK de `a3dbcfc` está feito
  (`build/bisect/apk/a3dbcfc.apk`), sem teste.

### 2026-11-03 08:06, `21c8a48`: o jogo não trava mais depois do KO (APK 0.2.0 de 08:06)

- **Sintoma (Odin 3, rota completa, build `0259d54`):** no fim da luta (CPU vence, v10046 KO) o jogo parou em v10257:
  log parado, som desligado (`playing 0`), tela branca com os controles por cima, processo vivo. A `GameThread` em R,
  100% de CPU: `simpleperf` mostra o alocador do heap do jogo (`0x1D7430` → … → `0x117C70` → `0x118640` →
  `0x118E60`) lendo um endereço inválido em laço; cada `read32` lança exceção (`PS2Memory::read32`, desalinhado), o
  `Load32` devolve 0 e o laço da lista livre nunca acaba (60% do tempo em `libunwind`).
- **Causa:** `hl_game.cpp` `cameraScale`, vindo de `2fee126` (11-03 03:17): ao sair do modo largo, punha 1,0 no `+0x274`
  de todas as câmeras que a luta tinha alargado, inclusive as que o jogo já tinha liberado. A escrita caía num bloco
  livre e quebrava a lista livre do heap. **Regressão de `2fee126`.**
- **Correção:** só volta a 1,0 a câmera cujo campo ainda tem a escala larga que nós escrevemos. Log: `[cam] v10272 2 of 3
  cameras back to 1` (a terceira já era outro dado).
- **Teste:** Odin 3 (16:9), mesma rota: KO → quadro branco → seleção de personagem → segunda luta (Naruto x Sakura) →
  KO → seleção, sem travar (até v17531). Falta a rota completa nos outros aparelhos e em 20:9.
- **Ainda não visto no Android:** a tela WINNER com o personagem (o vídeo de referência do PCSX2 tem). Os ✕ a cada 150
  vsyncs depois de v10400 podem ter pulado a tela; a rota nova espera até v11000.
- **Correção da entrada de 07:50:** a `libun5.so` das 03:31 já tinha o `264b764` (o `recomp/android/build.sh` das 07:50
  não recompilou nada); o APK das 05:27 lia `un5_mods.txt`.
- O vídeo de referência (`~/Vídeos/Capturas de vídeo/Gravação_de_tela_20261031_234651.webm`) **não tem trilha de
  áudio**: não serve para comparar o som.

### 2026-11-03 07:50, sem build novo: o registro começa

- Criado este arquivo a partir do `git log` e do `docs/plano.md` (as entradas abaixo são reconstruídas; números
  vêm do `plano.md`).
- **Atenção sobre o APK instalado:** o `app-debug.apk` das 05:27 (versão 0.2.0) leva a `libun5.so` das **03:31**
  (até `2fee126`), não a de `264b764` (mods de dados, `0053`): o jogo base do APK não lê `un5_mods.txt`. A
  `libun5_un6_a35.so` (05:27) tem tudo até `43b39fa`.
- Instalado no celular do desenvolvedor (Honor PTP-N49) às 07:15, com o `UN6 A35.iso` em `/sdcard/Download`.

### 2026-11-03 05:31, `43b39fa` `5d78ca5`: A35 luta no Cube

- **O quê:** `un5.py` acha todo o código do A35 (`data_code_islands`, `called_islands`, `ovlelf --from`, `fnmap`
  aceita `J` de outros ELFs e chamadas depois de preenchimento); `recomp/build.sh` passo 6 refaz os overlays com
  `--from`.
- **Testes:** Cube, A35 com save copiado: carga, seleção, "Lutem!", luta Naruto x Naruto (v5873–v9850), volta à
  seleção. Sem fps. Resultado não conferido.
- **Pela metade:** o jogo base não foi regerado com o `un5.py` novo (ganha BTL +4, ADV +63, ETC +3, principal +1);
  `recomp/build.sh --mod` do zero não foi repetido (só `build/lab/reovl.sh`).

### 2026-11-03 04:45, `264b764` `8af070b`: mods de dados e pacotes de código

- **Onde:** `recomp/patches/0053-disc-mods-over-the-image.patch` (`hl_image.cpp`), `recomp/android/host/un5_main.cpp`
  (`HL_MODS`, `un5_mods.txt`, `HL_PACK_ID`), `recomp/build.sh --mod`, `recomp/android/build.sh --mod`,
  `EngineBridge.libraryFor`, `Mods.kt`.
- **Testes:** A35 no Cube até os menus.

### 2026-11-03 04:10–04:17, `cc6696d` `34467f3`: opções do launcher

- Escondidas as opções sem efeito (efeitos do PS2, volumes, renderizador, profundidade precisa, ASTC, pular quadros,
  present mode, retrato). 120 só em tela ≥ 100 Hz (`DisplayCaps.kt`). Testado à mão: tema, atalho, buscar
  atualização (404).

### 2026-11-03 02:10–03:17, `a3dbcfc` `0d01a8f` `2fee126`: um botão para o especial; widescreen

- **Controles (`a3dbcfc`):** saiu o botão grande e a leitura do aviso pelo GS (`PromptView`, `hl_native.cpp`).
  `hl_game.cpp`: o toque vira a sequência na entrada do lutador e o gancho `0x6C99B0` (tabela do BTL) responde aos
  avisos do Ougi sozinho (log `[sp]`). Visto: um toque → Ougi → "Critical Damage"; uma etapa "ERROR".
- **Widescreen (`0d01a8f`, `2fee126`):** a mesma forma (`s_shape`) no nativo e no GS; câmera larga (`+0x274`) só no
  `ADV` e na luta com entrada de lutador nos últimos 6 vsyncs ou Ougi na tela. Gravação: 808 → 0 quadros com barra.
  `HL_PERF` loga `[snd]` (nível do SPU2) por segundo (`hl_spu2.cpp`).
- **Regressões possíveis (não medidas):** a câmera larga depender de entrada de botão pode fazer a luta alternar
  de largura quando ninguém aperta nada.

### 2026-11-02 09:23–12:56: launcher e painel (`9c6c786` … `1e8d294`)

- `9c6c786`: trava do IOP, cena para o pad, painel de pausa, widescreen (`0049`, `hl_game.cpp`, `GamePanel.kt`).
- `383548a`: gerenciador de memory card (`Ps2Card.kt`, `SaveManager.kt`).
- `2225a89`: 60 e 120 por interpolação, avisos de botão grandes, contador (`0050`, `hl_native.cpp`, `PromptView.kt`).
- `44cf73e`: janela PAL, quadro inteiro nas cenas de Ougi, opção de desbloquear tudo (`0051`).
- `fdec23b`, `228da15`, `70d2c75`: textos legíveis, painel some no editor, analógico parado não esconde os
  controles.
- `49a17a9` (`0052`): `va_list` com slots de 8 bytes (os "(null)" dos diálogos viram Yes/No).
- `1e8d294`: importação de pasta ignora arquivos grandes demais.
- **Regressões possíveis (relato de 11-03 07:50):** o widescreen, o quadro inteiro do Ougi e a interpolação entram
  aqui; a bisect ainda não foi feita.

### 2026-11-01: do PC ao Android a 50 fps (`9ace1a2` … `cd8fca7`)

- Madrugada: título e menus (`0032`, `0033`), GS rápido (`0034`), da seleção à luta (`0035`–`0041`, `fnmap`).
- Manhã e tarde: `libun5.so` com o GS do ARMSX2 (`8fac981`), VU rápida e `HL_PERF` (`0043`), VuWorker (`0044`),
  flags e UNPACK (`0045`), som pelo SPU2 do ARMSX2 (`1535b4f`, `0046`), vértices nas formas do GS (`0047`),
  renderizador nativo da luta (`91cdef9`, `0edbae2`, `cd8fca7`, `0048`).
- **Testes:** `measure.sh` na rota fixa: **50,0 fps** no RP5 (`sd480p`, 2,0% de pares > 44 ms) e no Cube (0,0%),
  nativo ligado sozinho pela luta (`cd8fca7`). 0 quadros pretos em 404 gravados. Segunda luta na mesma sessão no RP5.
- É o build de referência ("o que mediu 50 fps") para a bisect das regressões.

## Método de teste (desde 2026-11-03)

Rota completa, sempre a mesma, gravada no aparelho: boot → logos → filme de abertura (20 s sem pular) → título → menu
→ luta livre até o KO → vitória → resultado → menu → segunda luta → campanha (andar e trocar de área) → painel de pausa
(trocar widescreen e voltar) → sair. Revisão quadro a quadro (4 por segundo nas lutas): piscadas de personagem,
sombras pretas, quadros com barra, telas presas. Som: nível do SPU2 no log e a trilha no `dumpsys media.audio_flinger`.
Nada é corrigido sem passar pela rota nos 3 aparelhos (RP5, Cube, Odin 3) e no Odin 3 em 20:9
(`wm size 2400x1080`), e a mesma rota no A35 e na Alpha 1.1X.
