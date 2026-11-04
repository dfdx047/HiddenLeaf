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
| Som dos logos e da abertura (filme) | quebrado | build de 11-03 12:00 | Cube | `HL_MPEG_LOG`, `HL_RPC_LOG`, `[snd]` | causa: `sceSdRemote`/`sceSdTransToIOP` do EE são stubs mudos; recompilados dão som desde v1502, mas o jogo trava em ~v10000 (`missing-target`). Desfeito, a resolver |
| Título e menus | funciona | `2fee126` | RP5, Cube | rota fixa, prints, `[snd]` | som em título, Mode Select, Options, Shop, Collection, seleções, VS |
| Luta | parcial | build de 11-03 12:00 | RP5, Cube, Odin 3 | rota v3 gravada, `ana.py` | 50 fps; piscadas: só falsos positivos (golpes); Ougi do GS com SCANMSK sai escuro e listrado; quadro preto com HUD no Cube depois do Ougi "Linked" da CPU |
| Tela de vitória / resultado | parcial | `f00dd5b` | RP5, Cube, Odin 3 (16:9 e 20:9) | rota v3 gravada | WINNER, resultado e menu nas duas lutas; fantasma da luta 2 corrigido (SCANMSK, `3d79104`). Falta a rota completa (campanha, pausa, sair) e os mods. Vitória da CPU vai direto à seleção (não conferido no PCSX2) |
| Campanha (Master Mode) | parcial | `a6c40f5` | RP5, Cube, Odin 3 (20:9) | rota de campanha gravada (`*_camp*`) | anda, troca de área e menu START: RP5 50 fps, Odin 50, Cube 40–55 (VU1). Em 20:9, a cortina de transição não cobre a largura (~1 s por troca) |
| Widescreen na luta | parcial | `2fee126` | RP5 | gravação de 11-03: 808 → 0 quadros com barra | câmera larga presa à entrada do lutador nos últimos 6 vsyncs (não ao estado da cena). Relato de regressão |
| Widescreen na campanha | não testado | | | | câmera larga ligada pelo `ADV` |
| Widescreen nos menus | parcial | `2fee126` | RP5 | prints | esticados; "Esticar" e "Desenhar em 16:9" não existem |
| Widescreen em 20:9 | parcial | `a6c40f5` | Odin 3 (2400x1080) | rotas v3 e de campanha gravadas, `ana.py` | luta: câmera `x 0.600`, 0 barras; campanha: a cortina de transição não cobre a largura (~1 s por troca); menus esticados |
| Controles por cena | funciona | `9c6c786` | RP5 | prints (11-02) | cena pelo overlay e estado (`TouchBridge.nativeScene`) |
| Especial com um botão (Ougi) | parcial | `a3dbcfc` | RP5 | rota fixa, log `[sp]` | responde sozinho em segundo plano (não é o que se quer). Um toque dá o Ougi; uma etapa mostrou "ERROR"; △△, disputa e multiplicador não vistos |
| Painel de pausa | funciona | `9c6c786` | RP5 | prints (11-02) | não reconferido depois de 11-02 |
| Contador de fps | funciona | `2225a89` | RP5 | prints (11-02) | |
| 60/120 interpolados | não testado | | | | 120 só aparece em tela ≥ 100 Hz (`34467f3`); Odin 3 travado à noite |
| Resolução 1x–4x | não testado | | | | |
| Save manager (.ps2 in/out) | funciona | `1e8d294` | RP5 | teste de unidade `Ps2CardTest`, import/export à mão | |
| Desbloquear tudo | não testado | `44cf73e` | | | opção do launcher (`0050` poke) |
| Mods de dados | parcial | `264b764` | Cube | A35 com `HL_MODS` | só testado junto com o A35 |
| UN6 A35 (pacote de código) | parcial | `43b39fa` (libun5_un6_a35 de 11-03 05:27) | Cube | rota até a luta | precisa de save copiado à mão em `BASLUS-55606NARUTO6`; resultado não conferido |
| UN6 Alpha 1.1X | quebrado | | | | sem entrada em `KNOWN_MODS`/`CODE_RANGES`, sem pacote |
| Packs de textura | quebrado | | | | não implementado no motor |
| Opções do launcher | parcial | `34467f3` | RP5 | à mão | as sem efeito foram escondidas (`cc6696d`) |
| Backend próprio do Nominum (`NOMINUM_BACKEND=nominum`, teste) | quebrado | Nominum `2328b2c` (APK de teste de 11-03 23:27, não instalado agora) | Odin 3 (16:9 e 20:9); RP5 cai; Cube não sobe | rotas v3 e de campanha gravadas, `HL_PERF`, brilho quadro a quadro no KO | RP5: `VK_ERROR_DEVICE_LOST` na entrada do Mode Select (3 de 3). Cube: sem `dualSrcBlend`, volta ao ARMSX2. Odin: tudo certo, mas Ougi a 22 fps e vitória a 38; swapchain refeita a cada vsync no 16:9. Mantém o tingido do KO. Detalhe em 11-04 00:10 |
| Dumps do GS por cena (`HL_GS_DUMP`) | funciona | APK de 11-03 21:26 | RP5, Cube, Odin 3 (16:9 e 20:9) | 52 dumps, `tools/gsdump.py info` e capturas do cabeçalho | ferramenta de teste; o jogo cai a 4–9 fps enquanto grava (zstd na thread de replay). Não abertos no PCSX2 ainda |

## Histórico (mais novo primeiro)

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
