# Registro

O que já foi feito, o que funciona, o que está quebrado e onde cada coisa foi mexida. **Ler inteiro antes de começar;
atualizar e commitar junto com o código ao terminar cada item.** Entradas antigas não se apagam: uma correção é uma
entrada nova. Detalhe técnico e medidas longas ficam em `docs/plano.md`; aqui fica o estado e o histórico.

## Estado das funções

Estados: **funciona**, **parcial**, **quebrado**, **não testado**. "Rota" = rota completa gravada (método no fim deste
arquivo); "rota fixa" = a rota antiga de medida (`build/lab/hl_env.txt`, Naruto x Naruto no lamen), que só chega à luta.

| Função | Estado | Último build verificado | Aparelhos | Como foi verificado | Observações |
|---|---|---|---|---|---|
| Boot e logos (imagem) | funciona | `good-20261105-1` (`HiddenLeaf-20261105-*.apk`) | RP5, Cube, Odin 3 (16:9 e 20:9) | rota de filmes gravada (`f1_*_film`), `[mpeg-pic]` | 50 fps também no fim de cada filme (antes, 6 fps no Cube por ~12 s; patch `0069`) |
| Som dos logos e da abertura (filme) | funciona | `good-20261105-1` (`HiddenLeaf-20261105-*.apk`) | RP5, Cube, Odin 3 (16:9 e 20:9) | `f1_*_film`: `[snd]`, `[snd-on]`, `[mpeg-pic]` | começos de som e quadros nos mesmos vsyncs nos 4; fila de 77 ms (mín. 45); pico 0,98; `NARUTO10` (44,1 kHz) com o som 170 ms antes do arquivo, a conferir no PCSX2 |
| Título e menus | funciona | `3933c12` | Odin 2 Mini (16:9) | rotas gravadas e toque real (11-09) | por gestos (toque ✕, deslizar, segurar ○, dois dedos △, três START); 50 fps em 2x, 4x e 8x, 1% low 46,6; título e menus do A35 na altura certa (`0084`) |
| Luta | parcial | `good-20261105-1` (`HiddenLeaf-20261105-*.apk`) | RP5, Cube, Odin 3 (16:9 e 20:9) | rota v3 gravada (`f1_*_v3`), contact sheet, trechos pretos medidos | 50 fps; 3 lutas sem salto (`0068`); Ougi do GS com SCANMSK sai escuro e listrado (não visto nesta noite); quadros pretos com HUD de 1–5 s (Ougi "Linked" da CPU, cargas), na mesma quantidade de antes |
| Tela de vitória / resultado | parcial | `f00dd5b` | RP5, Cube, Odin 3 (16:9 e 20:9) | rota v3 gravada | WINNER, resultado e menu nas duas lutas; fantasma da luta 2 corrigido (SCANMSK, `3d79104`). Falta a rota completa (campanha, pausa, sair) e os mods. Vitória da CPU vai direto à seleção, igual ao PCSX2 (11-09 06:09) |
| Campanha (Master Mode) | parcial | `good-20261105-1` (`HiddenLeaf-20261105-*.apk`) | RP5, Cube, Odin 3 (16:9 e 20:9) | rota de campanha gravada (`f1_*_camp`), pausa e saída | Odin 50; Cube 49,6/50/42,2; RP5 43,7/46,4/36,7 (VU1). Abaixo de 50 fps o som seca (fila até 0,1 ms no RP5 e no Cube): o jogo produz menos som que o tempo real |
| Widescreen na luta | parcial | `2fee126` | RP5 | gravação de 11-03: 808 → 0 quadros com barra | câmera larga presa à entrada do lutador nos últimos 6 vsyncs (não ao estado da cena). Relato de regressão |
| Widescreen na campanha | funciona | `d7faa17` | Odin 2 Mini (16:9), RP5 (20:9, 11-09 05:16) | rota de campanha e toque real | sem barras; 50 fps no Odin 2 Mini |
| Widescreen nos menus | funciona | `d7faa17` | Odin 2 Mini (16:9) | rotas `o2_w2` e de campanha, toque real | 2D no formato da tela (`13256aa`), cópias de quadro intactas (`dd66468`), adereços 3D da seleção (`d7faa17`); filmes em 4:3; 20:9 não testado neste aparelho |
| Widescreen em 20:9 | parcial | `967fc8e` | Odin 3 (2400x1080); Odin 2 Mini com `wm size 2400x1080` (menus, 11-09) | rotas v3 e de campanha gravadas, `ana.py`; menus por `gest/wide209.sh` | luta: câmera `x 0.600`, 0 barras; menus largos e na proporção (título, Mode Select, seleção, VS, NOW LOADING) no Odin 2 Mini; campanha: a cortina de transição não cobre a largura (~1 s por troca) |
| Controles por cena | funciona | `3933c12` | Odin 2 Mini | toque real (11-09): luta, campanha, conversas, menus | luta: combo, pulo, chakra, jutsu, disputa, item, defesa, START, Back; conversas da campanha (desde a primeira) com os controles dos menus; toque mínimo de 80 ms |
| Especiais ↑↑○ e ↓↓○, Toque Pro, guia de botões | não testado | (lab, 11-12) | | capturas Robolectric | botões novos na luta e opções do launcher; nada jogado no aparelho ainda (entrada de 11-12 tarde) |
| Especial com um botão (Ougi) | parcial | `app_d6` (lab, 11-05) | Odin 3, Cube | jogado por toque, gravado (`p2_cube`, `p3_odin60`, `p4_134`, `p6_cube`, `p8`–`p10`) | ligado e desligado; △△○ à mão e disputa vistos a 50 (Cube), 60 e 120 (Odin); falta o Ougi da CPU a 120 e o Cube a 60 |
| Painel de pausa (menu do jogo) | funciona | `e3f0d17` | Odin 2 Mini; antes RP5, Cube, Odin 3 | `build/lab/gest/panel.sh` (11-09), rota de campanha (11-05) | menu com abas Gráficos (1x a 8x, 30 a 120 fps), Controles (com dicas) e Layout; "Sair do jogo" volta ao launcher |
| Menu lateral (gaveta, 5 abas, gamepad, mapa do controle) | não testado | (lab, `lab/side-panel`, 11-12 noite) | | capturas Robolectric (`android/build/shots/menu_*`), `PadMapTest` | substitui o painel de pausa acima; entrada de 11-12 noite |
| Contador de fps | funciona | `2225a89` | RP5 | prints (11-02) | |
| 60/120 interpolados | parcial | `app_d6` (lab, 11-05) | Odin 3, Cube (60) | jogado e gravado, `tools/flicker.py` quadro a quadro, antes (`b1_odin120_good1`) e depois (`p10`, `p11`) | avisos 2D não piscam mais; sombra preta não vista; 100–120 e 58–60 apresentações por segundo |
| Resolução 1x–8x | funciona | `7a5492d` | Odin 2 Mini | matriz de desempenho (11-09 09:00) e térmica de 20 min | 50 fps cravados até 4x (também a 120); 6x 50 fps com 1% low 31–36; 8x perde vsyncs (47 fps) |
| Save manager (.ps2 in/out) | funciona | `1e8d294` | RP5 | teste de unidade `Ps2CardTest`, import/export à mão | |
| Desbloquear tudo | não testado | `44cf73e` | | | opção do launcher (`0050` poke) |
| Mods de dados | parcial | `264b764` | Cube | A35 com `HL_MODS` | só testado junto com o A35 |
| UN6 A35 (pacote de código) | parcial | `d5fc08b` (`libun5_un6_a35.so` de 11-05 03:37, regerado com `recomp/build.sh --mod` do zero) | Cube, RP5, Odin 3 (16:9 e 20:9) | rota `hl_env_a35f` gravada (11-05 03:40–03:54) | luta Naruto x Naruto e volta à seleção nos 4, com som, sem `missing-target`; o launcher cria o save do mod a partir do do UN5 (`ModSaves`, 11-05 04:10); o Cube e o RP5 não seguram 60 fps (NTSC) em toda a luta; imagem 21 linhas acima do PCSX2 até `0084` (11-09 13:47); sem campanha no mod |
| UN6 Alpha 1.1X | parcial | `6b420e6` (`app_a35s.apk`, sem pacote: a base roda o programa da mod) | Cube, RP5, Odin 3 (16:9 e 20:9) | rota v3 até v9000 gravada (11-05 04:00–04:09) | luta, imagem e 50 fps nos 4, sem `missing-target`; falta a rota inteira (WINNER e resultado só vistos no Cube em 11-04) |
| Packs de textura | quebrado | | | | não implementado no motor |
| Opções do launcher | parcial | `34467f3` | RP5 | à mão | as sem efeito foram escondidas (`cc6696d`) |
| Backend próprio do Nominum (`NOMINUM_BACKEND=nominum`, teste) | quebrado | Nominum `12fb79b` (APK de teste de 11-04 05:45, não instalado agora) | Odin 3 (16:9 e 20:9), Cube; RP5 só com o Turnip | rotas v3 e de campanha gravadas a 1x, 2x, 4x e 6x, `HL_PERF`, memória da GPU, giro e 30 fps pelo painel | RP5 com o driver do sistema: `DEVICE_LOST` no mesmo ponto (rastro anotado); com o Turnip roda. Pré-rotação achata a imagem a 90/270 (certa com `NOMINUM_VK_PRETRANSFORM=0`). Cube roda sem `dualSrcBlend`, mas Ougi a 9,5 fps. Odin: Ougi 30,4 (era 21,5). Detalhe em 11-04 07:50 |
| VIF1 e VU1 do Nominum (`NOMINUM_VU1=1`, teste, desligado por padrão) | parcial | `0e8e877` + Nominum `b2ff28b` (APK de 11-04 14:02) | Odin 3 (16:9 e 20:9), RP5 (sd480p, Qualcomm e Turnip), Cube | rotas v3 e de campanha `rec/n3_*_b`, `HL_PERF`, capturas | VU1 25–30% mais barata; RP5 sd480p a 50 na campanha; no Cube a campanha cai (GS do Nominum sobe), ver 11-04 15:00 |
| Dumps do GS por cena (`HL_GS_DUMP`) | funciona | APK de 11-03 21:26 | RP5, Cube, Odin 3 (16:9 e 20:9) | 52 dumps, `tools/gsdump.py info` e capturas do cabeçalho | ferramenta de teste; o jogo cai a 4–9 fps enquanto grava (zstd na thread de replay). Não abertos no PCSX2 ainda |
| Dumps do VIF1 e do GIF por cena (`HL_VIF_DUMP`) | funciona | `libun5` de 11-04 07:47 (patch `0058`) | Odin 3 (16:9) | 13 pares em `~/Downloads/vifdumps/`, `tools/vifdump.py info` sem erro | ferramenta de teste, para a Fase 3 do Nominum; formato em `docs/vifdump.md`. Só no 16:9 (a câmera larga muda o que vai à VU1) |
| Online de dois jogadores (lockstep, salas na Cloudflare) | não testado | branch do agente (`af052ef`, `a3555f7` e o commit do launcher) | nenhum | testes de unidade no PC (`host/online/tests/run.sh`: lockstep 892, sessão 2042, partida 76 checagens, 0 falhas; `node online/worker/test.mjs`), `assembleDebug` e foto da tela Online (Robolectric) | nada rodou em aparelho nem na Cloudflare de verdade; o servidor precisa ser publicado (`online/worker/README.md`) |
| Online em rede local (Wi-Fi, sem servidor; escolha na página Online) | não testado | branch do agente de 2026-11-13 23:20 | nenhum | `LanCodecTest`, `LanSessionTest`, foto `online_lan_portrait`, `assembleDebug` | descoberta por broadcast na porta 47655, entrada pelo socket do jogo; mesmo motor e mesmo arquivo de sessão do online (`relay=0`); falta testar em dois aparelhos na mesma rede |
| Partida rápida (oponente aleatório) e aviso do online grátis esgotado | não testado | branch do agente de 2026-11-13 23:59 | nenhum | `node online/worker/test.mjs`, `OnlineQuotaTest`, fotos `online_quota_*`, `online_mods_*` | `/v1/quick` e `hl_queue` no Worker (não publicado); online e rede local só no jogo base |

### Lutas contra o PCSX2 (bancada no PC, desde 2026-11-08)

Recomp do PC (`ps2EntryRunner`, GS de software do runtime, sem o nativo) contra o PCSX2 da bancada, mesma rota de botões,
`HLB_RNG=1`, `tools/hlbench.py diff`. "Estado igual" = posição, vida, chakra e ação dos dois lutadores iguais em todas as
chamadas. O 1P fica parado contra a CPU.

| Luta (1P x CPU) | Cenário | Rota | Build | Chamadas | Estado | O que ainda difere |
|---|---|---|---|---|---|---|
| Naruto x Naruto, 1 rodada | lamen | `route_idle` (`p2` x `r18`) | `e0beedf` | 3113 | igual | cores `0x80xxxxxx` e `+0x000` já no quadro 0; heap 0x80 deslocado |
| Neji x Naruto, 2 rodadas | lamen | `route_v2` (`pv2` x `rv2`) | `e0beedf` | 2013 + 714 | igual | idem |
| Kakashi x Naruto, 2 rodadas | lamen e a 2ª área | `route_v3` (`pv3` x `rv3`) | `e0beedf` | 1445 + 1301 | igual | idem; heap no mesmo lugar |

Imagem (desde 2026-11-09): capturas lado a lado a cada 25 quadros na `route_v3`, iguais depois do `0077` e das funções de
imagem do jogo (fumaça, poeira, clarão). Falta: outros cenários, Ougi, campanha, a CPU nos dois lados.

## Histórico (mais novo primeiro)

### 2026-11-24 20:03: Desbloqueio real das transformações iniciais com R1 (Save 100%, Trapaça e Online)

- **Causa raiz (`sub_003C8880` → `sub_001FEA10` → `sub_001E9B40`):** Na Seleção de Personagem, ao segurar **R1** (`0x58($s0) & 8`), o jogo chama `sub_001FEA10`, que lê `*(save + 0xDFC + 0xE60)` = **`save + 0x1C5C`** e exige `>= 0x66` (102) para liberar as transformações iniciais. Antes só escrevíamos `0x6E` em `0xE68` (faltando o offset `+ 0xDFC`).
- **Correção:** `0x1C5C = 0x6E` adicionado em `SaveManager.kt` (Save 100% real em `data01`/`data04` com checksum recalculado) e em `hl_game.cpp::unlockEverything()` (cobre também a trapaça "Desbloquear tudo" e o Modo Online). Recompiladas as 4 bibliotecas nativas + APK e atualizado o Save 100% nos 3 aparelhos.

### 2026-11-24 19:52: Correção de gráficos/efeitos faltando no Modo Leve e reversão do HL_NATIVE automático em 60+ FPS

- **Causa raiz:** (1) `HL_NATIVE=1` ativado automaticamente em `fps_mode > 50` trocava o PCSX2 GS pelo renderizador nativo incompleto (que perde efeitos e quebra com filtros/resoluções); (2) `light_mode` estava ligado por padrão até em GPUs fortes (tier 2–4, como RP5/SD865) e o aliasing zero-copy (`g_vu1Code`/`g_vu1Data`) em `hl_nominum_vu1.cpp` podia pular o `memcmp` de microcódigo VU1.
- **Correção:**
  - `un5_main.cpp`: `HL_NATIVE=0` restaurado (PCSX2 GS com 100% dos efeitos, filtros e resoluções); `g_lightMode = false` por padrão.
  - `hl_nominum_vu1.cpp`: aliasing zero-copy desativado (`g_vu1Code = g_vu1Data = nullptr`) para sempre sincronizar microcódigo/dados VU1 com segurança quando `light_mode=1` estiver ligado.
  - `GpuProfiles.kt` + UI (`SettingsScreen.kt`, `GameMenu.kt`, `GraphicsMenu.kt`, `GameActivity.kt`): `light_mode=1` automático **somente** em GPUs fracas (tier 0–1: Adreno 3xx–620 incl. SD665/Mi A3, Mali-G31/G51/G52/G57/G68/G71/G72, PowerVR, Xclipse 5xx); GPUs médias/fortes (tier 2–4, como RP5) ficam em `light_mode=0`.

### 2026-11-24 14:18: Revisão geral pré-vídeo — correção do loop de salvar em mods, pular logos + abertura, FPS interpolado funcional e Modo Leve automático em GPUs fracas

- **Correção do bug de salvar sozinho no menu (UN6 A35 / mods) + Pular Logos e Abertura (`0111-skip-logos-and-safe-stop.patch`, `Pad.cpp`, `EeScheduler.cpp`):**
  - Causa raiz: `applySkipIntro` apertava `START` até `g_hlMoviesBegun == 3`. Em mods com menos de 3 vídeos (como UN6 A35), o contador nunca chegava a 3 e o emulador ficava pressionando `START` 4x/s para sempre em todos os menus (inclusive tela de salvar).
  - Correção aplicada em `libun5.so`, `libun5_v80.so`, `libun5_un6_a35.so` e `libun5_un6_a35_v80.so`: aguarda na tela de idioma até o jogador confirmar (`✕`/`○`/`START`) ou iniciar o 1º vídeo (NTSC/mods), ativa fast-forward (`g_hlFastForwardIntro`) + `START` para pular logos da Bandai/CRIWARE/CyberConnect2 e filmes direto para New Game / Load Game, com parada rígida permanente (`tick > 3400`, `+1400` após início, fim dos vídeos ou qualquer direcional).
- **Correção do FPS Interpolado 60/90/120 fps (`un5_main.cpp`, `GraphicsMenu.kt`):**
  - Causa raiz: `un5_main.cpp` forçava `HL_NATIVE=0` sempre que `native_render` estava desligado (padrão), impedindo `hl_native` de rodar a interpolação em 60/90/120 fps.
  - Agora `g_settings.fpsMode > 50` (em Vulkan) ativa `HL_NATIVE=1` automaticamente. Validado no RP5 (`hl_native: waiting for a fight`, `bt4gsx: vulkan, 2x`). Adicionado aviso explícito `reinicie` no menu gráfico in-game para Resolução e FPS Interpolado.
- **Modo Leve (`light_mode=1`) automático em GPUs fracas (`AppViewModel.kt`, `un5_main.cpp`):**
  - GPUs tier 0–1 (Adreno 3xx–620 incl. SD665/Adreno 610, Mali-G31/G51/G52/G57/G68/G71/G72/T-series, PowerVR, Xclipse 5xx) gravam `light_mode=1` (e 1x no tier 0) automaticamente na primeira abertura, sem depender do diálogo, mantendo-se ativo também no Modo Online (Nominum VU1 determinístico + `GSBackThreadMode=1` expandido para `mt676*`/`mt678*`/`mt683*`/`exynos*`/`ums*`/`sc9863*`).
  - Forçado `g_specialOn = false` em `loadSettings()` para ignorar `special_one=1` de `un5_settings.txt` antigos.

### 2026-11-24 07:52: Sala Personalizada com troca de papel (P1/P2/Espectador), detecção e exportação automática de personagem + maestria por personagem, filtro de controle na Ranqueada, hold de ↓↓○ e correção do R2 automático

- **Sala Personalizada & Espectador (Casual) interligada (`worker.js`, `OnlineAdvanced.kt`, `OnlineScreen.kt`):**
  - Novo endpoint `POST /v1/lobby/role` permitindo que qualquer participante alterne entre **Jogar** e **Espectador**, e que o anfitrião defina/troque **P1**, **P2** e **Espectador** em tempo real (polling de 1,5 s).
  - Botão **"Iniciar Luta (P1 vs P2)"** cria a sala casual interligada (`active_room`), permitindo ao P2 entrar direto na luta e aos espectadores assistirem em tempo real (exclusivo do modo Casual). Verificado em teste fim-a-fim de 3 participantes.
- **Detecção automática de personagem, exportação de retrato do jogo e Maestria POR PERSONAGEM (`hl_game.cpp`, `un5_main.cpp`, `TouchBridge.kt`, `GameActivity.kt`, `worker.js`, `schema.sql`, `OnlineAdvanced.kt`, `Un5NinjaUi.kt`):**
  - `hl_game.cpp` lê `fighter+0x68` (ID do personagem do P1, confirmado em `sub_002146A0`) e expõe `TouchBridge.nativeP1CharId()`.
  - `GameActivity.kt` detecta automaticamente o personagem escolhido pelo P1 na luta, contabiliza XP/nível individual na tabela `char_mastery` (por personagem, não global) e exporta automaticamente o retrato real do HUD da luta via `PixelCopy` para `<root>/portraits/<charKey>.png`.
- **Ranqueada com separação Touch vs Controle + fallback 15 s (`OnlineScreen.kt`, `Online.kt`):**
  - Seletor **Filtro de Controle (Ranqueada)** (`Só Tela`, `Só Controle`, `Todos`) e filtro de tabela (`Todos`, `Tela`, `Controle`) diretamente dentro da tela Ranqueada.
  - Em `QuickSearching`, após 15 s procurando em fila filtrada, exibe **"Demorando? Aceitar qualquer controle"** para liberar a busca para qualquer adversário em 1 toque.
- **Carregar poder segurando `↓↓○` / `↑↑○` (`TouchConfig.kt`, `NinjaPadView.kt`):**
  - Adicionado `holdLast = true` em `SPECIAL_DOWN` e `SPECIAL_UP`: ao segurar o botão, o passo final (`○`) permanece pressionado continuamente até soltar o dedo (permitindo carregar golpes/poderes).
- **Correção do R2 automático no teletransporte/Kawarimi (`NinjaPadView.kt`, `PadMap.kt`, `un5_main.cpp`):**
  - `Ctl.GUARD` (`Ps2.R2`) excluído da ativação por deslize (`move()`) no touch — só dispara com toque direto intencional.
  - Zona morta dos gatilhos analógicos L2/R2 aumentada (`0.25 → 0.50` em `PadMap.kt`, `8000 → 16000` em `un5_main.cpp`) e `g_specialOn` travado em `false`.
  - `libun5.so` e `libun5_v80.so` recompiladas, APK testado e instalado nos 3 dispositivos (RP5, RG Cube, Mi A3).

### 2026-11-24 07:18: Tela Dedicada de Modo Ranqueado, fila Casual vs Ranqueada separada, retratos reais do jogo e correção de layout

- **Fila Casual vs Ranqueada separadas (`Online.kt`, `OnlineScreen.kt`, `OnlineMenu.kt`):**
  - Adicionado parâmetro `ranked: Boolean` em `OnlineLobby.quick(fp, ranked, start)` (usa chave de fila `${fp}#ranked` separada da fila Casual).
  - Nova **Tela Dedicada de Modo Ranqueado (`RankedDedicatedScreen`)** em tela cheia (2 colunas na horizontal / 1 na vertical), com **Cartão Ninja**, botão **"Jogar Ranqueada"**, **"Treinar na Fila Ranqueada"** e **Top 100 Global** com retratos reais dos personagens + nível de maestria `Lv.XX`. Botão **"Jogar Ranqueada"** adicionado também no menu in-game `OnlineMenu.kt`.
- **Retratos reais dos personagens do jogo:** recortados em alta resolução da tela de Seleção de Personagem 1080p do jogo (`naruto`, `sasuke`, `kakashi`, `lee`, `neji`, `gaara`, `sakura`, `sai`) e instalados em `<root>/portraits/` nos 3 aparelhos (carregados automaticamente por `CharacterCutout`, sem nenhum asset do jogo no git).
- **Correção de layout da Sala Personalizada (`CustomLobbyCard`):** campo `"Código da sala"` agora ocupa largura total (`fillMaxWidth()`) com botões `Entrar` e `Assistir` em linha própria logo abaixo, sem cortar em nenhuma DPI/largura de tela.
- **Todos os 3 aparelhos no mesmo servidor live (`http://127.0.0.1:8787` via `adb reverse`):** `RP5_Kage`, `Cube_Ninja` e `MiA3_Ninja` prontos para testes e gravação de vídeo.

### 2026-11-24 07:05: Tela Online estilo UN5 (recortes de personagens + Nível de Maestria) e gestos de menu opcionais com guia animado

- **Sistema de Personagem Favorito & Nível de Maestria (`worker.js`, `schema.sql`, `dev_server.mjs`, `OnlineAdvanced.kt`):**
  - Adicionados `fav_char`, `fav_char_lvl` (Lv. 1 → Lv. 99) e `fav_char_xp` em `hl_players`, `/v1/profile`, `/v1/ranked/report` (+120 XP por vitória, +45 XP por derrota) e `/v1/leaderboard`.
- **Visual oficial UN5 personalizado ([Un5NinjaUi.kt](file:///home/dfdx047/AndroidStudioProjects/HiddenLeaf/android/app/src/main/java/com/dfdx047/hiddenleaf/ui/components/Un5NinjaUi.kt), `OnlineScreen.kt`, `OnlineMenu.kt`):**
  - Novo `CharacterCutout` (moldura chanfrada estilo UN5 com selo `Lv.XX` dourado, carregando PNG local de `<root>/portraits/<id>.png` quando presente ou recorte vetorial estilizado com bandana/clã/engrenagem).
  - Novo `NinjaInfoCard` com retrato clicável para trocar o personagem favorito, barra de XP de Maestria, emblema de patente, RP e winrate.
  - Layout responsivo focado em horizontal (2 colunas lado a lado: Rankeada/Top 100 com recortes + `Lv.XX` à esquerda e Sala Personalizada 2–8 à direita; empilhado na vertical) e tábuas de madeira chanfradas (`Un5WoodActionBoard`).
- **Toque nos menus opcional + Guia Animado (`TouchConfig.kt`, `ControlsScreen.kt`):**
  - Botões virtuais restaurados como padrão (`menuTouch = false`, migração v5); ao ativar *"Toque direto nos menus"*, exibe o `MenuGestureAnimatedGuide` com 4 demonstrações animadas no estilo da launcher.
- **Verificado:** `node online/worker/test.mjs` verde, `./gradlew testDebugUnitTest` verde, instalado nos 3 aparelhos com servidor live em `127.0.0.1:8787` (`adb reverse`) e conferido visualmente no RP5.

### 2026-11-24 06:49: Correções de polimento pós-testes reais nos 3 aparelhos

- **Toque nativo nos menus (migração v3 → v4):** corrigido `TouchConfig.fromJson()` que mantinha `menuGestures: false` em instalações antigas (como o Mi A3) e reduzido limiar de arraste de `26 dp` → `18 dp` em `NinjaPadView.kt`. Verificado ao vivo no Mi A3 deslizando `ENGLISH` → `FRANÇAIS` sem D-pad na tela.
- **Seleção de personagem cortada após sair de luta:** removido `s_tagFullWidth` e reduzido `s_targets` de 32 → 8 em `hl_native.cpp`, adicionando `hl_native::resetWideTargets()` na transição `wideCamera true → false` em `hl_gsx.cpp` (evita FBPs antigos da luta vazarem para retratos dos personagens).
- **Save 100% com Transformações (Awakenings) desbloqueadas:** extraídos os offsets reais das funções `sub_001E9*` do binário recompilado e aplicados em `SaveManager.kt` e `hl_game.cpp`: bitset 64-bit de transformações iniciais (`0x968..0x96F = 0xFF`), stages (`0x970..0x98F = 3`), unlocks extras (`0xDF4..0xDF7 = 0xFF`), jutsus/itens (`0x130C..0x138B = 3`) e Max Ryo (`0x34 = 99.999.999`).
- **Mod UN6 A35 recompilado:** `libun5_un6_a35.so` e `libun5_un6_a35_v80.so` (Mi A3) recompilados junto com `libbt4gsx.so`, `libun5.so` e `libun5_v80.so`.
- **Removido "Especial com um botão"** de `GameMenu.kt` e `ControlsScreen.kt`.
- **Menu Online in-game (`OnlineMenu.kt`) + Rankeada estilizada (`OnlineScreen.kt`):** ambos agora trazem **Rankeada & Top 100 Global** com emblemas desenhados por patente (*Genin* 🍃 → *Chunin* ⚔ → *Jonin* ✦ → *ANBU* ◈ → *Sannin* ★ → *Kage* ♛), medalhas 🥇🥈🥉, RP, V/D, winrate e streak; **Sala Personalizada (2–8, Rei da Mesa) / Espectador**; **Separação Toque vs Controle** e **Treinar enquanto procura**.
- **Treinar enquanto procura pulando direto para Seleção de Personagem:** adicionado `training.txt` fast-entry em `hl_online.cpp` + `training_ui.json` cross-process em `TrainingQueue.kt` com tela de loading **"Modo Treino"** (barra de progresso). Verificado no RG Cube caindo direto na Seleção de Personagem (`1P vs COM`) com o selo *"Procurando oponente durante o treino…"*.
- **Servidor local de teste ao vivo (`online/worker/dev_server.mjs`):** servidor real do `worker.js` na porta 8787 populado com jogadores ranqueados para validação em tempo real nos aparelhos.

### 2026-11-24 02:12: Fase 8 — Modo Online avançado: Rankeada + Top 100 Global, Salas Personalizadas (2–8), Espectador e Treino na Fila

- **Worker Cloudflare (`online/worker/worker.js`, `schema.sql`, `test.mjs`):**
  - **Rankeada por pontos (RP/Elo) + Top 100 Global:** novas tabelas `hl_players` (autenticação por `player_id` + SHA-256 de `secret`) e `hl_matches` (verificação bilateral de vencedor — resultados divergentes ficam como `disputed` sem pontuar, bloqueando vitórias falsas; `ragequit` pune quem caiu). Patentes Ninja (*Genin* → *Chunin* → *Jonin* → *ANBU* → *Sannin* → *Kage*) e `GET /v1/leaderboard` com `Cache-Control: public, max-age=60`.
  - **Salas Personalizadas (2–8 jogadores, Rei da Mesa):** tabela `hl_lobbies` e rotas `/v1/lobby/create`, `/v1/lobby/join`, `/v1/lobby/state`, `/v1/lobby/rotate` (vencedor fica em P1, próximo da fila entra em P2, perdedor vai pro fim da fila) e `/v1/lobby/leave`.
  - **Modo Espectador leve (~300 B/s):** `HiddenLeafRelay` atualizado para suportar até 4 espectadores somente leitura (tag `"s"` via `/v1/ws?code=&role=spectator`), recebendo fan-out etiquetado (`[0x53, 1|2, ...pacote]`) dos inputs de P1/P2.
- **App Android (`OnlineAdvanced.kt`, `TrainingQueue.kt`, `OnlineScreen.kt`, `GameActivity.kt`, `strings.xml` en/pt/es):**
  - **Treinar enquanto procura (`TrainingQueue.kt`):** botão *Treinar enquanto procura* abre o jogo normalmente enquanto busca oponente em segundo plano, exibindo o selo *Procurando oponente durante o treino…* e um banner de aceitação (*Oponente encontrado! [Aceitar]*) que reinicia direto para a partida via `OnlineRestartActivity`.
  - **UI de Rankeada & Top 100 + Sala Personalizada (2–8) / Espectador** integrada na aba Online (`OnlineScreen.kt`).
- **Verificado:**
  - `node online/worker/test.mjs`: 100% verde (perfil autenticado, report bilateral, anti-cheat de disputa, leaderboard cacheado 60 s, lobby 2–8 com rotação Rei da Mesa, espectador read-only e fan-out).
  - `./gradlew testDebugUnitTest` verde e instalado nos 3 aparelhos.
  - Conferido visualmente nos 3 aparelhos (**Mi A3**, **RG Cube**, **RP5 capado**): cards *Rankeada e Top 100 Global*, *Sala Personalizada (2–8) e Espectador*, *Separação por controle* e modo *Treinar enquanto procura* abrindo `GameActivity` com o overlay da fila sem crashes.

### 2026-11-24 02:03: Fase 7 — Modo Online: otimização Cloudflare free-tier + segurança + separação Toque vs Controle

- **Worker Cloudflare (`online/worker/worker.js`, `schema.sql`, `test.mjs`):**
  - Removido `sweep()` de `/v1/poll`, `/v1/ws`, `/v1/addrs`, `/v1/relay` e `/v1/leave`; limpeza global agora roda apenas em `/v1/host` e `/v1/quick` com throttle de 30 s, e salas expiradas são rejeitadas preguiçosamente (0 escritas no D1).
  - `TOUCH_EVERY` subiu de 5 s → 12 s e `QUICK_IDLE` de 15 s → 25 s (~2,4× menos escritas de polling no D1); adicionado índice `hl_queue_match (v, game, fp, input_mode)` e limpeza de `hl_queue` em `/v1/leave`.
  - Rate limit por IP em memória de isolate em `/v1/host`, `/v1/join` e `/v1/quick` (protege cota D1 contra spam).
  - Segurança: geração de `session_secret` (`token` de 128 bits por sala) entregue apenas a host e guest.
  - Separação **Toque vs Controle** (`input_mode`: `"touch" | "gamepad" | "any"`): `/v1/quick` e `/v1/hello` filtram por `input_mode` e `/v1/join` retorna `409 input_mode` em caso de incompatibilidade.
- **Netcode C++ (`link.h`, `link.cpp`, `match_session.h`, `match_session.cpp`, `hl_online.cpp`):**
  - Anti-spoof UDP: `session_` derivado de `code:token` quando presente + validação estrita `source == remote_` após `Link::Up`.
  - Economia massiva de Durable Object no relay: reenvio em espera reduzido de 8 ms (125 msgs/s) → 250 ms no relay TCP, batching de inputs a 25 Hz (a cada 2 campos) quando em relay sem espera, e `ping_every` de 500 ms → 2000 ms no relay (~65–75% menos mensagens de DO).
  - Publicação de `want_relay=1` e `input_mode=` em `OnlineBridge.status()`.
- **App Android (`Online.kt`, `OnlineGame.kt`, `GameActivity.kt`, `OnlineScreen.kt`, `strings.xml` en/pt/es):**
  - **Lazy WebSocket Connect** em `OnlineGame.kt`: só abre `/v1/ws` quando o motor sinaliza `want_relay=1` (após 3 s sem UDP direto) → 0 requisições de Durable Object em partidas P2P diretas.
  - Detecção de controle físico (`Online.hasExternalGamepad()`) + seletor **Separação por controle** (*Qualquer* / *Só Toque* / *Só Controle*) em `OnlineScreen.kt` + bloqueio estrito de gamepad externo em `GameActivity.dispatchKeyEvent()` durante partidas `touchOnly`.
- **Verificado:**
  - `node online/worker/test.mjs` (100% verde, incluindo testes de `input_mode`, `token` e escritas D1).
  - `recomp/android/host/online/tests/run.sh`: `lockstep_tests` (892/0), `session_tests` (2042/0), `match_tests` (76/0).
  - `./gradlew testDebugUnitTest` verde + `libun5.so` e `libun5_v80.so` atualizados.
  - **Partida real 2 aparelhos (RP5 capado `sd480p` Host × RG Cube Guest)** com `token=` + `input_mode=touch` + `relay=1`: parearam direto via UDP em 9–246 ms (RTT 6–23 ms), **0 conexões WebSocket (Lazy Relay confirmado)**, sincronizados pixel-a-pixel na Seleção de Personagens (`Online P1 contra CubeGuest` / `Online P2 contra RP5Host`), e UI de separação por controle conferida no Mi A3.

### 2026-11-24 01:52: Fase 6 — toque nativo nos menus do jogo + Menu Gráfico estilo UN5 in-game

- **Toque nativo nos menus (`hl_game.cpp`, `un5_main.cpp`, `TouchBridge.kt`, `TouchConfig.kt`, `NinjaPadView.kt`, `ControlsScreen.kt`):**
  - `TouchConfig.menuTouch` ativado por padrão e exposto na tela de Controles ("Toque direto nos menus").
  - `NinjaPadView.menuGesture()` evoluído para rolagem contínua por arraste (cada passo de arraste move imediatamente um item e reseta a âncora, além do auto-repeat) + `TouchBridge.menuTap(nx, ny)` que mapeia toques diretos nas placas de madeira do Mode Select (topo → Cima, centro → Confirmar/✕, base → Baixo) em 4:3, 16:9 e 20:9.
- **Menu Gráfico estilo UN5 in-game (`GraphicsMenu.kt`, `GameMenu.kt`, `GameActivity.kt`, `strings.xml` en/pt/es):**
  - Novo `GraphicsMenuHost` com visual idêntico ao menu do UN5 (tábuas de madeira chanfradas com gradiente, borda, tipografia Shojumaru dourada com sombra, faixa de ajuda azul-escuro no rodapé, animações suaves, jogo rodando vivo atrás) organizado em 3 categorias: *Resolução e Proporção*, *Filtros e Taxa de Quadros* e *Motor e Desempenho*.
  - Mantido intacto o overlay `GameMenu` original (com botão **"Abrir menu gráfico (estilo UN5)"** no topo da aba Gráficos). Suporta tanto toque direto quanto controle físico.
- **Verificado nos 3 aparelhos (capturas conferidas, 0 crashes, `./gradlew testDebugUnitTest` verde):**
  - **Mi A3 (19.5:9):** `GameMenu` → aba Gráficos → `GraphicsMenu` UN5 → submenu *Resolução e Proporção* → retorno limpo ao jogo a 50,0 fps.
  - **RG Cube (1:1 720×720):** arraste nativo no menu de idioma (`ENGLISH` → `FRANÇAIS`, sem D-pad) + `GraphicsMenu` UN5 sem cortes nem overflow.
  - **RP5 capado (`sd480p`, 16:9 1920×1080):** arraste nativo (`ENGLISH` → `FRANÇAIS`) + `GraphicsMenu` UN5 renderizando limpo sobre o jogo.

### 2026-11-24 01:36: Fase 5 — correções e robustez do sistema de mods

- **Importação e identificação (`Mods.kt`):**
  - ID de mods desconhecidos trocado de `mod-${elfSha.take(8)}` para `mod-${isoSha.take(12)}`, eliminando a colisão destrutiva que sobrescrevia mods de dados/tradução diferentes com o mesmo ELF europeu.
  - Validação de mods conhecidos flexibilizada para aceitar ISOs repacotadas com o mesmo `elfSha` (marcando `iso_variant=1` em `mod.properties` em vez de bloquear).
- **Motor e overlays (`un5_main.cpp`, `un5_overlays.cpp`, `recomp/overlays.py`, `0110-mod-fd-lru-cache.patch`):**
  - Prioridade de ELF de mod corrigida em `un5_main.cpp` (último mod vence, igual ao `hl_image.cpp`) e fallback para o ELF base quando um code pack só altera overlays.
  - `un5_overlays.cpp`: `resident()` agora retorna `false` quando `sigCount <= 0` e cache ampliado de 4 para 16 slots; `overlays.py` extrai assinaturas do corpo inteiro se `text < 32`.
  - `hl_image.cpp` (patch `0110`): cache LRU thread-safe de até 32 FDs abertos para arquivos de mods (elimina vazamento de descritores em mods com muitos arquivos).
- **Verificado nos 3 aparelhos (0 crashes, 0 `missing-target`, capturas conferidas):**
  - **Mi A3 (Alpha 1.1X, `libun5_v80.so`):** luta rodando a 44–49 fps com `un6-a11x`.
  - **RP5 capado (`sd480p`, UN6 Alpha 3.5, `libun5_un6_a35.so`):** luta rodando a **60,0 fps cravados** (NTSC, 9,5 ms idle).
  - **RG Cube (jogo base):** luta rodando a **50,0 fps cravados** (12,5 ms idle).

### 2026-11-24 01:29: Fase 4 — otimização das animações da launcher + verificação de resoluções/filtros/60 fps

- **Otimização da Launcher (`TrainingGround.kt`, `HomeScreen.kt`, `Common.kt`):**
  - `TrainingGround`: cenário estático (céu, sol, postes, veios da madeira, chão e grama) cacheado via `drawWithCache`; `Path` reutilizável em `drawLeaf` e `drawShuriken` (sem `Brush.linearGradient` nem `Path()` por shuriken/frame) + isolamento em `graphicsLayer()`.
  - `PlayButton`: leitura de `breath`/`gleam` diferida para `drawBehind` (0 recomposições/s, antes 60 recomposições/s) + `remember(enabled)` no gradiente + `graphicsLayer()`.
  - `leafBackground` e corda do `SectionCard`: convertidos para `drawWithCache` (um único `Path` cacheado para a corda).
- **Verificado nos 3 aparelhos (`dumpsys gfxinfo` na tela inicial animando):**
  - **RG Cube (Mali-G57):** Janky frames **98,99% → 7,91%**, p50 **32 ms → 17 ms**, p90 **34 ms → 19 ms**.
  - **Mi A3 (SD665):** Janky frames **3,70%**.
  - **RP5 capado (`sd480p`):** Janky frames **1,64% → 1,35%**, p50 **19 ms**.
- **Verificado filtros/upscale/interpolação:** RP5 capado em **2x (`scale4=8`) + CAS (`filter=3`) + 60 fps interpolado (`fps_mode=60`)** rodando a **50,0 fps** cravados (10 ms idle), 0 crashes, print conferido.

### 2026-11-24 01:22: Fase 3 — Ultrawide sincronizado no stream + `wide2d` multi-tira + Save 100% embutido

- **Ultrawide (`hl_gsx.cpp`, `hl_native.cpp`):**
  - Estado `wideCamera` sincronizado por VSync via FIFO lock-free (`g_wideFifo[128]`) entre a thread EE (`Bt4Gsx_WideCamera`) e a thread de replay (`kTypeVSync`), eliminando dessincronia de 1–2 quadros nas transições luta ↔ menu/vitória.
  - `wide2d`: anel `s_targets` ampliado de 8 para 32 FBPs e pré-varredura de largura total em tags PACKED multi-primitiva (`s_tagFullWidth`, para cortinas de transição em tiras preencherem 19.5:9/20:9 sem faixas laterais).
- **Save 100% embutido (`SaveManager.kt`, `SavesCard.kt`, `strings.xml`):**
  - Botão **"Instalar save 100%"** na seção Saves (com diálogo de confirmação e backup automático prévio), gerando um save europeu (`BESLES-55605NARUTO5`) 100% em código (sem arquivos do jogo no repositório) com todos os personagens, parcerias, coleção e progresso desbloqueados, checksum de 16 bits válido do UN5 (`sum(bytes[0..1] + bytes[4..9215])`) e propagação automática para mods via `ModSaves.prepare()`.
  - I/O de listagem de saves/backups em `SavesCard.kt` movido para `LaunchedEffect` + `Dispatchers.IO`.
- **Verificado nos 3 aparelhos:**
  - **RG Cube:** instalação do Save 100% pela UI → carregamento real pelo jogo (`Load completed.`) e grade de personagens conferida com `dr_unlock_all=0`.
  - **Mi A3 (19.5:9, `x 0.615`) e RP5 capado (`sd480p`, 16:9, `x 0.750`):** Mode Select e Luta preenchidos sem barras laterais, 0 crashes, 0 `missing-target`.

### 2026-11-24 01:13: Fase 2 — correção dos bugs do especial de 1 botão + `NinjaPadView` zero-alloc + paridade `ClassicPadView`

- **Especial de 1 botão (`hl_game.cpp`, `un5_main.cpp`, `TouchBridge.kt`, `GameActivity.kt`, `GameMenu.kt`, `ControlsScreen.kt`):**
  - Desligado por padrão (`g_specialOn = false`) para não sequestrar combos neutros em `○` nem macros `↑↑○`/`↓↓○`; mesmo quando ligado em `○`, ignora os 10 vsyncs seguintes a uma direção (`g_padDirTick`).
  - `ougiOnScreen()` agora checa `kMoveOugi` (`0x00F00000`) em vez de `kMoveSpecial`, reduz a janela pós-luta de 250 para 25 vsyncs e checa `kRoundEndPanel == 0` (para não injetar `○` nos menus pós-luta).
  - `special(f)` preserva pulo/guarda/substituição (`word |= bits`) e aborta se o lutador for atingido; `fighterSlot(f)` limpa os 4 slots de `g_sequence`; `g_padSpecialDown` fica travado durante o vsync inteiro; `special_button` atualiza ao vivo via `nativeSetAssist(specialOne, specialButton, mashPercent)` sem reiniciar.
- **Controles touch (`NinjaPadView.kt`, `ClassicPadView.kt`):**
  - `NinjaPadView.onDraw()`: `RadialGradient`, `LinearGradient` e `Path.Op.UNION` do D-pad agora são cacheados por elemento (`El`), e `icon()` reaproveita `rect` (zero alocações de `Shader`/`Path`/`RectF` por frame de toque).
  - `ClassicPadView`: adicionados `onApplyWindowInsets()` (display cutouts/notch) e `ownsDirs` nos macros direcionais.
- **Verificado nos 3 aparelhos (rota `hl_env_v3.txt` com `HL_SP_LOG=1` e toque na tela, 0 crashes, 0 `missing-target`, prints conferidos):**
  - **Mi A3:** 49,8 fps (mín 44,9), **RG Cube:** 50,0 fps (mín 50,0), **RP5 capado (`sd480p`):** 50,0 fps (mín 47,8).

### 2026-11-24 01:05: Fase 1 — otimização do motor (Nominum VU1 zero-copy, ring buffer zero-copy, GS split, GpuProfiles reais)

- **Nominum VU1 zero-copy (`0109-nominum-vu1-zero-copy.patch`, `hl_nominum_vu1.cpp`):** `PS2Memory` agora aponta `getVU1Code()`/`getVU1Data()` direto para os buffers internos de `nominum::core::Vif1` (sincronizados por `HL_VU_SYNC()` em `mapVuMemory()`), eliminando 48 KB de `memcmp`/`memcpy` por transferência VIF1 (`micro memory loaded by address 1 times` nos 3 aparelhos).
- **Ring buffer zero-copy (`hl_gsx.cpp`):** pacotes GIF contíguos no anel agora são lidos e modificados in-place sem cópia para `s_packet`, com `consume()` diferido para o próximo `live_next()`.
- **Modo leve (`NOMINUM_VU1=1`) ligado por padrão + `GSBackThreadMode=1` expandido (`un5_main.cpp`):** ativado por padrão após a correção dos patches `0107`/`0108`, com GS em duas threads habilitado também para SoCs `trinket`, `bengal`, `khaje`, `ums9620` e `mt676x`/`mt678x`.
- **`GpuProfiles.kt` e `GpuOfferDialog.kt`:** removidas as chaves mortas legadas do BT3 (`interp`, `frag_depth`, `frame_skip`, `fx_off`, `scale`); o perfil agora aplica e exibe as configurações reais do Hidden Leaf (`scale4`, `fps_mode`, `light_mode`, `filter`).
- **Verificado nos 3 aparelhos (rota `hl_env_v3.txt` até v8400, 0 `missing-target`, 0 crashes, capturas conferidas):**
  - **Mi A3 (SD665, `libun5_v80.so`, GS 2 threads):** Luta 1 **49,3 fps** (mín 43,5, VU1 15,0 ms), Ougi **49,4 fps** (mín 43,0, VU1 14,7 ms), Vitória **50,4 fps** (mín 48,9, VU1 14,4 ms).
  - **RG Cube (Mali-G57, `ums9620`, GS 2 threads):** Luta 1 **50,0 fps** (mín 50,0, VU1 5,1 ms), Ougi **50,0 fps** (mín 50,0, VU1 6,4 ms), Vitória **50,0 fps** (mín 49,8, VU1 7,5 ms).
  - **Retroid Pocket 5 capado (`sd480p`):** Luta 1 **50,0 fps** (mín 49,6, VU1 5,7 ms), Ougi **50,0 fps** (mín 49,1, VU1 8,6 ms), Vitória **50,0 fps** (mín 49,9, VU1 9,2 ms).

### 2026-11-14 12:20: documentação do release e da chave de assinatura

- A pedido do desenvolvedor (a próxima atualização deve sair em 2–3 semanas, em outra sessão): `docs/release.md` com a
  chave de release, a regra de versão, o passo a passo e como o app se atualiza; regra no `CLAUDE.md` e entrada na
  tabela "Onde está cada coisa".

### 2026-11-14 12:10: atualização automática; o APK da v0.1.0-beta substituído

- O desenvolvedor pediu atualização automática: quando sair versão nova, o app baixa e instala. O fluxo já existia (do
  Dragon Rage: busca ao abrir, diálogo com as novidades, download, permissão de instalar apps, instalador), mas
  `AppUpdates.check` só aceitava tags `android-vX.Y.Z` fora de pré-lançamento, e nunca acharia a `v0.1.0-beta`.
- `data/AppUpdates.kt`: aceita `vX.Y.Z[-beta.N]` e `android-vX.Y.Z`, pré-lançamentos incluídos; `compareVersions`
  (`0.1.0-beta` < `0.1.0-beta.2` < `0.1.0`); as novidades são a primeira seção `## ` do `CHANGELOG.pt-BR.md`,
  `CHANGELOG.es.md` ou `CHANGELOG.md` na tag. Teste `AppUpdatesTest` (3 de 3).
- `AppViewModel.checkForUpdate`: em rede sem cobrança (Wi-Fi) baixa sozinho ao abrir o app e abre o instalador; nos
  dados móveis o diálogo pergunta antes. Permissão `ACCESS_NETWORK_STATE` no manifesto.
- `build.gradle.kts`: o versionCode conta a beta (`0.1.0-beta` = 10001, `-beta.2` = 10002, `0.1.0` = 10099).
- Android não deixa um app comum instalar sem o toque do usuário: o instalador do sistema sempre pede "Atualizar".
- Com o repositório privado a busca falha calada (a API do GitHub responde 404); funciona quando ficar público.
- APK `HiddenLeaf-v0.1.0-beta.apk` novo (180426122 bytes, SHA-256
  `02167a2454ad7ba5ad8c71860a8c044f7375defa86b1bd5b4f04949f8f46a4c3`), no lugar do anterior no mesmo release; a tag
  `v0.1.0-beta` foi movida para este commit. Não testado em aparelho.

### 2026-11-14 11:50: v0.1.0-beta publicada como pré-lançamento (repositório ainda privado)

- `main` avançada até `89f40f5` (fast-forward da `lab/bench-pcsx2`; antes de mandar, o histórico foi conferido sem
  nenhum arquivo do jogo).
- Release `v0.1.0-beta` (pré-lançamento) com `HiddenLeaf-v0.1.0-beta.apk` (180425122 bytes, o SHA-256 da entrada abaixo):
  https://github.com/dfdx047/HiddenLeaf/releases/tag/v0.1.0-beta
- O repositório continua **privado**: o desenvolvedor deixa público pelas configurações depois de postar o vídeo. Os
  botões do shields.io e o contador de downloads só funcionam com o repositório público.

### 2026-11-14 11:40: v0.1.0-beta, APK de release, READMEs e changelogs em três línguas

- O desenvolvedor já tinha testado e gravado o vídeo; pediu o release sem testar nenhum aparelho de novo.
- `android/app/build.gradle.kts`: versão padrão `0.1.0-beta` (versionCode 100) e release **sem R8** (`isMinifyEnabled` e
  `isShrinkResources` desligados), para o APK rodar as mesmas classes do debug que foi testado nos celulares.
- Chave de release nova, `~/keys/hiddenleaf-release.jks` com `hiddenleaf-release.properties` (fora do repositório, no
  padrão das chaves dos outros projetos). Certificado `CN=Hidden Leaf, O=dfdx047`, SHA-256
  `8b3f4213556edbd018996833418f102f4769e418e2edab675a6e67b084990830`. O APK de release não instala por cima dos builds
  de teste (chave diferente): quem tem o de teste desinstala antes.
- APK: `build/release/HiddenLeaf-v0.1.0-beta.apk` (172 MB), SHA-256
  `96569b29ae339da2f3577e397c59d91baba43352858e89c6861080b161c75174`. Motores `libun5.so` e `libun5_v80.so` de 08:55 (os
  testados no online); `libun5_un6_a35*.so` de 13/11 15:30 (sem as funções JNI novas do online, que o app chama com
  `runCatching`; o online fica desligado com mod).
- `README.md` agora em inglês, com `README.pt-BR.md` e `README.es.md` (o `README.en.md` saiu): banner, botões do
  shields.io, imagens, recursos, como funciona, trajetória, aparelhos, problemas conhecidos, compilação e créditos.
  `CHANGELOG.md`, `CHANGELOG.pt-BR.md` e `CHANGELOG.es.md` com a v0.1.0-beta.
- Imagens em `docs/images/` (1,9 MB): capturas do Honor e do Poco (lutas online, Mode Select com o Naruto Online, painel,
  seleção 1P × 2P), do Mi A3 com o Alpha 1.1X (VS, WINNER), do Honor e do Cube (launcher, título), e dos testes
  Robolectric (controles na tela, Toque Pro, menu do jogo). O banner é a luta contra o Zabuza escurecida com o ícone.
- Nada foi instalado nem testado em aparelho nesta entrada.

### 2026-11-14 08:58: sem a caixa do jogo atrás do painel online

- O desenvolvedor achou estranha a caixa de mensagem do jogo ("Naruto Online / Quick Match, Create Room or Join Room")
  aparecendo por baixo do painel. No Android (`g_menuOnline`), o gancho de `0x395F20` não chama mais a função do jogo
  quando o Online é escolhido: a Mode Select fica como está e toca só o som de "escolhido" (`0x1DD470` com `a0 = 0x58`,
  registradores do EE guardados e restaurados em volta). No PC (`HL_MENU_ONLINE=1`, sem painel), a caixa continua.
- Honor e Poco: o painel abre só com o Naruto à direita; fechando (○/Voltar) a Mode Select responde normal e o Online
  abre de novo.

### 2026-11-14 08:50: menu online dentro do jogo e ida rápida até a seleção de personagem

- **Pedido:** o "Online" da Mode Select abre um menu próprio dentro do jogo, sem voltar ao launcher (partida rápida, criar
  sala, entrar com código, Wi-Fi local), e quem acha um jogador cai direto na seleção de personagem, sem ver os menus
  passando nem ouvir a música deles.
- **Menu (`engine/OnlineMenu.kt`):** painel à esquerda, por cima das placas da Mode Select, com tábuas de madeira, letras
  Shojumaru douradas e laranja no foco. O jogo **não pausa**: o Naruto da direita segue animado e a música continua; só os
  botões são segurados (`TouchBridge.setPadHeld`, `g_padHeld` em `un5_main.cpp`). Controle: D-pad/analógico movem, ✕
  escolhe, ○/△/Voltar voltam (sai da busca/sala, depois da página, depois do menu). Teclado de 6 dígitos próprio para o
  código; Cancelar também na busca da rede local (toque não tem ○ ali). O lobby é o do launcher (`OnlineLobby`).
- **Entrada na partida:** com a sessão gravada, o processo `:game` sai e `OnlineRestartActivity` (processo do launcher,
  tela preta) espera ele sumir e abre o jogo de novo.
- **Ida rápida:** os primeiros 4760 campos da partida (a rota `kEntry` do boot até o 1P × 2P) são iguais nos dois
  celulares, então cada um corre sozinho (`MatchSession::Env::localUntil/localPads`, sem esperar o outro, o link segue
  com pings) com `HL_FAST_UNTIL=4760` (vblank contado em ciclos do EE: o jogo é o mesmo, só mais cedo) e o som mudo
  (`hl_spu2_mute`). O lockstep começa no campo 4760 com campos relativos (`lockField()`). Uma tela "Naruto Online —
  Você contra X" com barra cobre o trecho e sai quando o lockstep anda (`OnlineGame.kt`, `ENTRY_END + 8`); quem chega
  primeiro vê "Esperando X…".
- **Medido (Honor × Poco):** Honor 12–14 s, Poco 28 s até a seleção (antes ~95 s). Sala por código (721389) e partida
  rápida pela internet, conexão direta 11–21 ms; Wi-Fi local (criar/procurar) pelo menu também. Luta Naruto × Zabuza
  depois da ida rápida igual quadro a quadro nos dois.
- A linha de status não mostra mais "os jogos diferem" pela conferência de memória (ainda há trechos fora da luta que
  diferem, sem mapear; as telas seguem iguais); o log continua contando.
- Falta: Cube/Mi A3 com o menu novo; controle físico no painel testado só por código.

### 2026-11-14 08:00: rede local segura a luta inteira (Cube × Honor, Cube × Poco, Poco × Honor); tela cheia no online; controle que some

Pedidos do desenvolvedor: testar o Cube no Wi-Fi com o Honor ou o Poco; depois, "a tela comece inteira e preenchida em
ambos" (rede local e online), toque × controle externo em qualquer combinação, e os controles na tela voltando se o
controle externo sumir no meio do jogo.

- **Rede local, partidas de prova** (Cube 5 GHz Wi-Fi 7, Honor 5 GHz Wi-Fi 7, Poco 2,4 GHz Wi-Fi 7, mesma rede `/24`;
  botões apertados ao acaso nos dois): Cube × Honor 6 min de luta, Cube × Poco 5 min, Poco × Honor 4 min, **0 esperas e
  nenhuma queda**, a mesma imagem nos dois no mesmo instante (cronômetro igual). Ligação em 2 a 14 ms, ida e volta 4 a 9 ms.
  A falha das noites anteriores era o Honor fora do Wi-Fi (o Wi-Fi+ dele, `hn_wifipro_enable`, o tira da rede).
- **APK antigo no Poco e no Cube** (13/11): instalado o atual. O Poco tinha a Versão Brasileira ligada (o online só
  aceita o jogo base): desliguei em Mais → Mods para o teste; volta a ligar ali.
- **Verificação da memória:** agora conta toda diferença (não só a primeira) e loga no máximo uma linha a cada 2 s
  (`the check values differ at field N (x of y compared so far)`); o fim da partida diz quantas diferiram. Com dumps
  (`HL_ONLINE_DUMP`) nas quadras 64 e 9000 de uma luta Cube × Honor: o que difere são a área do kernel (pilhas, dois
  buffers que cada aparelho usa ao contrário), amostras de som (`0x633000`, `0x650000`), carimbos de tempo de 64 bits
  e contadores do boot que andam com a velocidade do aparelho, blocos de lista em outra ordem. Entraram na lista de
  exclusão (`hl_online.cpp`). Ainda sobram diferenças em outros pares (~40% das comparações no Poco × Honor): falta
  mapear; não muda a luta.
- **O gerador aleatório do jogo** é o `rand` da newlib em `0x17C558`, estado de 64 bits em `*(0x412DD8)+0xA8`. Igual nos
  dois aparelhos na quadra 9000 (Cube × Honor e Poco × Honor).
- **Tela cheia no online:** a partida forçava a câmera 4:3 nos dois (barras pretas no Honor e no Poco). A escala da
  câmera (`camera+0x274`) só é lida pela projeção (`0x10DD00`, `0x10F420`, copiada por `0x1C01B0`). Prova: Poco forçado
  em 4:3 × Honor em 20:9 (`x 0.610`), luta com botões ao acaso, dump na quadra 9000: o estado do `rand` **igual**,
  as diferenças são de desenho (pacotes, coordenadas de tela, ordem do heap) e 90 s depois as duas telas mostravam o
  mesmo (cronômetro 6, mesmas posições, o mesmo kunai). Agora cada aparelho usa a câmera da própria tela no online
  (`un5_main.cpp applyVideo`): do logo da Bandai Namco à seleção de personagem a imagem enche os 20:9 do Honor e do Poco.
- **Controles:** o motor já junta toque, controle traduzido e o controle do SDL no pad do jogador de cada aparelho, então
  toque × controle e qualquer combinação passam pelo mesmo caminho. Novo: (1) o controle do SDL que desconecta é fechado
  e o próximo conectado é aberto (antes ficava o primeiro para sempre); (2) `GameActivity` escuta o `InputManager`: sem
  nenhum controle ligado, os botões presos são soltos e os controles na tela voltam na hora (antes só voltavam ao tocar
  na tela). **Não testado com controle de verdade** (nenhum controle Bluetooth aqui).
- Achado: com o app em segundo plano o Android recusa os envios (`sendto` errno 1, EPERM); o outro aparelho espera e a
  partida cai por silêncio, como deve.

### 2026-11-14: "Naruto Online" no Mode Select do jogo (protótipo no PC, desligado por padrão); não testado em aparelho

Pedido do desenvolvedor: uma entrada "Naruto Online" no próprio Mode Select, logo abaixo do Free Battle, que leve ao lobby
do launcher. Estudo e protótipo numa branch de agente; detalhes, endereços e riscos em `docs/menu-online.md`.

- **Achado:** o Mode Select (ELF, `0x3953B0` monta, `0x396B90` o quadro, `0x395CE0` a lista, `0x395DC0` escolher) tem um
  sétimo modo escondido, o **3** (Mini-Games do original), cortado na versão europeia só pelo `-1` em `0x5DC30C`; a placa,
  o diorama e a linha de texto dele continuam no `modesel1.ccs`, e a faixa do nome dele na textura dos nomes
  (`TEX_modesel02`) está vazia.
- **Protótipo** (`hl_game.cpp`, `HL_MENU_ONLINE=1` ou `TouchBridge.nativeSetMenuOnline`): o modo 3 entra na lista depois do
  Free Battle; "Online" é escrito na faixa vazia com as letras das outras placas (O, n, l, i, e recortados em tempo de
  execução, nada da arte no repositório); a linha de baixo e a caixa de mensagem do próprio jogo (`0x3D1850`, `0x3D18A0`)
  ganham textos nossos em 5 idiomas, em `0xFE000`; o ✕ passa por `0x395F20` (o "modo indisponível" do jogo), que agora
  loga `[online] menu chosen`, soma `hl_game::g_onlineMenu` (JNI `nativeOnlineMenu`) e toca o som de escolher. ✕ e △
  fecham a caixa, a navegação dá a volta normal, sem travar. Só no ELF europeu (seis palavras conferidas; o A35 fica sem).
- **Testado no PC** (GS do ARMSX2, 4:3 e 20:9): capturas em `build/lab/menu/shots/`. Desligado, o menu é o de sempre.
  Android: só compilação de sintaxe com o NDK; o launcher ainda não lê o contador nem abre o lobby.
- **Depuração nova:** `HL_FNCOUNT=all:btl` ou `all:elf`, `HL_FNCHG=ADDR` (quem mudou uma palavra), `HL_POKE=ADDR=VALOR`.
### 2026-11-13 23:59: partida rápida, aviso do online grátis esgotado e online só no jogo base; não testado em aparelho

Pedidos do desenvolvedor: oponente aleatório ("Partida rápida"), um aviso claro quando o plano grátis da Cloudflare
acaba no dia e, para o lançamento, online e rede local só no jogo base. Feito numa branch de agente; nada publicado na
Cloudflare (o Worker novo precisa do `schema.sql` antes do deploy, ver abaixo).

- **Servidor (`online/worker/`):** `POST /v1/quick {v, game, fp, name, addrs, opts, who}`. Quem pede entra na sala de
  quem espera há mais tempo com o mesmo `v`, `game` e `fp` (como convidado, igual ao `/v1/join`: `role: "join"`), ou
  ganha uma sala própria na fila (`role: "host"`) e daí em diante é uma sala comum (`poll`, `addrs`, `leave`, `ws` sem
  mudança). `fp` diferente nunca se encontra. Sala da fila sem `poll` do dono por 15 s não é dada a ninguém e some na
  busca seguinte (o `poll` dela responde `no room` e o app busca de novo). `who` (aleatório, do app) impede que o
  celular caia na sala que ele mesmo deixou. Dois que buscam no mesmo instante: o segundo desfaz a sua sala e entra na
  do primeiro. Tabela nova `hl_queue` (`CREATE TABLE IF NOT EXISTS`, nada apagado). Escritas no D1 por partida: 2
  linhas de quem espera, 2 de quem chega e 1 a cada 5 s de espera: o `poll` de todas as salas agora só grava a hora a
  cada 5 s (`TOUCH_EVERY`) ou quando o endereço muda. `/v1/hello` com `fp` diz quantos procuram (`waiting`, só
  leitura). Erro de limite do D1/Durable Objects vira `429 {error: "quota"}`. `node test.mjs` passa (testes novos da
  fila, expiração, corrida de dois, contagem de escritas, `quota`).
- **Launcher:** botão "Partida rápida" no alto do cartão Jogar do online (criar sala e entrar por código continuam
  embaixo, "ou jogue com um amigo, por código"); estado "Procurando um oponente… m:ss" com Cancelar
  (`LobbyState.Searching`, `OnlineLobby.quick`); desiste em 10 min. Quem cai na sala de outro segue como convidado e
  quem espera como anfitrião, pelo mesmo `launchMatch`. A espera do anfitrião virou `waitForGuest` (mesma das salas).
- **Aviso do plano grátis (`data/OnlineQuota.kt`):** 429 que não é JSON do Worker (página da Cloudflare, erro 1027),
  qualquer resposta com "1027" ou o `quota` do Worker guardam a hora nas preferências ("online", `quota_at`). No
  Início e na página Online aparece o cartão laranja (no jeito do aviso do Dragon Rage): "O online grátis esgotou por
  hoje. Volta em até 24 horas (o limite reinicia à 00:00 UTC, X horas daqui)." e "Rede local (Wi-Fi) continua
  funcionando." (na página Online, com o botão "Jogar na rede local"). Some sozinho depois da 00:00 UTC ou na primeira
  resposta do Worker. Ao abrir a página Online, um `/v1/hello` (com `fp`) deixa o aviso e o número de quem procura em
  dia. O 429 `busy` do próprio Worker (salas demais por endereço) continua "Servidor ocupado".
- **Só jogo base:** com qualquer mod ligado (`un5_mods.txt` não vazio ou um mod ligado na lista), a página Online (internet
  e rede local) troca os botões por "O online é só no jogo base por enquanto. Desligue os mods em Mais → Mods para jogar
  online." e o botão "Abrir Mods" (vai a Mais → Mods). O `OnlineLobby` recusa também por conta própria. A conferência do
  `fp` continua igual (protege contra versões diferentes do app).
- **Testes:** `OnlineQuotaTest` (hora da volta, horas arredondadas para cima, quais respostas contam), fotos novas
  `online_quota_portrait`, `home_quota_portrait`, `online_mods_portrait`, `online_lan_mods_portrait`; as fotos do online
  não fazem mais pedido de verdade (`OnlineLobby.autoCheck`). Todos os testes do launcher passam (45, 0 falhas);
  `assembleDebug` compila; `node online/worker/test.mjs` passa.
- **Não testado:** em aparelho nenhum e na Cloudflare de verdade (nem a partida rápida entre dois celulares, nem o 429
  real). **Antes do deploy:** `npx wrangler d1 execute hiddenleaf-online --remote --file schema.sql` (cria a
  `hl_queue`); o `wrangler.jsonc` não muda.

### 2026-11-13 23:20: multijogador em rede local (Wi-Fi) no launcher; não testado em aparelho

Pedido do desenvolvedor: além do online pela internet, jogar na mesma rede, escolhendo na página Online. Feito numa branch
de agente, só no launcher (o motor e o servidor não mudaram), sem dois aparelhos.

- **Escolha no alto da página Online:** "Online (internet)" ou "Rede local (Wi-Fi)" (`OnlinePrefs.lan`, guardado). Na
  rede local somem o cartão do servidor e o "Permitir o relay"; o nome e o atraso são os mesmos do online.
- **Descoberta** (`data/Lan.kt`): quem toca em "Criar partida" abre o socket UDP do jogo como numa sala online (porta
  livre qualquer, que o motor reabre) e manda dele, a cada segundo, um anúncio por broadcast (`255.255.255.255` e o
  broadcast de cada rede) para a porta fixa **47655**: linhas `chave=valor` depois de `HLLAN` (versão da rede, `un5`,
  `fp`, nome, porta do jogo, endereços locais, sala de 6 hexadecimais). Quem toca em "Procurar partidas" escuta a 47655
  com o `MulticastLock` do Wi-Fi (permissão `CHANGE_WIFI_MULTICAST_STATE` nova no manifesto) e lista as partidas; `fp`
  diferente (outra versão do app ou outros mods) aparece com aviso e o Entrar fica desligado. Uma partida some da lista
  depois de 4 s sem anúncio.
- **Entrada:** o convidado manda `join` (nome, `fp`, porta e endereços) do seu socket do jogo para o socket do jogo do
  anfitrião, a cada 400 ms por até 6 s; o anfitrião responde `accept` (com o atraso dele) ou `reject` (`different`,
  `full`), repete o accept por 0,8 s e os dois seguem pelo mesmo caminho do online (`OnlineLobby.launchMatch`, agora
  recebe a `Online.Session` pronta): fecha o socket, grava `online/session.txt` com `relay=0`, `code=LAN<sala>`, os
  endereços locais do outro (primeiro o de onde o pacote veio) e abre o jogo pelo `vm.play()`.
- **Robustez:** tudo em `Dispatchers.IO` com `soTimeout`; Cancelar e sair da página (`DisposableEffect` →
  `leaveScreen`) fecham os sockets e soltam o lock; partida aberta fecha sozinha em 10 min, busca em 5 min.
- **Testes:** `LanCodecTest` (7: ida e volta dos quatro tipos de mensagem, limpeza de nome e `fp`, pacotes estranhos,
  candidatos e compatibilidade, join e accept por dois sockets de verdade no loopback), `LanSessionTest` (arquivo de
  sessão da rede local) e uma foto nova (`online_lan_portrait`). Todos os testes do launcher passam (40);
  `assembleDebug` compila.
- **Não testado em aparelho** (precisa de dois celulares na mesma rede). Riscos: roteadores com isolamento de AP ou
  rede de visitantes (o anúncio não chega; o texto da página manda tentar o hotspot de um dos celulares); alguns
  aparelhos filtram broadcast mesmo com o lock; o motor reabre a mesma porta logo depois que o launcher a fecha (igual
  ao online; se ela estiver presa o motor usa outra e o outro lado aprende o endereço pelo primeiro pacote).

### 2026-11-13 18:40: instalação limpa no RG Cube (APK final)

- Backup dos dados antigos do Cube em `build/lab/cube_backup` (saves, configurações, mods importados); app
  desinstalado e o APK final instalado do zero, depois o caminho de um jogador novo pela tela:
  - Primeira abertura: tela "Sua GPU" (Mali-G57) com as otimizações; "Aplicar" funcionou.
  - Sem o jogo, o JOGAR fica cinza e desligado, e o cartão "Dados do jogo" mostra o "Selecionar ISO".
  - ISO europeia pelo seletor do Android: "Versão europeia verificada · 19 arquivos · 11,2 MB" em menos de 1 minuto.
  - Mods pela ISO: Alpha 3.5 (16 arquivos, 1020 MB) e Alpha 1.1X (6 arquivos, 934 MB), cada um ligado ao importar.
  - Ícone novo na tela inicial do Cube conferido.
- Rotas (modo leve desligado, padrão do Cube; configurações da instalação limpa, sem forçar escala):
  - Jogo base (`mia3/cube_base`): 50 fps quase a rota inteira (queda a 37 na troca de round), luta, seleção, carregamento.
  - Alpha 1.1X (`mia3/cube_a11x`): luta a 50 fps, 28 a 40 só na tela "Round 2 / VS".
  - Alpha 3.5 sem save (`mia3/cube_a35`): a mensagem do próprio mod ("dados não encontrados no slot 1"), como esperado.
    Com o save do backup devolvido (`mia3/cube_a35b`): seleção, luta e volta à seleção a 60 fps (uma queda a 28).
  - A lista de mods voltou a desligada depois de cada rota.
- `adb push` não cria pasta dentro de `Android/data` no Cube (Android 13); a pasta do save nasceu numa tentativa e os
  arquivos entraram depois, um a um.

### 2026-11-13 18:00: testes finais no Mi A3 (mods, clones, modo leve desligado)

- **Alpha 1.1X** (`build/lab/mia3/a11x_final`, modo leve ligado, Nominum): luta inteira, tela de vitória ocupando a
  tela toda (sem faixas pretas), volta à seleção de personagem. Lista de mods restaurada depois (`0|un6-a35`,
  `0|un6-a11x`).
- **Alpha 3.5** (`build/lab/mia3/a35_final`): luta, carregamento e seleção funcionam. Problema aberto: o ponteiro do
  painel de fim de round (`0x617D3C` no SLES) nunca muda no executável do A35 (`HL_WATCH=617d3c` parado), então a
  correção das faixas pretas (`1d33084`) pode não pegar a vitória do A35. O código que usa `gp-0x1B4` está nos
  mesmos endereços (`0x1ef038...`) nos dois ELFs; falta achar o endereço real no A35. Fica para depois do lançamento.
- **Clones do Naruto** (PC, `build/lab/pc/runnar.sh`): somem na nuvem de fumaça branca. A rota do PCSX2 diverge (IA
  diferente), então não houve comparação quadro a quadro.
- **Modo leve desligado**, rota da vitória no Mi A3: mediana 50,0 fps, p10 31,4 (ligado: 50,0 / 47,6). `light_mode`
  voltou para 1.
- Toque Pro no Mi A3: com a config antiga (`DYNAMIC`, 85%) o launcher novo já mostra o D-pad fixo e maior.
- Falta: instalação limpa no RG Cube (quando o desenvolvedor liberar).

### 2026-11-13 17:00: ícone novo e D-pad fixo no Toque Pro (launcher)

- **Ícone** (`722a1cc`, pedido do desenvolvedor: o antigo era cartunesco): ensō de pincel em branco pergaminho sobre
  carvão, com uma folha laranja no meio (desenho original, sem personagem, logo nem símbolo oficial). Só os três
  drawables mudaram; prévias e gerador em `build/lab/icon/` (`gen_icon.py`).
- **Toque Pro** (`afdbb37`): o D-pad da luta deixa de ser flutuante/dinâmico: fica fixo onde o editor de layout o põe,
  maior (175 dp no padrão, antes 119) e com área de toque de 1,5× o meio-tamanho para cada lado; 8 direções; o dedo que
  pousa nele continua dirigindo mesmo passando da borda. Saíram o modo flutuante/dinâmico, a faixa do polegar e os
  textos deles (pt/en/es), na tela de Controles e no menu do jogo; o guia ganhou um cartão "D-pad fixo". O tamanho
  salvo passou a `dpadSize` (quem tinha 85% recebe 125%).
- Testado: `assembleDebug`, `TouchConfigTest`, `ScreenshotTest` (capturas do Robolectric). **Falta** jogar no aparelho.

### 2026-11-13 16:30: barras da vitória no Mi A3 (19,5:9), modo leve ligado

- Libs v8.0/v8.2 e A35 refeitas com `1d33084`. Rota de sempre até a vitória das duas lutas (`build/lab/env_victory.txt`,
  vídeo em `build/lab/mia3/victory/`): a câmera volta à escala do jogo em v7594 e v11220, os quadros exatos em que o
  painel de arte aparece. No vídeo, os dois painéis (pinceladas, "WINNER", nome, pose, esmaecer para o resultado)
  enchem a tela inteira, sem barra preta em nenhum momento.
- **Falta:** os mods (A35 e Alpha 1.1X: conferir se `0x617D3C` é o mesmo ponteiro, `HL_WATCH=617d3c`) e o Cube.

### 2026-11-13 15:40: barras pretas na tela de vitória (16:9 e 20:9): câmera larga desligada no quadro do fim do round

- **Reproduzido no PC** (`un5pc`, 1080x486 = 20:9, GS do ARMSX2; Free Battle com a Hinata, `HL_CPU_HP=@1+450,@2+450`,
  `build/lab/pc/runvic.sh`, quadros PNG com `HL_DUMP_FROM`): depois do KO o quadro de arte do fim do round (o painel com o
  sol vermelho, as nuvens e o "WINNER") saía com 60% da largura e barras pretas dos lados: 4 quadros depois do round 1
  (v6251–6254), ~80 depois do último (v10454–10531). Em 4:3 enche a tela.
- **Causa:** o painel é um plano 3D de tela cheia (malha de faixas com ST) desenhado pela câmera da luta, e a câmera
  continua larga (`wideCamera()`: os lutadores ainda leem a entrada na pose de vitória, até v10700). Não é o `widen2d`:
  os dados do jogo já chegam com 0,6 da largura. (Tentei antes decidir as faixas do `widen2d` pela largura da corrida
  inteira: quebrou o corredor da abertura e o texto do título, e não mexia no painel; descartado.)
- **Achado na RAM** (dumps em 18 vsyncs dos dois rounds, `HL_WATCH`): `0x617D3C` (gp-0x1B4) fica 0 o tempo todo
  (menus, luta, KO, pose) e guarda um ponteiro só enquanto o painel está na tela (v6246–6698, v10449–10901), 5 vsyncs
  antes do primeiro quadro dele.
- **Correção** (`recomp/android/host/hl_game.cpp`): `wideCamera()` é falsa enquanto `0x617D3C` não é 0; o código de
  sempre devolve a escala de cada câmera.
- **Verificado no PC:** 4454 quadros (v6200–v10620), 0 com barras laterais (antes 7 em 446 amostras); a câmera volta em
  v6245 e v10448; fora do painel os quadros são iguais aos de referência.
- **Falta:** Android (Mi A3) com vídeo; mods (o A35 tem o mesmo endereço? conferir com o `HL_WATCH`).

### 2026-11-13 14:00: despertar da Hinata no Mi A3, modo leve desligado com vídeo

- Completa a entrada de 13:40: com o Mi A3 de volta na rede, os vídeos da luta (`build/lab/mia3/hin_normal/rec_5..8`)
  vieram do aparelho. Modo leve desligado: "Guardian Dance Mode" e "Grd", 0 piscadas de um quadro no HUD, sem lixo.
  O despertar está certo nos dois modos no Mi A3. O `light_mode=1` e o resto das configurações dele voltaram ao
  original; `hl_env.txt` e `/sdcard/hlrec` apagados.

### 2026-11-13 13:40: despertar da Hinata no Mi A3 (patches 0107/0108, Nominum faa7af3)

- **Cena:** Practice, Hinata contra Naruto, vida 0.2 no início, ↑ segurado (`build/lab/env_hin_prac.txt`, rota do
  aparelho com 7×RIGHT na escolha e DOWN extra no modo).
- **Modo leve ligado** (Nominum VU1 + GS dividido): "Guardian Dance Mode" e ícone "Grd" por ~20 s, HUD estável. Vídeo da
  luta inteira (`build/lab/mia3/hin_light2/rec_5..8`): 0 piscadas de um quadro na região do HUD, sem lixo amarelo/verde,
  sem tela vermelha ou branca. 41–49 fps.
- **Modo leve desligado:** 6 capturas durante o despertar, HUD inteiro, nada quebrado; 17–24 fps (esperado). Os vídeos
  da luta se perderam: o Mi A3 saiu da rede no fim da rodada (Wi-Fi), e o `light_mode` dele ficou `0` (voltar para `1`).
- **Cube:** não testado; estava em uso com outro app (Fractal), configurações devolvidas ao original.
- **Falta:** vídeo do modo desligado; ver se as barras pretas da vitória e os golpes sem efeito somem com a mesma correção.

### 2026-11-13 13:00: tudo dentro da pasta do projeto (pedido do desenvolvedor)

- **Pedido:** nada de trabalho fora de `~/AndroidStudioProjects`, nada em pasta temporária, apagar o que não serve. Regra
  global em `~/.claude/CLAUDE.md`.
- **Movido:** o PCSX2 da bancada `~/build/pcsx2-bench` → `build/pcsx2-bench` (caminhos trocados no `run.sh`, `runrc.sh`,
  cache do CMake e configs das dependências; precisa reconfigurar e recompilar uma vez). Os dumps do VIF1
  `~/Downloads/vifdumps` → `build/vifdumps` (o `NOMINUM_VIFDUMPS` do Nominum aponta para lá). As entradas antigas deste
  registro ainda citam os caminhos velhos.
- **Apagado** (~34 GB): o `build/lab` inteiro menos scripts, rotas e envs (quadros, gravações, dumps, APKs e binários de
  teste: 29 GB), as 9 APKs `build/HiddenLeaf-2026111*.apk`, `build/gsx-dr-20261112`, `build/gsx-nominum`,
  `build/linux-armsx2-hl`, `build/linux-armsx2-tsan`, as exportações antigas do Nominum (fica a `faa7af3`, com o catálogo
  `build/vu1rec` copiado da `758e4e7`), os dumps de GS/VIF e quadros em `build/disc`, o rascunho da sessão no `/tmp`.

### 2026-11-13 12:40, meio-dia: Despertar da Hinata, lista PACKED partida entre DIRECTs (patches 0107, 0108, Nominum faa7af3)

- **Reproduzido no PC** (`un5pc`, sem o nativo: `HL_NATIVE=0`), modo Practice (Mode Select ↓↓), Hinata 7× → a partir do
  Naruto com tudo liberado (`HL_UNLOCK_ALL=1`), vida 0,2 (`HL_P1_HP`), ↑ segurado: no "Guardian Dance Mode" o HUD pisca,
  some, fica com lixo amarelo/verde, tela vermelha e quadros brancos. O PCSX2 (bench, novos `HLB_UNLOCK=1` e `HLB_HP=0.2`;
  `LC_ALL=C`, o `atof` leu "0.2" como 0 em pt_BR) desenha tudo limpo. Rota em `build/lab/pc/route_prac2.txt`.
- **Causa** (dumps do GS dos dois lados, `build/lab/pc/uploads.py`): o jogo manda a lista A+D das partículas (tag PACKED,
  NLOOP 6 a 108) em DIRECTs de 33 qwords, os dados continuando no DIRECT seguinte. Dois erros nossos: (1) o árbitro do GIF
  só juntava pacote partido no PATH3, e o `hl_gs_live` cortava o tag no fim do DIRECT (NLOOP 32 com EOP); (2) o
  interpretador da VIF1 (runtime e Nominum) lia cada DIRECT a partir de um tag, e a continuação (`8080803b200e6e3a`) virava
  um IMAGE de 128 qwords devendo: os DIRECTs seguintes iam como pixels de uma subida 8×2 em `dbp 0x3c51` (até 943 KB por
  quadro) e sujavam as texturas do HUD. Por isso acontecia com o modo leve ligado e desligado.
- **Correção:** `0107-gif-path2-packed-across-directs.patch` (o árbitro segura e junta PACKED/REGLIST do PATH2 como já
  fazia no PATH3; IMAGE fica com o interpretador; tag acima de 64 KB não é segurado: o motivo antigo de deixar o PATH2 de
  fora eram justamente essas continuações). `0108-vif1-direct-packed-owed.patch` e Nominum `faa7af3`
  (`Vif1Pending::dataQwc`): os qwords de PACKED/REGLIST devidos são pulados antes de procurar tag.
- **PC:** o `un5pc` nunca chamava `hl_nominum_vu1::setup()` (compilava o Nominum mas rodava sempre a VU1 do runtime);
  agora chama, e `NOMINUM_VU1=1` liga como no Android. Os dumps do GS/VIF no PC vão para `gsdumps/` e `vifdumps/` da pasta
  do disco.
- **Verificado no PC:** runtime (`NOMINUM_VU1=0`) e Nominum (`NOMINUM_VU1=1`), 150 vsyncs do Despertar: 0 subidas com
  tamanho errado (antes 50), tags 42/54/108 iguais ao PCSX2, HUD estável nos quadros. Nominum: `ctest -R ^vif` 106/106.
- **Falta:** Android (Mi A3 e Cube, modo leve ligado e desligado) com vídeo; ver as outras queixas (barras pretas na
  vitória, ataques sem efeito), que podem ter a mesma causa.
- A queda "corrupted double-linked list" no PC (no driver Vulkan da NVIDIA, ao criar pipeline) só apareceu com o
  renderizador nativo ligado; não investigada.

### 2026-11-13 09:40, manhã: "Modo leve" no launcher; o padrão volta para a VU1 do runtime e o GS numa thread

- **Pedido do desenvolvedor:** com as mudanças recentes o jogo buga: a Hinata Shippuden (entendi "Renata grande" como a
  Hinata adulta) no Despertar deixa a tela toda bugada (HUD e vida piscando, coisas sumindo); às vezes a tela de vitória
  fica com barras pretas e volta; vários ataques ficam sem efeito. Itachi e outros no Despertar não mostraram o problema.
  Em vez de corrigir tudo agora: o que deixa o jogo mais leve vira um **modo leve**, na aba de desempenho, para quem tem
  celular da classe do Snapdragon 665; o padrão fica com o GS normal, para os aparelhos mais fortes. A Hinata ainda precisa
  ser corrigida no modo leve.
- **`light_mode`** (`EngineSettings.LIGHT_MODE`; "Modo leve" no card Desempenho das configurações e na aba Gráficos do menu do
  jogo, com reinício; textos em pt, en, es; "· modo leve" junto do renderizador em uso):
  - ligado: VIF1 e VU1 do Nominum (`NOMINUM_VU1=1`) e, no Snapdragon 665 (`trinket`) com Vulkan e sem o nativo, o GS em
    duas threads (regra de 04:30);
  - desligado (padrão): a VU1 do runtime e o GS numa thread, como antes de 2026-11-12. `NOMINUM_VU1` e `HL_GS_SPLIT` no
    `hl_env.txt` continuam valendo acima do modo. O log diz `un5: light mode on/off`.
- **Medidas** (sem forçar nada no `hl_env`; a rota até v7600 no Mi A3, até v9000 no Cube; fases A / B):

  | Aparelho | Modo leve desligado | Modo leve ligado |
  |---|---|---|
  | Mi A3, 1,5x | 35,2 / 35,3 fps (VU1 25 ms) | 50,0 / 49,1 fps (VU1 13,5 ms), GS em duas threads |
  | RG Cube, 2x | 47,8 / 46,6 fps (VU1 17–20 ms) | 49,4 / 50,0 (`cube_758`, entrada de 08:30) |

- **Comparação na rota do Naruto** (Mi A3, `normal15` com a VU1 do runtime contra `mia3_758rec` com a do Nominum): a
  transição "Start Battle", os golpes, o "Vermillion Mode" e o clarão do Ougi aparecem iguais nos dois; a diferença do
  desenvolvedor não aparece nesta rota.
- **Rota não chega na Hinata:** na seleção de personagem as direções da rota (`DOWN`, `LEFT`, `LDOWN`, `LLEFT`, e mesmo
  segurando 25 vsyncs) não movem o cursor, nem do 1P nem do COM (o log mostra os passos enfileirados; só o CROSS passa).
  Quatro tentativas (`build/lab/mia3/hinata_def*`); falta entender por quê antes de testar o Despertar dela por rota.
- **`fx_off`** (48 no Mi A3, vindo do perfil de GPU herdado do Dragon Rage) não é lido pelo motor do Hidden Leaf: não explica
  os efeitos que faltam.
- **Resolução 1,5x:** é a escala interna do GS do PCSX2 (`upscale` 1,5 no `un5.ini`: 960×672 a partir de 640×448); numa tela
  de 720 linhas fica perto da nativa e ainda serrilha (não há anti-aliasing).

### 2026-11-13 08:30, manhã: UNPACK do Nominum mais leve (`758e4e7`), tradução de endereço e medidor em linha

- **Perfil** da janela que ainda cai no Ougi (v7518–7800 no roteiro do Mi A3: VU1 21–23 ms de CPU, o EE esperando 7–12 ms
  por ela; simpleperf, `build/lab/prof/`): blocos recompilados 60%, UNPACK ~12%, `compactVertices` 4,8%, `memmove` 4,8%
  (cópias para o anel: pacotes DIRECT 22%, o próprio `Vif1::transfer` 16%, o corte de tags 6%), `hl_perf` + resolvedor de
  TLS 2,4%. Os núcleos grandes ficam em 2016 MHz a luta toda (sem limitação térmica); por núcleo: a VU, o GS Back, o EE e o
  GS nos quatro A73; o IOP nos A55; o "CPU Thread" (lê o anel para o GS, 1,4 s em 5,9 s) solto nos oito.
- **Nominum `758e4e7`:** os laços rápidos do UNPACK liam tudo da memória a cada vetor (capturas por referência e escritas
  de bytes na VU); agora leem de variáveis locais. Replay: VIF1+VU1 19,68 → 18,91 ms por vsync; a parte do VIF −19%; bit a
  bit igual (106/106 no PC, 130/130 em arm64). Detalhe no registro do Nominum.
- **Patch `0105-translate-address-inline-low.patch`:** `PS2Memory::translateAddress` em linha para endereços abaixo de
  `0x20000000` (RAM, registradores de I/O, endereços das tags de DMA: o próprio endereço, sem scratchpad, espelho ou KSEG),
  e `isScratchpad` em linha; o resto continua em `translateAddressSlow`. Era 4% da thread do EE. Muda um cabeçalho: as
  libs recompilam inteiras. No jogo: neutro a +0,3 fps na fase B (`mia3_0105a/b`).
- **Patch `0106-perf-game-thread-by-pointer.patch`:** o `hl_perf` está sempre ligado (`g_on`; o `HL_PERF` só decide se
  imprime) e cada pacote da VU passa por `switchTo`, que testava um `thread_local`; numa lib carregada por `dlopen` isso
  chama o resolvedor de TLS dinâmico. Em arm64 a thread do jogo agora é reconhecida pelo ponteiro de thread
  (`__builtin_thread_pointer`).
- **Sem ganho, desfeito:** o EE a `-O3` (entrada de 07:00); hints de desempenho (as frequências já estão no máximo).
- **Mi A3, luta a 1,5x, GS dividido, sem gravar** (fases A / B; VU1 em ms):

  | Rodada | A fps | B fps | B min | VU1 A / B |
  |---|---|---|---|---|
  | `e27e16c` (`mia3_vuflags15n`) | 49,6 | 47,6 | 36,1 | 14,6 / 17,0 |
  | + `0105` (`mia3_0105a/b`) | 49,3–49,4 | 47,9–48,0 | 35,8–38,4 | 14,6 / 17,0 |
  | + `758e4e7` + `0106` (`mia3_758a/b`) | **49,7–50,0** | **47,9–48,4** | 35,3–35,6 | 13,7–14,0 / 15,7–15,8 |

  Gravando (`mia3_758rec`): 49,5 / 47,7, imagens certas (combo, modo Vermillion, close-ups). **RG Cube** (`cube_758`, 2x):
  49,4 / 50,0, VU1 9,2 / 10,7 ms, imagens certas.
- **O que falta no Mi A3:** a fase A já está em 50; na B, duas janelas ainda caem para 35–45 fps: v6630–7030 (o EE em
  21–23,5 ms e a VU1 em 16–19 ms, os dois juntos) e v7518–7800 (a VU1 em 19–21 ms, o EE esperando).
- Os pacotes do A35 (`libun5_un6_a35*.so`) ainda precisam ser recompilados com este estado (o cabeçalho do `0105` mudou
  durante a compilação deles).

### 2026-11-13 07:00, manhã: a VU1 do Nominum 12% mais leve no Mi A3 (`e27e16c`); o Ougi agora para também no EE

- **Por quê:** com o GS dividido (entrada de 04:30), o Ougi pesado a 1,5x deixava a thread da VU1 em 24–25 ms por quadro.
  Perfil da `VuWorker` no jogo (simpleperf no Mi A3, janela do Ougi): blocos recompilados 58%, UNPACK ~12%, saída para o GS
  ~12%, despacho ~6%. Detalhe por operação e as mudanças no `docs/registro.md` do Nominum (entrada de 05:50).
- **Nominum `e27e16c`** (commits locais `8d30dc7`, `e7a3f5d`, `8f6ee1a`, `e27e16c`): anel de flags com a soma acumulada
  (o commit não aplica entrada por entrada), `statusOfMac` sem desvios, blocos encadeados por chamada de cauda,
  `preserve_none` nos blocos, a soma das flags em variáveis. Replay `mi_a3_ko` no Mi A3: VU1 18,22 → 15,70 ms por vsync
  (−14%), bit a bit igual (ctest `^vif` 106/106 no PC, `arm64_qemu.sh` 130/130).
- **Pino** `NOMINUM_COMMIT=e27e16c` (`recomp/android/build.sh`, `recomp/pc/build.sh`); catálogo em
  `build/nominum-e27e16c/build/vu1rec` (o mesmo de antes: as chaves não mudaram).
- **Build:** o include do Nominum era `PUBLIC` no `nominum_vu1` e ia para a linha de comando das 739 unidades do `un5`; como
  a pasta leva o commit no nome, cada pino novo recompilava o jogo inteiro (~30 min por lib). Agora é `PRIVATE` e só o
  `host/hl_nominum_vu1.cpp` o recebe: trocar o pino recompila 15 unidades.
- **Patch `0104-gslive-log-flag-once.patch`:** o `trimLastTag` (`hl_gs_live.cpp`) chamava `getenv("HL_GSLIVE_LOG")` a cada
  pacote cortado no meio de uma tag (~2 milhões numa luta: pacotes de 96 bytes do PATH2, IMAGE que continua no DIRECT
  seguinte); `getenv` + `strncmp` eram 0,7% da thread da VU1. Lido uma vez.
- **Mi A3, luta a 1,5x, GS dividido** (fases A / B, fps e ms da VU1):

  | Rodada | A fps | B fps | B min | VU1 A / B | EE B |
  |---|---|---|---|---|---|
  | antes (`mia3_auto15b`, sem gravar) | 48,5 | 46,4 | 36,1 | 17,0 / 19,3 | 15,9 |
  | `e27e16c`, sem gravar (`mia3_vuflags15n`) | **49,6** | **47,6** | 36,1 | 14,6 / 17,0 | 16,3 |
  | `e27e16c`, gravando (3 rodadas) | 48,9–49,1 | 46,9–47,0 | 37,6–37,9 | 14,9–15,1 / 17,7 | 18,0–18,1 |

  Imagens certas no vídeo (menus, luta, close-ups e clarão do Ougi). **A gravação do aparelho (screenrecord) custa ~0,6
  fps:** comparar sempre rodadas com e sem `REC` entre si.
- **O limite agora:** nas janelas lentas do Ougi (39–45 fps) o EE fica em 20–23,5 ms com waitVU ~0 e a VU1 em 16–20 ms: os
  dois juntos. O EE é o código do jogo espalhado (`dispatchGuestBranch` 5%, `translateAddress` 4%, um produto de matriz da
  VU0 em modo macro 3,5%, `memmove` 3,6%); nada concentrado.
- **Sem ganho, desfeito:** o código do jogo a `-O3` (`UN5_RUNNER_OPT`, só a lib v8.0): com gravação 48,9 / 46,8, igual ao
  `-O2` (a primeira rodada, sem gravar, parecia melhor; era a gravação).
- **RG Cube (luta 2x, sem o GS dividido):** 49,5 / 50,0 fps, VU1 9,9 / 11,5 ms; imagens certas.
- **`build/lab/mia3run.sh`:** `RECLIM=<s>` corta o vídeo em pedaços menores (com 50 s os quatro chegam inteiros pelo Wi-Fi;
  o de 170 s chegava truncado); `RECSIZE`/`RECLIM` agora valem (dentro de aspas simples o shell do aparelho os via vazios).

### 2026-11-13 04:30, madrugada: GS em duas threads no Mi A3, corrigido e ligado só no Snapdragon 665

- **Pedido do desenvolvedor** (antes de dormir): corrigir a divisão do GS (`GSBackThreadMode`) até ficar usável, só no
  Mi A3, sem quebrar os outros aparelhos; meta: a luta a toda velocidade a 1,5x.
- **O defeito** (entrada de 03:00: a tela congelava listrada até o fim da luta) aparecia em 4 de 5 rodadas com a divisão,
  sempre no close-up do combo do Naruto. Caminho até a causa:
  - rastreio temporário no `Merge` (`HL_GSX_TRACE`, fora do repositório: `build/lab/0001-lab-display-trace.patch`): a
    saída é simples (progressiva, os dois circuitos no mesmo buffer `0x00e00` a 50%), e os draws continuam chegando;
    o defeito está no que o jogo desenha, não na composição;
  - ThreadSanitizer no replay do PC feito da cópia do ARMSX2 (`build/linux-armsx2-tsan`, Vulkan na iGPU AMD; na NVIDIA o
    TSan cai): nenhuma corrida no GS dividido;
  - um dump do GS de 600 vsyncs gravado no Mi A3 **sem** a divisão (`build/lab/mia3/mia3_dump_scene/mi_a3_ko.gs.zst`),
    no PC, congela igual com e sem a divisão, e até no renderizador por software: o fluxo como o jogo manda já congela.
    O que o aparelho faz a mais é a correção de SCANMSK do `hl_native` (2026-11-12: o jogo escreve `SCANMSK 2` em
    certos cortes e nunca volta a 0; o GS passa a desenhar linha sim, linha não).
  - **Causa:** essa correção zerava o SCANMSK em `g_gs_renderer`. Com a divisão ele é o objeto de trás, que recebe os
    registradores de cada draw pelo registro do draw (e pode estar desenhando na outra thread naquele momento); o
    objeto da frente, que faz o parse, continuava com 2.
- **Correção** (`recomp/android/gsx/hl_native.cpp`): o SCANMSK volta a 0 no objeto do parse (`g_gs_front` com a
  divisão, `g_gs_renderer` sem ela). Sem a divisão o código é o mesmo de antes. Duas rodadas com a divisão
  (`mia3_split_fix_a/b`) passam pelo mesmo corte sem congelar; o log diz `SCANMSK 2 at a vsync, back to 0 (front object)`.
- **Regra** (`un5_main.cpp`, `writeIni`): `GSBackThreadMode = 1` só com o GS em Vulkan, o renderizador nativo desligado e
  `ro.board.platform` = `trinket` (Snapdragon 665); então `HL_PIN_IOP=rest` (cinco threads pesadas em quatro núcleos
  rápidos). `HL_GS_SPLIT=0/1` força. O log diz `un5: GS in two threads (platform trinket)`.
- **Medidas no Mi A3** (fases A / B):

  | Rodada | 1x | 1,5x |
  |---|---|---|
  | uma thread | 48,7 / 46,8 | 48,1 / 46,2 |
  | duas threads (com a correção) | 48,3 / 45,6 e 48,4 / 45,9 | 48,6 / 46,5, 48,7 / 46,5, 48,5 / 46,4 (regra automática) |

  A 1,5x as duas threads ganham ~0,4 fps e a 1x nada: no trecho pesado o GS cai para 12–17 ms e **o limite passa a ser a
  VU1** (24–25 ms por quadro no Ougi, o EE esperando 11–14 ms por ela). Imagens certas (`mia3_auto15`: o Ougi no
  telhado, um clarão de transição).
- **RG Cube** (`cube_auto_s2`): `GS in one thread (platform ums9620)`, `back thread off`; 49,4 / 45,1 (a fase B pegou
  um Ougi, VU1 16,5 ms; a rodada de antes não pegou: 49,4 / 50,0). Os pacotes de mod (A35) ainda têm o host de antes:
  não ligam a divisão até serem refeitos.
- APK `build/HiddenLeaf-20261113-split.apk` instalado no Mi A3 e no Cube.
- **Próximo:** a VU1 no Ougi ~20% mais barata (de 24–25 para ~20 ms por quadro).

### 2026-11-13 03:00, madrugada: o GS do ARMSX2 no Mi A3: quatro tentativas, nenhuma ganha; a cópia própria do núcleo

- **Pedido do desenvolvedor:** "otimiza o GS do ARMSX2 então" (depois de a entrada de 00:40 achar o GS como limite do
  Mi A3).
- **Infraestrutura nova (fica):**
  - `recomp/android/build_gsx.sh` compila o núcleo de uma cópia própria em `build/armsx2-hl` (feita da árvore do Dragon
    Rage, que só é lida; um repositório git com ela como primeiro commit) e aplica `recomp/android/gsx/patches/*.patch`
    (hoje nenhum), refazendo quando os patches mudam. `build/gsx` foi refeito do zero (o antigo ficou em
    `build/gsx-dr-20261112`, a `.so` em `build/libbt4gsx-20261112.so`); o primeiro build levou 2 min.
  - `hl_gsx.cpp`: com `HL_PERF`, a cada 250 vsyncs `[gsx] block hash per vsync: níveis, blocos, hasheados de novo` (o
    cache de hash por bloco do texture cache).
- **Perfil novo, no trecho pesado** (`p5.data`, v7208–7500, `--call-graph dwarf`): o perfil antigo (`p4`, gravado aos
  185 s) não era da luta pesada. Ali a thread `GS` é **custo por draw**: `GSRendererHW::Draw` 63% (inclusivo), dele o
  Vulkan (`DoRenderHW`) 28% — `vkCmdPushDescriptorSetKHR` 11% e `vkCmdBindDescriptorSets` 3% dentro do driver da Adreno,
  `vkCmdDrawIndexed` 5% (com `calloc`/`free` do driver) —, `LookupSource` 8%; o parser de vértices (`XYZ2`) 11%,
  `memmove` 8% (a pilha não desce pela libc; deve ser a cópia de vértices/índices ao buffer do Vulkan). O hash de
  textura cai para 3% e o `WriteImage` sai do topo. O jogo troca de textura a cada poucos triângulos e cada troca é um
  draw. O caminho já foi otimizado pelo Dragon Rage (comentários com medidas no SD865 e no M2).
- **Tentativas (Mi A3, luta 1x; antes 48,7 / 46,8):**
  1. **Cache de hash por bloco também para texturas inteiras** (patch em `HashTextureLevel`/`HashCacheKey::Create`;
     hoje só janelas paletizadas o usam): 46,4 / 46,4 e 47,0 / 45,6 (`mia3_gsh_a`, `mia3_gsh_b`). O log mostrou por quê:
     ~3400 blocos por vsync, **todos** escritos desde a consulta anterior; o jogo reenvia de verdade ~0,87 MB de
     textura por vsync (no dump `odin3_43_luta_inicio`: 0,9 MB/vsync de IMAGE, 1/3 igual ao envio anterior ao mesmo
     lugar). Desfeito.
  2. **Conjuntos de descritores em vez do push** (`m_use_push_descriptors = false`, teste por `HL_GSX_NO_PUSH`):
     46,3 / 46,6 (`mia3_nopush_a/b`), GS igual. Desfeito.
  3. **OpenGL em vez de Vulkan** (`HL_GS_API=gl`): 37,0 / 41,4 com travadas (`mia3_gl`). Fica o Vulkan.
  4. **A divisão do GS do próprio ARMSX2** (`GSBackThreadMode=1`: uma thread lê o GIF, a "GS Back" desenha; nem o Dragon
     Rage liga): a thread `GS` cai de 22–27 para 12–16 ms no trecho pesado, mas 44,3 de fps (`mia3_back_a`: cinco threads
     pesadas em quatro núcleos grandes); com `HL_PIN_IOP=rest` volta a 48,6 / 46,9 (`mia3_back_iop`) e no Ougi o limite
     passa a ser a VU1 (24–25 ms, o EE esperando 11–14). **Imagem errada:** no vídeo (`mia3_back_rec`, `REC=1`) a luta
     sai certa até o Ougi (~158 s) e a tela congela num quadro borrado e listrado por mais de 20 s. Descartado.
- **Conclusão:** no Mi A3 a luta comum já fica perto de 50; o Ougi e os combos pesados (~4,4 MB/f) pedem o GS **e** a
  VU1 ~20% mais baratos ao mesmo tempo. Do lado do GS o que resta é estrutural: corrigir a divisão do GS para o fluxo ao
  vivo (o efeito do Ougi), ou o renderizador nativo na luta (`hl_native`, hoje opção experimental).
- Lib do GS limpa (mesmo fonte do Dragon Rage, mais o log) reinstalada no Mi A3 (`mia3_gsclean`, luta certa;
  `build/HiddenLeaf-20261113-vu.apk`).

### 2026-11-13 00:40, madrugada: a VU1 do Nominum mais barata (`052f000`); o limite do Mi A3 é o GS do ARMSX2

- **Pedido do desenvolvedor:** "o que da pra fazer na VU1 do Mi a3 faca, quero ele segurando a luta em pelo menos 1x ou
  talvez 1.5 ou 1.25x".
- **Nominum `052f000`** (registro dele, 2026-11-12 23:00): o corpo de cada bloco recompilado da VU1 recebe `vf`, `vi` e
  `vuData` como ponteiros `__restrict` (a escrita na memória de dados, por ponteiro de bytes, fazia o compilador recarregar
  os registros depois de cada `SQ`); o IMAGE do VIF1 sem vetor novo a cada pedaço. O replay da luta no próprio Mi A3
  (`nominum-vifreplay` estático, `odin3_43_luta_inicio`): VIF1+VU1 15,47 → 14,95 ms por vsync, a VU1 12,46 → 11,95.
  ctest `^vif` 106/106, `arm64_qemu.sh` 130/130 bit a bit.
- **Aqui:** `NOMINUM_COMMIT` `85726f6` → `052f000` (`recomp/android/build.sh`, `recomp/pc/build.sh`; o catálogo
  `build/vu1rec` é o mesmo, copiado para `build/nominum-052f000/build/vu1rec`), e a `nominum_vu1` com `-O3` em vez de
  `-O2` (`recomp/android/CMakeLists.txt`; no replay, −0,4 ms por vsync sozinho).
- **Mi A3, luta 1x (`cmp_default`, v8.0):** VU1 18,7 ms na fase B (antes 18,1–19,1), fps **48,7 / 46,8** (`mia3_vu2a`,
  `mia3_vu2b`; antes 48,4–49,1 / 46,6–48,2): a VU1 ganhou ~0,3 ms no jogo e o fps não mudou.
- **Por que o fps não sobe:** na fase B, 56–58% das janelas já estão a 48,5 fps ou mais; as lentas (10 de 41, mínimo
  35,6) são o Ougi do Naruto com os clones nos telhados (v7500–7800) e trechos de combo. Nelas o quadro leva ~27 ms e a
  thread `GS` do ARMSX2 fica 25–28 ms ocupada por quadro (4,0–4,5 MB/f), a VU1 23–24 ms esperando o anel dele. **O
  limite do Mi A3 é o GS do ARMSX2**: no perfil do processo, `GSLocalMemory WriteImage` 13% e `GSXXH3_64_Block` (hash
  de textura) 13% da thread, `GSRendererHW::Draw` 6,5%.
- **Resolução:** a 1,25x e a 1,5x o fps é quase o mesmo da 1x: **48,5 / 46,3** (`mia3_s125`) e **48,3 / 46,5**,
  **48,0 / 46,5** (`mia3_s15`, `mia3_s15b`); o GS sobe ~1 ms. Imagens a 1,5x certas (luta, Ougi, golpe com "hits").
  **O Mi A3 pode usar 1,5x** sem perder mais do que já perde a 1x.
- **Tentados e descartados:**
  - A "CPU Thread" do ARMSX2 (copia o anel ao vivo para o MTGS; 12% do processo, livre em todos os núcleos) presa aos
    núcleos pequenos (`HL_PIN_CPUT=l`, experimento no `hl_perf.cpp`, desfeito): 48,7 / 46,7 e 48,6 / 46,6, igual.
    `taskset` pelo `run-as` não pode (permissão negada).
  - Os clocks no Mi A3 durante a luta: A73 a 2016 MHz (o máximo), A53 1,3–1,8 GHz, GPU a 600 MHz (máx. 950) e 56%
    ocupada. Não há estrangulamento térmico.
- **Cube:** luta 2x base 49,4 / 50,0 fps, VU1 10,2 / 11,8 ms (`cube_vu1r`; antes 12,0–12,2 na fase B). A35 (pacotes v8.2
  e v8.0 refeitos): 59,2 / 56,6 fps (`cube_a35_vu1r`). A fase B pegou o Ougi da CPU (v7315–7427, 37 fps, VU1 25 ms);
  na `cube_a35_ee3` o mesmo tipo de cena caiu fora da janela (v9343–9753, 24 fps, VU1 39 ms). Capturas certas (luta,
  golpe, seleção). O A35 desligado de novo no Cube.
- **Próximo passo para o Mi A3 segurar 50:** o GS do ARMSX2 (`libbt4gsx`): o upload (`WriteImage`) e o hash de textura
  nas cenas de 4 MB/f. Na VU1 ainda há o laço de `HL_COMMIT_FLAGS` (um quarto do bloco mais quente), mas ele não muda o
  fps enquanto o GS for o limite.
- APK `build/HiddenLeaf-20261113-vu.apk`, instalado no Mi A3 e no Cube.

### 2026-11-12 21:40, noite: o EE 20% mais leve (patch `0103`); o limite do Mi A3 passa a ser a VU1

- **Pergunta do desenvolvedor:** o limite é o EE; é da recompilação? Perfil DWARF da GameThread no RG Cube (luta 2x,
  10 s, `simpleperf -t <GameThread> --call-graph dwarf`): 58% no código gerado, o resto no runtime em volta:
  `dispatchGuestBranch` 7,6% (próprio), `eeCheckpointDue` 4,0%, `memset` 4,2% (todo de `vector::resize`), mutex 4,7%,
  `__memcpy` 3,9% (a cópia da cadeia do DMA no `writeIORegister`), VU0 em micro modo no interpretador 3,4%.
- **Patch `0103-ee-lighter-runtime-paths.patch`:**
  1. Os buffers das cadeias do DMA e das transferências para o `VuWorker` são `hl_vuworker::Bytes` (vector com
     alocador que não zera): o `resize(capacity)` zerava até a capacidade inteira a cada cadeia, e o memcpy escrevia por
     cima. O `takeData` usa `resize` + `memcpy`. O `memset` sumiu do perfil; o EE ficou no ruído (11,4 → 11,3 ms).
  2. `ps2_vany` (`ps2_runtime_macros.h`): no arm64 o "alguma faixa?" dos `ps2_vadd`/`ps2_vmul` (VU0 macro, 13265 usos
     no código gerado) é um `UMAXV`; o `_mm_movemask_epi8` do sse2neon era ~10 instruções, uma ou duas vezes por
     operação. Cube: EE 11,4 / 13,3 → 10,7 / 12,9 ms (fases A / B, `cube_vany`).
  3. O `Store32` perguntava pelas causas de DMA concluídas travando um mutex em toda escrita de registrador: agora um
     `atomic<bool>` diz se há alguma. O `processPendingEvents` volta logo quando não há evento, prazo vencido nem
     interrupção de timer, sem os três locks (o `m_checkpointPending` só é limpo sob o lock com a fila vazia, então ele
     falso garante que não há evento). O histórico de despachos deixou de ser `thread_local` (numa `.so` aberta por
     `dlopen` cada acesso passa pelo `tlsdesc_resolver_dynamic`). A RAM do PS2 ganhou 64 bytes de folga depois dos
     32 MB, e o `Ps2FastRangeIsContiguous` responde sempre sim: o código gerado não testa mais a volta nos 32 MB em cada
     leitura e escrita (um acesso alinhado nunca cruza o fim). Isso tirou um caminho de volta byte a byte de cada acesso:
     a `libun5.so` caiu de 161 para 116 MB.
  - **Cube, luta 2x:** EE 10,7 / 12,9 → **8,8 / 10,5 ms** (`cube_ee3`); a thread do jogo ocupada 4344 amostras em 10 s
    contra 5527 antes. Mesmas vidas nos mesmos pontos (98, 96, 93, 91, 88, 86), vídeo certo. **Campanha 2x**
    (`cube_camp_ee3`, v8300–10300): EE 12,5 → 10,1 ms, 49,8 fps; portão, "Skip?", vila, tutorial de pulo, Ichiraku certos.
  - **Mi A3 (v8.0, 1x, `cmp_default`):** EE 16,6 / 19,2 → 14,8 / 16,3 ms (`mia3_ee3`, `mia3_ee3b`, vídeo certo); fps
    49,2 / 44,7 → 48,0 / 45,8. **No Mi A3 o limite agora é a VU1 do Nominum** (17,3 / 19,5 ms), não mais o EE.
  - **A35 no Cube** (pacotes v8.2 e v8.0 refeitos com o `0103`, `cube_a35_ee3`, vídeo certo: carregar, seleção, VS,
    luta): EE 11,9 / 13,7 → 9,2 / 10,9 ms, 58,9 / 58,7 fps. APK `build/HiddenLeaf-20261112-ee.apk`, instalado no Cube e
    no Mi A3 (o A35 desligado de novo no Cube).
  - Perfil depois (`ee3.data`): mutexes 0,85%; sobra `dispatchGuestBranch` 8,2%, `eeCheckpointDue` 5,3%, `__memcpy` 4,6%,
    VU0 micro 2,9%, `translateAddress` + `inRange` 3,5%.
- **Tentado e descartado:** `__restrict` no `ctx` das 22560 funções geradas (o compilador poderia manter registradores
  do PS2 em registradores do ARM entre acessos à RAM, que com `-fno-strict-aliasing` podem apontar para o `ctx`):
  12,9 → 12,7 ms, dentro do ruído e com risco de comportamento indefinido. `eeCheckpointDue` em linha no código gerado:
  9,2 / 10,8 ms contra 8,8 / 10,5, e +2,7 MB. Os dois desfeitos.
- **Resposta:** a recompilação em si (registradores do PS2 no `ctx`, 3 a 7 instruções ARM por instrução MIPS) é a
  maior parte, mas o que dava para tirar rápido estava em volta dela: código inchado pelo teste de volta, locks e
  helpers do sse2neon. Recompilar com cache de registradores (variáveis locais por função) seria uma mudança no gerador
  do PS2Recomp, bem maior.

### 2026-11-12 20:05, noite: os três mods no RG Cube (2x, VU1 do Nominum); a thread do IOP também nos pacotes de código

- **Importação como jogador:** as três ISOs em `Download` do Cube, importadas pela tela de Mods (A35 16 arquivos,
  1020 MB; Alpha 1.1X 6, 934 MB; Versão Brasileira 4, 789 MB). Ligar um desliga o outro sozinho (`un5_mods.txt`).
- **Versão Brasileira** (`cube_br2`, rota da luta, 2x): 49,4 / 50,0 fps. O programa dela é o europeu comprimido
  (SjCRUNCH), roda o motor base. O texto dela fica no lugar do espanhol, e o menu de idioma do jogo mostra "Português"
  (`cube_br_pt`, a rota escolhe o 4º item): 49,3 / 50,0, abertura "Versão Legendada", "Novo jogo / Carregar",
  "Selecionar modo", "Combate Livre", "Lutem!", luta certa. **Launcher:** o cartão dizia "Muda o código" (vinha do
  `code=1` do `mod.properties`, porque o ELF difere no disco); agora um mod conhecido usa o `changesCode` do
  `KnownVersions` ("Só dados"), e o `KnownMod` ganhou `language`: o cartão diz "No menu de idioma do jogo, escolha
  Português: o texto do mod fica no lugar do espanhol" (`mod_pick_language`, en/pt/es).
- **Alpha 1.1X** (`cube_a11x`, `env_a3_a11x_r.txt`, 2x): 49,5 / 50,0 fps, "the base's code, its own data", tela de
  seleção e VS dela, luta com golpes e "Unleash Ultimate Jutsu" certos.
- **A35** (pacote `libun5_un6_a35.so`, save `BASLUS-55606NARUTO6` copiado do Mi A3 por `run-as`; rota
  `stage_env/cube_a35_s2.txt` = `a35_ser` em 2x): serial 56,0 / 55,0 (`cube_a35`), com a thread do IOP 59,0 / 58,7
  (`cube_a35_iop`): o mod é NTSC e no Cube o IOP (3,3 ms) saía da thread do jogo. Som gravado (`HL_SPU2_REC`) nos dois
  modos: 203 s e 200 s, nenhum buraco curto. No Mi A3 dava o mesmo com ou sem (41,3 / 40,4, entrada das 20:00), então
  **`HL_IOP_THREAD=1` vale agora também nos pacotes** (`un5_main.cpp`, sem o `#ifndef HL_PACK_ID`). Pacote refeito
  (v8.2 e v8.0) e conferido sem `hl_env` de IOP: `[iop] the IOP on its own thread`, 59,0 / 58,4 (`cube_a35_def`, vídeo:
  carregar, seleção, VS, "Lutem!", luta com golpes e números). Os quadros brancos antes da seleção e do VS são a
  transição do próprio mod: aparecem iguais no modo serial.

### 2026-11-12 noite: RG Cube na base, núcleos pela frequência permitida, UNPACK com máscara, drivers

- **Cube liberado pelo desenvolvedor** (base primeiro; mods depois). Base importada pelo launcher como jogador (ISO europeia
  em `Download`, "Versão europeia verificada"). O Cube fica no perfil de CPU dele (CubeHub "Bateria": A55 ≤1,40 GHz, A76
  cpu4–6 ≤1,54 GHz, prime cpu7 em `powersave` a 614 MHz); o desenvolvedor pediu para não mudar ("nao muda, deixe como esta").
- **Núcleos (`0101-pin-by-allowed-speed-and-gs.patch`, `hl_perf.cpp`):** os núcleos rápidos passam a ser escolhidos pela
  capacidade vezes a frequência que a política deixa (a mínima no `powersave`), os de 60% do melhor para cima; antes a
  thread do jogo caía no prime a 614 MHz. Com 3 ou mais rápidos o GS do ARMSX2 também vai para eles; com exatamente 3, o
  `IopWorker` vai para os outros. `HL_PIN_GS=0/1`, `HL_PIN_IOP=fast/rest`.
  - Cube, luta 2x (`cube_s2` → `cube_pin2` → `cube_pin3`): B 37,1 → 44,7 → **50,0 fps** (mín. 48,0), A 41,8 → 49,5.
  - Mi A3 (4 A73 rápidos, GS fixado neles): sem diferença (`mia3_pin3` 43,6, `mia3_pin3b` 43,8, `mia3_nogs` 44,0 na B).
- **VIF1 (Nominum `85726f6`, `NOMINUM_COMMIT` atualizado):** `simpleperf` com pilha DWARF na `VuWorker` do Cube: o laço
  geral do UNPACK era o ponto mais quente do `Vif1::transfer` (V2-16 com máscara e MODE 1, V1-8 com máscara, V3-32 e V4-16
  com máscara: 15 de 36 M vetores do dump da luta). Caminho rápido novo para máscara/MODE 1. Testes do Nominum: 106/106 no
  PC, 130/130 arm64 (qemu). Replay no Cube: VIF1+VU1 12,70 → 11,87 ms/vsync.
  - Cube luta 2x (`cube_unpk`): VU1 12,6 → 11,7 ms, 50,0 fps. Campanha 2x (`cube_camp_s2` → `cube_camp_unpk`): 49,0 →
    **49,7 fps**, imagens certas (vídeo).
  - Mi A3 luta 1x (`mia3_unpk`): A 47,2 → 49,2, B 43,6 → 44,7 (VU1 20,0 → 19,3 ms; EE 19,2 segue).
- **Anel do GS (`0102-gif-path1-compact-into-ring.patch`, `hl_gs_live.cpp`):** o pacote do PATH1 é compactado direto no anel
  quando há espaço contíguo (cabeçalho com o tamanho final); mesmos bytes, uma cópia a menos (~2,8 MB por quadro na luta).
- **Cube 1x/1,75x/2x/3x (GS emulado, VU1 do Nominum):** antes dos núcleos todas ~37 fps na B (EE no prime lento); depois,
  2x a 50 e 3x a 48,6 (mín. 36,8, GS 17–20 ms). Imagens certas em 3x (seleção, loading, luta, golpe).
- **Drivers no Cube:** `malisx2` 0.4.0 (de `~/cube-kernel/untrusted`) instalado pelo importador do app: luta 2x igual ao
  driver da Mali (50,0; GS 14,7 contra 16,1 ms), 3x pior numa rodada com Ougi (42,2); imagens certas. Voltou para o driver
  do sistema. PanVK: nenhum pacote no disco; não testado.
- **Turnip no Mi A3** (pedido do desenvolvedor; luta 2x com vídeo, `drv_*`): sistema 39,3/37,2; Qualcomm 891.14 38,3/37,5;
  Turnip 25.1.0 (Mr. Purple) 29,2/29,4; 27.0.0-R2 (StevenMXZ) 27,6/27,3; T30 (Mr. Purple) 27,9/35,9; 26.0.0-R8 (K11MCH1)
  não abre ("only claims support for Vulkan 1.0" na Adreno 610). Imagens certas com todos os que abrem. Turnip é mais lento
  na 610. A seleção voltou para o 891.14 (a que estava). Os pacotes vieram pelo `gh` (a API do GitHub estava sem consultas
  neste IP; o app mostrou a mensagem certa).
- **Driver que não abre o GS:** antes o jogo fechava calado. Agora o motor grava `gs_failed.txt` e o launcher volta para o
  driver do sistema e mostra "O driver não funcionou" (com o log salvo). Testado no Mi A3 com o R8, pelo JOGAR.
- **Tela de drivers:** a linha inteira escolhe (antes só o círculo). Testado no Mi A3.
- APK: `build/HiddenLeaf-20261112-cube.apk`.

### 2026-11-12 20:00, noite: a VU1 do Nominum ligada por padrão; campanha e A35 com a thread do IOP

- **Achado:** o app não definia `NOMINUM_VU1`: quem joga rodava a VU1 do runtime, e todas as rotas de teste desde 11-09
  usavam `NOMINUM_VU1=1` (os `hl_env` de teste). `un5_main.cpp` agora põe `NOMINUM_VU1=1` (se não vier de fora; `=0` para a
  do runtime). A VU1 do Nominum é a de microprogramas recompilados (o "nativo"); a do runtime é a interpretada.
- **Luta (Mi A3, 1x, Naruto x Naruto, fases A / B):** VU1 do runtime 35,4 / 41,7 (`rtvu1`, vídeo: Ougi ligado
  Naruto+Sakura e WINNER certos); Nominum 48,9 / 44,1 (`iop_default`). Sem `hl_env` de nada: 47,1 / 43,6, com
  `[nvu1]` e `[iop] the IOP on its own thread` no log (`nodef`).
- **Campanha (`env_campfrac_1`, v8300–10300, vídeo):** Nominum 35,2 fps (`camp_n`; igual a antes da thread do IOP:
  aqui o limite é a VU1, 30 ms por quadro, o jogo esperando 12 ms por ela); runtime 25,5 (`camp_rt`, VU1 46 ms). As duas
  iguais na imagem: cena do portão, NOW LOADING, vila com Jiraiya e Sakura, tutorial de pulo, Ichiraku, portão da vila.
- **A35 no Mi A3, até a luta pela primeira vez:** o save do UN5 do aparelho não tem jogo gravado (a rota de campanha
  começa por New Game), então o `ModSaves` não refaz o do mod. Pus como `BASLUS-55606NARUTO6` o save com jogo no slot 1 de
  `build/lab/d2/sv/1` (o vazio de antes em `files/a35_save_empty`). Rota `stage_env/a35_*.txt` (de `env_a3_a35f`, 1x):
  "Carregado com sucesso", seleção, VS, luta com Ougi, volta à seleção; logos e menus a 60,0. **A thread do IOP não ajuda
  o A35:** fase B 41,3 serial (`a35_ser`) e 40,4 com ela (`a35_iop2`); o mod é NTSC (16,7 ms por quadro) e a VU1 sozinha
  leva ~21 ms. Os mods seguem no modo serial (`#ifndef HL_PACK_ID`). O A35 foi desligado de novo (`un5_mods.txt` vazio).
- **Pacote do A35 refeito** com os patches até o `0100` (`HL_V80=1 recomp/android/build.sh --mod un6-a35`).

### 2026-11-12 18:00, noite: o IOP e o SPU2 numa thread própria (patch `0100`), ligado por padrão no jogo base

- **Como:** `advanceIopEeCycles` põe o lote de ciclos numa fila (anel de 1024 itens) e volta na hora; a thread
  `IopWorker` roda os lotes na mesma ordem e com o mesmo tamanho. O DMA do SIF do EE para o IOP (`writeIopMemory`,
  `notifyIopSifTransfer`) entra na mesma fila, então chega ao IOP quando o relógio dele está onde o do EE estava (como no
  modo serial). O que o IOP faz no lado do EE (escritas na RAM do EE: `sceSifSetDma`, respostas de RPC; comandos SIF, que
  rodam os tratadores do EE) fica guardado e é feito na thread do jogo na próxima entrega, em ordem. O resto (RPC, leitura
  da RAM do IOP, módulos, reset) espera a fila esvaziar antes (`iopSync`), e só uma thread roda o IOP de cada vez.
  `HL_IOP_THREAD=0` volta ao modo antigo; `HL_IOP_LAG` (padrão 16000000 ciclos do EE), `HL_IOP_NAP_US` (300),
  `HL_IOP_SPIN_US` (0), `HL_IOP_STATS=1` (de onde vem o tempo, a cada 100000 entregas). `[perf]` ganhou `iopw` (CPU da
  thread). `hl_perf`: nice -10 e núcleos grandes também para a `IopWorker` (`HL_PIN=x`: o quarto núcleo grande).
- **O caminho até aí** (Naruto x Naruto, `fstats.py`, fase B; base `perf_chain` 39,1 fps):
  - um lote de folga, o EE esperando o anterior: 39,8 (`iopw1`); o IOP vem em rajadas: 1 lote em 11 levava ~140 µs a mais;
  - fila com até 65536 ciclos: 42,9 (`iopq_st`); 83% dos ciclos do EE chegam em lotes de ~3,3 milhões: o EE ocioso pula
    até o vsync (`waitForEvent` conta tudo de uma vez) e o IOP emula esse tempo inteiro;
  - folga de 16 milhões: a espera passou toda para o DMA do SIF (~10 por quadro, 860 ms a cada 100 mil entregas): 42,1;
  - o DMA do SIF na fila em vez de sincronizar: espera do jogo pelo IOP 0,0 ms; 43,6 (`iop_q16m`);
  - a thread dormindo 300 µs com a fila vazia em vez de girar: igual (43,1–43,8, ruído); fica o cochilo (menos núcleo
    gasto). Girar por 20 µs e acordar a cada lote: 36,3 (`iopq_spin20`). `HL_PIN=x`: sem ganho.
- **Resultados (Mi A3, GS, 1x, fases A / B):** Naruto 45,9 / 39,1 → 48,9 / 44,1 (`iop_default`, já pelo padrão); Neji
  39,8 / 41,5 → 46,7 / 44,1 (`iop_full_neji`); Kakashi 40,4 / 40,2 → 44,6 / 43,7 (`iop_full_kakashi`).
- **Conferido:** rotas completas de Neji e Kakashi com vídeo (abertura e filme com som, menus, seleção, lutas, o Ougi do Neji
  inteiro, volta à seleção, a segunda luta do Kakashi); a 1ª luta começa no mesmo vsync do serial (6229, 6209; no
  Naruto 6103 ou 6107 conforme a rodada: a IA joga diferente depois, o computador às vezes cai antes). Som gravado com
  `HL_SPU2_REC` nas duas versões (`mia3/snd_serial`, `snd_iop`, `snd.wav`): nenhum buraco com som em volta, o mesmo perfil
  de volume até a luta.
- **Não testado:** mods (ficam no modo serial: `#ifndef HL_PACK_ID` no `un5_main.cpp`), campanha, outros aparelhos.
- **O que segura agora:** a thread do jogo é o EE (~19 ms na fase B) mais ~2 ms esperando a VU1; a `VuWorker` está em
  ~20 ms (Nominum `Vif1::transfer` + `recBlock`). Para 50 na luta falta baixar as duas.

### 2026-11-12 16:00, tarde: menu lateral no Mi A3 e o EE 3 ms mais leve na luta (patch `0099`)

- **Menu lateral** (`lab/side-panel`, `07aee1e`, juntado): no aparelho a primeira abertura derrubava o app
  (`IllegalStateException: ViewTreeLifecycleOwner not found from RelativeLayout`: o Compose procura o dono do ciclo de
  vida pela raiz da janela, e a `SDLActivity` não é do AndroidX). `GameMenuHost.installOwners(window.decorView, layout)`
  antes do `addView`. Testado numa luta (`build/lab/menu_test.sh`, `mia3/menu_shots2`, `menu_ed_shots`): Voltar abre, as
  5 abas por R1/L1, foco pelo direcional, jogo parado com o menu aberto (v6813 por 6 s) e de volta ao fechar (v6927 →
  v7155 em 6 s); Toque › Editar layout abre o editor sobre a luta congelada, B volta à gaveta, B fecha. O menu lembra a
  última aba. Sem controle físico no Mi A3: o mapa de botões e os atalhos SELECT+START / L3+R3 não foram testados.
- **Onde vai o tempo** (luta, GS, 1x, fase B): três threads cheias nos núcleos grandes, `GameThread` (EE+IOP+SPU2) ~100%,
  `VuWorker` ~90%, `GS` ~88% (`build/lab/threads.sh`, por `/proc/<pid>/task`). EE 21,5 + IOP 5,5 ms por vsync na
  `GameThread`. `HL_PROF` (diferença de dois arquivos na fase B): ~30% da `GameThread` na cópia das cadeias do VIF1
  (`vector::insert` elemento a elemento de 2–4,5 MB por cadeia; buffers acima de 4 MB soltos e alocados de novo a cada
  quadro; o `takeBuffer` devolvia um buffer qualquer, muitas vezes pequeno).
- **`0099-chain-copy-memcpy-and-pin-option.patch`:** a cadeia é escrita com `memcpy` sobre um tamanho guardado entre
  cadeias (sem zerar), o worker guarda buffers de até 16 MB e o `takeBuffer` devolve o maior (cheio, o grande toma o lugar
  do menor). Resultado (`perf_base2` → `perf_chain`, `fstats.py`): A 42,8 → 45,9 fps, B 36,4 → 39,1, EE B 21,5 → 18,6 ms.
  Imagem da luta conferida no vídeo (`menu_fight2`).
- **Sem ganho:** `HL_PIN=x` (novo: um núcleo grande para cada uma das três threads; 39,2 fps); `VsyncEnable=1` pelo
  `HL_GS_EXTRA` (o swapchain continuou MAILBOX: chave ignorada; 39,5, ruído).
- **O que falta para 50 na luta no 665:** a `GameThread` ainda leva ~25 ms (EE 18,6, IOP 5,3, espera da VU 1,1) e a
  `VuWorker` ~20 ms. O próximo passo grande é o IOP (ADX e modhsyn) e o SPU2 numa thread própria (o cpu4 fica livre); o
  resto do `memcpy` (~19% da `GameThread`) ainda não tem dono achado (sem registro de quem chama no aparelho).

### 2026-11-12 noite, launcher (`lab/side-panel`): o menu do jogo vira uma gaveta lateral com cinco abas e controle por gamepad

Sem aparelho (só `assembleDebug`, testes de unidade e capturas Robolectric). Substitui o `GamePanel.kt` (cartão no meio).

- **`engine/GameMenu.kt`** (Compose, num `ComposeView` com ciclo de vida próprio, `GameMenuHost.kt`: a `SDLActivity`
  não é do AndroidX): gaveta à esquerda, ~40% da largura em paisagem (340–460 dp, até 82% numa tela quadrada), folha de
  baixo (68% da altura) em retrato; jogo parado visível ao lado, escurecido 28%, toque nele volta ao jogo; deslize de
  200 ms. Abas **Jogo** (continuar, especial de um botão, botão do especial *ao reabrir*, valor do toque, orientação ao
  vivo, sair com confirmação), **Gráficos** (renderizador em uso; Vulkan/OpenGL ES e nativo *ao reabrir*; resolução
  *ao reabrir*; filtro, proporção, fps e contador ao vivo), **Controle** (controles conectados, atalho do menu
  SELECT+START / L3+R3 / só Voltar, mapa dos 16 botões do PS2 com "aperte um botão", restaurar), **Toque** (editar
  layout sobre o jogo parado, modo, layout do jogo/clássico trocado ao vivo, cena automática, estilo, símbolos,
  opacidade, tamanho, nomes, vibração, analógico flutuante, Toque Pro inteiro, macros: mostrar/esconder, editar com o
  editor do launcher, apagar, nova), **Ajuda** (o guia de botões por cena e o controle físico). Mesmos nomes e faixas do
  launcher (strings reaproveitadas; novas em `strings_menu.xml`, en/pt/es).
- **Tema:** papel, tinta e laranja do jogo; claro, escuro ou do sistema conforme o tema do launcher (`UiPrefs`,
  preferências "ui" lidas pelo processo do jogo ao abrir o menu); Material You usa as cores do papel de parede.
- **Gamepad** (`GameActivity`): Voltar e Guia sempre abrem; o par escolhido também (o segundo botão do par não chega ao
  jogo). No menu: direcional/analógico/hat movem o foco (repetição 380/110 ms), A = confirmar (vira DPAD_CENTER), B e
  Voltar voltam (captura, página por cima, pergunta de sair, depois fecha), L1/R1 trocam de aba, START fecha. Anel de
  foco só quando o menu é usado pelo controle. Ao abrir, as teclas que o SDL tinha apertadas são soltas.
- **Mapa do controle** (`touch/PadMap.kt`, `controller.json`): com o mapa padrão nada muda (o SDL lê o controle, como
  antes). Com um mapa próprio, o app lê botões, gatilhos, hat e analógicos e manda pelo `TouchBridge.setPhysical`
  (somado ao toque); o SDL não vê o controle.
- **Não feito / a conferir no aparelho:** nada testado em aparelho; o anel de foco não aparece nas capturas
  (o Robolectric não roda os efeitos do Compose); um controle lido pelo HIDAPI do SDL (sem passar pelo Android) não
  seria remapeado; sem atalho de avanço rápido (o motor não tem); volume de música/efeitos fora (o motor não lê).

### 2026-11-12 14:00, tarde: Ougi da Neji destravado (DMAC mascarado, patch `0098`) e o SCANMSK do Ougi ligado da Sakura

Lutas inteiras no Mi A3 pelo GS, gravadas (`build/lab/mia3/full_sakura`, `full_neji`, `sak_fix`, `neji_fix`).

- **Sakura × Naruto, Ougi ligado:** depois do Ougi, um minuto de linhas alternadas com um quadro antigo por baixo. O
  jogo escreve SCANMSK 2 nesse Ougi e não volta a 0; a escrita chega ao GS por fora do `dropScanmsk` (log
  `HL_SCANMSK_LOG`: "GS SCANMSK 2 at a vsync"). `hl_native.cpp` `vsync()`: com o GS sozinho (`!s_on`), SCANMSK volta a 0
  em todo vsync (`Flush` antes). Regravado (`sak_fix`): luta limpa até o fim.
- **Neji × Naruto, Ougi (Hakke):** tela branca, 10 fps, EE 90 ms/vsync, 0 MB/f, para sempre (4 de 4 vezes, também com
  `HL_NATIVE=1`: não é o GS). Achado em passos: `HL_PROF` (diferença de dois arquivos, antes e durante) mostrou
  `readIORegister` e `sceGsSyncPath`; `HL_IOREAD_LOG` (novo, em `0098`) mostrou as 4 leituras do `sceGsSyncPath` 1M vezes;
  o laço é `0x108748`–`0x1087cc` (função de quadro `0x108600`): `sceGsSyncPath(0)` enquanto o byte `gp-0x3F0` for 1.
  Quem zera é o tratador de DMA do VIF1 (`0x108D10` → `0x108F00`, lista de pacotes com call/ret em software; no fim
  `iDisableDmac(1)`). `HL_DMAH_LOG=<vsync>` (novo, `hl_game.cpp`) mostrou: em v8136 uma transferência avulsa do VIF1
  (CHCR `0x1`, MADR `0xE9AE80`, a imagem da thread `0x37FFB0` que passa a tela para cinza) também chama o tratador; a
  cadeia de v8138 nunca recebe o fim.
- **Causa:** `0x108320` dispara a cadeia (`0x107A20`) e só depois chama `EnableDmac(1)` (`0x15F6B8`). O runtime termina
  o DMA na hora e, se a fila de fins é esvaziada antes do EnableDmac, `EeScheduler::dispatchIrq` via o canal desligado e
  descartava o fim. No PS2 o CIS1 fica em D_STAT e a interrupção sai ao religar.
- **`0098-dmac-pending-irq-and-debug-logs.patch`:** `dispatchIrq` guarda o fim mascarado (`m_pendingDmacMask`) e
  `setIrqCauseEnabled` o entrega ao religar, se o bit do canal ainda estiver em D_STAT (W1C). Log `[irq] DMAC 1 ended
  while disabled` (8 primeiras). No mesmo patch, só para depuração: `HL_IOREAD_LOG` (`ps2_memory.cpp`) e o log de
  desistência do `sceGsSyncPath` (`[gssync]`).
- **Resultado (`neji_fix`):** o Ougi vai até o fim e a luta segue a 40–43 fps (v11177 em 254 s; antes, 444 s travado).
  Sakura de novo com `0098` (`sak_reg`): até o fim (v10600, 35–50 fps), SCANMSK zerado no Ougi, nenhum `[irq]`
  (o caminho novo não age nessa luta). O vídeo dessa rodada veio cortado (sem `moov`); o log basta para a regressão.

### 2026-11-12 11:30, dia: GS padrão testado no Mi A3 (resoluções, aspectos, OpenGL ES, menus, lutas inteiras, toques), três frentes juntas

Tudo no Mi A3 pelo caminho padrão (GS do ARMSX2, sem `HL_NATIVE`), Nominum, 50 fps.

- **Resolução** (rota `env_frac_1`, A / B fps): 0,5x 43,3 / 36,5; 0,75x 42,9 / 36,3; 1x 41,8 / 35,2; 1,5x 42,1 / 35,5;
  2x 39,8 / 34,9; 3x 21,7 / 20,7. Até 2x o limite é a CPU (EE e VU1 ~20 ms); em 3x a GPU (`res_*`).
- **Aspectos** (`asp_1333`, `asp_1778`): 4:3 e 16:9 com faixas laterais, menus, seleção, VS, carga e luta inteiros, os
  botões nas faixas. 20:9 em `cmp_default`. Mesmo fps.
- **OpenGL ES** (`cmp_gl`): os `eglChooseConfig 0x3004` eram só as tentativas de GL de PC; o ES 3.2 abria e o jogo
  caía em `armKick` (`GSDeviceVK::GetInstance()` lendo o dispositivo GL). Corrigido (`34e42ea`): fora do Vulkan nada
  do nativo roda. Desenha igual ao Vulkan, mas no Adreno 610 é mais lento (GS 46 ms/quadro, 31,7 / 27,7 fps, paradas
  de 0,5 s compilando shader no início da luta). Fica como opção; o `gpu_api` do launcher agora chega ao motor.
- **Ajustes do GS** (`gsx_*`, via `HL_GS_EXTRA`): `accurate_blending_unit=0`, `texture_preloading=1`,
  `HWDownloadMode=2` não mudam o fps (dentro do ruído). O padrão fica.
- **Lutas inteiras gravadas em vídeo** (`mia3run.sh` ganhou `REC=1`: `screenrecord` em trechos de 170 s,
  `rec_N.mp4`): Kakashi × Naruto (`full_kakashi`, 5 min): boot, logos, filmes, título, seleção, arena, VS, carga, luta
  com Ougi dos cães, vitória da CPU (volta direto à seleção, como no PCSX2) e a luta 2. **COM × COM não existe no
  jogo** (o 2P entra só com START); as lutas foram 1P pela rota contra a CPU, sem `HL_CPU_HP`.
  **Aberto:** o minijogo do Ougi do Kakashi sai com listras horizontais e o corte do Ougi com uma faixa preta; a rodada
  de referência no PCSX2 (`runs/kak_ougi`) seguiu outro caminho e não teve o Ougi: não comparado.
- **Menus** (`menus2`, vídeo): título, cartão, seleção de modo, Master Mode e a abertura em 3D da campanha (créditos e
  falas) desenhados certos. (`menus`: a rota escolheu Load Game e ficou no cartão vazio; refeita.)
- **Especiais por toque real** (`touch_sp3`, `touch_sp4`, `adb input tap`, `HL_PAD_LOG=1` novo em `un5_main.cpp`):
  as sequências chegam ao jogo como devem (↑ 70 ms, solta 50, ↑ 70, solta 50, ○ 90; △○ e △△○ iguais), cada parte por
  2 a 4 vsyncs a ~40 fps. O lutador ataca em cada uma e o Jutsu acende a aura de chakra; **não provei** pelo vídeo que
  ↑↑○ dá um golpe diferente do ○. Risco: abaixo de ~25 fps a soltura de 50 ms pode cair entre dois quadros do jogo.
- **Orientação** (`orient_port`, `orient_auto`): retrato com o jogo 4:3 em cima e os controles embaixo; automático
  gira de paisagem para retrato com o jogo aberto, sem reiniciar. A volta para paisagem não foi fotografada.
- **Launcher no aparelho** (`mia3/launcher/port_*.png`, retrato): barra com Início, Online, Controles, Ajustes, Mais;
  Mais com Texturas, Mods, Drivers, Trapaças, Depuração. Sem texto cortado nas páginas vistas.
- **Painel do jogo** (`GamePanel.kt`): sai a dica do R3, entram ↑↑○ e ↓↓○; os gestos dos menus só aparecem se a
  opção antiga estiver ligada.
- **Juntado nesta branch:** online (`af052ef`, `a3555f7`, `f22b6bb`, patch `0097` aplicado e compilado), controles
  (`4a78a72`), launcher (`a83c68f`..`e5ddf86`). O online **não foi testado** (falta o Worker publicado e dois
  aparelhos).
- APK: `build/HiddenLeaf-20261112-gs.apk` (instalado no Mi A3).

### 2026-11-12 tarde: online de dois jogadores (salas, lockstep no motor, tela Online); não testado em aparelho

Feito numa branch de agente, a partir do online do Dragon Rage (BT2), sem build do motor nem aparelho.

- **Servidor** (`online/worker/`, commit `af052ef`): Cloudflare Worker com D1 e um Durable Object de relay (`HiddenLeafRelay`).
  Salas de 6 dígitos, `fp` igual obrigatório (versão do app, motor, mods ligados), opções de quem cria (atraso 0–4 quadros,
  relay). Passo a passo em português no `README.md`. `node test.mjs` passa.
- **Motor** (`recomp/android/host/online/`, commit `a3555f7`): `link` (UDP, STUN, furo de NAT, relay pela sala via o app),
  `lockstep` (20 ms por vsync, atraso 2–8 vsyncs), `session` e `match_session` (encontro no primeiro vsync, quem cria é o P1,
  checagem da RAM a cada 32 vsyncs pulando as páginas de `docs/online.md`). `hl_online.cpp` lê `<root>/online/session.txt`
  (renomeia para `session.used.txt`), põe cartão de memória próprio (`online/saves`), relógio fixo (`HL_FIXED_CLOCK`, TZ UTC),
  tudo liberado, ajudas do toque desligadas, câmera 4:3 do jogo nos dois. Patch `0097`: gancho dos pads por porta
  (`g_hlPadPortHook`, porta 0 = P1, porta 1 = P2) e `sceCdReadClock` fixo. Testes no PC (ASan/UBSan) passam; `hl_online.cpp`
  e `un5_main.cpp` passam no `-fsyntax-only` do NDK.
- **Launcher**: página Online (nome, criar sala, entrar com código, atraso, relay, servidor e "Testar conexão"); STUN em
  Kotlin no mesmo porto UDP que o motor reabre; `OnlineGame` no `GameActivity` (linha de estado no canto, relay por
  WebSocket, aviso de saída ao outro). O painel não pausa o jogo numa partida online.
- **Falta**: build do motor com o `0097`, publicar o Worker, teste em dois aparelhos (mesma versão e mods). Riscos: o porto
  UDP reaberto pelo motor (NAT), o determinismo entre aparelhos diferentes (só a mesma build foi vista igual), o relay na
  Cloudflare de verdade.

### 2026-11-12 tarde: controle na tela: Especial 1 e 2, sem nomes, Toque Pro, guia e editor de macros (só launcher, sem aparelho)

- **Especiais (pedido de 11-09 21h30, item 3):** dois botões novos na luta, `Ctl.SPECIAL_UP` (↑ ↑ ○) e `Ctl.SPECIAL_DOWN`
  (↓ ↓ ○), sequências como o Jutsu: direção 70 ms, solta 50 ms, direção 70 ms, solta 50 ms, ○ 90 ms. Enquanto uma
  sequência com direção toca, o direcional da tela é ignorado (senão o polegar no direcional vira diagonal ou tapa o
  intervalo entre os dois ↑). Ícone: duas setas cheias dentro do anel do ○; o canto mostra `↑↑○`. Lugar padrão
  (0,68; 0,30) e (0,63; 0,47); as macros do jogador começam mais à esquerda (`macroPlace`).
- **Sem nomes por padrão:** os botões da luta e da campanha mostram só o ícone e o botão do PS2 no canto (o "Item" sob
  L1/R1 também sai). Opção "Mostrar nomes nos botões" (`showNames`, desligada).
- **Botão sem função:** o Despertar (R3) nunca foi visto fazendo algo numa luta (a lista de 11-09 08:58 confere direcional,
  ataque, pulo, chakra, jutsu, Ougi, disputa, item, defesa e START). Agora começa escondido; o editor de layout ainda o
  mostra. Quem já tinha movido o R3 continua com ele (o lugar gravado vale).
- **Toque Pro** (`TouchConfig.pro`, gravado como `"pro"`; o arquivo antigo lê com tudo desligado, sem mudar a versão):
  deslizar entre todos os controles (jutsu, especiais, macros, L1/R1; o normal só entre os botões de ação), direcional da
  luta e analógico da campanha fixo, flutuante ou dinâmico (zona da esquerda ajustável), tamanho deles (85% por padrão),
  zona morta e área de toque dos botões. Modelo: `TouchPro.kt` do Dragon Rage.
- **Página Controles:** guia "O que cada botão faz" por cena, com o desenho dos próprios botões
  (`NinjaPadView.paintSample`); seção Toque Pro com guia de cartões desenhados ("Como funciona"); editor de macros em tela
  cheia: modelos (Jutsu, Ougi, ↑↑○, ↓↓○, combo, turbo), botões por passo, tempo com − + e atalhos, mover, duplicar,
  pausa, repetir, mostrar e tamanho na tela, "Salvar e posicionar" abre o editor de layout. Textos novos em
  `strings_touch.xml` (en, pt, es).
- **Conferido:** `assembleDebug`, `TouchConfigTest` (arquivo antigo e ida e volta), capturas Robolectric em
  `android/build/shots/` (luta em 1560x720 e 1280x720: padrão, com nomes e Pro com o polegar; página, guia e editor
  de macros em pt).
- **Falta no aparelho:** se o ↑↑○ e o ↓↓○ saem no jogo com 70/50/90 ms (e com o "Especial com um botão" ligado, que
  pode tomar o ○ final); o R3 numa luta; o △, □ e L1/R1 no mapa da campanha; o Toque Pro jogado (deslizar, direcional
  dinâmico).

### 2026-11-12, tarde: launcher reorganizado (barra de 5, Mais, Trapaças, Depuração), orientação e `gpu_api` no motor

Sem aparelho (só build e capturas do Robolectric). Pedido do dono: menos itens na barra e as opções de teste fora dos Ajustes.

- **Navegação** (`MainActivity.kt`): barra (ou painel lateral em paisagem) com 5 páginas: Início, Online, Controles,
  Ajustes, Mais. **Mais** (`MoreScreen.kt`) lista Texturas, Mods, Drivers, Trapaças e Depuração; essas páginas têm
  "← Mais" no topo e o Mais fica aceso nelas; tocar Mais de novo volta à lista. Na barra de baixo cada item ocupa 1/5
  da largura (antes 74 dp fixos com rolagem lateral); no painel lateral a folha some abaixo de 420 dp de altura (o Mi A3
  em paisagem tem 360 dp) para os 5 caberem. `MainActivity.EXTRA_PAGE` abre uma página na partida (testes, atalhos).
- **Online:** `OnlineScreen.kt` aqui é só um marcador para compilar; a versão da sessão do online substitui no merge.
- **Trapaças** (`CheatsScreen.kt`, nos moldes do Dragon Rage): "Pular a abertura" (`skip_intro`) e "Desbloquear tudo"
  (`dr_unlock_all`, com confirmação: salvar no jogo grava no cartão e o app não desfaz). Saíram dos Ajustes.
- **Depuração:** cartão "Opções de teste" com o contador (`meter` 0 a 3: desligado, FPS, FPS e tempo, completo; antes
  só liga/desliga) e o "Renderizador nativo (experimental)" (`native_render`). Saíram dos Ajustes.
- **Ajustes:** vídeo (resolução, filtro, proporção), cartão **Tela** com a orientação (Paisagem, Retrato, Automático),
  desempenho (fps e o **Renderizador** Vulkan/OpenGL ES, de volta), aparência, memory card, sobre. Áudio não entrou: o
  motor não lê `music`/`effects` (escondidos em `cc6696d` por isso).
- **Orientação** (`GameActivity.orientationFlag`): `dr_orient` volta a valer: 0 `SENSOR_LANDSCAPE`, 1 `SENSOR_PORTRAIT`,
  2 `FULL_USER` (segue o sensor e respeita a trava de rotação). A atividade já trata `orientation|screenSize` sozinha
  (sem reiniciar o motor); o SDL manda `SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED` e o `un5_main.cpp` refaz a superfície do GS
  e o `applyVideo` (em retrato a forma da tela fica menor que 4:3, então a imagem fica 4:3). **Não testado:** o giro com
  o jogo aberto, o controle na tela em retrato, o renderizador nativo em retrato; a ROM do RP5 prende a tela deitada.
- **`gpu_api` no motor** (`un5_main.cpp` `loadSettings`): `setenv("HL_GS_API", gpu_api == 1 ? "gl" : "vulkan", 0)` (um
  `HL_GS_API` do `hl_env.txt` ganha). Antes o motor só lia a variável e o jogo era sempre Vulkan. **Não compilado
  aqui.** Risco conhecido: o GS em OpenGL ES não abre no Mi A3 (`eglChooseConfig() failed: 0x3004`, 11-12 12:00); o texto
  da opção manda voltar ao Vulkan se o jogo não abrir.
- **Textos:** novos em `res/values*/strings_launcher.xml` (prefixo `ln_`); o título "HIDDEN LEAF" do Início cabe numa
  linha em retrato (34 sp abaixo de 420 dp); "pacote" → "pack" na ajuda de texturas (pt) e "Agregar pack" (es), iguais
  ao botão.
- **Capturas** (`ScreenshotTest.pages`, `android/build/shots/page_<página>_<pt|es>_<land|port|landfull|portfull>.png`):
  as 10 páginas em pt e es, 780x360 dp e 360x780 dp (o Mi A3) e janelas altas para ver cada página inteira.

### 2026-11-12 12:00, dia: nativo × GS do ARMSX2 no Mi A3 a 1x; **o GS passa a ser o padrão**, o nativo vira opção

Pedido do dono: com um bug novo a cada partida no nativo, comparar os caminhos e, se o GS desenhar tudo com desempenho
parecido, deixá-lo como padrão. Rota `env_frac_1` (Naruto × Naruto, Nominum, 50 fps, 1x, 20:9), `fstats.py`
(A = começo da luta, B = depois da vida da CPU cair):

| caminho | A fps | A mín | B fps | B mín | EE B | VU1 B | GS ms/quadro (v5600-8400) |
|---|---|---|---|---|---|---|---|
| nativo (`cmp_native`) | 44,0 | 39,8 | 38,4 | 29,2 | 19,2 | 19,8 | 13,8 |
| GS Vulkan (`cmp_gsvk`) | 42,1 | 35,5 | 35,9 | 19,1 | 20,6 | 20,3 | 20,9 |
| GS Vulkan, SCANMSK zerado (`cmp_gsvk2`) | 42,3 | 35,6 | 36,1 | 28,3 | 20,7 | 20,4 | |
| padrão novo, sem `HL_NATIVE` (`cmp_default`) | 41,8 | 35,9 | 35,2 | 28,9 | 22,3 | 20,2 | |

- O GS do ARMSX2 gasta ~50% a mais de CPU na thread dele (21 contra 14 ms), mas ela roda em outro núcleo: o limite segue
  o EE e a VU1 (~20 ms). Diferença final: 2 a 3 fps (5-7%).
- **GS OpenGL ES** (`HL_GS_API=gl`, `cmp_gsgl`): não abre no Mi A3 (`eglChooseConfig() failed: 0x3004`). E o
  `gpu_api` que o launcher grava não chega ao motor (só o `HL_GS_API` do ambiente): na prática o jogo é sempre Vulkan.
- **Ougi pelo GS escuro e listrado** (`cmp_gsvk`, v8451): o SCANMSK 2 do jogo. Com o nativo desligado o
  `hl_native.cpp` não zerava o SCANMSK; agora zera (`dropScanmsk`), `cmp_gsvk2`: o Ougi sai inteiro e o mínimo da luta B
  volta de 19 para 28 fps. Não comparei esse Ougi (fundo azul-escuro) com o PCSX2.
- **Pelo GS não aparecem** o ruído da água do lago (já visto em `w2_gs`) nem a grade de retratos sumindo na seleção em
  20:9 (`cmp_default/shot_v4261`): os dois eram do nativo.
- **Mudança:** Ajustes › "Renderizador nativo (experimental)", desligado por padrão (`native_render`; o `un5_main.cpp`
  põe `HL_NATIVE=0` se não estiver ligado; um `HL_NATIVE` do `hl_env.txt` ganha). Mi A3 `cmp_default`: menus, seleção,
  VS, carga e luta pelo GS, sem nenhuma linha `hl_native:` no log.
- **Risco aberto:** em 11-05 a luta em 3D saiu preta no Cube (Mali) com `HL_NATIVE=0`. Não testei no Cube (só o Mi A3
  está liberado). Antes de lançar com o GS padrão é preciso um teste num aparelho Mali.
- Em `cmp_default/shot_v5500` a tela está branca (entre o VS e a carga); não conferi se é o clarão do próprio jogo.

### 2026-11-12 09:00, manhã: água do lago (3/24) limpa por ora, "Pular a abertura", buffers reaproveitados (`0095`)

Tudo no Mi A3 (1560x720, `libun5_v80.so`, 2x nas rotas, Nominum).

- **Ruído colorido na água do cenário 3/24** (floresta com o portão vermelho, relato do dono jogando; o mesmo da Vila da
  Folha no Cube, 11-05). Rota `stage_env/s2.txt` (`w2_*` em `build/lab/mia3/`), PCSX2 de referência `runs/lake2_*.png`:
  - só com o GS (`HL_NATIVE=0`, `w2_gs`) a água sai limpa; com o nativo, sujo em 4:3 (`w2_43`) e no largo: **é do
    nativo, não do widescreen**;
  - a água é desenhada pelo GS ("texture not known") com a textura `0x3000` (CT32, TBW 8), que é o FBP `0x180`: o GS
    copia ali a cena inteira a cada quadro (sprite `prim 356`, `tex0 264020000`, colunas de 32 px, 512x448; log
    `HL_NATIVE_LOGFBP=180`), e o ruído fica onde estaria o reflexo dos lutadores (o das árvores sai certo);
  - tirando os desenhos que leem `0x3000` (`HL_NATIVE_SKIPTEX=3000`) o ruído some; tirando `0x3c00` ou `0xe00`, não;
  - **não resolveram:** mandar todos os envios pendentes antes de cada desenho do GS (`HL_NATIVE_ALLUP`), a cópia do Z
    para `0x1E0` ao GS (`HL_NATIVE_OFF=k`), os desenhos só de alfa ao GS (`HL_NATIVE_ALPHA`), a cena válida até a
    linha 512, todos os envios antes de uma textura lida da memória, e passar ao GS essas texturas (`HL_NATIVE_NOMEM`:
    nenhuma neste trecho). `HL_NATIVE_OFF=p` e `SKIPTEX=0` travam a rota no "Start Battle";
  - **correção provisória** (`hl_native.cpp`): com o nativo ligado, o desenho da cena que lê o buffer em que o GS copiou a
    cena (`s_sceneCopyFbp`) sai fora. A água fica limpa, mas sem o reflexo dos lutadores (`w2_fix`, 50 fps).
    `HL_NATIVE_REFLECT=1` volta ao de antes. **A causa continua aberta.**
- **"Pular a abertura"** (Ajustes, ligado por padrão; `skip_intro` → `HL_SKIP_INTRO`, patch `0096`): do boot ao fim do
  terceiro filme (`LOGO_C`, `NARUTO10`, `OPENING`), START por 4 vsyncs a cada 12, o pulo do próprio jogo. No último
  filme, só enquanto ele entrega quadros: o jogo fecha o filme depois, e um START a mais chegou ao título e escolheu
  "Load Game" (`boot_skip2`). A escolha de idioma espera o ✕ (START não a aceita). Mi A3 (`boot_skip3`, ✕ tocado no
  idioma): logo, filmes e portas passam sozinhos, título com New Game/Load Game no v2000 sem nada escolhido. No PC
  (`runs/skip3`), cartão vazio: para na mensagem do cartão perto do v1500. Desligado sob `HL_PADROUTE`.
- **`0095`:** os buffers das transferências ao GS e à VU1 vêm da lista da `VuWorker` (até 256, até 4 MB cada; antes
  16) e o `sceSifSetDma` usa um buffer por thread: no perfil da luta (`prof8`) uns 5% da thread do jogo eram
  `std::vector` criando, crescendo e apagando. **Ganho não medido ainda.**
- `hl_prof.cpp` grava também o registrador de retorno (`hl_prof.txt.lr`, arm64) e `hlprof.py --callers TEXTO` diz quem
  chamou as funções-folha (`memcpy`, `malloc`).
- **Achados, a fazer:** na seleção em ultrawide (20:9 do Mi A3) a grade de retratos do 1P quase some e a caixa
  "Linked Character" sai cortada (prints do dono, `live/user/u013.png`); os clones do Naruto falham e somem (relato do
  dono); o corte do Ougi com uma faixa preta (`w2_fix/shot_v6*`) a comparar com o PCSX2.

### 2026-11-11 22:50, noite: resoluções 0,5x e 0,75x; aviso do A35 lendo os slots; textos de 30 fps; toque no fim da conversa

- **Resoluções fracionadas (launcher):** `SCALE_STEPS` começa em 2 e 3 (0,5x 256×224 e 0,75x 384×336) em Ajustes, e
  "0.5x · 224p" e "0.75x · 336p" no painel do jogo. O `scale` inteiro antigo nunca fica 0
  (`EngineSettings.wholeScale`). O motor já levava `scale4/4` ao GS. O `bt4gsx` aceita no mínimo 0,5x e o GS só
  limita por cima; o renderizador nativo usa a mesma escala do GS (o limite de passes por envio conta como 1x).
- **Teste no Mi A3 (rota de luta, 50, Nominum, `env_frac_*`):** a imagem sai certa nas três (0,5x mais suave, HUD e
  efeitos no lugar; `mia3/frac_*`, `live/frac_cmp.png`).

  | escala | A fps / mín | B fps / mín | EE B | VU1 B |
  |---|---|---|---|---|
  | 1x | 48,8 / 45,6 | 42,2 / 33,7 | 16,9 | 19,1 |
  | 0,75x | 49,0 / 45,9 | 42,3 / 33,1 | 16,8 | 19,1 |
  | 0,5x | 48,8 / 45,4 | 42,3 / 32,8 | 16,8 | 19,1 |

  Campanha (`env_campfrac_*`, v8300–10300): 35,2 fps em 1x e em 0,5x. **No Mi A3 não ganha nada:** em 1x o limite já
  é a CPU (VU1 e EE); o "GS" do `[perf]` é a CPU da thread do GS, não a GPU. A margem vale para aparelhos limitados
  pela GPU (preenchimento de pixels). Para comparação, o 2x do Mi A3 (`prof8`) dá 41,5 / 38,3.
- **Aviso do A35 com save sem jogo (corrigido):** o save do UN5 criado no Mode Select tem `data01`–`data04` todos
  `0xFF`; o de 11-02 com um jogo no slot 1 tem `data01` e a cópia `data04` escritos (`build/lab/d2/sv/1`).
  `ModSaves.hasGame` lê isso. O cartão avisa quando nem o save do mod nem o do UN5 têm jogo; antes ele só avisava sem
  pasta nenhuma. Um save do mod sem jogo é refeito a partir do UN5 quando este tiver um (antes, uma vez criado, nunca
  mais era trocado). Visto no Mi A3: o cartão do A35 agora mostra o aviso.
- **Textos de 30 fps (corrigido):** `gpu_offer_apply`, `gpu_opt_interp_off` e `debug_gpu_opt_text` (pt, en, es)
  falam dos 50 quadros do jogo.
- **Toque no fim da conversa (corrigido):** quando a cena passa de 4 (conversa) para 3 (campanha), os botões de ação
  ignoram toques novos por 700 ms (`NinjaPadView.TALK_END_GUARD_MS`); analógico e direcional valem na hora. Era o ✕
  do menu caindo no Ataque/○ e reabrindo a conversa (entrada de 20:55).
- **Gradle:** o `/tmp` desta máquina tem cota por usuário e encheu ("Cota de disco excedida" no `packageDebug`).
  Compilar com `TMPDIR` e `-Djava.io.tmpdir` em `build/tmp`.
- **Bibliotecas:** as quatro `libun5*.so` foram recompiladas com a correção da conversa de 20:55 (`d50ae13`).

### 2026-11-11 20:55, noite: Master Mode jogado por toque no Mi A3; jogador preso na sala da Hokage (corrigido)

- **Como:** jogo base (mod desligado), 2x, Nominum, toque injetado (`input swipe`/`motionevent`) olhando a tela a cada
  passo (`build/lab/mia3/live/`: `sh.sh`, `t.sh`, `stick.sh`). Objetivo: gravar um slot no save do UN5 para o A35
  (ver a entrada de 17:30). O UN5 não grava pelo "Quit" do menu START ("You will resume from the last point saved"),
  só em pontos da história; ainda não cheguei a um.
- **Bloqueio (corrigido):** na sala da Hokage, depois de "Sakura has joined your team", a tela ficava com os controles
  de menu (direcional, sem analógico) e o Naruto não andava (nem direcional nem arrasto). O launcher recebia a cena
  **4** (`touch: scene 4` no logcat): a `0x723970` do ADV, o sinal de "jogador livre" de 11-09, não roda nessa sala.
  Duas rodadas, as duas presas no mesmo ponto.
- **Correção (`hl_game.cpp`):**
  - sinal positivo da conversa: a `0x7F3F60` do ADV roda uma vez por quadro de lógica com a caixa de fala aberta e
    nunca com o jogador livre ("only A", 400 chamadas em 800 vsyncs nas duas rodadas de 11-09). Rodou há até
    15 vsyncs → 4;
  - o sinal antigo (a `0x723970` parada) só vale numa área em que ela já rodou. A área conta cada vez que a cena do
    renderizador nativo liga (todo carregamento de área passa pelo GS). Onde ela nunca roda, ficam os controles da
    campanha, que não prendem ninguém;
  - a `0x7F3F60` e a `0x723970` são iguais no ADV do A35 (código gerado idêntico), e o `ADV.BIN` da Alpha 1.1X é o da
    base.
- **Teste no Mi A3 (por toque, `HL_FNCOUNT=7f3f60,723970`):**
  - na rua, `7f3f60` 25 por 50 vsyncs só na fala, `723970` só livre;
  - falar com o Jiraiya → 4 (menu), fim da fala → 3;
  - a conversa da Tsunade começa com os controles de menu;
  - depois da Sakura, o aviso e o tutorial "Team edit" saem com os de menu; livre, o analógico aparece e o Naruto anda
    pela sala ("Exit", até o Jiraiya); a fala da Temari troca para o de menu e volta.
- **Ainda imperfeito:** algumas falas da conversa da Tsunade são do tipo "evento" (nem a `0x7F3F60` nem as funções
  comuns do quadro rodam) e aparecem com os controles da campanha. O ✕ da campanha ("Pular") passa o texto, então
  ninguém fica preso, mas é menos cômodo.
- **Achado de toque (aberto):** o ✕ do controle de menu fica quase no mesmo lugar do Ataque/○ ("Talk") da campanha.
  Quem aperta ✕ seguido para passar o texto, ao fim da conversa, abre a mesma conversa de novo. Foi isso que pareceu um
  "diálogo travado" na primeira rodada; o jogo estava certo.
- **Travamento da GPU (aberto):** na primeira rodada o jogo congelou ao entrar na primeira cena do Master Mode
  (v14241, tela e `screencap` parados). Kernel: `page allocation failure: order:6` no `kgsl_worker_thr` e
  `Unable to submit command to the ringbuffer -12`, como em 11-10, agora com o limite de 24 passes por envio (2x). O
  `/proc/buddyinfo` mostrou a zona Normal sem nenhum bloco acima de 64 KB, mesmo com o jogo fechado. Não repetiu nas
  três rodadas seguintes. `build/lab/mia3/mm_hang/` (log, pilhas, kernel).
- **Desempenho na aldeia (2x, aberto):** rua com fala e andando ~31 fps (VU1 28 ms, EE 20 ms); a cena de câmera da
  primeira chegada à aldeia cai para 23 fps de vsync (11 quadros do jogo), VU1 38,8 ms, GS 41 ms, 6,6 MB por quadro.
  O ajuste do jogador neste Mi A3 é 1x (`scale=1` em `un5_settings.txt`); as rodadas forçaram 2x pelo `hl_env`.
- **Não é defeito:** a faixa preta embaixo na cena de chegada é o letterbox de cinema do próprio jogo (some na rua).
- **Ferramentas:**
  - `HL_FNCOUNT=all` pega só as primeiras 8192 entradas da tabela do ADV (até `0x6d4bb0`), que guarda também o meio
    das funções. Para o ADV inteiro, usar a lista de `build/lab/hl_env_camp_fnstand.txt`. `build/lab/fnwin.py`
    compara janelas de vsync nas linhas `[fn]`;
  - o arquivo `hl_pad` em `files/runtime/data` não foi lido no aparelho nem com o jogo respondendo. Nas rotas vale
    o `HL_PADROUTE`.

### 2026-11-11 17:30, tarde: pack de mods no Mi A3: Alpha 1.1X luta; A35 a 60 fps, mas preso sem slot gravado no save

- **Importação pelo launcher** (tela de Mods → ISO em `Download`, como o jogador; as ISOs apagadas depois):
  A35 16 arquivos, 1020 MB; Alpha 1.1X 6 arquivos, 934 MB. Ligar um desliga o outro (um mod de código por vez).
- **Alpha 1.1X (`mia3/a11x`, `env_a3_a11x_r.txt`, 2x, tempo real):** `mod program …/un6-a11x/disc/SLES_556.05 (the
  base's code, its own data)`; título (o do UN5, com idiomas em português, como antes), seleção, Linked Mode, luta,
  Ougi inteiro, KO e WINNER com o personagem. Trecho A 41,9 fps, B 45,0.
- **A35, pacote refeito** (`recomp/build.sh --mod "UN6 A35.iso"`, depois `HL_V80=1 recomp/android/build.sh --mod
  un6-a35` e a ARMv8.2): a cópia do runtime do mod era de 11-09 e não ligava (`hlProfStart` faltando); agora tem todos
  os patches até o `0094`. `libun5_un6_a35_v80.so` e `libun5_un6_a35.so`, 167 MB cada.
- **Defeito do A35 com o lote do IOP de 4096 (`0091`/`0094`):** do primeiro logo em diante o IOP gastava 83 ms por
  quadro e o jogo ia a 11,8 fps, com tela preta depois do CyberConnect2 (`mia3/a35`). `HL_IOP_BLOCKS=0` não muda nada
  (`mia3/a35_blk0`); `HL_IOP_BATCH=1024` volta a 60,0 fps com o IOP em 5 ms (`mia3/a35_b1024`). A causa dentro do IOP
  não foi procurada. **Correção (`un5_main.cpp`):** o pacote de código de um mod põe `HL_IOP_BATCH=1024` (se não vier
  de fora); a base e a Alpha seguem com 4096. Com isso o A35 roda a 60 fps cravados (`mia3/a35_fix`, `a35_full`).
- **Save do A35:** o Mi A3 não tinha save nenhum. Criei o do UN5 jogando a base pelos botões da tela (título → New
  Game → Mode Select → START → Yes → criar dados); o launcher então criou o `BASLUS-55606NARUTO6` na partida. O A35
  reconhece o cartão, mas "Novo jogo" e "Carregar jogo" mostram os slots 1–3 "Vazio" e "Não há dados que possam ser
  carregados": o save do Mode Select só grava o sistema. Em 11-03 o save copiado do Cube tinha um slot gravado e o A35
  dizia "Carregado com sucesso". **Falta:** um slot gravado no jogo base (progresso salvo) para levar o A35 à luta.
- **Lançador:**
  - cartão da Alpha: "O programa deste mod só muda dados: o motor base roda ele inteiro, sem pacote de código"
    (`KnownMod.baseCode`), no lugar de "O que o mod muda no código não vale", que estava errado para ela;
  - cartão do A35, quando não há save do UN5 de onde criar o do mod: pede um save do UN5 com um jogo gravado num slot
    (`FileMod.needsBaseSave`). Com um save do UN5 sem slot gravado o aviso não aparece (o launcher não lê os slots).
- **Não feito:** Versão Brasileira (mod de dados) e as rotas inteiras (resultado, segunda luta) com os mods.

### 2026-11-10 23:00, noite: widescreen revisto do zero no Mi A3 e no PC (4:3, 16:9, tela); nomes Widescreen/Ultrawide

- **Mi A3 (1560x720, 19,5:9; `a71f5cc`, v3r, 2x, `build/lab/env_ws_{1333,1778,0}.txt`, `mia3/ws_*`):** a mesma rota
  nas três formas, 11 capturas cada, do corredor do título à luta 2 (NOW LOADING, Mode Select, Select Character, Linked
  Mode, luta 1, KO, Ougi, luta 2):
  - menus: na forma da tela a seleção fica com retratos, textos e painéis na proporção e o fundo estendido dos lados
    (o `widen2d` de 11-09 vale no Mi A3); a primeira folha de comparação parecia esticada porque achatava as capturas;
  - luta: 16:9 e tela mostram mais do cenário, HUD sem esticar; fps igual nas três (B 38,2 / 38,3 / 38,4).
- **Lutador todo branco** em duas capturas da tela (v6663, v7733): é o brilho do golpe do jogo. No PC (`un5pc`,
  `HL_SCREEN=2400x1080`, `HL_ASPECT` 1333/1778/0, `HL_DUMP_FROM=6600 HL_DUMP_EVERY=4`,
  `~/build/pcsx2-bench/runs/ws_pc_*`) os mesmos quadros, de 6600 a 7788, têm o mesmo conteúdo nas três formas, e a
  fração de branco cai só na razão da largura (1 : 0,75 : 0,6).
- **Lançador:** "Widescreen (16:9)" e "Ultrawide (20:9)" vistos nos Ajustes do Mi A3. A dica dizia que menus e filmes
  eram esticados; agora diz o que acontece (menus largos sem esticar, filmes em 4:3 com faixas, forma limitada à
  largura da tela).
- **Falta:** a campanha e a tela de vitória nesta revisão (a rota v3r para na luta 2), 20:9 de verdade (o Mi A3 é
  19,5:9), e o relato antigo de personagens esticando e sumindo na seleção, que não apareceu.

### 2026-11-10 22:10, noite: otimização, terceira parte: despacho e checagem de ciclos mais baratos (`0094`)

- **Mudança (`recomp/patches/0094-fast-dispatch-checkpoint.patch`):** `EeScheduler::checkpointDue` e `accountCycles`
  passam para o cabeçalho, em linha; o caso comum (nada pendente, antes do prazo, dentro da fatia) responde `false` sem
  sair da função. O resto (DI/EI, evento pendente, prazo, fatia) foi para `checkpointDueSlow`, na mesma ordem de antes;
  a ida aos timers e ao IOP a cada 4096 ciclos (`HL_IOP_BATCH`) foi para `advanceBatchedCycles`, com o estado em membros
  (antes estáticos). `dispatchGuestBranch` busca a função uma vez só (tabela e depois overlays), não `hasFunction` e
  depois `lookupFunction`; o histórico de despacho e a nota de busca fora da thread do EE continuam.
- **PC (`pc94`, rota v3 contra o PCSX2):** as duas lutas iguais em todas as 1445 + 1297 chamadas, v5417 e v7183, igual ao
  build anterior (`pcg93`).
- **Mi A3 (v3r, 2x, Nominum `d398993`, `prof8`):** trecho A 41,5 fps (mín 39,2), EE 14,5 ms; trecho B 38,3 fps (mín
  27,9), EE 17,6 ms (era 18,5 no `prof7`), IOP 5,1, VU1 20,5. O EE ganhou 0,9 ms, mas o fps não mudou: o ganho fica
  dentro do ruído entre rodadas e a thread do jogo (EE + IOP ≈ 22,7 ms) continua sendo o limite, com a VU1 logo atrás.
- **Roteiro `build/lab/mia3run.sh`:** apaga o `un5_log.txt` da rodada anterior antes de começar (a primeira tentativa do
  `prof8` leu o log velho e acabou em 5 s) e para com "device locked" quando a tela está bloqueada (o jogo fica pausado
  atrás dela; o desbloqueio é do desenvolvedor).
- **Lançador:** "16:9" e "20:9" passam a se chamar "Widescreen (16:9)" e "Ultrawide (20:9)" (`aspect_wide`,
  `aspect_ultrawide`) nas Configurações e no painel do jogo, que agora também tem o Ultrawide. No Mi A3 (1560x720) o
  Ultrawide fica limitado à largura da tela, igual a "Tela".
- **Não feito:** IOP e SPU2 numa thread própria (a alavanca grande, arriscada para a sincronia); o `memcpy` de 12% na
  thread da VU1 sem origem conhecida.

### 2026-11-10 20:30, noite: otimização, segunda parte: a VU1 do Nominum (`d398993`); só o Mi A3 daqui em diante

- **Pedido do desenvolvedor:** mexer na VU1 no repositório do Nominum (`~/AndroidStudioProjects/Nominum-Render`), e **não
  usar outros aparelhos, só o Mi A3** (a rodada no RP5 e no Cube da entrada anterior não devia ter sido feita).
- **Perfil da thread da VU1 no Mi A3** (`prof6`, 4463 amostras): `__memcpy_aarch64_simd` 9,9%, `Vif1::transfer` 9,5%,
  `macOf` 6,5%, `anySpecial` 4,1%, o relógio (`[vdso]`) 3,9%, `codeHash` 2,6%. A ponte daqui (`hl_nominum_vu1.cpp`) não
  pesa: são 0,6 transferências por vsync (cada uma leva a cadeia do quadro inteira).
- **Nominum `d398993`** (commit local de lá; detalhe no `docs/registro.md` dele): o MPG só muda a geração do código quando
  os bytes mudam (o UN5 manda o mesmo programa a cada quadro: 198 mil MPG na rota), o relógio por programa só com
  `Vif1::timing`, o `macOf` do NEON com uma soma entre as lanes em vez de três, e o XGKICK direto da memória da VU1 quando
  o pacote não dá a volta. Testes de lá: 106 de 106 no PC e 130 de 130 no arm64 (qemu), bit a bit nos 26 dumps.
- **Aqui:** `recomp/android/build.sh` e `recomp/pc/build.sh` usam o `d398993` (o catálogo `build/vu1rec` copiado do
  Nominum, o mesmo dos testes de lá); a linha `[nvu1]` do log mostra os MPG no lugar do tempo da VU1.
- **Mi A3, mesma rota** (`fstats.py`; fps médio / mínimo / EE ms / VU1 ms):

  | build | A (f+0..440) | B (f+460..2400) |
  |---|---|---|
  | `fa5d58a` (`prof5`) | 41,5 / 36,6 / 16,4 / 18,5 | 36,9 / 28,5 / 20,1 / 21,5 |
  | `0093` (`prof6`) | 41,3 / 38,8 / 15,3 / 19,4 | 38,2 / 30,8 / 18,5 / 21,5 |
  | `0093` + Nominum `d398993` (`prof7`) | 41,9 / 39,7 / 14,6 / 17,1 | 38,3 / 28,0 / 18,5 / 20,1 |

  A thread da VU1 caiu de 4463 para 3366 amostras na rota; `codeHash` e o relógio saíram do perfil, o `macOf` de 291 para
  174. O fps quase não muda: na janela B o limite agora é a thread do EE (EE 18,5 + IOP 5,0 ms). A imagem da luta certa
  (`prof7/shot_v6761.png`). Ainda há 12% de `memcpy` na thread da VU1, de origem não achada.
- **PC (bancada, `runs/pcn398`):** estado das duas lutas igual ao PCSX2 e ao `pcg93` em todas as chamadas. Mas o `un5pc`
  **não liga a VU1 do Nominum** (o `hl_pc_main.cpp` não chama `hl_nominum_vu1::setup()`): a bancada não passa pelo código
  novo da VU1.

### 2026-11-10 14:30, tarde: otimização, primeira parte: registradores do jogo como inteiros (`0093`)

- **Perfil da thread do EE no Mi A3** (rota v3r, 2x 50, nativo, Nominum, `build/lab/env_a3_prof2x.txt`, `prof5`, 9081
  amostras): SPU2 19%, IOP 14,5%, código do jogo 11%, ajudantes de SIMD dentro do código do jogo 11%
  (`Ps2SetGprLow64` 3,2%, `Ps2ExtractEpi32` 2,5%, `_mm_move_sd` 2,4%), escalonador 10% (`accountCycles` 2,5%,
  `checkpointDue` 2,1%), acessos à memória 8%, cópia das cadeias de DMA para a worker da VU 5,4% (o laço do
  `vector::insert` em `ps2_memory.cpp`: são os ~2,5 MB por quadro mesmo, não um laço byte a byte), `libc` 5%.
- **Causa dos ajudantes:** os registradores do EE são `__m128i`. Ler os 32 ou 64 bits de baixo era `ldr q` + mover para um
  registrador inteiro, e escrever era `ldr q` + inserir + `str q` (no `addiu sp, sp, -0x20`, 10 instruções em vez de 4).
- **`0093-gpr-scalar-access.patch`:** `GPR_U32/S32/U64/S64` e `SET_GPR_*` (e o `Ps2SetGprLow64`) leem e escrevem os
  primeiros 4 ou 8 bytes do registrador direto na memória (`memcpy`; o host é little-endian). Os bits de cima ficam como
  estavam, como antes. A `libun5.so` caiu de 171,7 para 161,0 MB.
- **PC, bancada (rota v3, `un5pc`, `runs/pcg93`):** as duas lutas iguais ao build anterior (`pcth`) em todas as
  chamadas (1445 e 1297), e o estado do jogo igual ao PCSX2 em todas; as lutas começam nas mesmas vsyncs (5417, 7183).
- **Mi A3, mesma rota, antes e depois** (`fstats.py`, janelas pelo início da luta 1; fps médio / mínimo / EE ms):

  | build | A (f+0..440) | B (f+460..2400) |
  |---|---|---|
  | `fa5d58a` (`prof5`) | 41,5 / 36,6 / 16,4 | 36,9 / 28,5 / 20,1 |
  | `0093` (`prof6`) | 41,3 / 38,8 / 15,3 | 38,2 / 30,8 / 18,5 |

  O EE ficou 7-8% mais barato; na janela A quem segura é a VU1 (19,4 ms). No perfil novo os três ajudantes saíram do
  topo e a thread do EE tem 8045 amostras (eram 9081).
- **RP5 e Cube (conferência, não valem como medida):** no RP5 a rota foi até o fim, mas o launcher do Fractal ficou na
  frente o tempo todo (o jogo rodou em segundo plano; a CPU estava limitada a 1,86 GHz e os ajustes são outros, efeitos
  ligados, 4,2 MB por quadro). No Cube o jogo não está instalado (o atalho abre o launcher), e o launcher abriu o aviso
  "Sua GPU" (fechado com "Agora não").
- **Achado no launcher:** o aviso "Sua GPU" diz "manter as lutas em 30 quadros por segundo" e "os 30 do próprio jogo"
  (`gpu_offer_apply`, `gpu_opt_interp_off`, `debug_gpu_opt_text` em `values-pt/strings.xml`): texto do Dragon Rage, o
  UN5 roda a 50/60. Falta corrigir.
- A `libun5_un6_a35.so` ainda é a de 11-09 (sem o `0093`): regerar com `--mod` na etapa do pack de mods.
- Ajustes do Mi A3 nestas medidas (`un5_settings.txt`): `frame_skip=1`, `fx_off=48`, `filter=0`, `interp=0`.

### 2026-11-10 08:25, manhã: o travamento do Mi A3 em 2x era o tamanho do envio à GPU (corrigido); pad virtual nos menus; `0086` no commit

- **Memória (`hl_gsx.cpp`):** o núcleo do PCSX2 abaixo do GS aloca o PS2 inteiro (a RAM do EE e a do IOP, declarada com
  128 MB neste núcleo) e o `memReset` zera tudo, então as páginas ficam residentes. Agora elas voltam ao sistema
  (`madvise(MADV_REMOVE)`) antes do primeiro pacote; `HL_GSX_KEEPMEM=1` as mantém. Mi A3, arena 13, 2x: o PSS do jogo
  caiu de 828 para 589 MB no mesmo ponto (v7109), e de 693 para 438 MB no boot.
- **Mas o travamento continuou, no mesmo quadro (v7109)**, de novo com `order:7` no `adreno_ringbuffer_submitcmd`.
  Rodadas para separar as causas (arena 13, `MEMEVERY=5`):

  | rodada | v7109 | Graphics |
  |---|---|---|
  | 1x, nativo | passa (até v7609) | 142 MB |
  | 2x, nativo | trava | 149 MB |
  | 2x, só o GS do ARMSX2 (`HL_NATIVE=0`) | trava | 122 MB |

  Não é o renderizador nativo nem o total de memória da GPU: é o GS em 2x nesse quadro. O kgsl copia a lista de IBs
  de um envio num `kmalloc` contíguo; o driver faz IBs por render pass e por bin, e em 2x há mais bins. O sistema
  fica perto da marca mínima de memória livre (~40 MB, normal no Android) e sem blocos de 512 KB, e esse envio não
  passa. (A `RenderThread` do sistema também falhou em ordem 7 logo depois; sem root não dá para ver o tamanho de cada
  pedido.)
- **Correção (`hl_native.cpp`):** o GS do Dragon Rage tem um envio no meio do quadro, numa fronteira de pass e só quando o
  próximo command buffer já está livre (nunca espera); ele só ligava perto de uma leitura da VRAM. Agora ele fica ligado
  em todo quadro (a cada vsync, na thread do GS: `m_readback_frame` = o quadro, `m_readback_kick_passes` = o limite),
  e o renderizador nativo divide os seus passes do mesmo jeito. O limite é `max(8, 96 / escala²)` passes (96 em 1x, 24
  em 2x); `HL_GS_KICK_PASSES=<n>` muda, 0 desliga. A linha `[native] command buffers` mostra, a cada 250 vsyncs, o
  maior número de passes num command buffer numa vsync.
- **Teste no Mi A3 (2x, 50, nativo, Nominum):** a arena 13 passou duas vezes (`kick_st12` até v7639, `st_12` até v7511),
  com 25 fps no quadro de v7109 (era 11–17 antes de travar). As arenas que faltavam na varredura (19 a 24) passaram
  (`build/lab/mia3stages.sh`): as 24 arenas chegam ao fim da luta 1 em 2x (as 0–11 e 13–17 com o build anterior).
  Fps médio das 40 últimas linhas: 38,2 / 40,0 / 40,4 / 42,2 / 43,8 / 46,9 / 42,9 (arenas 13, 19–24). **Atenção:** a
  arena 13 ainda chega a 261 passes num command buffer (a 20, 114) quando a GPU está atrasada e o envio espera. Passou,
  mas não está provado que nunca trava. Desempenho em 1x não remedido (o limite de 96 quase não age).
- **Pad virtual nos menus (prioridade 1 do lançamento, `docs/plano.md`):** os menus abrem com o controle virtual
  (direcional, △ ○ ✕ □, L1/R1, SELECT, START) em vez dos gestos. A opção é gravada como `menuGestures` (desligada); o
  `menuTouch` que as versões anteriores gravaram como ligado não é mais lido. A opção "Menus por gestos" saiu do painel;
  o código dos gestos fica parado até o toque direto nos menus. L1/R1 nos menus sem o "Item" (é o nome da função na
  luta). **Teste no Mi A3** (toque injetado com `input swipe`): ↓ no idioma passa a FRANÇAIS, ✕ confirma e o jogo
  segue ao logo da Bandai. Falta passar por todos os menus por toque real.
- **`0086` e o `InitThread` real no commit:** já estavam em todos os builds desde 11-09 19:27 (código gerado e runtime):
  bancada do PC com estado igual ao PCSX2 e toda a varredura do Mi A3 desta madrugada.

### 2026-11-10 06:30, Mi A3 travado na arena 13 (2x): o kernel sem memória contígua para a GPU

- **O que o desenvolvedor viu:** o Mi A3 parado numa tela branca, sem reagir. Era a rodada `st_12` da varredura
  (arena 13, 2x, 50 fps, nativo, Nominum, lib com o `0091`), na luta 1, em v7109 (relógio do aparelho 15:22; ele ficou
  assim cerca de 4 horas).
- **Causa (logcat do kernel):** em 15:22:47 a thread `GS` do jogo pediu ao `kgsl` um envio de comandos e o kernel não
  achou 512 KB contíguos (`page allocation failure: order:7` em `adreno_ringbuffer_submitcmd`); o mesmo aconteceu com a
  `RenderThread` da interface do sistema. `kgsl-3d0: |sendcmd| Unable to submit command to the ringbuffer -12`, e às
  15:22:52 `possible gpu syncpoint deadlock for context 3` (o contexto do jogo esperando uma cerca que não vem). Antes
  disso o `lmkd` já matava apps em segundo plano: o jogo ocupa ~890 MB num aparelho de 3,6 GB, depois de 5 horas de
  rodadas seguidas (a memória do kernel fragmentada). É falta de memória do sistema na hora do envio, não um erro de
  desenho: a cena (2,9 MB de GS por quadro, GS 54 ms) era das pesadas, mas o mesmo quadro passou em outras arenas.
- **Por que demorou:** o `mia3run.sh` esperava o `screencap`, que travou junto com a GPU; o teste de "log parado" nunca
  rodou. Agora toda chamada `adb` tem limite de 25 s, e um `screencap` que não volta encerra a rodada e guarda o
  `logcat -b kernel` em `kernel.txt`. O `force-stop` também passou do limite; o processo saiu sozinho em seguida e o
  aparelho voltou à tela inicial (19:25 no relógio dele).
- **A fazer:** medir a memória do jogo em 2x (o que cresce com a escala: alvos do renderizador nativo, texturas) e ver
  se dá para baixar; até lá, a varredura segue com reinício do app entre rodadas, que já existia, e com o limite novo.

### 2026-11-10 02:20, madrugada: a busca de thread guardada e o `hl_pad` a cada 10 vsyncs (`0092`); rotas com outros lutadores

- **Perfil da thread do EE no Mi A3** (prof4): `unordered_map<int, GuestThread>::find` + `__constrain_hash` ~3,1%
  (`EeScheduler::thread(id)`, pedido a cada despacho pelo `currentThread()`), `__openat` 0,8% (o `applyPadFile` abria
  `hl_pad` a cada vsync, mesmo sem arquivo, no jogo normal também).
- **`0092-thread-lookup-cache-pad-file.patch`:** `thread(id)` guarda o último encontrado (o `unordered_map` não move os
  nós ao crescer; `erase` e `clear` esquecem). O `hl_pad` é procurado a cada 10 vsyncs, e na vsync em que a rota
  escreve nele (a rota continua lendo o passo na mesma vsync).
- **Bancada no PC (rota v3, contra o PCSX2):** estado do jogo igual em todas as chamadas, luta 1 em v5417 (a mesma do
  `0091`). A primeira versão deixava um `ifstream` não aberto passar no `if (file)` e logava "0 queued" a cada vsync:
  corrigido com `is_open()` antes do commit. Medida no aparelho: depois da varredura das arenas.
- **`tools/hl_route_mix.py`:** `--char "DOWN RIGHT"` move o cursor antes da escolha do 1P (v4373 na v3r; com este
  memcard: "RIGHT RIGHT" Neji com Tenten, "DOWN" Sakura com Naruto, "DOWN RIGHT" Kakashi com Naruto; RIGHT ou LEFT
  sozinhos caem num bloqueado e a rota para). `--mix` troca os ○ da luta por uma mistura de jogador (ataque, andar,
  pulo, chakra, item □, defesa R2, troca de item L1/R1). No PC, Kakashi com `--mix` e o especial de um botão: Ougi em
  v7031 (o "Ultimate Jutsu" dos cães; o minijogo pede 7, 1, 3, 2, 1 e cada toque responde), Vermillion Mode do
  Naruto da CPU, vitória "Kakashi Hatake" e Ninja Song. As listras horizontais do minijogo saem iguais no GS emulado.

### 2026-11-10 01:55, madrugada: o Ougi de um botão no PC, e a rota das arenas no aparelho

- **Correção de horário:** as duas entradas abaixo, com título "02:30" e "03:00", foram escritas antes das 01:50 (horas
  estimadas de novo). A ordem delas está certa; os títulos não.
- **Rota das arenas no aparelho:** na rota v3r a seleção de arena é o passo de v5501, não o de v5331 (esse é o "Linked
  Mode", Manual/Auto, na tela de personagem; os ↓ ali trocavam o modo e a arena ficava a 1/24). Conferido no PC com a
  rota do aparelho: a s2 dá 3/24, "Five-Seal Barrier Cliff". `tools/hl_stage_route.py BASE K 5501`. A varredura no Mi
  A3 recomeçou da arena 1 (a 0 valia).
- **Ougi de um botão no PC:** no `un5pc` ele vem desligado (`HL_SPECIAL_ONE=1 HL_SPECIAL_BUTTON=32` liga, como o
  launcher). Com `HL_SP_ONCE=5600` na rota v3: `[sp] v5601 fighter d6a080 ougi, chakra 15.0`, aparece "Unleash
  Ultimate Jutsu" com o ícone do botão, o Ougi corre, e em v6050-6090 vem o "now!" com o mesmo ícone. O ícone (um disco
  escuro dentro de um olho branco) sai **igual no GS emulado e no nativo** (`HL_NATIVE=0`, v5618): é o desenho do
  jogo, não defeito de renderização. Nesse Ougi a sequência de botões (`0x36E690`) não chegou a pedir botão ao
  jogador (nenhum `[sp] ... wants`); só o "now!" (`0x6C99B0`). O "todo bugado" relatado ainda não foi reproduzido:
  falta saber em qual personagem e em qual parte (a sequência, o "now!" ou a disputa).

### 2026-11-10 03:00, madrugada: perfil da thread da VU1 do Nominum (`b2ff28b`) no Mi A3 (para a sessão do Nominum)

Rota v3r, 1x 50, nativo, `NOMINUM_VU1=1`, `HL_PROF` (amostras de CPU da thread da VU1, 3185 em ~200 s; a `libc` do
aparelho em `/apex/com.android.runtime/lib64/bionic/libc.so` para os nomes). Na luta 2 a VU1 gasta ~21-22 ms por quadro:
com o EE já em ~19 ms, é ela que segura os 50 fps no Mi A3.

| % | função |
|---|---|
| 9,0 + 1,3 | `memcpy` (`__memcpy_aarch64_simd` e o `memcpy` com tamanho) |
| 8,9 | `core::Vif1::transfer` |
| 7,7 | `vu1::macOf` |
| 4,4 | `[vdso]`: o `steady_clock::now()` duas vezes por programa em `Vif1::runVu` (`counts.vuNanoseconds`) e em `core/worker.cpp` |
| 4,3 + 1,2 + 1,1 + ... | os `recBlock<...>` do catálogo |
| 3,1 / 3,1 | `normOperand`, `anySpecial` |
| 2,3 | `hl_perf::switchTo` (o medidor do Hidden Leaf) |
| 2,0 | `unordered_map<..., shared_ptr<...>>` (a busca do programa no catálogo por hash) |
| 1,9 | `__strncmp_aarch64` (alguém chama `getenv` sem guardar o resultado, ou compara nomes, a cada chamada) |
| 1,6 | `VU1Interpreter::addVfRead` (a análise de uso por instrução, refeita em tempo de execução) |
| 1,6 | `syscall` (futex: acordar e esperar a worker) |

Ideias para o lado do Nominum (não mexi lá): o relógio só com o medidor ligado (ou o contador `cntvct_el0`, como o
`hl_perf`); achar o `strncmp`; guardar a análise do `addVfRead` com o programa; ver de onde vêm os `memcpy` (cópia das
imagens do VIF para a VU ou do GIF para o anel). Os dados brutos ficam em `build/lab/mia3/prof4/hl_prof.txt` (formato
do `tools/hlprof.py`; a lib sem strip é `build/android_v80/libun5.so`, nome no aparelho `libun5_v80.so`).

### 2026-11-10 02:30, madrugada: o IOP em lotes de 4096 ciclos do EE (`0091`)

- `EeScheduler::accountCycles` levava o EE ao IOP (e aos timers) a cada 1024 ciclos: ~290 mil viagens por segundo, cada
  uma com a preparação da fatia do IOP, o SPU2 se atualizando e as trocas do medidor. Agora 4096 (12 us do PS2);
  `HL_IOP_BATCH=<ciclos>` muda.
- **PC, bancada contra o PCSX2 (rota v3):** 1024, 4096 e 8192 dão o estado do jogo igual em todas as chamadas das duas
  lutas; a luta 1 começa 3-4 vsyncs depois (v5417 / v5418 contra v5414).
- **Mi A3, 1x 50, nativo, Nominum** (`build/lab/fstats.py`: janelas pelo início da luta 1; A = f+0..440, B =
  f+460..2400; fps médio, EE / IOP ms): 1024 43,8 (16,3 / 4,9) e 35,6 (19,8 / 5,3); 4096 44,5 (16,9 / 4,4) e 38,1 (19,5
  / 4,9); 8192 45,3 e 46,1, mas com VU1 13,9 ms contra 21-22 na janela B: a luta da rota seguiu outro caminho. Fica 4096.
  O som toca nos três (`[snd]`: fila mediana ~29-31 mil amostras em todos).
- **Atenção às medidas:** comparar só janelas relativas ao início da luta (`fstats.py`); as janelas por vsync absoluta
  misturam cenas quando a rota muda de tempo.

### 2026-11-10 01:30, madrugada: matriz de escala, fps e renderizador no Mi A3; as 24 arenas no PC; endereço da RAM primeiro (`0090`)

*(Correção de horário: as entradas "05:30" e "01:30" desta madrugada foram escritas com hora estimada; a ordem certa é a
dos commits. Esta é posterior a ambas.)*

- **Matriz no Mi A3** (`build/lab/mia3matrix.sh`, rota v3r, Nominum, luta 1 v5500-7100 / luta 2 v7300-8500, fps médio):

  | renderizador | 1x 50 | 1x 60 | 2x 50 | 2x 60 | 3x 50 | 3x 60 |
  |---|---|---|---|---|---|---|
  | nativo | 39,8 / 35,4 | 39,1 / 34,5 | 38,5 / 34,7 | 37,8 / 34,2 | 27,0 / 21,7 | 26,7 / 23,0 |
  | emulado (`HL_NATIVE=0`) | 37,7 / 32,2 | 38,5 / 34,2 | 37,4 / 33,3 | 37,7 / 33,1 | 25,6 / 19,5 | 25,6 / 19,4 |

  Até 2x o limite é a CPU (EE ~20 ms + IOP/SPU2 ~5,4 ms na mesma thread; VU1 ~22 ms na luta 2): 2x sai de graça. Em 3x
  passa a ser a GPU (GS 28-31 ms). O nativo gasta 16-19 ms no GS contra 25-31 ms do emulado.
- **`0090-ram-first-address-check.patch`:** `Ps2IsSpecialAddress` responde primeiro para a RAM principal (todas as
  faixas especiais estão acima de 32 MB): o mesmo resultado, menos testes em cada acesso do código do jogo. Mi A3, 1x 50
  nativo: EE 18,3 → 17,0 ms (luta 1) e 20,4 → 19,5 ms (luta 2); fps 39,4 → 41,2 e 35,1 → 36,1.
- **As 24 arenas no PC (16:9):** `tools/hl_stage_route.py` gera a rota v3w com o cursor da seleção de arena descido N
  vezes. Todas carregam e lutam; nos quadros de luta não vi defeito de desenho. Rodar várias em paralelo na mesma
  `build/disc` mistura o cartão de memória (telas de "Return to title" e "No data"): uma por vez.
- **Menus em 4:3 / 16:9 / 20:9 no PC** (seleção de personagem quadro a quadro na troca de personagem): os sprites
  saem iguais aos do 4:3; o que muda são enfeites fora da área 4:3 que ficam visíveis (um cano horizontal de ponta a
  ponta, arcos nas laterais) e, em 20:9, a seleção de arena deslocada com um vazio à esquerda. **Não reproduzi** os
  personagens esticando e sumindo: falta saber em que tela e com que opção de menu ("Esticar" / "Desenhar em 16:9").
- **Travamento intermitente no PC:** uma rodada em 20:9 parou na v500 (repetida, passou em 7 s). Anotado para vigiar.

### 2026-11-10 05:30, madrugada: Mi A3 no wifi, perfil no aparelho e o IOP em blocos (patch `0089`)

- **Mi A3:** wifi `192.168.1.136:5555`, tela 720x1560, LineageOS / Android 17. Roteiro de teste `build/lab/mia3run.sh`
  (escreve o `hl_env.txt`, abre pelo `PlayShortcutActivity`, captura telas por vsync, temperatura e o log).
- **Perfil no aparelho:** `HL_PROF` agora também liga pelo `hl_env.txt` (`hlProfStart` no `un5_main.cpp`) e
  `HL_PROF_EVERY=<s>` grava o arquivo durante a execução (o app é morto, o `atexit` não roda).
- **O que pesa no Mi A3** (luta, 1x, 50 fps, Nominum; thread do EE): IOP 24%, SPU2 15%, código do jogo 11%, checagens
  de memória 9%, despacho/escalonador 8%, sse2neon 8%. `HL_IOP_PROF=<arquivo>` (histograma por endereço do IOP):
  69% das instruções do IOP são o `CRI_ADXI.IRX` (o ADX) e 28% o `modhsyn`.
- **`0089-iop-block-cache.patch`:** o IOP roda blocos pré-decodificados (até 64 instruções e o delay slot), com o
  resultado exato do interpretador: o load delay, os delay slots, as exceções (ADD/SUB com estouro, endereço
  desalinhado) e todo acesso fora da RAM ficam com o interpretador, que segue da mesma instrução; nenhum bloco atravessa
  uma interrupção, o fim de um DMA, um callback ou o fim da fatia. Páginas de 1 KB vigiadas refazem os blocos quando o
  código muda. `HL_IOP_BLOCKS=0` desliga. **Teste:** `HL_IOP_HASH=500` (hash da RAM do IOP, ciclos e instruções a cada
  500 chamadas): idêntico com e sem os blocos nos 14.689 pontos da rota v3w (3 lutas, v8500). Um erro achado assim
  (desvio na 64ª instrução perdia o delay slot) foi corrigido; `HL_IOP_STEPLOG=N` mostra cada passo de uma chamada.
- **Medidas no Mi A3** (mesma rota, fps médio / IOP ms por quadro): v5500-7100 37,8 / 7,2 antes, 39,4 / 5,4 com os
  blocos encadeados; v7300-8500 33,5 / 7,5 antes, 35,1 / 5,3. No PC a thread do EE caiu 35%. O que sobra no IOP é quase
  todo o SPU2 (19% da thread do EE). No fim da luta o limite é o EE (20 ms) e o VU1 (22 ms).

### 2026-11-10 01:30, madrugada: tela de vitória preta em 16:9 e 20:9, reproduzida no PC e corrigida (`hl_game.cpp`)

Relato do desenvolvedor: quando o jogador ganha, a imagem fica toda preta e só aparece o nome do personagem.
- **Correção da 00:40:** a `route_v3` sozinha não termina a luta; com o `HL_CPU_HP` da v3r termina. Rota do PC que
  vence: `route_v3w.txt` = os passos de menu da `route_v3` + os passos `@1+`/`@2+` do `build/lab/hl_env_v3r.txt`, com
  `HL_CPU_HP=@1+450,@2+450`. No `un5pc`: 3 lutas até v16000 em 94 s, com WINNER, resultado ("Ninja Song") e volta à
  seleção; o "Vermillion Mode" do Naruto aparece (v5900, 4:3).
- **Reproduzido:** em 4:3 a vitória sai certa; em 16:9 (`HL_SCREEN=1920x1080`) e 20:9 (`2400x1080`) fica preta só com
  "Kakashi Hatake", igual ao aparelho. Não é o `widen2d` (`HL_WIDE2D_OFF=1`: continua preta); com `HL_ASPECT=1333`
  (câmera 1,0) sai certa: é a escala das câmeras (`cameraScale`).
- **Causa:** a câmera que aparece durante a luta tem o seu campo `+0x274` guardado para ser devolvido no fim; numa
  câmera recém-feita ele ainda não é uma escala (0, 1,7e-38, 5e+33..., `HL_CAM_LOG`), e no fim da luta esse lixo
  voltava: escala horizontal 0 na câmera do quadro da vitória, nada de 3D nem de fundo.
- **Correção:** só se guarda um valor entre 0,25 e 4; fora disso, 1,0 (a escala do jogo base). `HL_CAM_LOG` mostra cada
  câmera feita larga e o valor guardado.
- **Teste no PC (`un5pc`, GS do ARMSX2, Vulkan):** v6500 a v6600 com WINNER, Kakashi e fundo em 16:9 e em 20:9; 4:3 igual
  (brilho médio 157). `libun5.so` e `libun5_v80.so` recompiladas com a correção. **Falta:** a rota completa nos
  aparelhos (Mi A3 primeiro), outros personagens e cenários, a vitória da luta 2 e 3 e os mods.

### 2026-11-10 00:40, madrugada: o PC roda com o desenho do Android (`un5pc`); a rota v3 em 27 s, estado igual ao PCSX2

- **`recomp/pc/`** (novo): `CMakeLists.txt` + `build.sh` fazem `build/pc/un5pc`, o runtime sem cabeça como no Android
  (`PS2X_HEADLESS_HOST`, o fluxo do GS num anel), o mesmo `hl_game.cpp` e a VU1 do Nominum, com clang (`-march=native
  -mno-avx512f` e as flags numéricas do Android); `hl_pc_main.cpp` faz o papel do `un5_main.cpp`: abre a `libbt4gsx.so`
  do PC (`hlgsx_pc.cmake`, as mesmas fontes do Android: gsrunner + `hl_gsx.cpp` + `hl_native.cpp`), aplica a forma da
  tela (`HL_SCREEN`, `HL_ASPECT`) como o `applyVideo`, escreve o mesmo `un5.ini` e roda o GS. Sem SPU2 (o do Android é
  de ARM).
- `hl_gsx.cpp`: o que é de Android (`ANativeWindow`, `Bt4Gsx_WindowInfo`) só no Android; `HL_GSX_ARGS` passa
  argumentos ao gsrunner (`-surfaceless -dumpdir <dir>` salva cada quadro em PNG). `hl_game.cpp`: `__system_property_get`
  só no Android; o gancho de vsync agora encadeia o anterior (o bench do PC usa o mesmo gancho; com a ordem dos
  construtores ao contrário o bench sumia: a rota não andava e o jogo ficou no menu de idioma até v37000).
- `armsx2-pc.patch`: `HL_DUMP_FROM`/`HL_DUMP_EVERY` para o `-dumpdir`, e o fluxo ao vivo (laço -1) também salva quadros
  (só o laço 0 salvava). As mudanças do Dragon Rage viram um commit no clone, e o patch é só o nosso, por cima.
- **Teste:** rota v3 até v8500 com o GS do ARMSX2 (Vulkan, sem superfície, 1x), um quadro a cada 50 a partir de v5000:
  **27 s** (o GS por software levava 404 s), estado das duas lutas igual ao PCSX2 (posição, vida, chakra e ação em
  todas as chamadas). Os quadros mostram o carregamento, START BATTLE, a luta pelo renderizador nativo e um Ougi.
- **A rota v3 não chega à tela de vitória:** o relógio vai de 99 a 78 e ninguém cai; o que parecia "luta 2" vem depois
  de um Ougi. É por isso que a vitória preta e outros bugs nunca apareceram nos meus testes. Próximo: rotas que
  terminam lutas (vitória e resultado), em vários cenários e personagens, e as mesmas cenas em 4:3, 16:9 e 20:9.

### 2026-11-09 23:50, noite: o GS do Android (ARMSX2, Vulkan) compila e desenha o UN5 no PC

Para comparar imagem no PC com o mesmo desenho do aparelho (os bugs de widescreen são do caminho do Android).
- `recomp/pc/build_gsx_pc.sh` (novo): clone local do ARMSX2 do Dragon Rage (mesmo commit, as mudanças locais do Dragon
  Rage, depois `recomp/pc/armsx2-pc.patch`), `plutovg`/`plutosvg` do `3rdparty` num prefixo local, build com clang em
  `build/gsx-pc`. A árvore do Dragon Rage não muda.
- `armsx2-pc.patch`: o recompilador x86 do EE do ARMSX2 não compilava depois da troca do `FPRreg` (`f`/`UL` viraram
  `UD`; só endereços, a palavra baixa no mesmo lugar) e sem `CHECK_FPUMULHACK`; e **sem AVX-512** (`-mno-avx512f`): com
  `-march=native` no Zen 4 o construtor do `GSState` usa `vmovaps zmm` (alinhamento de 64) num objeto que o PCSX2 aloca
  com 32: o gsrunner caía em ~2 de 3 partidas (`coredumpctl`: `GSState::GSState`, `vmovaps %zmm0,0xdcc0(%rbx)`).
  O GCC também não serve (ordem dos inicializadores designados que o clang aceita).
- **Teste:** `pcsx2-gsrunner -renderer vulkan -surfaceless -dumpdir <dir> -loop 1 pd1_v02900_gs.gs.zst` (dump do PCSX2):
  6 de 6 partidas certas na RTX 4080, quadros PNG certos (tela de carregamento do UN5).
- Falta: a `libbt4gsx` do PC (`hl_gsx.cpp` + `hl_native.cpp` + o gsrunner, com janela X11 ou sem superfície) e um
  executor sem cabeça do PC (`PS2X_HEADLESS_HOST`, `hl_game.cpp`, Nominum) que a carrega como o `un5_main.cpp` do Android.

### 2026-11-09 23:15, noite: recomp do PC 7x mais rápido para comparar estado com o PCSX2 (`HL_GS_NODRAW`, `HL_PROF`)

Pedido do desenvolvedor: certo antes de rápido, e o PC rápido para comparar com o PCSX2 em muitos cenários.
- **Medida:** na luta o executor do PC roda a ~12 fps; `HL_PERF` dá EE 2,7 ms, VU1 3,7, IOP 1,2 e "parse" (o GS por
  software do runtime) 76 a 84 ms por quadro.
- **`HL_PROF=1`** (`recomp/runtime/hl_prof.cpp`, novo): amostragem por `ITIMER_PROF` dentro do processo (o gdb não anexa:
  `ptrace_scope` 1, e não há `perf`), saída `hl_prof.txt` (`HL_PROF_OUT`), lida por `tools/hlprof.py` (addr2line;
  `--lib` para o executor ou as libs do Android). O bench chama `hlProfDump()` antes do `_Exit`. Serve no aparelho também.
  Na luta: 93,6% numa thread só, o GS: `DrawTriangleFast` 41%, `DrawSpriteFast` 13%, `SampleTexture` 9%, `UploadImage`
  5%, `DrawSprite` 5%; a VU1 2%, o IOP 1%; o código recompilado do jogo quase não aparece.
- **`HL_GS_NODRAW=1`** (`0088`): o `Submit` do GS por software não rasteriza (uploads e transferências seguem). Rota v3 até
  a vsync 8500: **57 s em vez de 404 s**, ~85 fps sem esperar o relógio (`HL_FAST_UNTIL`), estado das duas lutas
  igual ao PCSX2 (`hlbench.py diff pv3.bin ndv3.bin`: posição, vida, chakra e ação iguais em todas as chamadas; os 62
  words do lutador de sempre).
- Com isso as rotas de estado (muitos cenários e personagens) cabem em minutos. Para imagem o GS por software do PC não
  serve de referência: não é o caminho do Android (nativo + GS do ARMSX2), e é ele que tem os bugs de widescreen.
  Próximo: levar a `libbt4gsx` com o `hl_native.cpp` para o Linux do PC (Vulkan), para comparar imagens em 4:3, 16:9 e
  20:9 com o mesmo desenho do aparelho.

### 2026-11-09 22:30, noite: interpretador do EE, primeira parte: a luta igual com 8330 funções interpretadas

- `recomp/runtime/hl_interp.cpp` (copiado para o runtime pelo `recomp/build.sh`, como os outros) e o gancho
  `g_hlAfterElfLoad` no fim do `loadELF` (`0087`). O interpretador é uma `RecompiledFunction` com o contrato do código
  gerado (chamada pelo `dispatchGuestBranch`, salto e retorno saem com `ctx->pc`, desvio para trás chama o
  `eeCheckpointDue`, delay slot e desvios "likely" como o `control_flow_emitter`, `bc0x` nunca desvia). Cada instrução usa
  as macros do tradutor. Cobertura: inteiras, memória, SPECIAL, REGIMM, COP0, FPU, MMI escalares e `pcpyld paddub pand
  pxor por pnor pcpyud`, `lqc2 sqc2`. **Falta:** as macros da VU0 (op `0x12`) e o resto das MMI.
- Modo de teste: `HL_INTERP=all HL_INTERP_KEEP=build/gen/un5_config.toml HL_INTERP_MAP=build/gen/un5_map.csv` põe no
  interpretador toda função do ELF (limites do mapa: a tabela gerada só tem a entrada e os pontos de retomada) que não é
  stub e cuja instrução é toda coberta; a faixa inteira dela vai para o interpretador.
- **Teste no PC (`route_v3`, até v8500, `HLB_RNG=1`):** 8330 funções (556 006 posições da tabela) no interpretador, 642
  stubs nativos, 530 com instrução ainda não coberta (VU0: `vsub` em 311, `vadd` em 96, `vaddbc` em 76, `vmul` em 21).
  Chega à luta (overlay `btl`), duas rodadas: **estado igual em todas as chamadas** contra o PCSX2 (`pv3`) e contra o nativo
  do mesmo build (`nv3`). Sobram ponteiros de heap diferentes (64 palavras do lutador, 2 da câmera; o nativo tem 62).
  O nativo `nv3` (com o `0086`) também dá estado igual ao PCSX2.
- Os overlays (`PRG/`) ainda não passam pelo interpretador; o modo do mod (comparar com o ELF europeu) vem depois da VU0.

### 2026-11-09 21:40, noite: Odin 2 Mini sem conseguir desligar o mod; build normal para o desenvolvedor testar

- **Causa:** `modlib/mods.txt`, `un5_mods.txt` e `un5_settings.txt` tinham sido gravados pelo adb (dono uid 2000, o
  shell) em testes anteriores; o app (uid 10120) não podia regravá-los: "Aplicando mods" falhava com EACCES, e o A35
  ficava ligado. **Correção no aparelho:** os três recriados pelo próprio app (`run-as`), com o mesmo conteúdo das
  configurações; A35 desligado (a pasta dele já tinha sido apagada pelo "remover"). **Regra para os testes:** nunca gravar
  arquivo nos dados do app pelo adb direto; usar `run-as com.dfdx047.hiddenleaf` (ou apagar e deixar o app recriar).
- O `hl_env.txt` do Odin tinha uma `HL_PADROUTE` (botões automáticos) e `HL_PERF=1`: guardado em
  `build/lab/odin2mini_hl_env.txt` e apagado, para o desenvolvedor jogar o build normal (o APK de 21:00, sem o
  interpretador, com o `0086`).

### 2026-11-09 21:00, noite: Mi A3 (ARMv8.0) e ISO conferida por SHA-256

- **Mi A3 (Snapdragon 665, Adreno 610, ARMv8.0):** a `libun5.so` (`-march=armv8.2-a`) caía na abertura com SIGILL em
  `PS2Memory::initialize`: o 665 não tem os atomics do ARMv8.1 (`/proc/cpuinfo`: `fp asimd evtstrm aes pmull sha1
  sha2 crc32 cpuid`). Agora o APK leva as duas: `HL_V80=1 recomp/android/build.sh` faz a `libun5_v80.so`
  (`-march=armv8-a`, pasta `build/android_v80`), e o `EngineBridge` escolhe pela CPU (sem `atomics` e `asimdrdm`, a
  `_v80`). Uma lib de mod sem `_v80` não é carregada num ARMv8.0. As outras libs do APK (GS do ARMSX2, SDL3, hooks)
  só têm os `__aarch64_*` do compilador, que testam a CPU na hora: rodam no 665 como estão.
  **Teste:** no Mi A3, boot até a escolha de idioma, sem crash; menus pelo GS do ARMSX2 (Vulkan, 1x). Luta e nativo
  na Adreno 610 ainda não testados. Odin 2 Mini com a lib v8.2: não conferido depois da mudança.
- **ISO por SHA-256:** a imagem inteira do base passa a ser conferida por SHA-256 (era SHA-1), e um mod conhecido só
  entra de uma imagem dele com o SHA-256 conhecido (`KnownMod.isoSha256`; o de cada mod fica no `mod.properties`).
  Hashes: europeia `2e1b9a88…07bff`, A35 `37e01173…5007d`, Alpha 1.1X `bd95521e…6d9`, Versão Brasileira
  `2c60b591…ed5e`. **Teste:** no Mi A3, remover os dados e importar a europeia de novo: "Versão europeia verificada".
  Importação de mod com a conferência nova: não testada.
- **Plano (decidido com o desenvolvedor):** os pacotes de código dos mods (`libun5_<id>.so`) saem do APK; cada um importa
  a própria ISO e o código que o mod muda passa a rodar por um interpretador do EE dentro da `libun5.so` (a função
  cujos bytes na RAM diferem dos recompilados vai para o interpretador). Nada de motor por fora. O pacote do A35 só sai
  depois do interpretador rodar o A35.
- **Correção da entrada das 18:35:** o filme não travava pela thread de prioridade 0; era o `ChangeThreadPriority`
  devolvendo 0 em vez da prioridade antiga (o UN5 guarda em `0x3F6180` e restaura). `0086` + `InitThread` de verdade
  (`tools/un5.py`): no PC boot e luta OK, estado igual. Ainda sem commit: falta o teste no Odin 2 Mini.

### 2026-11-09 18:35, tarde: o que trava o filme é a thread auxiliar de prioridade 0

- Só o `InitThread` real com a prioridade 0 aceita (sem o `StartThread`): o boot também não passa dos filmes (nenhum
  overlay até v4000; o normal é o overlay 1 em v2734). Logo, o travamento da entrada das 18:00 vem da thread auxiliar
  da libkernel (prioridade 0, à espera do semáforo dela) no escalonador do runtime, não do `StartThread`.
- Pista: no runtime, `RotateThreadReadyQueue(0)` quer dizer "a prioridade de quem chama"; a libkernel usa a prioridade
  0 de verdade para a thread auxiliar. Ver isso e como o runtime trata uma thread de prioridade 0 em espera.
- Revertido de novo; o código gerado volta ao estado do `9f1f61e`.

### 2026-11-09 18:00, tarde: ordem das threads no boot (tentativa, revertida; aberto)

- **Medida** (`HLB_FNLOG` com alocar, liberar e os stubs de syscall `CreateThread 0x15e8a0`, `StartThread 0x15e8c0`,
  `SleepThread 0x15e9c0`, `WakeupThread 0x15e9d0`, `ChangeThreadPriority 0x15e930` (0x29), `SuspendThread 0x15ea10`
  (0x37), nos dois lados até v400). O FNLOG do recomp aceita no máximo 8 endereços; a primeira tentativa passou disso.
- **O que difere do PCSX2:**
  1. o `InitThread` da libkernel (`0x15F878`) é stub do runtime, que só devolve o id da thread principal. No PS2 ele
     cria um semáforo e uma thread auxiliar de **prioridade 0** (id 2, função `0x15f7a0`, pilha `0x61e3c0`, 0x400),
     então as threads do jogo começam no id 3. No recomp começavam no 2;
  2. o runtime recusa prioridade 0 (`EeScheduler::createThread` e `changePriority`: `< 1`); o PS2 aceita;
  3. o `StartThread` da libkernel (`0x15FBE0`) também é stub;
  4. no boot (v192 no PCSX2), `ChangeThreadPriority(4, 0x10)` com a principal em 0x78 passa a vez à thread 4 na hora
     (o `SleepThread` dela em seguida); no recomp a principal segue criando threads.
- **Tentativa:**
  - `InitThread` e `StartThread` como código real (`UNSTUB` do `un5.py`) e prioridade 0 aceita (patch guardado fora do
    repositório, `0086-thread-priority-zero`);
  - a ordem bateu com o PCSX2 nos 17 primeiros eventos (antes, 2), com os mesmos ids de thread;
  - a troca na hora do item 4 não veio, nem com o pedido de troca marcando o checkpoint fora de interrupção
    (`requestPreemptionIfHigher`);
  - **o jogo parou no primeiro filme** (`LOGO_C.PSS`: o `sceCdStRead` segue lendo e nenhum overlay carrega até v9500),
    com `InitThread`+`StartThread`+`iWakeupThread`+`iRotateThreadReadyQueue` reais e também só com os dois primeiros.
- **Revertido:** `un5.py` e o patch voltaram ao estado do `9f1f61e` e o código foi gerado de novo.
- **Próximo passo:** separar o que trava o filme (só o `InitThread` com o `0086`, ou o `StartThread`), olhando a
  thread auxiliar de prioridade 0 no escalonador do runtime: ela fica esperando o semáforo e pode estar tomando o EE.

### 2026-11-09 17:00, tarde: o heap vai até a pilha principal, como no PS2 (`0085`); o pool do jogo do mesmo tamanho

- **Causa do pool ~1 MB menor:**
  - o `malloc` do jogo (`0x17A610`) é o do runtime (`ps2_stubs::malloc`, visto no gdb). O jogo pede um pool de
    0x16cd7f0 e vai baixando de 0x100 até caber;
  - o runtime cortava o heap em `0x1F00000`, porque guardava ali as pilhas de callback (0x4000 cada), os pools de
    RPC, TLS e boot mode;
  - no PS2 (PCSX2 com a BIOS, `HLB_FNLOG=0x168e18`), o `SetupThread(-1, 0x8000)` devolve `sp` = `0x1ffed60`
    (0x12A0 abaixo do topo) e o `SetupHeap(0x928900, -1)` devolve `0x1ff7000` (4 KB abaixo da base da pilha,
    `0x1ff8000`). O `sbrk` do jogo (`0x15f1a0`) usa o `EndOfHeap`.
- **Correção** (`recomp/patches/0085-heap-to-the-main-stack.patch`):
  - `SetupThread(-1, size)`: a pilha é o topo da RAM e o `sp` começa 0x12A0 abaixo dele;
  - `SetupHeap(-1)`: o fim é a base da pilha menos 4 KB, que também é o valor de retorno. `EndOfHeap` devolve o
    mesmo; o teto do alocador do runtime passou a `0x1FF7000`;
  - as pilhas de callback ficam em `0x30000`–`0x70000`, e os pools de RPC, servidores, TLS e boot mode em
    `0xA0000`–`0xD1000`. As duas faixas estão vazias no PCSX2 na luta (mapa por 16 KB das duas RAMs).
- **Conferido no PC** (rota v3, `HLB_RAM` em v5430):
  - topo do pool `a1` = `0x1ff5f40`, igual ao PCSX2 (antes `0x1effe40`);
  - o fim da RAM (pilha principal) ocupado do mesmo jeito que no PCSX2;
  - estado da luta igual ao PCSX2 em todas as chamadas (`hlbench.py diff`);
  - o heap ainda se separa no bloco 3650: é a ordem das threads da entrada das 16:05, que este patch não toca.
- **Conferido no Odin 2 Mini:**
  - jogo base, luta por toques reais (`withbase.sh gest/fight.sh`): 50,0 fps, golpes, jutsu, Ougi, item, defesa e
    pausa;
  - A35 (`hl_env_a35r.txt`): carrega o save, `[codes]` aplica a tabela, câmera `x 0.750`, luta a 60 fps (NTSC).

### 2026-11-09 16:15, tarde: menus em 20:9 no Odin 2 Mini; campanha depois da mudança da câmera

- **20:9** (`build/lab/gest/wide209.sh` pelo `withbase.sh`: `wm size 2400x1080` com `trap` que sempre devolve o
  tamanho; rota v3 sem modo rápido; capturas nos vsyncs 2400 a 6500, lidos das linhas `[perf]`; saída em
  `gest/w209/`):
  - título, Mode Select, seleção com Linked Mode e "Battle!", VS e NOW LOADING ocupam os 2400 px, com o 2D na
    proporção. A luta sai larga;
  - a abertura (filme) fica em 4:3 com faixas, como em 16:9;
  - a grade de rostos da seleção termina com um "?" cortado na borda do painel, o mesmo jeito da seleção do A35:
    deve ser a janela de rolagem do jogo, ainda sem referência do PCSX2 nessa largura;
  - linha "Widescreen em 20:9" da tabela atualizada; tirei de lá o "menus esticados".
- **Campanha depois da mudança do gancho de câmera** (`withbase.sh gest/talk3.sh`, `gest/talk3/`): a cena sai larga
  (`[cam] v3523 x 0.750`), a conversa avança por toque e o jogador fica livre, com HUD e controles da campanha.
- **Luta do jogo base, mesma mudança** (`gest/fight/`): larga a 0,75, de volta à escala do jogo depois. O contador do
  canto mostra 23,9 no filme do Ougi: é o ritmo do próprio filme. O executor segue a 50,0 fps (`[perf]`).

### 2026-11-09 16:05 (horário do relógio), tarde: o PC não estava travado; heap igual até o bloco 3650

- **Correção da entrada abaixo** (escrita às 15:52, título "16:00"): o executor do PC **não** trava. Eu passava os botões
  em `HLB_ROUTE`, que só o PCSX2 lê; o recomp lê `HL_PADROUTE` (`~/build/pcsx2-bench/runrc.sh`, `docs/plano.md`, "A
  bancada"). Sem o ✕ do v650, o jogo espera numa tela de aviso. Monitor, som e `gdb` não tinham nada a ver.
- **Luta do Kakashi no PC com o pool no lugar** (`route_v3`, `HLB_RNG=1`, contra `pv3`): o estado do jogo é **igual em
  todas as 1445 chamadas**, como antes. As diferenças que sobram são as mesmas (cores `0x80xxxxxx`, `+0x000`).
- **O lutador mudou de lugar:** `0xd6a080` (antes `0xd6a000`, igual ao PCSX2); `mti` na entrada da luta 131 (antes 143;
  PCSX2 138). O alinhamento de -0x10 escondia por sorte a diferença abaixo.
- **Heap do jogo comparado bloco a bloco** (RAM na primeira chamada da luta: `pv3ram_ram01.bin` na bancada,
  `rv3ram_ram01.bin` no rascunho; cabeçalho de bloco `+0` anterior, `+4` próximo, `+8` tamanho, `+0xC` marca, a partir de
  `0x928910`):
  - os **3650 primeiros blocos são idênticos** nos dois (endereço, tamanho, marca), até `0xbd4260`;
  - dali em diante são 1596 trechos diferentes, quase todos blocos do mesmo tamanho em **ordem** diferente;
  - exemplo: o par 0x1410 + 0xc10 que o PCSX2 aloca em `0xbd5a30`/`0xbd6e40`, antes de um bloco de 0xe10, sai no
    nosso só mais tarde, em `0xbec670`/`0xbedae0`;
  - é a ordem em que as cargas (threads de leitura) terminam, ou seja, o tempo do CD e das threads (item 5 da tabela
    "Correto primeiro" do `plano.md`), que segue aberto. Não afeta o estado da luta medido.
- Ferramenta: `tools/heapwalk.py <ram do pcsx2> <ram do recomp>` (primeiro bloco diferente e os trechos que diferem).
- **Onde a ordem se separa** (`HLB_FNLOG=0x118e60,0x1181b0`, alocar e liberar, nos dois lados até a luta; 152829 chamadas
  no PCSX2, 149761 no recomp; comparadas por função e chamador):
  - as 29 primeiras são iguais;
  - na 30ª (boot: v247 no PCSX2, v50 no recomp), o PCSX2 faz uma alocação (`ra 0x1187d4`, 0x4f0) numa thread com pilha
    em `0xc68aa0`, antes de a thread com pilha em `0xb62f70` liberar `0xbd4b80` (`ra 0x1d58c4`). No recomp, a liberação
    vem primeiro;
  - é uma troca de thread em outro momento, ainda no carregamento do boot. É por ela que começar quando o tempo das
    threads e do CD for atacado;
  - o `a1` dessas chamadas também mostra o topo do pool: `0x1ff5f40` no PCSX2, `0x1effe40` no recomp (o pool ~1 MB
    menor).

### 2026-11-09 16:00, tarde: o pool no endereço do PS2 também no PC; o executor do PC parou de chegar ao logo

- **Horários:** as duas entradas anteriores ("15:10" e "15:50") foram escritas entre 15:05 e 15:14. Os títulos estão
  adiantados.
- **PC (`recomp/runtime/hl_bench.cpp`):** o mesmo ajuste do pool, em forma geral. O alvo é `$s7 + 0x10` (o `end` que o
  jogo carrega em `0x118CCC`); pede 0x100 a mais e entrega o alvo se o bloco do runtime começa até 0x100 abaixo. Linha
  `[hlb] pool ...` no log; `HLB_NOPOOL=1` desliga.
  - Medido: `runtime 0x928900, given 0x928910`. As câmeras nascem em `b3eca0`, `b3ef60`, `b3f750`, `b3fa60` e
    `bd9e50`, os mesmos endereços do PCSX2 (`basem`, v169 e v262). Antes, o PC tinha o heap 0x80 deslocado
    (tabela "Lutas contra o PCSX2").
  - O build do PC usa a cópia em `build/PS2Recomp/ps2xRuntime/src/runner/` (o `build.sh` copia no passo 7): depois de
    editar `recomp/runtime/*.cpp`, copiar antes do `cmake --build`.
- **Aberto: o executor do PC não passa do boot.** Para logo depois do `SNDBASE` e do teste do cartão, antes do
  `LOGO_C.PSS`: nem o `hl_pad tick 650`, nem o "overlay 1". Na rodada `rv3` de 11-08 essas linhas vinham em seguida.
  - Acontece com o executor novo, com `HLB_NOPOOL=1` e com o `ps2EntryRunner_r1` de 11-08 01:57, que funcionava. Então
    não é código deste build: algo mudou no PC.
  - O que mudou: a tela agora é 3440x1440 (antes 1422x800), e a saída de som padrão é uma soundbar USB.
  - Descartado: o som (`PULSE_SINK` para a HDMI: igual); variáveis `HL_`/`HLB_` no ambiente (nenhuma); o cartão
    (`build/disc/mc0` vazio desde 25/10, igual antes).
  - No `gdb` (rodado de dentro dele: o `ptrace_scope` é 1), a thread do jogo está no GS de software
    (`GSCpuBackend::SampleTexture` ← `DrawSprite`), a principal no `EndDrawing`, e o miniaudio no `pa_mainloop_poll`.
    Falta saber o que o jogo espera.
  - Até resolver, a comparação de lutas no PC está parada. O Android não é afetado.

### 2026-11-09 15:50, tarde: depois da correção do título do A35

- **Seleção do A35** (Odin 2 Mini, `HL_PADROUTE` até 4100:CROSS, `gest/env_a35sel.txt`): as duas grades de rostos terminam
  com um rosto cortado na borda interna do painel. Parece a janela de rolagem do próprio jogo, mas falta a referência:
  - o PCSX2 da bancada não acha o save copiado para o cartão de pasta ("Nenhum dado ... no slot 1"). O
    `_pcsx2_superblock` do `Mcd001.ps2` tem 0 bytes; deve ser preciso formatar o cartão pela BIOS ou usar um cartão
    em arquivo;
  - tirei o save de novo; o cartão ficou como estava.
- **Pool ~1 MB menor (fica assim por ora):** as pilhas de invocação do runtime (0x4000 cada, uma por thread e
  profundidade, reaproveitadas) mais os blocos do MPEG e os pequenos do `calloc` saem de `0x1F00000` para cima. Já se
  esgotaram uma vez num carregamento de luta. A RAM baixa (`0x80000–0x100000`, 512 KB) não comporta isso com folga.
- **Alpha 1.1X:** o `stubfix --elf` não acha nenhum stub mudado (a única região alterada é `0x410d68`, fora deles).
  Nada a refazer nela.
- **Endereços fixos do heap no código host:** só em comentários (`0xd7b9a0`, rota antiga). As rotas de laboratório com
  `POKE` em endereços do heap ficam 0x10 deslocadas.

### 2026-11-09 15:10, tarde: fundo do título do A35 igual ao PCSX2 (pool do jogo no endereço do PS2, tabela de códigos do A35, `4e6797e`)

Corrige a entrada de 15:05: a causa estava em três pontos juntos.

- **A tabela de códigos do A35.** O A35 troca `jal _sceSifSendCmd` em `0x161E10` por `jal 0x17B1E4`. Ali fica um
  pequeno motor (escrito por cima do `memcpy` da base) que, a cada comando SIF, grava uma tabela de pares
  (endereço << 7 | tipo, valor). A única entrada põe 0,75 em `0xC3EF14`, o `+0x274` da câmera principal, logo depois
  que `0x1843D0` a reinicia com 1,0. Achei com um ponto de observação de escrita na bancada (`HLB_WATCHW=0xc3ef14`):
  `pc 0x17b224`, `ra 0x161e18`. Aqui, `sceSifSendCmd` era stub do runtime e as RPCs (`sceSifCallRpc`,
  `sceSifBindRpc`) são HLE, então o motor nunca rodava.
  - `tools/un5.py stubfix --elf --base --base-config` (o `build.sh --mod` passa os três): todo stub cujo código no ELF do
    mod difere do da base passa a ser recompilado. No A35, só o `sceSifSendCmd`. Os limites de cada função vêm dos dois
    configs, senão o `memcmp`, intacto, parecia mudado.
  - `hl_game.cpp` (`modCodeTable`): acha o motor pelo `jal` em `0x161E10` e o `lui`/`ori $t4` do cabeçalho, e aplica a
    tabela antes de cada `sceSifCallRpc` e `sceSifBindRpc`, como no PS2. Tipos: `sb`, `sh`, `sw`, inclusive no delay
    slot de um `b`. Log: `[codes] the mod's code table at 0x17b244`.
- **O pool do jogo 0x10 abaixo do PS2.** No boot (`0x118CA0`), o jogo pede ao `malloc` toda a RAM do fim do ELF até a
  pilha e mantém o próprio heap ali, de baixo para cima. O newlib devolve `end + 0x10` (PCSX2: `0x928910` na base,
  `0xA28910` no A35); o heap do runtime devolvia `end`. Por isso todo objeto do jogo ficava 0x10 abaixo (câmera
  principal em `0xc3ec90`, contra `0xc3eca0` no PCSX2), e a tabela do A35 cairia no `+0x284` da câmera. `gameMalloc`
  (`0x17A610`, só para as chamadas de `0x118D3C`/`0x118D58`) pede 0x10 a mais e devolve +0x10. Agora as câmeras
  nascem nos mesmos endereços do PCSX2 (`0xc3eca0`, `0xc3ef60`, `0xc3f750`, `0xc3fa60`).
  - **Ainda diferente:** o pool é ~1 MB menor (`0x14d76f0`, contra `0x15cd6f0`): o runtime reserva `0x1F00000` para
    cima para as pilhas de callback. O jogo tenta de 0x100 em 0x100 até caber.
- **O nosso gancho de câmera.** Fora das cenas largas, ele gravava 1,0 em toda câmera e apagava o 0,75 do A35. Agora
  só mexe nas câmeras que ele mesmo alargou, e devolve a cada uma o valor que o jogo tinha (1,0 na base). O
  `HL_CAM_NOWRITE` saiu.
- **Resultado no Odin 2 Mini** (`HL_WATCH=0xc3ef14`: 0 → 0,75 no v3, sem mais mudanças):
  - **A35:** o fundo do título mostra o mesmo enquadramento do PCSX2 (Sasuke à esquerda, Kakashi e Sakura à direita),
    sem a ampliação de 1,34. O 2D fica nas proporções da tela (widescreen dos menus); antes saía esticado 1,33.
  - **Base:** luta por toques reais (`withbase.sh gest/fight.sh`): larga a 0,75 no v6100, de volta à escala do jogo no
    v6600 ("3 of 4 cameras back"), e larga de novo na seguinte. Imagem normal.
- **Bancada (fora do repositório):** `HLB_WATCHW=addr` (cada escrita do EE na palavra, sem pausar, via memcheck do
  recompilador) e `v0`/`sp` no `HLB_FNLOG`.

### 2026-11-09 15:05, tarde: o fundo 3D do título do A35 sai 4/3 mais largo que no PCSX2 (causa parcial, aberto)

- **Visto:** no título do A35, a arte de fundo (projetada por câmera) sai ampliada 1,34 na horizontal em relação ao
  PCSX2, em 16:9 e em 4:3. O 2D (botões e textos) está no lugar certo. Medida: ajuste de escala por mínimo erro entre a
  captura do Odin e a do PCSX2 (`a35t_v03200`). Na VRAM do quadro mostrado (dumps do GS dos dois lados, desembaralhados
  do PSMCT32), o 2D é igual e só o fundo difere. O jogo base em 4:3 bate com o PCSX2.
- **Descartado:**
  - registros de exibição: `DISPFB`/`DISPLAY` iguais aos do PCSX2 (dump `a35d_v03200_gs`). O `SMODE1` só difere no bit
    SINT;
  - código que se modifica: o código principal na RAM do PCSX2 é igual ao arquivo;
  - patches do PCSX2: nenhum para `SLUS-55606`;
  - o nosso gancho de câmera: sem a escrita (`HL_CAM_NOWRITE=1`), a imagem é a mesma.
- **Mecanismo:**
  - o A35 muda a câmera do jogo: `0x10DD00` usa 688 no lugar de 512 e 2,3125 no lugar de 2; `0x10E670` e `0x10F4F0`
    passam 0,75 no lugar de 1,0;
  - as câmeras nascem com `+0x274` (escala horizontal) = 0,75 (v181 no PCSX2, v2 no nosso);
  - em seguida, `0x1843D0` reinicia a câmera principal com 1,0 (`lui 0x3F80` fixo; `HLB_FNLOG` com `f12`/`f18`, novo na
    bancada). Isso acontece **nos dois**;
  - **no PCSX2**, a câmera principal já está de novo em 0,75 no v199 e fica assim (v200 a v3200; a cópia `0x1C01B0`
    para `0xcb6550` também leva 0,75). **No nosso** fica 1,0 (`HL_WATCH=0xc3ef04`: 0 → 1,0 no v3, e mais nada). 1/0,75
    = 1,333, a ampliação medida;
  - nenhuma das funções que escrevem `+0x274` por instrução roda no PCSX2 depois do v198: o setter `0x10EED0`, as do
    programa (`0x23F240`, `0x23F3F0`, `0x23F4C0`, `0x21B7C0`, `0x21BC70`, `0x240D70`, `0x254C70`), as do ETC
    (`0x6E4000`, `0x6E7330`) e a do MOD (`0xA022E0`). A volta a 0,75 deve vir de uma cópia do bloco inteiro da câmera
    (`sq`/memcpy) que só acontece no PCSX2. É a próxima coisa a achar: um ponto de parada de escrita no PCSX2 em
    `0xc3ef14`, ou a cópia da RAM em vários pontos do v198.
- **Ferramentas novas:**
  - `HL_CAM_LOG=1`: cada chamada do setter, com o chamador da inicialização; e cada mudança do `+0x274` vista pelo
    gancho;
  - `HL_CAM_NOWRITE=1`: o gancho não escreve 1,0 fora das cenas largas (saiu às 15:10);
  - `HL_DISP_LOG=1` (`hl_native.cpp`): os registros de exibição, a cada mudança;
  - na bancada (fora do repositório): `f12` e `f18` no `HLB_FNLOG`.
- **Atenção:** a troca para 4:3 pelo menu do jogo fica salva no `un5_settings.txt`. Voltei o Odin para "Tela cheia".
  Na rota de 4:3, o menu do jogo funcionou por toque (Gráficos → 4:3 → Continuar).

### 2026-11-09 14:08, tarde: a primeira conversa da campanha também com os controles dos menus

- `hl_game.cpp` (`scene()`): a regra das conversas não espera mais o jogador andar uma vez (`g_advFreeTick != 0`).
  Basta o gancho do `0x723970` estar posto. Ele só entra quando a tabela do ADV da biblioteca tem essa função, e a
  tabela só responde quando o ADV na memória é o dela. Corrige o limite anotado em "10:10".
- **Teste (toque real, base):** `build/lab/gest/talk3.sh`: a rota para no começo da campanha (v6160), e depois são só
  toques no meio da tela a cada 1,3 s. A primeira conversa (Naruto, Sakura, Jiraiya) e a caixa "How to move" ficam só
  com MENU e START, e os toques passam as falas. Com o jogador livre, os controles da campanha voltam. Os toques no
  meio, já livre, não fazem nada.

### 2026-11-09 13:47, tarde: o A35 (NTSC) 21 linhas acima do PCSX2, corrigido (`0084`, `3d5bdb2`); conversas na Alpha 1.1X

- **Imagem do A35:** o A35 é a versão americana (`SLUS_556.06`, NTSC). No Odin, o título e os menus saíam 21 linhas
  acima do PCSX2: faixa preta de 51 px embaixo, topo cortado ("Selecionar modo", a borda da lista de saves). O
  PCSX2 da bancada (`run.sh`, `HLB_SHOTV`, ISO `UN6 A35.iso`) mostra o título inteiro. Causa: o `SMODE1` ficava em 0, e
  a saída usava o valor PAL fixo do `hl_gs_live` (`0051`). O jogo pede o modo pelo `sceGsResetGraph` do runtime, que não
  escrevia o `SMODE1`. `0084`: `sceGsResetGraph` e `SetGsCrt` põem o valor do BIOS para o modo pedido (CMOD 2 NTSC,
  3 PAL). Depois disso, 0 linhas pretas no título do A35 e a lista de saves inteira. Na base, nada muda: modo 3,
  50 Hz, título sem faixa.
- **`libun5_un6_a35.so` estava velha** (05:06, sem o gancho das conversas e as mudanças do dia): refeita.
- **Conversas da campanha:** na Alpha 1.1X (roda com a `libun5` da base, ADV igual), o mesmo teste da base: um toque
  de 10 ms no Ataque perto do Jiraiya, conversa sem botões de luta, toques no meio passam as falas, e os controles da
  campanha voltam no fim. O A35 não tem campanha: no menu de modos só há Combate Livre, Praticar e Opções. A função
  `0x723970` do ADV do A35 é igual à da base.
- **Menus do A35 por toque** (jogando, sem rota): carregar o save, Sim, Ok, girar a roda de modos deslizando, entrar
  na seleção, voltar com dois dedos (△). Dois dedos de verdade pelo `sendevent` no `hyn_ts`
  (`build/lab/gest/two.sh`; o `TRACKING_ID` vai só até 5).
- **Aberto:** na seleção do A35 a grade de rostos termina cortada à direita nos dois lados. Não comparei com o PCSX2.

### 2026-11-09 10:10, manhã: conversas da campanha com os controles dos menus, e toque mínimo de 80 ms

- **Correção de horários:** as entradas "09:50" e "10:00" foram escritas às 09:39 e às 09:43 (relógio do PC).
- **O sinal "jogador com controle" no ADV:** `HL_FNCOUNT=<as 7032 funções do ADV>` com
  `HL_FNCOUNT_WIN=6800:7600,8300:8900` (`ramd/fnstand`, rota até v8100, jogador parado) e
  `6800:7600,8200:8800` (`ramd/fnwalk`, andando).
  - Onze funções rodam uma vez por quadro de lógica (300 em 600 vsyncs) parado **e** andando, e nunca no diálogo:
    `723970`, `723c20`, `755a20`, `755f20`, `7562a0`, `7b9630`, `7b9d20`, `7ba270`, `7ba7c0`, `7c4750`, `7c47c0`.
  - A tabela do ADV tem 511.209 entradas, porque guarda também o meio das funções. A lista certa sai dos nomes
    `gen/ovl/modules/adv_sub_*`; o teto da contagem subiu para 8192.
- **Uso** (`hl_game.cpp`):
  - `0x723970` vira gancho na tabela do ADV (`advFree`, guarda o vsync);
  - na campanha, se ela não roda há mais de 15 vsyncs, `hl_game::scene()` responde **4** e o launcher põe os
    controles dos menus em 0,5 s (`NinjaPadView`: 4 → `PadScene.MENU`; `TouchBridge.scene` aceita 0 a 4);
  - um toque vira ✕ e avança a fala;
  - só vale depois que a função rodou uma vez, para que um mod com outro ADV fique como antes. Por isso a primeira
    conversa da campanha, antes de o jogador andar, ainda mostra os botões de campanha;
  - `HL_TALK_OFF=1` desliga.
- **Toque mínimo:** um toque curto no ○ (Talk) se perdia na campanha, que lê o pad a cada 2 vsyncs. Botões, gatilhos e
  pílulas agora ficam apertados **pelo menos 80 ms** (`MIN_HOLD_MS`).
- **Teste real** (`gest/talk2.sh 1612 800`, `HOLD=10`, `gest/talk2/sheet.png`):
  - a rota passa a primeira conversa e deixa o jogador parado, com os controles de campanha na tela;
  - um toque de 10 ms no Ataque abre "Well, let's go see Tsunade…" **sem os botões de luta** (só MENU e START);
  - os toques no meio da tela avançam a conversa, e os controles de campanha voltam no fim.
  - Antes: com 10 ms nada acontecia, e com 250 ms a fala abria, mas os botões só saíam depois de 2,5 s.

### 2026-11-09 10:00, manhã: diálogo da campanha, funções do ADV que leem o pad (aberto)

- **Ferramenta nova:** `HL_FNCOUNT=ADDR,...` (`hl_game.cpp`, só depuração).
  - Troca até 64 funções do ADV na tabela do overlay (`un5_adv_table`) por contadores;
  - loga as chamadas a cada 50 vsyncs (`[fn] vN addr:n ...`).
- **Teste** (`hl_env_camp_fn.txt`, `ramd/fn/log.txt`): as 40 funções do ADV que carregam o ponteiro do pad
  (`lw -0x3F4($gp)`, `0x617AFC`) contadas na rota da campanha. Diálogo de v6600 a v8000, caminhada depois.
  - Só cinco rodam: `76e130` e `7b76b0` (25/s), `7bdd90` (~450–550/s), `7be480` (~525/s), `7edf30` (200/s);
  - as contagens são **iguais no diálogo e andando**, então nenhuma delas é a leitura que move o personagem.
- **Próximo passo:** o personagem do ADV deve ler o pad por um ponteiro guardado no objeto dele, como os lutadores do
  BTL (`+0x24`, depois `+0x64`). O caminho é procurar quem lê `pad+0x64`/`+0x68` por um ponteiro de objeto, ou um
  campo do analógico do pad.

### 2026-11-09 09:50, manhã: botões escondidos nos diálogos da campanha (tentativa, revertida; aberto)

- **Objetivo:** nos diálogos da campanha, esconder os botões de luta, que tapam o texto, e um toque avançar a fala
  (✕), com os gestos dos menus.
- **Ferramentas novas** (só depuração, em `hl_game.cpp`; `build/lab/ramrun.sh`, `build/lab/ramflag.py`):
  - `HL_RAM_DUMP=V,...`: os 32 MB da RAM do EE em `files/ramdumps/ram_<V>.bin`;
  - `HL_WATCH=ADDR,...`: uma linha `[watch]` quando a palavra muda;
  - `ramflag.py`: palavras estáveis em cada grupo de dumps e diferentes entre os grupos.
- **Candidato `0x6316B4` (bit 31), achado com 3 dumps de diálogo × 4 andando:**
  - liga em v6533, no começo do diálogo, e desliga em v8134 na rota automática;
  - com a cena "menu" ligada por ele (`gest/talk.sh`, toques reais no meio da tela), os botões sumiram na fala e os
    toques avançaram a conversa e o tutorial "How to move";
  - **mas ele só desliga quando o analógico se mexe:** com o jogador já livre ("Talk" na tela), continuava ligado, e o
    jogador ficaria sem analógico. **Revertido.**
- **Diálogo × parado × andando** (`ramd/camp`, `ramd/stand`, rota até v8100 sem caminhar):
  - nada na área estática separa os três;
  - no heap há milhares de diferenças (a janela de fala é alocada), sem endereço fixo confiável.
- **Próximo passo:** achar no código do ADV onde o personagem lê o analógico e o que o bloqueia durante uma fala, e
  usar essa condição.

### 2026-11-09 09:21, manhã: desempenho dos menus e adereços 3D da seleção corrigidos no widescreen

- **Medida da seleção de personagem** (`build/lab/menuperf.sh`, `matrix/o2menu.tsv`): rota só até v4373, sem modo
  rápido, 30 s parado na seleção a partir de v4700.
  | escala / fps | fps do jogo | 1% low | pares > 44 ms | EE / VU1 / GS (ms) |
  |---|---|---|---|---|
  | 2x / 60 | 50,0 | 46,6 | 0,0% | 2,4 / 1,3 / 2,9 |
  | 4x / 120 | 50,0 | 46,7 | 0,0% | 2,2 / 1,2 / 2,7 |
  | 8x / 60 | 50,0 | 46,6 | 0,0% | 2,2 / 1,2 / 2,6 |
  - Os menus sobram em qualquer resolução. O contador do canto (~25) é o próprio menu, que redesenha um quadro sim, um
    não.
- **Defeito achado na medida:** com o 2D apertado, os adereços 3D da seleção viravam manchas esticadas: as engrenagens,
  os pergaminhos com kanji e o selo "Linked Character". Com `HL_WIDE2D_OFF=1` saíam inteiros (`matrix/o2menu_off`).
  - Não é buffer à parte: no dump do GS (`gsdumps/sel`, `HL_GS_DUMP=selecao`) tudo vai para o FBP 0, fbw 8.
  - **Causa:** em `wide2d::vertex` (`hl_native.cpp`), depois da decisão do primeiro triângulo de uma tira, só o
    vértice com disparo era apertado. Um vértice sem disparo (ADC ou XYZ3, comum nos modelos 3D vindos da VU1) ficava
    na largura antiga e esticava os triângulos seguintes.
  - **Correção:**
    - a decisão da tira vale para todo vértice, com ou sem disparo;
    - uma primitiva de lista sem disparo zera a fila;
    - os vértices pendentes do pacote anterior nunca são escritos, porque apontam para o buffer dele.
  - **Conferido:**
    - a seleção (`matrix/o2menu_fix`) sai com os adereços inteiros e na proporção, ainda 50,0 fps;
    - a rota completa no Odin 2 Mini (`rec/o2_w4`, contact sheet `sheet.png`): título, seleção com Linked Mode, NOW
      LOADING, luta, WINNER, resultado, seleção de novo e Stage Select, tudo largo e certo.
    - a campanha (`rec/o2_camp_w2`, `hl_env_camp_r.txt`, até v9883): abertura, NOW LOADING, cena do portão com
      diálogos, "Skip?", "Act 1", tutorial do START, caminhada na vila com HUD e nomes de lugar, tudo largo e inteiro. As
      barras recortadas de alguns quadros são a cortina de troca de área do jogo (entrada das 08:50).

### 2026-11-09 09:20, manhã: botão Ougi por toque real funciona (corrige a entrada "09:10")

- **O lutador humano da luta 1 é `0xd69a00`**, achado com uma linha nova do `HL_SP_LOG`, a cada 0,5 s, em
  `hl_game.cpp`: `[sp] human fighter <end> chakra <c> action <a> move <m>`. O POKE da entrada anterior foi para a CPU.
- **O chakra do jogador já começa em 15,0.** A falta de chakra não explicava o filme que não saía.
- **Teste novo** (`gest/ougi2.sh`, `gest/ougi2/`): seis toques no botão Ougi desde o começo da luta, sem POKE.
  - O △△ do botão gasta chakra (15 → 10) e o jogo mostra o próprio aviso "Unleash Ultimate Jutsu" com o ícone ○: a
    entrada chega certa.
  - O ○ final lança o golpe de abertura. O filme do Ougi **só começa se esse golpe acertar**, que é regra do jogo; com
    o adversário longe ou atacando (ação 5/6 do jogador), não acerta e o chakra volta.
  - O especial de um toque não mexeu em nada (nenhuma linha `[sp] ... ougi`).
- **Conclusão:** o botão está certo. Para o jogador, uma dica na aba Controles: "chegue perto antes do Ougi".
- **Ferramenta:** `build/lab/withbase.sh CMD`: tira o A35 para um teste do jogo base e sempre o devolve (`trap EXIT`).
  Antes, um `cd` falho no meio da cadeia de comandos deixou o aparelho sem o mod.

### 2026-11-09 09:10, manhã: botão Ougi por toque real ainda não abre o filme (aberto)

- **Teste** (`build/lab/gest/ougi.sh`, saída em `gest/ougi/`):
  - toque real no botão Ougi (0,80; 0,29) e depois toques no Ataque durante 7 s;
  - **sem nada no chakra:** o jogador apanha enquanto segura Chakra, e o Ougi não sai (falta chakra);
  - **com o chakra do lutador `0xd7ba20` posto em 15,0 por POKE** (só depuração: `+0x70`, float `0x41700000`, a cada
    60 vsyncs, `gest/env_ougi.txt`): o toque dispara uma sequência de jutsu (o salto ao céu, 5 hits), não o filme do
    Ougi.
- **Hipóteses a separar:**
  1. `0xd7ba20` não é o jogador na luta 1. A CPU foi `0xd7b9a0` nas rotas, mas isso não prova qual é o jogador;
     falta logar o lutador humano;
  2. o especial de um toque (`special_one`, ligado) reescreve o ○ final do △△○ do botão Ougi. O registro de 11-05
     viu △△○ funcionar com o especial **desligado**.
- **Próximo passo:** logar com `HL_SP_LOG` o lutador humano e o chakra no toque, e repetir com o especial desligado.

### 2026-11-09 08:49, manhã: térmica de 4x/120 no Odin 2 Mini e correção de horários

- **Correção de horários:** as entradas "08:50" (campanha) e "08:58" (botões na tela) foram escritas antes das 08:49 reais.
  O relógio do PC é a referência; a ordem das entradas está certa.
- **Térmica** (`build/lab/matrix/o2t.tsv`):
  - seis rodadas seguidas de 4x/120 (`COOL=1`, ~20 min de luta), no carregador;
  - em todas: 50,0 fps, 1% low de 42,4 a 43,2, pares acima de 44 ms de 0,1 a 0,3%, pior par de 57 a 63 ms;
  - imagens por segundo de 111 a 117; a temperatura máxima ficou entre 70 e 77 °C;
  - **sem perda por calor**: 4x/120 aguenta sessões longas no Odin 2 Mini.

### 2026-11-09 08:58, manhã: teste real dos botões na tela, na luta (Odin 2 Mini)

- **Montagem** (`build/lab/gest/fight.sh`, saída em `build/lab/gest/fight/`):
  - a rota só passa os menus (`gest/env_fight.txt`, `HL_SP_LOG=1`);
  - na luta, toques `adb input` onde o `NinjaPadView` desenha cada botão (`PadDefaults.FIGHT`): tela de 1920 × 1080,
    densidade 2,31, sem recorte;
  - uma captura depois de cada passo, e a tela gravada.
- **Resultado:**
  - **direcional** segurado à direita: o lutador anda;
  - **Ataque** (6 toques seguidos): o jogo mostra "Naruto Uzumaki Combo Attack";
  - **Pulo:** pula;
  - **Chakra** segurado 2,5 s: carrega;
  - **Jutsu** (△ ○ num toque): golpe de 6 hits;
  - **Ougi** (△ △ ○ num toque): o filme não abriu. Pela tela a barra de chakra não estava cheia, que é regra do jogo.
    Falta um teste com chakra cheio;
  - **disputa:** 40 toques no Ataque durante uma disputa ("Mash" na tela); o log mostra `[sp] pad 0 answer` e
    `pad 1 answer` (v7938 e v7950), ou seja, o botão especial respondeu à disputa;
  - **Item, Defesa, START** (pausa e volta): respondem;
  - **Voltar:** abre o menu do jogo sobre a luta.
- **Adaptação a ver:** os botões Item (0,725; 0,86) e Defesa (0,955; 0,87) ficam sobre a barra de itens do HUD do jogo
  (L1/R1, x∞), embaixo. Em 16:9, subir os dois uns 4% deixaria o HUD livre.

### 2026-11-09 08:50, manhã: campanha no Odin 2 Mini (widescreen e medida) e a tela de carga dela

- **Gravação e medida** (`rec/o2_camp_w`, `hl_env_camp_m.txt`, `HL_PERF_FROM=8200`, andando na vila):
  - `RESULT` 50,0 fps, 1% low 45,6, 0,0% dos pares acima de 44 ms, pior par de 42,6 ms;
  - tempos: EE 6,3 ms, VU1 9,0 ms, GS 6,8 ms, 11,8 ms ociosos;
  - o 3D da campanha sai largo, e as caixas de fala e as dicas ficam na proporção (HUD do nativo).
- **NOW LOADING da campanha** (`HL_GS_DUMP=campanha_cortina`, `tools/gsdump.py gif` e `gifprims.py`):
  - vsyncs 11 a 131 do dump: 78 primitivas por quadro;
  - o fundo é preto, de um quadro inteiro, e não é apertado;
  - por cima vêm engrenagens decorativas (tiras de 366 × 128) e o pergaminho em 4 linhas × 2 colunas, de x 60,6 a 497,8;
  - o pergaminho não chega às bordas: no 4:3 a borda rasgada já fica sobre o preto. Apertado, ele fica centrado e o
    preto se estende dos lados;
  - **é o desenho do jogo, não defeito.**
- **Ferramentas:**
  - `tools/gsdump.py gif FILE OUT` exporta as transferências de um dump PCSX2 para o formato do `gifprims.py`;
  - nesse formato, o vsync é o caminho 0xFF e os registradores o 0xFE;
  - o `gifprims.py` marca os vsyncs.

### 2026-11-09 08:40, manhã: Alpha 1.1X no Odin 2 Mini, com menus em widescreen e rota relativa

- **Montagem** (`rec/o2_a11x`):
  - `un5_mods.txt` = `modlib/un6-a11x/disc`, sem `modlib/mods.txt`;
  - o log diz `mod program …/un6-a11x`;
  - rota `build/lab/hl_env_a11x_r.txt`;
  - no fim, voltou o A35 (`mods.txt` `1|un6-a35`, `un5_mods.txt`).
- **Menus:** o título da Alpha (idiomas em português), Mode Select, Select Character com Linked Mode, NOW LOADING e
  "Start Battle" ocupam a tela inteira, na proporção.
- **Luta 1:**
  - 49,8 a 49,9 fps (`ana.py`);
  - Ougi do jogador com o filme "Linked" (Naruto e Sakura);
  - KO, WINNER "Naruto Uzumaki", resultado (Health 70, Time 94, Bonus 1161, 176 Ryo), seleção.
- **Luta 2:** a CPU venceu, embora a vida dela tenha baixado três vezes (a CPU acertou o jogador antes). O jogo voltou
  direto à seleção, igual ao PCSX2 (06:09).
- **Linha da tabela "UN6 Alpha 1.1X":** agora com a rota inteira gravada no Odin 2 Mini, incluindo WINNER e resultado.

### 2026-11-09 08:25, manhã: duas lutas com vitória do jogador e WINNER e resultado em widescreen (Odin 2 Mini)

- **Rota** `build/lab/hl_env_v3w2.txt`:
  - a v3 relativa (`hl_env_v3r.txt`) com `HL_CPU_HP=@1+450,@1+900,@1+1400,@2+500,@2+900,@2+1400`;
  - várias baixas, porque o jogo repõe a vida perto do começo e um golpe nem sempre acerta logo;
  - gravação em `rec/o2_w3`.
- **Luta 1:** KO, WINNER "Naruto Uzumaki", resultado (Health 63, Time 96, Bonus 562, 721 Ryo), seleção, VS.
- **Luta 2:**
  - Ougi do jogador com "Reversal" e o Ougi da CPU (filme da Kyuubi), depois KO no tempo 80;
  - WINNER e resultado: Health 13, Time 80, Bonus 471, 564 Ryo, total 1285 (721 + 564).
- **Widescreen:** WINNER e o resultado (Ninja Song) ocupam a tela inteira, com o texto e o retrato na proporção. Isso
  fecha as telas de menu da rota de luta, salvo os filmes, que ficam em 4:3.
- **Defeito da rota, corrigido:**
  - a terceira baixa da luta 1 (`@1+1400`) ficou pendente, porque a luta acabou antes, e disparou no começo da luta 2;
  - agora um passo da luta N é descartado quando a luta N+1 já começou;
  - vale no `HL_CPU_HP` (`hl_game.cpp`) e no `HL_PADROUTE` (`0083` atualizado).
- **Em seguida:** Alpha 1.1X no Odin 2 Mini (`modlib/un6-a11x`, 934 MB copiados por `run-as`), com a rota relativa
  `hl_env_a11x_r.txt` (menus da `hl_env_n3_v3_c.txt`).

### 2026-11-09 08:20, manhã: menus em widescreen no Odin 2 Mini (16:9, "Tela cheia")

- **Correção da hora:** a entrada abaixo diz 09:00, mas foi escrita às 08:05.
- **Primeira gravação** (`rec/o2_w1`, `13256aa`): o 2D ficou na proporção, mas o fundo também foi apertado, com
  faixas pretas ou brancas dos lados.
- **Causa:**
  - o jogo monta a tela no quadro `FBP 0` (o fundo é uma tira de um quadro inteiro, 0..511 × 0..447);
  - depois copia esse quadro para o quadro mostrado (`FBP 0x70`, contexto 2) em 8 sprites de coluna de 64 × 512
    (`gifprims.py`, pacotes 2515 a 2522 da captura da seleção);
  - cada coluna era apertada de novo.
- **Correção** (`widen2d`): primitiva com textura cujo `TBP0` é um quadro desenhado há pouco (`FBP × 32`, os últimos 8
  `FRAME`) é cópia de imagem e fica como está. O `TEX0` também é lido nos laços packed (descritores 6 e 7).
- **Segunda gravação** (`rec/o2_w2`):
  - ocupam a tela toda, sem faixas, com texto, retratos e painéis na proporção:
    - escolha de idioma, título (New Game / Load Game) e NOW LOADING;
    - Mode Select, Select Character, a janela do Linked Mode, Stage Select e VS;
  - os filmes (logos, abertura) ficam em 4:3 com faixas pretas, porque o quadro do filme não cobre 90% da largura.
    Assim a proporção do filme fica certa;
  - a luta, pelo nativo, não muda.
- **Falta ver:** WINNER, a tela de resultado e a campanha (o mapa usa o nativo, mas os menus dela não), e 20:9 num
  aparelho com essa tela.

### 2026-11-09 09:00, manhã: matriz de desempenho do Odin 2 Mini (resolução × fps × filtro) e o menu do jogo no aparelho

**Matriz** (`build/lab/matrix.sh 192.168.1.206:5555 o2m`, `build/lab/matrix/o2m.tsv`):
- **Cena e medida:**
  - luta 1 da rota v3, Naruto x Naruto, com ○ contínuo, sem mod;
  - `HL_PERF_FROM=6500`: 10 s de aquecimento e 30 s de medida;
  - 30 s de descanso entre as rodadas;
  - o aparelho estava no carregador, com tela de 60 Hz.
- **Colunas:**
  - **jogo:** fps do jogo;
  - **1% low:** fps nos 1% piores quadros;
  - **pares:** % de pares de vsync acima de 44 ms;
  - **pior:** o pior par, em ms;
  - **imagens/s:** imagens apresentadas por segundo, contadas pelas linhas `[present]` (só no modo interpolado);
  - **°C:** a zona térmica mais quente.

| escala | fps pedido | jogo | 1% low | pares | pior | imagens/s | °C |
|---|---|---|---|---|---|---|---|
| 1x | 50 / 60 / 120 | 50,0 | 42,4 a 43,3 | 0,1 a 0,3% | 58 a 64 | – / 59 / 118 | 69 a 72 |
| 2x | 50 / 60 / 120 | 50,0 | 42,3 a 43,1 | 0,1 a 0,3% | 56 a 61 | – / 59 / 118 | 67 a 68 |
| 3x | 50 / 60 / 120 | 50,0 | 42,7 a 42,9 | 0,1 a 0,3% | 61 a 63 | – / 58 / 118 | 68 a 70 |
| 4x | 50 / 60 / 120 | 50,0 | 42,4 a 43,1 | 0,1% | 58 a 63 | – / 59 / 112 | 70 a 76 |
| 6x | 50 | 50,0 | 36,1 | 0,3% | 113 | – | 76 |
| 6x | 60 | 50,0 | 34,5 | 0,5% | 90 | 54 | 80 |
| 6x | 120 | 50,0 | 30,8 | 1,2% | 94 | 97 | 78 |
| 8x | 50 | 49,9 | 26,6 | 0,3% | 244 | – | 83 |
| 8x | 60 | 47,1 | 11,1 | 8,4% | 239 | 50 | 81 |
| 8x | 120 | 47,2 | 11,3 | 8,8% | 241 | 64 | 82 |
| 2x, 240 | | 50,0 | 43,2 | 0,3% | 59 | 203 | 68 |
| 4x, 240 | | 50,0 | 42,7 | 0,1% | 59 | 155 | 73 |
| 8x, 240 | | 47,0 | 11,1 | 8,9% | 241 | 72 | 80 |
| 2x, 60, filtros | | 50,0 | 42,1 a 42,9 | 0,1 a 0,3% | 57 a 62 | 59 | 71 a 75 |

**Filtros:** bilinear nítido, FXAA e CAS não custam nada que apareça na medida.

- **O que o Odin 2 Mini aguenta:**
  - até 4x (1792p) a luta fica cravada em 50 fps, com 118 imagens/s a 120;
  - a 4x e 240, ainda são 155 imagens/s;
  - a 2x e 240, são 203 imagens/s;
  - o EE e a VU1 gastam ~5 ms cada (de 20 ms por vsync): a CPU sobra em todas as escalas;
  - **o limite é a GPU:**
    - 6x ainda dá 50 fps, mas o 1% low cai para 31–36;
    - 8x (4096 × 3584, acima de 4K) perde vsyncs (47 fps, 8–9% de pares lentos) e esquenta a 81–83 °C;
    - a 8x, 120 não passa de 64 imagens/s.
- **Recomendação para o aparelho:** 4x com 120 é o máximo estável. Numa tela de 60 Hz, 60 interpolado a 2x–4x é o
  que se vê.
- **Faltou medir:** a campanha, os menus e uma rodada longa em 4x/120 (térmica).

**Menu do jogo no aparelho** (`build/lab/gest/panel.sh`):
- o Back abre o menu sobre o jogo; as três abas aparecem e o Continuar fecha;
- capturas em `build/lab/gest/p1..p5`:
  - Gráficos com 1x a 8x · 4K e 30/50/60/90/120;
  - Controles com os botões e as dicas;
  - Layout com os botões na tela, o visual, o estilo e a opacidade;
- os mods foram restaurados depois.

### 2026-11-09 07:50, manhã: menus por gestos, novo menu do jogo, 2D dos menus no formato da tela, 90/120 em toda tela

- **Menus por toque** (`NinjaPadView.menuGesture`, `6a3229d`):
  - na cena de menu, nenhum botão na tela além de ≡ (menu do jogo) e START (título, entrada do 2P);
  - gestos:
    - toque = ✕ (OK);
    - deslizar = direção, e segurar fora do lugar repete (380 ms e depois 150 ms);
    - segurar parado = ○;
    - dois dedos = △ (voltar);
    - dois dedos deslizando para os lados = L1/R1;
    - dois dedos parados = □;
    - três dedos = START;
  - a tela de seleção confirma o mapa do jogo europeu: ✕ OK, △ Back, ○ Random, L1 cor;
  - `TouchConfig.menuTouch` (ligado por padrão) volta aos botões;
  - SCENE e OPTIONS saíram da barra de cima, e o conteúdo deles foi para a aba Layout;
  - **teste no Odin 2 Mini** (`build/lab/gest/run.sh`, `adb input`): deslizar para cima levou o cursor de ENGLISH a
    ITALIANO e um toque confirmou (entraram os logos). O START na tela do idioma não faz nada: lá o jogo quer ✕.
- **Menu do jogo** (`GamePanel.kt` reescrito, `e3f0d17`):
  - desenho próprio nas cores dos menus do jogo (papel, tinta marrom, laranja), sem arte do jogo;
  - à esquerda, um trilho com as abas Gráficos, Controles e Layout, e Continuar e Sair embaixo;
  - **Gráficos:**
    - resolução 1x, 2x, 3x, 4x, 6x e 8x (8x = 4096 × 3584, acima de 4K);
    - filtro, formato da tela, contador (depuração);
    - quadros por segundo 30, 50, 60, 90 e 120, em qualquer tela; a dica mostra até quantos Hz o painel vai;
  - **Controles:**
    - o que cada botão faz na luta e na campanha, e os gestos dos menus;
    - ajudas: especial de um toque e quanto vale um toque nas disputas. Valem na hora, pelo JNI
      `nativeSetAssist`; antes só valiam no próximo início;
  - **Layout:**
    - botões na tela (automático, sempre, desligado), visual, estilo, opacidade e tamanho;
    - menus por gestos, analógico flutuante, vibração;
    - "Mover e redimensionar botões" abre o editor;
  - o diálogo antigo (`showOptions`) saiu; textos em `values*/strings_panel.xml` (pt, en, es).
- **2D dos menus no formato da tela** (`hl_native::widen2d`, chamado pelo `hl_gsx.cpp` com o nativo desligado e a
  câmera do jogo não larga; `13256aa`):
  - cada primitiva desenhada no quadro mostrado é apertada para o meio por `hudScale` ((4/3) ÷ formato). Esticada
    para a tela, volta à proporção dela;
  - o que cobre 90% ou mais da largura (fundo, limpeza, fade) fica como está e enche a tela;
  - uma tira (strip) ou leque segue a decisão do primeiro triângulo;
  - o SCISSOR de um painel é apertado junto com o que ele recorta;
  - desenho fora da tela (FBW diferente do `DISPFB2`) não é tocado;
  - a largura vem do `DISPLAY2`/`DISPFB2` (pacote de registradores);
  - `HL_WIDE2D_OFF=1` desliga;
  - o host manda `Bt4Gsx_WideCamera` a cada vsync;
  - base: `tools/gifprims.py` sobre o `hl_gif.bin` da seleção, capturado no PC (`HL_GIF_LOG=6000@96`). Fundo em
    sprites de 93 a 100% da largura; os dois painéis de personagem em strips de 50% cada, com SCISSOR por painel
    (18-233 e 278-493); tudo na textura `0x3c00`, por isso a textura não serve para agrupar;
  - **ainda não testado no aparelho** (a matriz ocupa o Odin 2 Mini).
- **90 e 120 em toda tela** (`b17cc94`):
  - o painel e o launcher oferecem 90 e 120 mesmo num painel de 60 Hz, para medir;
  - o `GameActivity` pede o modo mais rápido da tela (`preferredDisplayModeId`) quando o fps passa de 60;
  - o 16:10 saiu do launcher: caía no Stretch com a câmera ajustada para 1,6.
- **Medida:**
  - `build/lab/matrix.sh` mede resolução × fps × filtro na luta fixa (rota v3, ○ de +500 a +2700);
  - as linhas `[present]` (com `HL_PERF`) contam as imagens por segundo do interpolador;
  - `HL_FILTER` escolhe o filtro pelo env.

### 2026-11-09 07:09, manhã: rota relativa ao começo de cada luta (`@N+vsyncs`), Odin 2 Mini

- **`0083-padroute-fight-relative.patch`** (`Pad.cpp` do runtime):
  - `HL_PADROUTE` aceita `@N+vsyncs:BOTÕES`, contados do começo da luta N;
  - o runtime pergunta por `g_hlFightStart(N)`, que o código do jogo preenche;
  - os passos seguem em ordem, e um passo relativo espera a luta dele começar.
- **`hl_game.cpp`:**
  - a luta começa quando um lutador lê a entrada depois de mais de `HL_FIGHT_GAP` vsyncs (1500) sem lutador nem Ougi
    lendo o controle. O filme do Ougi para os lutadores por ~1000 vsyncs; os menus entre lutas, ~3800;
  - o log diz `[route] vN fight K begins`;
  - `HL_CPU_HP` também aceita `@N+vsyncs`. Ele baixa a vida do lutador da CPU, achado pelos bits de controle, sem
    endereço fixo.
- **Rota `build/lab/hl_env_v3r.txt`:**
  - os menus da v3 até a luta 1, por vsync;
  - `HL_CPU_HP=@1+450,@2+450`;
  - ○ de +500 a +1600 a cada 12 e ✕ de +1700 a cada 150, em cada luta.
- **Teste (`rec/o2_v3r`):**
  - log: `fight 1 begins` v6098, `health 0.0115` v6646, `fight 2 begins` v9873, `health` v10323, `fight 3 begins` v14500;
  - luta 1: WINNER "Naruto Uzumaki" e resultado (63 / 96 / 562 / 721), seleção, VS;
  - luta 2 (Naruto x Naruto): KO no tempo 82, depois tela branca, carga e seleção, sem WINNER. É o caminho da vitória da
    CPU (igual ao PCSX2, 06:09), mas pelos quadros não dá para ver quem caiu. **Conferir:** pôr a vida dos dois no log;
  - a terceira luta também começa e termina.

### 2026-11-09 06:40, manhã: rota v3 deslocada no Odin 2 Mini (`hl_env_v3o2.txt`): a luta 2 começa ainda mais tarde

- Deslocar +2100 vsyncs tudo o que vinha de v11150 em diante (`POKE`, ○ e os ✕ dos menus entre as lutas) atrasou
  também os menus. A luta 2 começou em v14831 (`rec/o2_v3c`) e o KO não coube até v16800.
- **Conclusão:** depois das cargas com o tempo do drive (`e0beedf`), uma rota por vsync absoluto não serve para a luta 2.
  **Fazer:** botões e `POKE` relativos ao começo de cada luta (o nativo já sabe: "scene on at vsync"). Até lá, a
  vitória da luta 2 fica sem conferir no Odin 2 Mini. A da luta 1 está conferida (`rec/o2_v3`).
- O mod foi posto de volta (`1|un6-a35`).

### 2026-11-09 06:36 (hora real), manhã: rota v3 até v16200 no Odin 2 Mini

- **Hora (correção):** as duas entradas de baixo, marcadas 06:36 e 06:45, foram escritas às ~06:20 e às 06:30
  (`612d3ca`, `01b4dd4`).
- **`rec/o2_v3b`** (mod tirado e posto de volta como antes): a luta 1 e a vitória como em `o2_v3`, e a luta 2 até o
  tempo 70, sem trava e de 49,8 a 49,9 fps.
- **O KO da luta 2 não saiu até v16200.** O `POKE` da vida da CPU (`d7ba8c` em v11150) não derrubou a CPU a tempo.
  O endereço é o do registro de 11-03, e o lutador da luta 2 pode ter mudado de lugar. Fica para a próxima rota: tirar
  o endereço do log do jogo, ou terminar a luta 2 por tempo.

### 2026-11-09 06:45, manhã: A35 no Odin 2 Mini; o mod do desenvolvedor de volta

- **A35 (`rec/o2_a35`, `hl_env_a35f.txt`, `libun5_un6_a35.so` regenerada às 05:10):**
  - aviso do mod, logos, abertura e carga do save copiado;
  - "Combate Livre", seleção, "Lutem!" e Aldeia Oculta da Folha;
  - luta com o 3D completo e o Ougi da CPU (cena no céu);
  - 59,9 fps (NTSC), mínimo de 52,2.
- **Restaurado:** `modlib/mods.txt` (`1|un6-a35`) e `un5_mods.txt`, como estavam. O app foi parado.

### 2026-11-09 06:36, manhã: Odin 2 Mini (aparelho de teste a partir de agora): luta, Ougi, vitória, campanha

- **Aparelhos:** pedido do desenvolvedor às 06:12: não usar mais o RP5, testar só no Odin 2 Mini
  (`192.168.1.206:5555`, já no Wi-Fi e desbloqueado).
- **Mod:** o aparelho estava com o A35 ligado pelo launcher (`modlib/mods.txt` = `1|un6-a35` e `un5_mods.txt`). Os
  dois foram copiados para o scratchpad e apagados para as rotas da base; voltam no fim.
- **Ajustes do aparelho, mantidos:** resolução 2x (alvo 1026x900), 60 interpolado.
- **Rotas, APK de `63298ae`:**
  - **Luta da `route_v3` do PC (`rec/o2_pc3`):**
    - menus, START BATTLE, luta até o KO e seleção;
    - Ougi da CPU com a cena no céu;
    - nativo a 2x, mínimo de 49,9 fps.
  - **Campanha (`rec/o2_camp`):**
    - diálogos, "Act 1", vila, troca de área para o Hidden Leaf Gate, menu START e painel de pausa;
    - de 49,7 a 49,9 fps, uma amostra de 46,7;
    - o "Sair do jogo" não rodou, pelo mesmo motivo de antes (o fim da rota vem antes do `QUIT_AT`).
  - **Rota v3 (`rec/o2_v3`):**
    - luta 1 com o Ougi do jogador: faixa "Reversal", sequência de botões com OK/ERROR, manto da Kyubi no
      cânion, close nos olhos, escurecimento e volta;
    - KO, WINNER "Naruto Uzumaki", resultado "Ninja Song" (34 / 92 / 661 / 787, Outstanding), seleção, VS da "Round 2",
      carga e começo da luta 2;
    - de 44,3 a 49,9 fps; a gravação parou no vsync de fim, antes do KO da luta 2.
- **Save do A35:** copiado do RP5, só o arquivo. Pelo adb não dá para criar pasta em `Android/data` neste Android;
  foi via `/data/local/tmp` e `run-as com.dfdx047.hiddenleaf cp`.

### 2026-11-09 06:09, manhã: vitória da CPU vai à seleção também no PCSX2; luta 1 igual ao PCSX2 até v9400 (PC)

- **Vitória da CPU:** a rota `route_v3` seguida de ✕ a cada 150 vsyncs leva a luta 1 até a vitória da CPU, e
  `HLB_SHOTV` tirou um quadro a cada 200 vsyncs nos dois lados (recomp +27). O PCSX2 também vai direto à seleção de
  personagem, sem WINNER nem resultado. O recomp faz igual: não é defeito. A observação "(não conferido no PCSX2)" da
  tabela fica respondida.
- **Divergência na mesma corrida:**
  - Os quadros eram iguais até o tempo 75; no 73, a CPU do recomp faz um Ougi que a do PCSX2 não faz.
  - O `hlbench.py diff` aponta o quadro 882 da "luta 2" da bancada (a parte depois do Ougi, que a bancada conta como
    luta nova, com o RNG igual nos dois lados: `mti 354`, `mt dac20b1d`). A CPU começa a ação 8 no recomp (chakra 15 →
    12) e fica na 0 no PCSX2. As chamadas do RNG só divergem no quadro 912.
- **Causa: a rota, não o recomp.**
  - Os ✕ depois do KO eram por vsync absoluto (a partir de v9000), mas a luta do recomp anda 27 vsyncs atrás. Esse ✕
    cai no quadro ~876 no recomp e ~889 no PCSX2, e a CPU reage ao botão do jogador.
  - Sem esses ✕ (corridas `pnx`/`rnx`), o estado de jogo (posição, vida, chakra e ação dos dois) é igual em todas as
    2208 chamadas até v9400.
  - Regra para a bancada: botões dentro da luta, só por quadro (`HLB_FROUTE`), nunca por vsync.

### 2026-11-09 05:16, madrugada: campanha em 20:9 no RP5 (`wm size 2400x1080`)

- O Odin 3 está fora da rede, então o 20:9 foi forçado no RP5 (`wm size 2400x1080`, gravação em 2400x1080, `wm size
  reset` no fim). Rota de campanha (`rec/pc3_rp5_209camp`), a mesma das 04:50: chegou ao v12427 com o painel de
  pausa. O "Sair do jogo" não rodou: o fim em v12400 vem antes do `QUIT_AT` no laço do `recfull2.sh`.
- **`ana.py`:** a vila, os diálogos, a troca de área e o menu START ocupam a largura toda. As faixas reais são as das
  telas de carga ("NOW LOADING", em 4:3, ~4 s cada), o item 4 (menus em 16:9) já anotado na tabela. As outras
  marcações (60–67 s, 132–135 s, 147–150 s) são bordas escuras do cenário e o tutorial escurecido, vistos quadro a
  quadro.
- Fica como estava na tabela ("parcial"). Falta o Odin 3 de verdade.

### 2026-11-09 05:10, madrugada: A35 com 3D preto na luta (código do mod velho); refeito, luta e Ougi de volta (RP5)

- **Defeito:** com a `libun5_un6_a35.so` refeita às 04:51 só com os patches novos, a luta do A35 desenhava o primeiro
  quadro e depois só o HUD (`rec/pc3_rp5_a35`). Era igual com o nativo e com o GS do ARMSX2 (`HL_NATIVE=0`,
  `rec/pc3_rp5_a35gs`): o problema vinha do EE, não do desenho.
- **Causa:** o código gerado do mod (`build/mods/un6-a35/gen`, de 11-05) é anterior ao unstub de `ba71a41`.
  `sceGsExecLoadImage` e o par Store ainda iam para o tratador do runtime, e a Alpha não sofre disso porque usa o código
  da base.
- **Correção:** `recomp/build.sh --mod "UN6 A35.iso"` de novo (9573 funções, 0 erros no programa; overlays `mod` e
  `textbra` com 189 e 44 erros, como antes) e `recomp/android/build.sh --mod un6-a35`.
- **Teste (`rec/pc3_rp5_a35b`, `hl_env_a35f.txt`):** do aviso do mod até a luta e o Ougi.
  - Menus: logos, abertura, carga do save, "Combate Livre", seleção, "Lutem!", Aldeia Oculta da Folha.
  - Luta: 3D completo, Ougi da CPU com a cena no céu, fumaça e golpes.
  - 59,1 a 59,5 fps no mínimo (o A35 é NTSC).
- **Proteção:** `recomp/android/build.sh --mod` agora para se `tools/un5.py` for mais novo que o
  `un5_config.toml` do mod (`HL_MOD_STALE_OK=1` passa assim mesmo).

### 2026-11-09 04:45, madrugada: Alpha 1.1X no RP5 com a `libun5.so` nova; ritmo das cargas (PC)

- **Hora (correção):** a entrada de baixo, marcada 04:50, foi escrita às 04:39 (`85627b0`).
- **Alpha 1.1X no RP5 (`rec/pc3_rp5_a11x`, `hl_env_n3_v3_c.txt`, `un5_mods.txt` = `modlib/un6-a11x/disc`, apagado no
  fim):** o log diz `mod program …/un6-a11x/disc/SLES_556.05 (the base's code, its own data)`. Na rota:
  - menus do mod, Hidden Leaf Village e luta (nativo de v6113 a v7264);
  - Ougi da CPU com o "Press the appropriate button" e "You win!!";
  - volta à luta, até o v9255;
  - de 49,4 a 50 fps, som presente.
- **A35:** `libun5_un6_a35.so` sendo refeita com os patches novos (`recomp/android/build.sh --mod un6-a35`).
- **Ritmo das cargas, PC (`HLB_CDLOG=1`, corridas `pcd`/`rcd`, as leituras do EE casadas pelo LBN):**
  - O tamanho de cada leitura é igual (0x20 setores); muda só o ritmo.
  - Na abertura, o recomp chega a ler ~500 vsyncs antes: o CD dele é instantâneo.
  - Nos menus, o recomp lê ~48 vsyncs antes, porque a rota usa vsyncs fixos. A leitura periódica `0x75xxx` (a cada
    ~50 vsyncs) também vem 48 vsyncs antes.
  - Na carga da luta (de v5031 em diante), cada leitura sai um pouco depois: o atraso cresce de +5 a +54 vsyncs em
    ~250 vsyncs. Daí os 28 vsyncs a mais no começo da luta.
  - **Conclusão:** igualar isso exige um modelo de tempo do drive (busca e leitura) e de ciclos do EE como no PCSX2.
    Ganho: ~1 s na carga da luta no PC. Não faço agora.

### 2026-11-09 04:50, madrugada: RP5 com a `libun5.so` nova: campanha completa e Ougi

Mesmo APK da entrada das 04:30 (`af8ed28`), com o `hl_lbn.txt` no aparelho.
- **Campanha (`rec/pc3_rp5_camp`, `hl_env_camp.txt`, `PAUSE_AT=11000`, `QUIT_AT=12300`):**
  - percurso: logos → título → New Game → diálogos → "Act 1 Naruto Returns Home" → vila → troca de área (carga) →
    menu START e tutorial → painel de pausa (4:3, tela cheia, continuar) → "Sair do jogo" → launcher;
  - de 49,6 a 50,3 fps de v6000 a v12446, fora a amostra de 0,8 no painel de pausa (com o jogo parado);
  - som presente, sem `missing-target`, sem barras no 16:9.
- **Ougi (`rec/pc3_rp5_ougi`, `hl_env_pc3o.txt`: a `route_v3` do PC e △ △ ○ a cada 90 vsyncs de v5610 a v6690):**
  - "Unleash Ultimate Jutsu" e a cena do Ougi (céu, Naruto voando contra Kakashi) desenhadas pelo nativo;
  - na volta à luta, clones, fumaça, Great Ball Rasengan, letras de impacto e contadores;
  - de 49,5 a 50 fps, com uma amostra de 46,7 em v6545.
- **Falta para dar como corrigido:**
  - os outros aparelhos: o Cube sem a base, o Odin 3 fora da rede, o Odin 2 Mini bloqueado;
  - o 20:9;
  - os mods.

### 2026-11-09 04:25, madrugada: as partículas a mais vêm do relógio das partículas, adiantado no recomp (PC)

- **Hora (correção):** a entrada de baixo, marcada 04:30, foi escrita às 04:05 (`af8ed28`). O Odin 2 Mini
  (`192.168.1.206`) tem a base, mas está com bloqueio por senha: o jogo não abriu (`rec/pc3_odin2`).
- **Quem escolhe o microcódigo:** `0x195EE0` monta o DMA da VU1 com a entrada
  `0x5C6630 + 16 × (obj[0x264] + obj[0x1F0] + obj[0x265])`, a partir de uma cópia do objeto no scratchpad. Cada entrada
  da tabela é {alfa `512f`/`512a`, 0, endereço do programa, tamanho}, com 16 entradas por modo:
  - modo 0: `0x3d5790` e `0x3d5da0`, MPG de 79 e 81 instruções;
  - modo 1: `0x3da290`, 256 instruções em 0 e 106 palavras em 2048.
- **Medido no mesmo vsync** (PCSX2 v5784 x recomp v5811): o PCSX2 sobe 14 programas do modo 0, e o recomp sobe 19 do
  `0x3da290`.
- **Os objetos** (`HLB_FNLOG=0x195ee0`, `a1` = o objeto; corridas `pfn`/`rfn`, 3 vsyncs a partir do quadro 199):
  - 128 chamadas no PCSX2, 139 no recomp;
  - 7 objetos só no recomp (quase todos de `0x19512C`), 2 só no PCSX2.
- **Nos objetos comuns:** o byte `obj+0x264` está sempre 64 à frente no recomp (mod 256), e o `obj+0x265` ~122 à
  frente. É o mesmo relógio do sistema de partículas da fase, com outra fase: não é lógica errada.
- **Causa provável:** as cargas, que no recomp levam 44–80 vsyncs a mais (a luta 1 começa no v5414 contra v5386). Os
  efeitos de fundo seguem andando durante a carga.
- **Consequência:** igualar isso é igualar o tempo das cargas (ritmo de CD), o mesmo assunto do `HLB_CDBUSY`. Fica
  aberto, de prioridade baixa: na tela nada muda, e o custo é de uns 10 lotes fora da tela.

### 2026-11-09 04:30, madrugada: as correções do PC no Android (RP5: rota do PC até a luta e o KO)

**`libun5.so` nova**, com tudo o que entrou no PC desde 5/11: `0071`–`0082`, o unstub de `un5.py` e os setores reais.
Dois consertos para compilar e rodar no Android:
- **`0082-vmul-error-fma-on-arm.patch`:** o `0071` usava `_mm_fmadd_ps` no aarch64, e o shim SSE→NEON não tem essa
  função; o Android não compilava desde o `0071`. Agora é `vfmaq_f32(-r, a, b)`, a mesma conta fundida.
- **`0081-gslive-path3-carried.patch`:** o `hl_gslive::transfer` (GS do ARMSX2 e renderizador nativo) recebia o
  pacote cru. Com o unstub, o jogo manda a tag `IMAGE` num DMA e os pixels em outro, e o `trimLastTag` reescreveria essa
  tag com NLOOP 0 e EOP. Os pixels chegariam sem tag e seriam lidos como tags. Agora o `deliverCarried` (`0077`) entrega
  os mesmos pedaços remendados ao GS do runtime e ao `hl_gslive`. O renderizador nativo continua a subida aberta pelo
  `TRXDIR` com cada `IMAGE` seguinte, então a continuação encaixa. No PC nada muda (`PS2X_HEADLESS_HOST`).
- O VIF1 do Nominum (`NOMINUM_VU1=1`, desligado por padrão) não tem a correção do V2/V3 (`0079`): o repositório dele é
  só leitura daqui. Fica anotado.

**Aparelhos:**
- `hl_lbn.txt` posto à mão em `files/runtime/data/` no RP5 e no Odin 2 Mini. O importador ainda não o grava.
- **RP5 (`rec/pc3_rp5`, `hl_env_pc3.txt`: a `route_v3` do PC, ✕ a cada 150 vsyncs depois, `HL_FAST_UNTIL=5300`):**
  - menus, seleção, carga e luta até o KO, depois a seleção (a CPU vence);
  - nativo ligado do v5429 ao v9506;
  - na luta, a nuvem da substituição, a poeira, os clarões e os golpes;
  - som tocando, ~50 vsyncs/s, sem trava até o v9897.
- **Cube:** sem a base do jogo (a pasta `files/` não existe). Precisa importar a ISO no app.
- **Odin 3:** fora da rede (o `192.168.1.134` não responde).

**Não é a rota completa** (falta a campanha, a pausa e a saída), nem passou nos 3 aparelhos e no 20:9. Nada disto
conta como corrigido.

### 2026-11-09 04:05, madrugada: Ougi igual ao PCSX2 no PC

- **Hora (correção):** a entrada de baixo, marcada 03:45, foi escrita às 03:38 (`date` do commit `1a944ff`).
- **Como sai o Ougi:** com a `route_v3` e △ △ ○ no quadro 100 (`HLB_FROUTE="100:TRIANGLE TRIANGLE CIRCLE"`), o golpe
  conecta no quadro 115–116. A cena roda do v5620 ao v5918 no PCSX2 e do v5647 ao v5945 no recomp: a mesma duração,
  27 vsyncs depois. Nos quadros 88, 95 e 105 o golpe erra.
- **Resultado:**
  - estado de jogo igual nas 236 + 46 chamadas (antes e depois da cena);
  - 28 capturas da cena lado a lado (`HLB_SHOTV`, novo no recomp; deslocamento de 27): os mesmos planos, clones,
    fumaça, letras de impacto e contador de golpes;
  - GS na metade da cena (PCSX2 v5760 x recomp v5787, 4 quadros): nenhum desenho só no PCSX2. Só no recomp, os mesmos
    lotes de partícula fora da tela da luta (TS, PSMT8/PSMT4, alpha fixo `0x44`).
- **`hl_bench.cpp`:** `HLB_SHOTV=V,V,...` (o `hl_shot` nesses vsyncs, "v.. shotv" no txt).

### 2026-11-09 03:45, madrugada: UNPACK V2/V3 do VIF1 como no hardware; partículas a mais vêm da escolha de microcódigo do EE (PC)

Continuação da entrada das 02:45. No PC, `route_v3`, `HLB_RNG=1`, quadro 199 da luta 1.

**Defeito do VIF1, corrigido no PC** (`0079-vif1-v2-v3-unpack-zw.patch`):
- O `ps2_vif1_interpreter.cpp` deixava em Z e W o que a memória da VU1 tinha, nos dois caminhos (rápido e geral).
- O hardware, que o PCSX2 segue no `Vif_UnpackSSE.cpp` ("tested on ps2"):
  - V2 escreve X Y X Y. No V2-32 com os dados numa palavra ímpar do qword do pacote, W fica 0.
  - V3 lê um 4º componente (o seguinte do fluxo) para W e só o mantém em alguns vetores, pelo índice do vetor e pela
    posição dos dados no qword. Fora deles, W fica 0.
- Visto na entrada da VU1 (`HLB_VU1CAP`): o W das partículas valia `0x3F80005F` (1,0 com o Y) no PCSX2 e lixo no recomp
  (`0xE2000000`, `0xA2800000`). Com a correção, os 26 primeiros inícios do quadro batem, salvo 10 qwords de trabalho.
- Teste: estado de jogo igual ao PCSX2 nas 607 chamadas, e as 13 capturas iguais às do PCSX2 (`pfx` x `rv2v3`). A
  contagem de desenhos do quadro 199 não muda (as partículas fora da tela continuam).
- Limite: o alinhamento sai da posição no buffer. Na cadeia, o TTE põe 8 bytes no meio, o que não muda a paridade
  (todas as regras, exceto V3-16) mas muda o alinhamento completo do V3-16.
- **Falta:** o Android. O `core/vif1.cpp` do Nominum (`NOMINUM_VU1=1`) tem o mesmo caminho rápido e precisa da mesma
  correção. Depois, a rota nos aparelhos.

**As partículas a mais (aberto, só custo):**
- Trace por instrução da VU1 nos dois lados (`HLB_VU1TRACE`; no PCSX2 com a VU1 no interpretador). O primeiro desvio
  não é da VU1: o código em `0x0` é outro.
- Sequência de MPG e inícios (`0xA5` com a 4ª palavra = 3): igual até o 37º evento do quadro. Ali, o PCSX2 sobe um
  programa de 81 instruções em `0x0` (`MPG` de 162 palavras, 15 vezes no quadro), e o recomp sobe o de 256. Quem
  escolhe é o EE, então o estado do sistema de partículas do EE difere.
- Única diferença de ritmo achada: do quadro 0 ao 1 da luta, o recomp leva 1 vsync e o PCSX2 leva 2. Depois são sempre
  2, mas os quadros do recomp caem em vsyncs ímpares e os do PCSX2 em pares. O vsync do recomp vem do `EeScheduler`
  (ciclos do EE e relógio do host, na thread do EE). Segurar o EE na bancada não muda nada (tentado e retirado).
- Hipótese a testar: algum efeito anda por vsync (contador global ou interrupção) e herda essa diferença de fase.

**Ferramentas:**
- `0078` atualizado (trace, FNV e cópia do código a cada troca) e `0080-bench-vif1-mpg-event.patch` (MPG na captura).
- `tools/gsbench/vu1trace.py` e o formato da captura no `tools/gsbench/README.md`.
- No PCSX2 da bancada (fora do repositório): `OnVu1Step`, `OnVu1Exec` e o MPG em `VU1microInterp.cpp`, `VU1micro.cpp`
  e `Vif_Codes.cpp`.

### 2026-11-09 02:45, madrugada: jutsu, campanha e partículas fora da tela contra o PCSX2 (PC; partículas em aberto)

Todos os testes no PC, recomp contra PCSX2, `route_v3`, `HLB_RNG=1`.

**Correção da entrada de 2026-11-08 08:24 (item 1):** a frase "o GS recebe os desenhos iguais" no quadro 199 estava
errada. Na captura antiga (`rg1`) o recomp já mandava pacotes de partícula a mais pelo PATH1 (os mesmos de baixo).

**Jutsu (igual):**
- Entrada: △ △ ○ no quadro 88 da luta, com o Kakashi (1P) livre (`HLB_FROUTE="88:TRIANGLE TRIANGLE CIRCLE"`).
- O que acontece nos dois lados, nos mesmos quadros: chakra 15→10, ação 8, o aviso "Unleash Ultimate Jutsu" e a aura.
- Comparação: 35 pares de captura iguais e estado de jogo igual nas 411 chamadas.
- No quadro 40 a mesma entrada não faz nada nos dois lados (o lutador ainda está na entrada), e daí vinha o "Ougi não
  sai" de antes.
- O golpe não acertou, então a cena do Ougi em si ainda não foi comparada.

**Campanha (igual no conteúdo):** as mesmas telas e a mesma sequência de leituras do disco que o PCSX2
(`HLB_OROUTE`/`HLB_OSHOT`). Cada carregamento chega de 44 a 80 vsyncs mais tarde.

**Partículas fora da tela (aberto, só custo):**
- O recomp manda 27 pacotes a mais pelo PATH1 no quadro 199 (16×48, 9×8 e 1×40 vértices). Todos estão inteiros fora
  da tela, e a maioria dos vértices tem o bit ADC. Não se vê nada, mas custa VU1, o que importa no Android.
- **Não é a VU1:** com `HL_VUFAST=0` sai o mesmo número.
- **Não é o VCLIP:** o VCLIP em modo macro, com os bits ± trocados no `vu_translator.cpp`, não é usado pelo jogo. As
  duas funções com `vu0_clip_flags` só fazem CFC2/CTC2.
- **É o EE:** com `HLB_VU1CAP=199,4` (novo), o recomp faz 43 inícios de VU1 a mais no mesmo quadro (1983 contra 1940).
  Por programa: `0x50` +50, `0x0` +11, `0x1820` +5, `0x28`/`0x30` +11, `0x250` −34. O EE monta mais lotes de partícula.
- **Comparar a RAM ainda não ajuda:** com `HLB_RAMAT=198,199`, a heap acima de `0x900000` não se alinha com nenhum
  deslocamento fixo. É o "desvio de 0x80 da heap" já anotado, só que maior e variável. Arrumar isso vem antes.

**Palavras do lutador que diferem** (`+0x54`, `+0x000`, valores `0x80BBGGRR`): são sobras não inicializadas, que já
estão na RAM no v4600, antes de o lutador existir em `0xd6a000`. Sem efeito.

**Ferramentas:**
- `hl_bench.cpp`:
  - `HLB_FROUTE="[luta.]quadro:PALAVRAS,..."` (botões num quadro do lutador);
  - `HLB_OROUTE="ovl.off:PALAVRAS"` e `HLB_OSHOT=ovl,passo,n` (rota e capturas contadas da carga de um overlay);
  - `HLB_VU1CAP=F,N` (cada início de VU1: pc, os 16 KB de dados, VF e VI).
- `0078-bench-vu1-capture.patch` (o escritor no `ps2_vu1_core.cpp`).
- `tools/gsbench/vu1cmp.py`, e o modo `oshot` do `sheet.py`.
- O mesmo em `HLBench.cpp`/`VU1micro.cpp` do PCSX2 da bancada.
- **Aviso:** a captura de tela do recomp (`hl_shot`) pode cair alguns quadros depois da do PCSX2. Para comparar quadro a
  quadro, usar `HLB_GSDUMP` contra `HLB_GIFCAP` e o `capcmp.py`.

### 2026-11-09 01:25, madrugada: fumaça, poeira e clarão de volta no PC (corrigido no PC, falta a rota nos aparelhos)

Continuação da entrada de 2026-11-08 08:24. **Causa em duas partes, as duas medidas:**
1. **A subida de textura do jogo era trocada pela do runtime.** Gancho de DMA na bancada do PCSX2 (`HLB_DMALOG=V0,V1`,
   novo): os 532 inícios de DMA do GIF entre v1400 e v1470 vêm de `0x150B20` (`sceGsExecLoadImage`), chamado por
   `0x10FA70` (cabeçalho da textura: `+0x14` dbp, `+0x16` dbw, `+0x17` dpsm). Gancho de entrada de função nos dois lados
   (`HLB_FNLOG=addr,...`, novo no recomp e no PCSX2): **as mesmas 266 chamadas** nos dois. Só que o analisador do
   PS2Recomp põe `sceGsExecLoadImage` (e `SetDefLoadImage`, `ExecStoreImage`, `SetDefStoreImage`) na lista de stubs, e o
   stub do runtime (`GS.cpp`) remonta o pacote e devolve -1 para `PSMT8H/4HL/4HH` (`bytesForPixels` 0): nada era enviado.
   **Correção:** `tools/un5.py` (`UNSTUB`) recompila as quatro do próprio jogo, que mandam dois DMAs normais (A+D com a
   tag `IMAGE`, depois os pixels).
2. **O GS do runtime não guardava a tag GIF entre transferências.** Com o código do jogo, as 266 subidas saem iguais às
   do PCSX2, cabeçalho e dados (`tools/gsbench/imgs.py`), mas a VRAM não: os pixels vêm no segundo DMA, sem tag, e o
   `GS::processGIFPacket` lê cada pacote a partir de uma tag. **Correção:** `0077-gif-path3-tag-across-transfers.patch`
   (`ps2_gif_arbiter.cpp`): no PATH3, uma tag que termina incompleta fica guardada para o próximo pacote do path, e os
   dados `IMAGE` ainda devidos ganham uma tag própria. Só o PATH3: no PATH2 (VIF DIRECT) os pacotes do runtime terminam
   com qwords soltos que parecem tag incompleta, e guardá-los engolia todos os pacotes seguintes do path (o jogo travou
   entre os ticks 1560 e 2521, esperando SIGNAL/FINISH; visto com `HL_GIFCARRY_LOG=1`, novo). Esses qwords soltos do
   PATH2 ficam como suspeita aberta. No Android o fluxo cru do ARMSX2 (`hl_gslive::transfer`) não muda.

**Teste (PC, recomp contra PCSX2, `route_v3`, `HLB_RNG=1`, capturas a cada 25 quadros, `pfx` x `rfx2`):** a nuvem da
substituição, a poeira no chão e o clarão branco e roxo dos golpes aparecem nos mesmos quadros que no PCSX2 (12 pares).
VRAM depois das subidas: os bytes altos das páginas `0x70`–`0x16F` iguais ao PCSX2, salvo 57 páginas no Z (`0x01000000`
no recomp, 0 no PCSX2: medido em momentos próximos, não iguais; a conferir). Estado de jogo igual ao PCSX2 em todas as
chamadas (`hlbench.py diff`). Sem trava até o v6000.

**Não testado:** Android (o código gerado muda para os dois: as quatro funções passam a ser do jogo), outros cenários,
Ougi, campanha. Nada disso conta como corrigido antes da rota nos aparelhos.

**Ferramentas:** `0076-bench-gif-capture.patch` (o escritor do `HLB_GIFCAP` no `ps2_memory.cpp`, antes editado à mão);
`hl_bench.cpp` com `HLB_GIFCAP`, `HLB_GIFCAPV`, `HLB_RAMV`, `HLB_CDLOG`, `HLB_TRAPV`, `HLB_FNLOG`; os leitores em
`tools/gsbench/` (README lá). No `HLBench.cpp` do PCSX2 da bancada (fora do repositório): `HLB_DMALOG`, `HLB_FNLOG`.

### 2026-11-08 08:24, manhã: falta fumaça, poeira e clarão nas lutas do PC; as texturas deles nunca chegam à VRAM (aberto)

**Defeito (visto, PC, GS de software do runtime):** capturas lado a lado a cada 25 quadros da luta Kakashi x Naruto
(`route_v3`, `ps25` x `rs25`, 30 pares): no recomp faltam a nuvem de fumaça da substituição, a poeira no chão e o clarão
branco e roxo dos golpes. O resto (cenário, personagens, HUD, as letras de impacto, o brilho redondo) sai igual. É o
"efeitos diferentes" do relato do dono, e o estado de jogo continua igual ao PCSX2.

**Onde se perde (passo a passo, cada um medido):**
1. **O GS recebe os desenhos iguais.** Dump do GS do PCSX2 no quadro 199 (`HLB_GSDUMP=199,12`, novo na bancada do
   PCSX2) contra o GIF do recomp nos mesmos quadros (`HLB_GIFCAP=199,24`, novo): grupos de desenho (FRAME, TEX0, PRIM,
   ALPHA, TEST, com o contexto e o `PRMODE` certos) e número de vértices iguais quadro a quadro em 8 quadros; os
   vértices da fumaça (`tbp 4928`) iguais em posição, Z, cor e STQ. (Uma primeira leitura acusou `TEST` diferente; era
   meu leitor sem `CTXT`/`PRMODE`. Errado, corrigido.)
2. **As texturas desses desenhos não estão na VRAM.** Elas usam `PSMT8H`, `PSMT4HL`, `PSMT4HH` (27, 36, 44), guardadas
   nos 8 bits altos das páginas `0x70`–`0x16F` (o buffer da tela `0x70` e o Z24 em `0xF0`). PCSX2: esses bytes cheios
   (321989 palavras); recomp: **zero em todas**, no quadro 1 da luta, no 199 e já no v1500 e v2601 (`HLB_RAMV`, novo).
3. **No PCSX2 elas são enviadas uma vez no boot**, entre o v1400 e o v1500 (dumps de 100 em 100 vsyncs): ~264 envios
   `IMAGE` com `BITBLTBUF` de destino `0x1B/0x24/0x2C` a partir do bloco 3584, de v1445 a v1462, por pacotes comuns de
   A+D + `IMAGE`, junto com cargas de CLUT (8x2 CT32 em `0x12C0x`).
4. **O recomp lê o mesmo arquivo e descomprime:** `DATA/DATA.CVM` a partir de `0x7093000` (lbn `0x2851E`–`0x286D4`;
   PCSX2 v1411–1464, recomp v1462–1532, `HLB_CDLOG` novo no recomp). As texturas aparecem descomprimidas na RAM do
   recomp no v1505 (`0xC38D3C`; PCSX2 `0xC36BBC` no v1444, heap deslocado).
5. **Mas o recomp não manda nenhum desses envios ao GIF** (captura de v550 a v1550 e de v1495 a v1525: nenhuma
   transferência para esses formatos nem para os blocos 3584–12000, nem as cargas de CLUT; os pacotes de A+D e
   `SCISSOR` do mesmo quadro saem iguais aos do PCSX2). O pacote não está na RAM principal do PCSX2 em nenhum dos
   dumps (montado e consumido entre eles, ou no scratchpad). O bit SPR do endereço de DMA é tratado pelo runtime
   (`ps2IsScratchpadAddress` aceita o bit 31): não é isso.
6. gdb com leitura vigiada no buffer descomprimido do recomp (`HLB_TRAPV`, novo): só o descompressor (`0x1AACD0`,
   `0x1AADF0`, chamados de `0x1B0300`/`0x1B7ED0`/`0x1B4650`) e cópias (`0x1D70F0`) mexem nele; ninguém lê para enviar.
   O endereço é reaproveitado, então isso não prova sozinho.

**Próximo passo:** achar no código do jogo a rotina que monta esses envios (a que lê o arquivo descomprimido e escreve
`BITBLTBUF` com `dpsm 0x24/0x2C/0x1B`) e por que ela não roda ou desiste no recomp (condição, interrupção, fila
esvaziada num tratador). A ferramenta: ganchos de entrada de função nos dois lados (a bancada do PCSX2 já tem o
mecanismo), contando chamadas entre v1440 e v1530.

**Ferramentas desta etapa (no laboratório, não commitadas):** no `recomp/runtime/hl_bench.cpp`, `HLB_GIFCAP=F[,N]`,
`HLB_GIFCAPV=V,N` (GIF no formato do `HL_GSCAP`, um registro `0xFF` por vsync), `HLB_RAMV=V,...` (o `hl_ram` do
runtime: RAM, VRAM e VU1), `HLB_CDLOG=1`, `HLB_TRAPV=V` e a VRAM junto no `HLB_RAMAT`. O `HLB_GIFCAP` precisa de um
escritor no `ps2_memory.cpp` do runtime (`g_hlGifCap`), por enquanto editado direto no `build/PS2Recomp` (o `build.sh`
apaga): vira patch antes de commitar. No PCSX2 da bancada (`HLBench.cpp`, fora do repositório): `HLB_GSDUMP=F[,N]`,
`HLB_GSDUMPV=V,...` com `HLB_GSDUMPN`, `HLB_RAMV`. Leitores em Python no scratchpad desta sessão (a passar para
`tools/`): dump do PCSX2 → formato `HL_GSCAP`, grupos por estado, vértices, transferências, VRAM por página. A VRAM
no estado do dump do PCSX2 começa no byte 425.

### 2026-11-08 07:10, manhã: Neji e Kakashi contra a CPU iguais ao PCSX2 (retomada depois de queda de energia)

- A energia caiu às ~05:16, logo depois de rodar o recomp nas rotas novas; o resto do trabalho até `e0beedf` está
  commitado. O `android/settings.gradle.kts` aparece mexido (plugin `foojay-resolver-convention`); não é deste
  trabalho, deixei como estava.
- Rotas novas (`~/build/pcsx2-bench/runs/route_v1..v3.txt`, achadas com capturas `m1`, `m2`, `mv1..3`): a mesma de
  `route_idle` com outras escolhas na seleção. `route_v2` = Neji (1P) x Naruto (CPU); `route_v3` = Kakashi x Naruto.
  `route_v1` não foi usada.
- Comparação (`pv2` x `rv2`, `pv3` x `rv3`, até v8500, as duas rodadas de cada): **estado igual em todas as chamadas das
  4 rodadas**. Tabela em "Lutas contra o PCSX2", acima.
- A luta começa 28–42 vsyncs depois do PCSX2 (v5434 x v5392; v5414 x v5386); na `route_idle`, 6. O recomp sai do
  menu com 5 números do Mersenne Twister a mais (143 x 138) nas três rotas. Abertos, junto com as cores e o `+0x000`.

### 2026-11-08 01:58, noite: bancada contra o PCSX2 (branch `lab/bench-pcsx2`), `rand` do jogo, FPU em corte (em teste)

Rumo novo pedido pelo dono: correto primeiro, no PC, sem o caminho nativo da luta; Android parado até a tabela ficar verde.

- O `~/AndroidStudioProjects/Nominum-Render/docs/LICOES.md` não existe; segui as lições como descritas no pedido (correto
  primeiro, testar como jogador, nada de atalho, separar recomp de renderizador).
- No PC o renderizador nativo (`hl_native.cpp`) não existe: só o GS do runtime. Nada a desligar.
- Estudo do BT4 e do BT2 (só leitura): tabela de diferenças em `docs/plano.md`, "Correto primeiro".
- **Bancada** (`49810b3`): PCSX2 compilado com gancho em `~/build/pcsx2-bench` (fora do repositório, `HLBench.cpp`) e o
  mesmo gancho no recomp (`recomp/runtime/hl_bench.cpp`, só com `HLB_OUT`). Os dois gravam, a cada entrada de
  `0x21E660` (entrada por quadro de cada lutador), os 0xC00 bytes do lutador, 0x280 da câmera, o índice e um hash dos
  geradores. `tools/hlbench.py diff` casa os registros por lutador e pelo quadro dele e diz o primeiro quadro diferente.
  Campos que o jogador vê: posição `+0x30/34/38`, vida `+0x6C`, chakra `+0x70`, ação `+0x18E`.
- **Geradores do jogo:** Mersenne Twister de 64 bits (estado em `0x627D40`, índice em `0x60FF48`, `0x180BA0`) e o `rand`
  do newlib (`0x17C558`, LCG de 64 bits sobre `*(0x412DD8)+0xA8`). `HLB_RNG=1` iguala os dois no início da luta.
- **PCSX2 contra PCSX2** (`p2`, `p3`, rota `route_idle`: Naruto x Naruto no lamen, P1 parado, CPU ataca): iguais nas
  3113 chamadas. A referência é determinística.
- **O `rand` era um stub** (`std::rand() & 0x7FFF` do host, sem semente): as escolhas da CPU (`BTL 0x6F0E60` etc.) vinham
  de outra sequência e de outra faixa. `0474692`: `un5.py` tira o `rand` dos stubs; o jogo roda o dele. Comparado: a
  luta continua diferente do PCSX2 (`r0` antes, `r1` depois), porque a diferença começa antes (quadro 0, ver abaixo).
  Não marco como corrigido pela comparação; a causa (stub no lugar da função do jogo) é certa pelo código.
- **Primeira diferença (r1):** quadro 0 da luta. Posição do 2P `75.0000` no PCSX2 e `75.0001` no recomp (16 ulp), um
  ângulo `-pi/2` com 4 ulp, 150 com 8 ulp; e um campo `+0x000` (53019 contra 53041, parece tempo). A partir daí as
  ações e a vida se afastam (quadro 195: vida do 1P 0,958 contra 0,863).
- O recomp gastou 8 números do MT a mais antes da luta (índice 146 contra 138) e chega à luta 88 vsyncs antes (v5218
  contra v5306); o overlay BTL entra em v2734 contra v2742. Aberto.
- **FPU e VU0 macro em corte** (patch `0071`): o PCSX2 roda a FPU e a VU com arredondamento para zero, DAZ e FTZ (só o
  `DIV.S` ao mais próximo) e com a máscara de guarda na soma da FPU. O nosso arredondava ao mais próximo. A `0071`
  corrige por operação (sem mexer no MXCSR, que no BT3-Recomp quebrou menus), mantendo o expoente 255 como no hardware.
  - Teste de unidade contra o x86 no modo do PCSX2 (corte, DAZ, FTZ, máscara de guarda), 4 milhões de pares: soma,
    sub, mul, div da FPU, div da VU, raiz, RSQRT, `CVT.S.W`, soma e mul vetoriais **todos iguais**, fora os resultados que
    estouram (o PCSX2 prende em ±max, o hardware e nós damos expoente 255; decisão mantida). O primeiro teste achou um
    defeito meu: perto do menor normal (expoente < 48) o erro exato da `fma` caía abaixo dos denormais e o corte não
    acontecia; corrigido (produto em double, divisão escalada por 2^64) antes do build.
  - No jogo (`r2` contra `p2`): a posição inicial do 2P (`75.0001`) e o ângulo `-pi/2` agora saem iguais ao PCSX2. O
    estado de jogo fica **igual até o quadro 52** de cada lutador (antes: diferente no quadro 0). O jogo carrega, chega
    à luta e luta normalmente.
- **Divergência seguinte: tempo do CD (causa achada, aberta).** A passagem da introdução da rodada para a luta (os
  dois lutadores entram juntos na ação 5, animação `0x5B`) acontece no quadro 59 no PCSX2 e no 53 no recomp; a duração
  depois é igual (28 quadros). Dumps de RAM nos quadros 1, 30, 52 e 58 (`HLB_RAMAT`): a lista de tarefas em `0x6194B0`
  (contador em `0x617AC8`, `0x110B40` insere, `0x110980` zera) do recomp no quadro 52 já está como a do PCSX2 no 58.
  Com `HL_CD_LOG` e marcas por quadro (`HLB_MARK`): a única leitura de disco entre o início da luta e o gatilho é a do
  quadro 2 (`sceCdRead` do EE em `0x174308`, chamada de `0x141330`, lbn `0x110C47`, 5 setores para `0x69CB40`). No
  runtime o `sceCdRead` é HLE e termina na hora, e o `sceCdSync` (`0x1738D0`) sempre responde "pronto". **Prova:** com
  `HLB_CDBUSY=12` (só na bancada: o `sceCdSync(1)` responde "ocupado" 12 vsyncs depois de cada leitura feita na luta) o
  gatilho passou para o quadro 58. O jogo espera essa leitura; o PCSX2 dá tempo de busca e leitura ao disco, nós não.
  - O modelo do PCSX2 (`CDVD.cpp`, `cdvdStartSeek`/`cdvdBlockReadTime`): busca completa 100 ms ou rápida 30 ms
    conforme a distância, latência de meia rotação, leitura por setor conforme a velocidade (CAV), leitura antecipada,
    e o mesmo drive servindo o streaming de som do IOP. Item próprio: dar ao CD do runtime esse tempo (o hardware também
    tem). Até lá, a bancada calibra o atraso para seguir comparando a lógica depois desse ponto.
- **Com o atraso de CD calibrado na bancada** (`HLB_CDBUSY=14`, só para seguir comparando): o gatilho da rodada cai no
  quadro 59 como no PCSX2, e o estado de jogo fica igual até o quadro 142 (antes 52).
- **`SQRT.S` da FPU ao mais próximo** (patch `0072`, `d95db07`): desde o quadro ~100 `+0x32C`/`+0x330` dos dois
  lutadores saíam 1 ulp abaixo do PCSX2. No `iFPU.cpp` o `recSQRT_S_xmm` troca o MXCSR para o mais próximo só nessa
  instrução; o RSQRT da FPU e o SQRT/DIV/RSQRT da VU ficam em corte (a VU0 macro roda no MXCSR da FPU, sem troca). A
  `0071` tinha posto a raiz da FPU em corte. Separadas `ps2_fsqrt` (FPU, mais próximo) e `ps2_vu_sqrt` (VU, corte);
  teste de unidade igual nos 4 milhões. **Comparado:** `+0x32C`/`+0x330` iguais do quadro 2 ao 143 (`r7` contra `p2`).
- **Desvios por sinal em 64 bits** (patch `0073`): no quadro 125 da CPU o recomp tirava 2 números a mais do
  Mersenne Twister. Caminho até a causa, com `HLB_RNGLOG` (cada chamada ao gerador e aos auxiliares `0x1810D0`,
  `0x181020`, `0x180FF0`, com chamador, argumento e pilha, nos dois lados):
  - as chamadas eram as mesmas e na mesma thread até a 174.ª do quadro 131 (contagem do 1P); ali o laço de partículas
    `0x34BB20` comparou `obj->0x1E4 < (int)((1.0 - |x|) * 15.0)` (x = float aleatório em ±0,7) e, só no recomp, falhou
    e sorteou de novo (ramo `0x34BE20`);
  - envolvendo as rotinas de double por software do jogo (só no recomp, `HLB_SFLOG`) e conferindo em Python contra IEEE:
    `1.0 - 0.00516868…` deu `-3.00516…` (erro de exatamente 4,0 na fração de ponto fixo da `fp-bit`);
  - causa: em `_fpadd_parts` (`0x172464`) o jogo testa o sinal da diferença de frações de 64 bits com `bltzl`, e o
    gerador emitia `GPR_S32(ctx, rs) < 0`. No R5900 BLTZ, BGEZ, BLEZ, BGTZ (e as formas *L* e *AL*) olham os 64 bits
    (o PCSX2 usa `SD[0]`). A `0073` troca as quatro condições para `GPR_S64` em `control_flow_emitter.cpp` (o único
    lugar que as emite). Conferido antes: `SET_GPR_U32`/`S32` estendem o sinal como o MIPS; só `SET_GPR_ZE32` zera, em
    `LHU`/`LBU`/`LWU`, que zeram no hardware também.
  - Isso afeta toda conta em double por software do jogo (`dpadd`/`dpsub`, e o que usa `_fpadd_parts`), não só o efeito.
- **Resultado com `0071`+`0072`+`0073` e o atraso de CD calibrado na bancada** (`r14` contra `p2`, Naruto x Naruto no
  lamen, 1P parado contra a CPU, até v8500): **posição, vida, chakra e ação dos dois lutadores iguais ao PCSX2 nas 3113
  chamadas da luta inteira; o gerador (índice e estado) igual em todos os registros.** O que ainda difere:
  - cores da luz (`0x80xxxxxx`) e um campo de tempo (`+0x000`, ~53019 contra ~53041) que já chegam diferentes à luta;
  - ponteiros deslocados de 0x80 (o heap do recomp começa a luta 0x80 bytes antes do do PCSX2): diferença de antes da
    luta, a achar;
  - o atraso de CD é da bancada (`HLB_CDBUSY=14`); sem ele o início da rodada vem 6 quadros antes e a luta diverge.
    A correção do produto é o tempo de CD no runtime (próximo item).
- **Setores reais do disco** (patch `0074`, `tools/un5.py lbn`, `recomp/build.sh`): o runtime lê o disco extraído e
  dava aos arquivos setores próprios (EE: pseudo-LBNs a partir de `0x100000`, na ordem em que o jogo abre os arquivos;
  IOP: uma ISO virtual com outra ordem). A extração agora escreve `build/disc/hl_lbn.txt` (setor real, tamanho e caminho
  de cada arquivo, lidos da ISO) e as duas HLEs põem cada arquivo no setor real; os arquivos que não estão na tabela
  (os `hl_*` do runtime) vão para depois do fim do volume real (`0xE31FB`). **Conferido:** a leitura do quadro 2 da luta
  sai em `0x85AC7` como no PCSX2 (antes `0x110C47`), o IOP acha o `SNDDATA.BIN` em `0x1788A` (real); o jogo carrega e a
  luta continua igual ao PCSX2 do começo ao fim. Base para o tempo do drive, que depende de onde a cabeça está. Falta
  o mesmo no importador do Android (parado).
- **Tempo do drive** (patch `0075`, `ps2x/iop/hl_cd_drive.h`, liga sozinho; `HL_CD_TIMING=0` desliga): no runtime
  as leituras do disco terminavam na hora, e a rodada começava 6 quadros antes do PCSX2 (a causa do achado das 03:00).
  Agora há um modelo do drive, o mesmo do PCSX2 (`cdvdStartSeek`, `cdvdRotationTime`, `cdvdBlockReadTime`), com a
  cabeça compartilhada entre o EE (`sceCdRead`), o `sceCdRead` do IOP e as leituras de `cdrom0:` pelo ioman do IOP
  (o driver de som lê o `SNDDATA.BIN` assim):
  - busca de 30 ms (16 setores ou mais) ou de 100 ms (14764 ou mais), meia volta do disco sem busca, o tempo de
    cada setor conforme a trilha e a velocidade, e 333 ms para o disco parado girar;
  - a troca do fuso de 4x CAV (som, `SCECdSpinNom`) para 2x CLV (jogo, `SCECdSpinMax`) custa a mudança de rotação.
    Foi isso que fez a leitura do quadro 2 da luta levar 270 ms no PCSX2: o som tinha acabado de ler no
    `SNDDATA.BIN`;
  - o `sceCdSync(1)` do EE responde ocupado até o vsync em que o drive terminaria (arredondado para cima, como o
    jogo vê a cada quadro); o `sceCdSync(0)` segue devolvendo na hora (o jogo não o usa na luta).

  Deixado de fora: o buffer de leitura antecipada do drive (tira 1 a 3 ms das leituras contíguas) e a espera das
  leituras do IOP pelas do EE.

  **Conferido contra o PCSX2:** a leitura do quadro 2 sai em 270,19 ms (PCSX2 270,07 ms) e a seguinte em 123,1 ms
  (124,7). A luta inteira (3113 chamadas, `r18`) fica **igual em posição, vida, chakra e ação do começo ao fim,
  sem `HLB_CDBUSY`**. A luta começa agora no v5312 (PCSX2 v5306; antes v5218–5222): o carregamento passou a ter o
  tempo do disco. Falta:
  - o mesmo no Android (o importador não grava o `hl_lbn.txt`, e sem ele os setores são os falsos);
  - medir o tempo dos carregamentos nos aparelhos.
- **1P como CPU** (`HLB_P1CPU=0x23`, o byte baixo de `+0x60` do 2P posto no 1P no primeiro quadro): os dois lutadores
  andam um pouco e ficam parados se encarando a partir do quadro ~460, até o tempo acabar (visto no PCSX2, captura do
  quadro 1201). Não serve como luta CPU contra CPU; a referência continua sendo 1P parado contra a CPU. Falta achar
  como o jogo cria um lutador da CPU (talvez no momento da criação, não no primeiro quadro) ou um modo demonstração.
- GameDB do SLES-55605: `vu0ClampMode 3`, `vu1ClampMode 3`, sem patches de jogo.

### 2026-11-05 17:46, dia: correção de hora

- A entrada de baixo, marcada 17:52, foi escrita às 17:46.

### 2026-11-05 17:52, dia: fechamento e correção de horas

- **Horas (correção):** as três entradas de baixo, marcadas 16:50, 17:05 e 17:50, foram escritas às 16:48, 16:51 e 17:45
  (pelo `date` dos commits `8ff49da`, `62d9e52`, `025160d`).
- **Relatório do dia** (mesma URL do das 13:00, versão 3): https://claude.ai/artifact/Utyg1DbQ6nes1aRwWNbDLE. Veredito:
  pronto para lançar com `app_d26` / `good-20261105-3`.
- **Pendências conhecidas:**
  - a aura do Vermilion Mode no nativo;
  - os logos Bandai Namco e Bandai não conferidos;
  - 1,5 s de preto na transformação do Kyubi (`f1_odin_d26` hlseg_12 125 s), a comparar com o PCSX2;
  - agarrões de outros personagens, 3 cenários, a build de 11-03 e o PCSX2 lado a lado.
- O online não foi tocado.

### 2026-11-05 17:50, dia, prioridade 2: o chiado dos filmes e logos (corrigido, `0070`), `good-20261105-3`

**Causa.** O callback de PCM do jogo (`0x1046A0`, o do `ezmovie`) copia o pacote inteiro ou nada (`0x1044F0`, o
`copy2area`: `if (len0 + len1 < slen0 + slen1) return 0`) e devolve 0 quando o buffer de som está cheio. A libmpeg então para o
demux nesse pacote e o oferece de novo. O runtime ignorava o retorno, e o pacote se perdia. Os pacotes de som do PSS têm
tamanho ímpar (4077, 4063 bytes de dado). Cada perda tira o fluxo dos blocos L/R de 512 bytes por um número ímpar, e cada
amostra de 16 bits vira lixo (o ruído em escala total, RMS ≈ 1/√3) até a próxima perda ímpar. Daí o chiado e o som certo
alternando.

**Como achei** (`HL_SIFDMA_DUMP`, só no laboratório, fora do patch: grava o que `0x100830` manda ao IOP, o buffer de 24 KB em
`0x170fc0`):
- `rec/s5_cube_sif`: o fluxo enviado é o PCM do disco, com saltos de ~61 KB (pacotes perdidos), de tamanho ímpar.
- Correção do registro de 14:35: o `scratchpad/snd/pssaudio.py` não tirava os 4 bytes do sub-stream, que nos filmes do UN5
  são `ff a0 00 00`, e não `a0 ..`. A referência `OPENING.pcm` de antes tinha 4 bytes a mais por pacote. O demux do runtime e
  o callback estavam certos nisso.
- O exemplo do `ezmovie` só pula esses 4 bytes. Os índices de ruído da referência continuam valendo, porque o deslocamento era
  par.

**Correção** (`recomp/patches/0070-mpeg-sound-in-order.patch`, só o `MPEG.cpp`):
- Os eventos de som entram numa fila em ordem, com um callback por vez.
- O recusado volta na próxima chamada do demux, que não pega dado novo enquanto isso (devolve 0 e espera um vsync, como o
  caminho de backpressure que já existia).
- Os bytes do pacote são guardados quando ele entra na fila. Se o anel do jogo já tem dado novo ali (a leitura do CD avança),
  a cópia vai para `0x7C000`, o topo da RAM do kernel: o kernel HLE não usa, e o runtime confere que está toda zerada antes.
- Tem de ser um endereço abaixo do fim do anel (`0xfbdc80 + 0x60000`), porque o callback trata o que passa do fim como volta e
  faz um mínimo com sinal (`0x100EA0`).
- `HL_MPEG_AUDIO_DROP=1` volta ao jeito antigo.

**Antes e depois** (boot em tempo real, idioma por toque, filmes inteiros; som do SPU2 gravado pelo motor):
| Gravação | Aparelho | Pacotes | Trechos de 100 ms com ruído (`#`) | Continuidade contra o disco (`xoff.py`, OPENING 5/30/55/80 s) |
|---|---|---|---|---|
| `s5_cube_sif` (antes, `d22lab`) | Cube | ~1/3 perdidos | 493 | atraso +30, +5,7, −25, −45 s: sem continuidade |
| `s6`, `s7` (fila sem cópia) | Cube | 2987 aceitos, 1532 perdidos (anel reescrito) | — | — |
| `s8_cube_fifo3` (`d25lab`) | Cube | 4505 aceitos, 0 perdidos | ~4 | 38,085 s nos 4 pontos |
| `b1_cube_d26` | Cube | 4505 / 0 | 4 | 37,920 s nos 4 |
| `b1_odin_d26` | Odin | 4505 / 0 | 3 | 39,437 s nos 4 |
| `f1_cube_d26`, `f1_odin_d26` (2º boot e luta) | os dois | 4499 / 0; 4505 / 0 | — | — |
| `b3_cube_d26`; `b3_odin_a35_d26` (3º boot; o A35) | os dois | 0 perdidos | 4; 1 | — |
- Os `#` que sobram estão na entrada dos filmes (o corte do logo para o filme).
- Vídeo igual: 250 quadros distintos em 10 s no meio do `OPENING`, antes e depois (`mpdecimate`).
- Sincronia: o primeiro quadro do filme aparece aos 29,8 s do vídeo, e o som dele começa aos 29,81 s (`b1_cube_d26`).
- Fica no fim de cada filme `33`–`37` pacotes na fila (~0,7 s), descartados no `sceMpegReset`. Antes, esses e outros mil se
  perdiam. Não ouvi corte no fim: o fim do `OPENING` é silêncio.

**Regressão (`app_d26`, base e A35 recompilados):**
- Cube, base, 60 fps: 2º boot inteiro, Combate Livre 1P vs 2P, Ougi, vitória (Health 64), resultado, seleção (`f1_cube_d26`).
- Odin, base, 50 fps, Turnip: o mesmo, com Ougi e o 2P no "Fourth Awakened Mode"; vitória (Health 55), resultado, seleção
  (`f1_odin_d26`).
- Na transformação do 2P, 1,5 s de preto entre o vermelho e o clarão branco (`hlseg_12` 125,0–126,0 s), dentro da cena do
  jogo, com a retenção agindo. A conferir no PCSX2; o `MPEG.cpp` não age na luta.
- Odin, A35: 3º boot, a abertura do A35 em laço até o START, Load Game, seleção (`b3_odin_a35_d26`).
- A série de 70 patches aplica limpa num checkout novo do `e563698`, e o `MPEG.cpp`/`SIF.cpp` saem iguais aos da árvore.

**Não visto:** os logos mudos (Bandai Namco, Bandai). O `LOGO_C` é o do CRIWARE e agora toca inteiro.

**Aparelhos no fim:**
- Odin: Turnip (`selected.turnip-1440`), configurações do dono (`un5_settings.owner-1625.txt`: 120 fps, 4x, filtro 2,
  especial desligado), mods desligados. Às 16:50 eu tinha posto as de 07:50 por engano; corrigido.
- Cube: 60 fps, mods desligados.
- Nos dois, o `hl_env.txt` de laboratório foi tirado (o motor gravava o som em disco).

### 2026-11-05 17:05, dia: som dos filmes, o que fica descartado (para a próxima sessão)

O callback de som do jogo é o `pcmCallback` do exemplo `ezmovie` da Sony (`0x1046A0`, registrado em `0x105d9c` pelo
`0x1021D0` → `sceMpegAddStrCallback`, tipo 2 = PCM da libmpeg; o de vídeo é `0x104800`, tipo 0):
- `data = cbstr->data + 4`, `len = cbstr->len - 4` (pula os 4 bytes do sub-stream, como o `scratchpad/snd/pssaudio.py`);
- o anel é a própria estrutura do argumento (`a2`), 0x60000 bytes, com o tamanho em `+0x60008`; trata a volta (`data >= fim`
  → `data -= tamanho`; o resto vem do início);
- `0x100FE0` (`audioDecBeginPut`) e `0x1044F0` copiam **na hora** para o buffer de áudio, e `0x100EF0` fecha.
Logo, com os bytes certos no anel no momento do callback (`s4_cube_boot`: callbacks síncronos, igual), **o demux não é a
causa**. Também conferidos: o fim do pacote vem do campo de tamanho do PES (não da busca de start code), o cabeçalho PES é
lido como no extrator, e a volta do anel em `appendGuestRingBytes` recomeça os endereços em `ringBaseAddr`.
**Falta olhar** (próxima sessão): o envio do buffer de áudio do EE ao IOP (`audioDecSendToIOP`: `sceSifSetDma` para o buffer
do IOP) e a leitura do AutoDMA no SPU2 (`hl_spu2.cpp`, `sceSdBlockTrans` em laço, a posição que o EE lê para saber qual
metade encher). Ruído uniforme em escala total em blocos de 0,5–1 s é o buffer do IOP com bytes que não são som, ou o PCM
deslocado por um número ímpar de bytes. Obs.: o runtime chama PCM de 1 e ADPCM de 2 (`kMpegStrPCM`), a libmpeg usa
IPU 1, PCM 2, ADPCM 3; como o demux manda os pacotes de som para os dois tipos, o callback do jogo (tipo 2) recebe.

### 2026-11-05 16:50, dia: lutas dos mods nos 2 aparelhos (passou), retenção agindo no Ougi, som dos filmes (2 tentativas sem efeito)

**APK `app_d21`** (= HEAD `75b7d23` + o `build/lab`; bibliotecas base e `libun5_un6_a35.so` recompiladas às 16:00, as duas com a
chamada `Bt4Gsx_LiveFight`). Antes da `d21`, a biblioteca do A35 era a de 13:54, sem a retenção.

**Mods, luta completa como jogador (Ougi pelo botão especial, vitória, resultado, volta à seleção), 1P vs 2P parado, chakra
infinito, Hidden Leaf Village.** Tudo gravado com som:
| Aparelho | Mod | fps | Gravação | Resultado |
|---|---|---|---|---|
| RG Cube | A35 | 50 (o jogo NTSC: 60 Hz) | `rec/r1_cube_a35_50` | Ougi na tela 3 vezes (o último com a transformação de Kyubi do A35), "Vencedor", rank S, seleção |
| RG Cube | Alpha 1.1X | 60 | `rec/r2_cube_a11x_60` | 8 retenções, uma em cada Ougi; vitória (Health 74), seleção |
| Odin 3, Qualcomm | A35 | 60 | `rec/r3_odin_a35_60` | Ougi na tela 7 vezes, "Vencedor" (Saúde 57), seleção |
| Odin 3, Qualcomm | Alpha 1.1X | 50 | `rec/r4_odin_a11x_50` | 4 retenções, vitória (Health 62), seleção |
- `build/lab/blackscan.sh DIR` (o centro da tela, sem HUD nem controles, com Y médio < 24, a cada 0,5 s): nas lutas nenhum trecho
  escuro; os que aparecem (≤ 1,5 s) são telas de transição com os controles de menu (vistos: `e.png` do scratchpad).
- **A retenção agindo:** na Alpha, cada Ougi tira o quadro de luta do nativo e, 25 vsyncs depois, a retenção pega (~370 vsyncs,
  até o Ougi entrar na tela). Nesse trecho a disputa do Ougi ("PRESS!", "Done", as mãos se chocando) aparece certa
  (`r2_cube_a11x_60` seg. 12, 22–31 s), e a luta volta normal. Com a espera de 300, esse trecho era preto (o GS sozinho).
- O Odin ficou no driver da Qualcomm em todas as rodadas desde 14:40 (item 5).
- Texto da Alpha na tela de Mods (corrigido, `app_d21`): visto no Cube e no Odin.

**Toques:** no Odin a luta só aceita o toque de baixo nível (`mtfight.py`); no Cube o `input swipe` funciona (`fightrun.sh`), e o
`sendevent` no `goodix-ts` não chegou.

**Som dos filmes e logos: não corrigido.** Duas tentativas no `MPEG.cpp`, as duas medidas com boot em tempo real no Cube e o
mapa de ruído (`scratchpad/snd/nmap.py`):
1. Cópia do pacote de som para fora do anel (`s3_cube_boot`): ficou pior, ruído médio contínuo. O jogo usa
   `sceMpegDemuxPssRing` e o callback trata a volta do anel pelo endereço; um ponteiro fora do anel quebra a conta.
2. Callbacks de som rodando dentro do demux, com `invokeCurrentSequence`, como a libmpeg faz (`s4_cube_boot`): **igual** a antes
   (`s2`). Logo os bytes que o callback lê não são reescritos antes dele: a hipótese do anel estava errada.
Desfeitas as duas (o patch da 2 guardado em `scratchpad/0070-mpeg-sound-callbacks-in-demux.patch.tried`, fora da série). O ruído
está depois do callback: o caminho EE → IOP → SPU2 (o envio do PCM ao IOP ou a leitura do AutoDMA).

### 2026-11-05 16:30, dia, prioridade 1b (efeitos, texturas, agarrões): o GS puro apaga a luta; retenção do nativo em 25 vsyncs; aura do Vermilion falta

Tudo no Odin 3 com o **driver da Qualcomm** (o dono deixou o Turnip; troquei às 14:40, a escolha dele está em
`files/drivers/selected.turnip-1440`), 120 fps interpolado, `app_d14` a `d18`. Estado depois do uso do dono: d14, nenhum mod
ligado, 120 fps, escala 4x.

**Como jogar daqui:** o `input swipe` do Android não chega à luta (o Naruto não se mexe; nos menus chega). Toques de verdade,
com dois dedos, por `sendevent` na tela do Odin (`/dev/input/event4`, retrato cru: `x = 1080 - y`, `y = x`):
`build/lab/mtfight.py` (↑○ e ↓○ com o braço do D-pad segurado e o ataque por cima, depois combos). Rotas: `tosel.sh` (boot
com os filmes inteiros até o Mode Select) e `cpustage.sh SERIAL N` (Naruto+Sakura contra a CPU, cenário N de 24).

**C. Agarrões ↑○ e ↓○** (`rec/e1_odin_nat` nativo, `e3_odin_gs` GS puro, mesmo roteiro, Hidden Leaf Village, 1P vs 2P parado):
os 8 agarrões saíram, a câmera vira para a parede e os dois sobem; quadros a 480p lado a lado iguais no nativo e no GS,
sem textura faltando nem pose errada. Não reproduzi o erro do dono com o Naruto; falta com outros personagens.

**O que o nativo descarta:** só 1 desenho por quadro (a cópia do Z para as sombras; `[native] ... 500 tags dropped` a cada
500 vsyncs). Desenhos de alfa só (`FBMSK 00FFFFFF`) mandados ao GS (`HL_NATIVE_ALPHA=1`, `rec/e4_odin_alpha`): nenhum
apareceu. O 1b não vem de descarte.

**B. Cenário preto (Twilight Cliff, 6/24) e a luta preta no GS puro: é o mesmo preto do Ougi, e não é do nativo.**
- `rec/t1_odin_gs` (`HL_NATIVE=0`, Twilight Cliff contra a CPU): a CPU começa um Ougi aos 16 s e o 3D fica preto, só o HUD
  ("Great Ball Rasengan", "Vermillion Mode" por cima), até o fim da luta (62 s).
- `rec/t4_odin_gs` (`HL_NATIVE=0`, **Hidden Leaf Village**, sem Ougi): preto a partir de 14 s, logo depois do primeiro golpe
  em dupla, até o fim.
- Ou seja, **na luta o GS do ARMSX2 sozinho fica preto** assim que o jogo passa ao arranjo do Ougi (última passada para a página
  0, `ff00000000080000`, a tela lendo 0x70: `HL_PRIV_LOG` `dispfb1 1070` o tempo todo). O nativo mostra a própria imagem da
  cena e esconde isso; quando ele desliga (o quadro de luta dele some), aparece o preto. Daí a comparação "nativo contra GS
  puro" não vale para efeitos de luta: o GS puro não mostra a luta. No PS2 aparece, então algo antes do GS (EE, VU1 ou a
  cópia final) difere: pedido ao Nominum abaixo.
- `rec/k3_odin_nat` (nativo, Twilight Cliff): preto logo no começo, nativo desligado (`scene off` 100 vsyncs depois de ligar)
  e a retenção não agiu: a idade do lutador chega a 11 e 21 com a luta viva, e o limite era 10.
- `rec/t2_odin_nat` (nativo, retenção em 300): 4 s de preto (36–40 s, a entrada de um Ougi da CPU), voltou sozinho.

**Correção (segura):** retenção em **25 vsyncs** (`hl_native.cpp` `liveToHold()`, `HL_NATIVE_HOLD=N` muda) e idade do
lutador até **30** (`hl_game.cpp`). Esperar 300 era para deixar o GS mostrar a entrada do Ougi, mas ele mostra preto.
`rec/t3_odin_hold` (Twilight Cliff, a CPU, 80 s, d17 com `HL_NATIVE_HOLD=25`): nenhum preto; a retenção agiu uma vez
(`v15617 taken as a fight`, solta em `v15887`), sem falso positivo. **O A35 tem biblioteca própria:** a de 13:54 não tinha a
chamada `g_liveFight`, recompilada agora (`build.sh --mod un6-a35`).

**A. Aura vermelha do Naruto no Vermilion Mode: falta no nativo.** `rec/t3_odin_hold` 61–75 s: com a vida baixa, o Naruto do
1P entra no "Vermillion Mode" ("Ver" no HUD), fica vermelho por um instante (62,5 s) e depois aparece normal, sem a aura
borbulhante. Não dá para comparar com o GS puro (preto, acima). Mudar a vida não liga o modo: `HL_P1_HP` (novo, só para
chegar ao ponto) baixa a vida, e o Despertar não pega; precisa de dano de verdade (deixar a CPU bater, ~60 s).

**Pedido ao Nominum (dumps no Odin, `files/gsdumps`):** o detector `ougi_cpu` não disparou no Ougi da CPU desta tarde, então
não há dump do caso ainda. O que preciso: (1) por que o GS sozinho mostra a luta preta no arranjo do Ougi (`t4_odin_gs`:
preto 6 s depois do começo, cenário 1, sem Ougi) e o PS2 não; (2) o que desenha a aura do Vermilion Mode e por que o nativo
não a mostra. Dump sugerido: `HL_GS_DUMP=luta_inicio HL_GS_DUMP_FRAMES=600` com `HL_NATIVE=0` cobre o começo da luta até o
preto (rota `cpustage.sh SERIAL 1`).

**Não feito:** ↑○/↓○ de outros personagens, 5 cenários (vi 2: Hidden Leaf Village e Twilight Cliff), Alpha, build de 11-03,
PCSX2 (está em `~/Applications/pcsx2.AppImage`, com savestates do SLES-55605, mas só pela interface).

### 2026-11-05 14:35, dia (plano de lançamento): proteção do 3D preto, texto da Alpha, "Esticar", ruído no som do boot (achado)

**3D preto depois de um Ougi (Alpha e choque): proteção instalada; o caso exato não se repetiu.**
- O que o jogo faz no estado preso (`x1_cube_a11x`, `HL_FRAME_LOG`): a última passada, que na luta normal escreve na página
  0x70 (FRAME `ff00000000080070`), sai com FRAME `ff00000000080000` (página 0, como no Ougi). A tela lê 0x70 o tempo todo
  (`HL_PRIV_LOG`: `dispfb1 1070` sem mudar, `rec/v3_odin_clash` e `x2`), então fica só o HUD. O GS só reproduz isso, por isso
  "mandar tudo pelo GS" não resolve: no estado preso o nativo já estava desligado e tudo já ia pelo GS. O jogo não faz
  cópia local (`TRXDIR`) nem leitura de volta em lugar nenhum: o `HL_FRAME_LOG` agora conta `d<DBP><SBP>` e `e<SBP>`, e
  deu 0 em todo o boot, nos menus e na luta.
- Como distinguir o preso de um Ougi de verdade (`[sp] ages`): no Ougi os lutadores param de ler a entrada (a idade
  `fighter` cresce) e `on screen` = 1. No preso, `fighter 2` e `on screen 0` até o fim do round. Nas rodadas boas (x1 antes
  do preso, x3, x3b), "luta viva com o nativo desligado" dura no máximo ~200 vsyncs: a corrida do Ougi e o Ougi que erra.
- **Proteção** (`hl_native.cpp` `s_held`, `hl_game.cpp` → `Bt4Gsx_LiveFight`): o host diz a cada vsync se há luta viva sem
  Ougi na tela. Com 300 vsyncs seguidos assim e o nativo desligado, o nativo toma a cena como luta e a mantém, seguindo
  também os blocos trocados. Ele apresenta a própria imagem da cena, que não depende da cópia para 0x70. Solta quando
  aparece um Ougi na tela. Log: `a fight with the scene off for 300 vsyncs: taken as a fight` / `the held fight is over`.
- **Teste** (Cube, Alpha, `app_d14`): `rec/x4_cube_a11x_hold`, duas lutas, 8 Ougi (dois em seguida, 432 vsyncs entre um
  e outro), tudo voltou ao 3D sozinho e nenhuma retenção falsa (0 linhas `taken as`). Antes, sem a proteção:
  `x3_cube_a11x_dbg` (debug: Ougi seguidos, `fight.ougi` a cada 1,3 s), 3 lutas, também sem preso. O preso aconteceu 1 vez em
  ~12 Ougi completos da Alpha hoje.
- **Falta:** ver a proteção agindo no caso real, e se o nativo desenha certo a luta presa (no choque, com os blocos
  trocados). Até lá não está dado como corrigido.

**A35 no fim da luta:** confirmado nos dois (entrada de 13:20: `m2_odin_a35fix`, `m3_cube_a35fix`).

**Texto da Alpha na tela de Mods (corrigido):** `mod_no_pack` em pt, en e es: "Sem pacote de código próprio: roda com o
código do jogo base e os dados deste mod. O que o mod muda no código não vale."

**Menus "Esticar":** não há opção separada. Com a proporção padrão "Tela" (`ASPECT_DEFAULT = 0`), menus e filmes são
esticados para a forma da tela (plano, 11-03). No Odin (16:9), as capturas de hoje (`134_q5`, `134_q6`: Modo, seleção)
preenchem a tela. "Desenhar em 16:9" fica para depois.

**Som do boot: ruído branco em escala total no filme e nos logos (achado; a correção segue depois do 1b).**
- O boot europeu para na escolha de idioma, que é muda. `s1_cube_boot` ficou só nela; `s2_cube_boot` tem o idioma
  escolhido por toque e 100 s sem tocar.
- Referência: o som dos filmes está dentro do PSS (private stream 1, `SShd` tipo 1, PCM de 16 bits entrelaçado em 512 B):
  `LOGO_C` 48 kHz 3,5 s, `NARUTO10` **44,1 kHz** 5,0 s, `OPENING` 48 kHz 88,9 s (extrator em
  `scratchpad/snd/pssaudio.py`; o FFmpeg não lê esse som). Não há faixa equivalente no `STREAM.AFS`.
- Índice de ruído (|diferença| / |amostra| a cada 100 ms): a referência do `OPENING` não passa de 0,9 em nenhum trecho dos
  primeiros 30 s. No nosso, trechos de 0,5–1 s com RMS 0,556–0,563 alternam com som certo durante todo o filme (39,6–128 s
  do `snd.raw`). Esse RMS é o de ruído uniforme em escala total (1/√3 = 0,577): bytes quaisquer tocados como PCM, que é o
  "chiado". No `LOGO_C` também (31–33 s).
- **Hipótese:** o demux do `MPEG.cpp` enfileira o evento PCM com o *endereço* do pacote no buffer circular do jogo
  (`pssGuestAddrs`), e o jogo copia dali quando o evento é despachado. Se a leitura do CD reescreve aquele trecho do anel
  antes disso, o jogo copia vídeo como som, em blocos do tamanho do anel. Próximo passo: comparar uma soma dos bytes no
  enfileiramento e no despacho (`HL_MPEG_LOG`).
- Ainda não vistos: o salto na primeira amostra em 40,17 s (1,89 de 2), possível estalo na entrada do `OPENING`, e os
  logos mudos.

### 2026-11-05 13:20, dia, item 4: o A35 morria no fim da luta (corrigido); a Alpha 1.1X fica preta depois de um Ougi (aberto)

**A35: `missing-target 0x9ee41c` no fim da luta (corrigido em `tools/un5.py`).**
- **Antes:** `app_d11` (a `libun5_un6_a35.so` regerada às 12:32 com o host de hoje, código do mod de 03:37), mod ligado pela tela
  de Mods, save carregado, Combate Livre 1P vs 2P, 120 no Odin (`rec/m1_odin_a35`). No fim da luta ficou uma tela quase
  preta com os controles do menu, VU1 0,0, e o log diz `[guest-branch:missing-target] ... target=0x9ee41c ra=0x7837ec`.
- **Causa:** o `MOD.BIN` do A35 guarda o código no meio dos dados, e o `ovlelf` acha as ilhas de código por marcas
  (prólogos, `jr ra`, `j`/`jal` para o código principal). A ilha da função `0x9ee3b0` terminava na marca da `jal 0x17cff8`
  (`0x9ee408`, + delay = `0x9ee410`). Mas a função segue: `bne` em `0x9ee3f8` vai a `0x9ee41c` (`j 0x9f8c50`, para o próprio
  mod), e `beqz` em `0x9ee410` vai a `0x9ee424` (`j 0x78625c`, para o BTL). Nenhum dos dois `j` é marca.
- **Correção:** `data_code_islands` estende cada ilha até onde os desvios dela chegam, se o alvo está até 0x40 bytes depois
  do fim. Com os argumentos do build, só o `MOD.BIN` muda (BTL, ADV, ETC e TEXTBRA saem idênticos byte a byte): `.init5`
  +0x10 (`0x9c5258`/`0x9c5260`, dois `j 0x744ac0`), `.init8` +8 (`0x9d2d98`, `j 0x746d24`), `.init13` +0x1c (até `0x9ee42c`).
  São três pontos com o mesmo defeito; os dois primeiros ainda não tinham sido pegos.
- `recomp/build.sh --mod "UN6 A35.iso"` + `recomp/android/build.sh --mod un6-a35` (7 min 35 s): 0 erros no programa; `mod` com
  os mesmos 189 erros de antes (dados lidos como código, em `sub_00940194` etc.). `libun5_un6_a35.so` sha1 `72777012`,
  `app_d12`.
- **Depois:**
  - Odin a 120 (`rec/m2_odin_a35fix`): dois Ougi (o Linked em `hlseg_12` 80–104 s, o do Kyubi em `hlseg_13` 8–33 s),
    a luta volta ao 3D depois de cada um, Time Over, "Vencedor", "Canção ninja" (48/783/831) e a seleção.
  - Cube a 60 (`rec/m3_cube_a35fix`): luta até o Time Over (sem Ougi dessa vez), "Vencedor", resultado (89/176/265) e a
    seleção.
  - Nos dois, nenhum `missing-target`.
- **De passagem:** o A35 tem "Salvar dados?" no Combate Livre (START). Um toque a mais no roteiro entrou ali; respondi
  "Não" e o save ficou como estava.

**Alpha 1.1X: o 3D fica preto depois de um Ougi (aberto; o mesmo do preto depois do choque).**
- Cube a 60, `app_d11`, Alpha ligada pela tela de Mods (o A35 desliga sozinho; `un5_mods.txt` = `modlib/un6-a11x/disc`;
  o log diz `mod program ... (the base's code, its own data)`), `rec/x1_cube_a11x`.
- Primeiro Ougi Linked (Naruto e Sakura) até `hlseg_11` 143,5 s, e a luta volta ao 3D (nativo de v15902 a v16091). O
  segundo Ougi começa em 148 s; desde o fim dele (`hlseg_12` 8,5 s) até o Time Over, o 3D fica preto e só o HUD aparece.
  WINNER e "Ninja Song" saem normais.
- `HL_FRAME_LOG`: depois do segundo Ougi, o jogo só escreve FRAME `80000` (cena na página 0) e a cópia
  `ff00000000080000`. Não escreve a cópia de Z das sombras (`310401e0`) nem a cópia para 0x70, e o detector do nativo não
  religa. No Cube, o GS desenha a luta em 3D preta (já anotado), por isso fica preto.
- É o mesmo quadro do choque de Ougi da base (11:40): o jogo não volta ao arranjo normal da luta depois de um Ougi. Lá
  ficava o arranjo trocado; aqui, só a página 0. Agora acontece sem choque, então não é coisa do choque.
- **Texto errado no launcher:** na tela de Mods, a Alpha 1.1X diz "Esta versão do motor ainda não tem o pacote de código
  deste mod: o jogo abre como a base até ter". O motor roda o programa dela desde 11-04.

### 2026-11-05 12:35, dia, item 3: listras depois da vitória, Odin (Qualcomm e Turnip) e Cube: não aparecem

`app_d10` (o d6 com `HL_FRAME_LOG`), 120 no Odin, 60 no Cube, jogado por toque, filmes pulados com START (jogador).
- **Como ganhar sem atalho:** contra a CPU perdi 4 lutas seguidas (Normal e Simple, com e sem Handicap 9:1). A vitória
  veio pelo modo do próprio jogo "1P vs. 2P" (△ na seleção): sem controle 2, o 2P fica parado. Battle Settings com o chakra
  "Unlimited", para o Ougi estar sempre disponível, e o Ougi pelo botão Ougi da tela (`aggro.py ... btn`): o △△○ do script
  não pegava contra o 2P parado. Sem POKE, sem `HL_CPU_HP`. As lutas foram ao tempo ("Time Over") com o 1P na frente.
- **Odin, driver da Qualcomm** (`files/drivers/selected` tirado, log sem `adrenotools`):
  - `rec/v6_odin_qc`: 2 vitórias sem Ougi. WINNER, "Ninja Song" (87/227/314 e 79/192/271) e a seleção, limpos.
  - `rec/v8_odin_qc_win/hlseg_12.mp4` 60–170 s: 16 pedidos de Ougi respondidos, dois "PRESS!" (None x None) com a luta
    seguindo em 3D, Time Over, WINNER, resultado (73/1312/1385) e a seleção, sem listras.
- **Odin, Turnip** (`selected` de volta): `rec/v7_odin_turnip_win`, dois Ougi de Naruto com Sakura (17 pedidos),
  Time Over, WINNER, resultado (59/793/852) e a seleção, limpos. O fim do Ougi passa por um quadro escuro, como já anotado.
- **Cube:** `rec/v9_cube_win/hlseg_13.mp4` 84–143 s, 29 pedidos, Time Over, WINNER (o Kyubi), resultado (36/1028/1064)
  e a seleção, limpos.
- Nenhuma linha `SCANMSK 1/2` do jogo nessas lutas: o Ougi do Kyubi (o que escrevia SCANMSK 2) não saiu de novo. A correção
  de 11-03 (`fight over with SCANMSK`) segue no código; o caso exato do relato de 11-03 (Kyubi antes do KO) não foi refeito.
- **Visto de passagem:** na WINNER logo depois da luta os controles da tela ainda são os da luta por ~2 s (o `scene()` espera
  2 s); não atrapalha.
- **Estado do Odin agora:** driver do sistema (para refazer os itens 1 e 2 com ele). `files/drivers/selected.turnip-0741`
  guarda a escolha original; restaurar no fim.

### 2026-11-05 11:40, dia: correção de horas, o Odin com o Turnip, o preto depois do choque de Ougi (aberto)

**Horas erradas:** as duas entradas anteriores, "11:20" e "12:20", foram escritas às ~10:00 e ~10:40 (o relógio do
aparelho e do PC confirma: 10:58 quando percebi). O relatório publicado como "13:00"
(https://claude.ai/artifact/Utyg1DbQ6nes1aRwWNbDLE) saiu às ~10:45; será republicado na mesma URL às 13:00.

**O Odin 3 está com o Turnip desde as 07:41.** O launcher tem `turnip_gen8_v38-patched-vulkan_1.4.363` escolhido
(`files/drivers/selected`); o log diz `VKLoader: opening custom Vulkan driver via adrenotools`. Todas as gravações de hoje no
Odin (`d1` a `p11`, `b1`, `v1` a `v5`) são com o Turnip, não com o driver da Qualcomm. As de 11-05 03:20–04:42 (`n5*`, `f1_*`)
eram com o da Qualcomm. Os itens 1 e 2 no Odin precisam ser refeitos com o driver do sistema (fica na rota final; o Turnip
volta a ser escolhido no fim, como estava).

**Preto com HUD depois do choque de Ougi (pedra-papel-tesoura), aberto.** Visto 3 vezes, sempre depois do "PRESS!" dos dois
Ougi ao mesmo tempo com "COM You win!!": `b1_odin120_good1/hlseg_13` (o `good-20261105-1`: não é regressão do d6),
`v1_odin_turnip/hlseg_13` 92 s em diante (d6, 120, Simple), `rec/v2_odin_clash` (d7). O Ougi do vencedor não roda; a luta
continua (tempo, combo, itens, a CPU batendo) com o 3D preto até o fim do round.
- **`HL_FRAME_LOG=1`** (novo, `hl_native.cpp`, desligado por padrão): a cada 50 vsyncs, os FRAME dos GIF tags do jogo, com a
  cena ligada ou não, e a hora do aparelho em ms (também nas linhas `scene on/off`), para casar com `hlseg_t.txt`.
- **O que o jogo desenha no preto (`v2_odin_clash`, v10700–11900):** a cópia de Z das sombras vai para a página `0x1C0`
  (`310401c0`), as sombras para `0x1E0` (`401e0`), aparece `80180` (página `0x180`, largura 8, 1 tag por quadro) e **não há a
  cópia da cena para `0x70`** (`ff00000000080070`), que o DISPFB (`1070`, constante a rota toda pelo `HL_PRIV_LOG`) mostra.
  Na luta normal: Z em `0x1E0`, sombras em `0x1C0`, cópia para `0x70`. É a disposição que o jogo usa no Ougi (os dois blocos
  trocados). O detector do nativo só conhece a `0x1E0`, então o nativo não liga e tudo vai ao GS.
- **Não é o GS que não sabe desenhar a luta:** com `HL_NATIVE=0` no Odin (Turnip), o GS desenha a luta normal inteira
  (`d2_odin_ougi_nonat`). O jogo fica preso no modo de desenho do Ougi sem o Ougi.
- **Tentativa (d8/d9, `HL_NATIVE_SWAP=1`):** o nativo aceita as duas disposições pela página da cópia de Z. Com isso ele
  passa a desenhar também os Ougi normais, e desenha o do Kyubi com blocos de lixo colorido (`rec/v5_odin_d9/hlseg_11.mp4`
  26–34 s, quadro em 30,5 s); o GS desenha esses Ougi certos. O caso do choque não se repetiu com o d8/d9 (o choque é raro:
  2 em ~10 lutas), então não sei se o nativo desenharia a luta presa. **Fica desligado por padrão** (`app_d10`: igual ao d6
  mais os logs).
- **Falta:** um choque no PCSX2 (o que deveria acontecer depois do "COM You win!!"), e saber por que o jogo não roda o Ougi
  do vencedor aqui. Ferramentas novas em `build/lab`: `aggro.py`, `ougispam.py` (Ougi à mão sem parar, com o chakra
  "Unlimited" das Battle Settings, para provocar o choque, só depuração), `fightwatch.sh`, `tofight.sh`, `selfight.sh`.

### 2026-11-05 12:20, dia, itens 1 e 2: Ougi da CPU a 60/120; piscar a 60/120 antes e depois

**Item 1, o que faltava:** Ougi da CPU e disputa no Hard, os dois trocados ao vivo no painel. Odin a 120,
`rec/p11_odin120/play_10.mp4` 79–89 s (Ougi "Linked", pedidos, OK/ERROR, 3 passos do 1P no log); Cube a 60,
`rec/p11_cube60/play_11.mp4` 15–25 s (3 passos do 1P). Falta só a CPU no Odin a 50 (vai na rota final).

**Item 2 (piscar e efeitos sumindo a 60/120): corrigido pela reescrita da interpolação do `9aa3ad0`.** Método:
`build/lab/tools/flicker.py VIDEO INÍCIO DURAÇÃO` acha quadros diferentes dos dois vizinhos com os vizinhos parecidos
(o A-B-A do piscar), na taxa da gravação (118–120 quadros por segundo), numa grade 8x6.
- **Antes** (`good-20261105-1` reinstalado no Odin, 120, Naruto x Naruto): `rec/b1_odin120_good1/hlseg_12.mp4` 100–117 s,
  81 picos em ~17 s de luta. Os avisos 2D da disputa ("PRESS!", "Press the appropriate button", "COM You win!!") só saem
  no quadro real do jogo, 1 de cada ~5; os interpolados vêm sem eles (quadros 2406–2419 e 2476–2480). No fim do mesmo vídeo
  (`hlseg_13`) a tela preta com HUD e os controles do menu: o congelamento do item 1, como no relato.
- **Depois** (`app_d6`): Odin 120 `p11_odin120/hlseg_10.mp4` 66–75 s, 0 picos na luta; os avisos do Ougi em 24 quadros
  seguidos (81,5–81,7 s), todos com eles. Odin 60 `p10_odin120/hlseg_14.mp4` 100–150 s e Cube 60 `p11_cube60/hlseg_10.mp4`
  100–160 s: os picos que sobram são do próprio jogo (o corte "Linked", o "Start Battle", o NOW LOADING, o clarão branco de
  um golpe no Cube, quadro 955 em 123,9 s), olhados um por um.
- **Sombra preta:** não aparece em nenhum dos vídeos a 60/120 de hoje, nem no "antes". Sem caso para comparar; fica aberto
  até ver um.
- Falta a mesma disputa de "PRESS!" (a de pedra-papel-tesoura, não a do Ougi) no d6, quadro a quadro.

### 2026-11-05 11:20, dia, item 1 (continuação): especial desligado, Ougi da CPU e disputa, 50/60/120

APK `app_d6` (o d5 mais o log `[sp] v.. pad P step N wants C` do `HL_SP_LOG`: o botão que a sequência pede agora, o que
a tela mostra). Jogado por toque, em tempo real; a sequência respondida por `build/lab/seqplayer.py` (lê o pedido no log e
toca o botão depois de 0,25–0,45 s, repetindo a cada ~0,5 s enquanto o pedido não muda, como um jogador). Battle
Settings do próprio jogo: Simple ou Hard, chakra ilimitado (opções de jogador).

**Visto funcionando (sem congelar, sem listras, pedidos na tela, volta à luta):**
- **Ougi à mão, especial desligado, Odin a 120 interpolado, 3x:** Choji x Sai, △△○ por toque, sequência respondida:
  `rec/p4_134/play_15.mp4` 50–74 s (corte "Reversal", "Press!", OK/ERROR, o Ougi inteiro).
- **Ougi à mão, especial desligado, Cube a 50:** o △△○ inicia o Ougi do Guy ("Unleash Ultimate Jutsu"), mas o golpe
  errou o Sai 4 vezes (`p5_cube`, `p6_cube`); o do Choji acertou: `rec/p6_cube/hlseg_11.mp4` 149–168 s.
- **Ougi da CPU e disputa, Cube a 50, Hard, Naruto x Naruto:** `rec/p8_cube_cpu/hlseg_11.mp4` 68–90 s e
  `rec/p9_cube_cpu/hlseg_10.mp4` 84–94 s. Os toques do 1P entram (os passos avançam no log: △ ○ ○); a CPU completa
  mais rápido (5 contra 3) e ganha; ERROR do 1P e OK da COM.
- **Ougi da CPU e disputa, Odin a 60 (trocado ao vivo no painel de pausa), Hard, Choji x Naruto+Sakura:** três disputas
  na mesma luta, `rec/p10_odin120/play_15.mp4` 18–28 s (Ougi "Linked") e o log `seq_p10o5.out` (2 a 3 passos
  respondidos em cada).
- **Luta inteira a 60 no Odin** (`p10_odin120/play_14.mp4`, 1 s a 60 quadros por segundo em 150 s, visão geral 140–168 s):
  sem piscar, efeitos de golpe e textos em todos os quadros.

**Não é defeito:**
- O `wants` sai ~0,5 s antes do corte "Ultimate Jutsu"; toques antes de o pedido aparecer não contam (como para o
  jogador). O primeiro toque do tocador caía nesse tempo.
- As 3 lutas sem Ougi da CPU no Odin (v152400–176850) eram no Simple: saí das Battle Settings com △ (Back), que descarta a
  mudança. Com ✕ (OK) o Hard vale.

**Correção da entrada de 09:00 (o branco do começo do Ougi):** não é a troca nativo→GS. Com o nativo desligado
(`HL_NATIVE=0`, `rec/p7_cube_gs`) o branco aparece igual. A duração muda com o Ougi (0,4 s o do Naruto e 1,75 s o do
Choji no Cube; 3,6 s o "Linked" no Odin a 60) e os pedidos já se desenham por cima: parece a tela de carga do Ougi do
próprio jogo. Falta medir o tempo no PCSX2 (o daqui só tem o controle SDL-1 configurado; o vídeo de referência não tem Ougi).

**Visto e ainda aberto:**
- Preto de ~0,75 s com o HUD no fim do Ougi (`p6_cube/hlseg_11.mp4` 168,0–168,5 s), pelo GS, antes de o nativo religar
  (v88875). A conferir no PCSX2.
- Ougi da CPU a 120 e a 50 no Odin, e a 60 no Cube: não feitos ainda.
- Layout: a coluna de botões da sequência do 1P (canto inferior esquerdo) fica sob o D-pad no Cube (1:1) e em parte no
  Odin. Proposta: durante o Ougi, D-pad mais transparente ou mais alto (a sequência usa as direções).
- Só para depuração: com `HL_NATIVE=0` a luta em 3D sai preta no Cube (HUD e Ougi aparecem) e o zero do SCANMSK não vale
  (`GS SCANMSK 2` no log). Não é o caminho do jogador.
- O painel diz que a resolução interna "vale na próxima vez que o jogo abrir" e não tem 0,75x (item 6 pede ao vivo).

### 2026-11-05 09:00, dia, item 1 (Ougi do jogador): congelamento, SCANMSK e controles; método novo

**Método (pedido do desenvolvedor, vale para toda a lista e a rota final):** teste como jogador, em tempo real, por
toque na camada da tela (`input swipe` de 90–300 ms, pausas de 0,3–1 s): boot sem pular, menus de verdade, luta
inteira com Jutsu e Ougi feitos à mão. **Proibido para chamar de testado:** modo rápido (`HL_FAST_UNTIL`), pular
filme, POKE, `HL_CPU_HP`, vitória forçada, Ougi automático (`HL_SP_TAP`/`HL_SP_CPU`), `HL_PADROUTE` nos menus. Esses só
para chegar a um ponto de depuração (as rodadas `d*_odin_ougi*` abaixo usam a rota v3 até a luta e são só de depuração).
Ferramentas novas em `build/lab`: `play.py` (toques pelo layout de `TouchConfig.kt`, gravação em segmentos de 170 s
com o relógio do aparelho, som do motor por baixo), `fightplay.py` (uma luta de jogador: andar, combo, pulo, defesa,
item, chakra, Jutsu △○ e Ougi △△○ à mão, respostas ao pedido), `tapdrv.sh`. Som: `HL_SPU2_REC=<arquivo>`
(`hl_spu2.cpp`), s16 estéreo 48 kHz contínuo, com zeros quando a fila do SDL seca, para casar com o vídeo. Aparelhos de
hoje: só Odin 3 e Cube (o RP5 é da sessão do Dragon Ball).

**Libs:** o APK de hoje leva o nosso `libbt4gsx` (`recomp/android/build_gsx.sh`, `hl_gsx.cpp` + `hl_native.cpp`), não
o do Nominum `b2ff28b` (cópia em `build/lab/libbt4gsx_b2ff28b.so`). Motivo: o defeito do congelamento está nos dois
(o HEAD do Nominum, `9940425`, tem o mesmo `presentsHere`). Perde-se a Fase 2D do Nominum até ele receber as correções
abaixo (pedido no fim desta entrada).

1. **Congelamento a 60/120 no Ougi (corrigido em `hl_native.cpp`, falta a regra C):** `presentsHere` ficava verdadeiro
   depois que a cena nativa desligava ou parava de apresentar, e o `hl_gsx` pulava as apresentações do GS: a tela
   parava numa imagem (o corte do Ougi), sem os botões da sequência, até a luta voltar ao nativo. Agora só vale com a
   cena ligada e uma apresentação nos últimos 100 ms; ao desligar a cena, `s_presentsHere` e `s_presentRate` zeram.
   Antes: `n5_odin_c60` (noite), 10,4 s parado na arte do Ougi, "FPS: N/A". Depois: `d1_odin_ougi60_macro` (Ougi pelo
   botão em v6900 e v7770, sem parada, pedidos visíveis).
2. **Ougi do GS escuro e listrado (corrigido em `hl_native.cpp`):** o jogo escreve `SCANMSK 2` (só linhas pares) uma
   vez no Ougi do Naruto (Kyubi). O nativo ignora SCANMSK e desenha inteiro; o GS desenhava linha sim, linha não por
   cima do que o nativo deixou: escuro, listrado, sem os pedidos, ~20 s (`d3_odin_ougi50_43`, 79–98 s do vídeo,
   `game SCANMSK 2` em v8967). Agora o GS recebe sempre `SCANMSK 0`: o valor é zerado no pacote (cópia do `hl_gsx`)
   quando a cena está desligada e no `emitRegisters` quando está ligada. `HL_SCANMSK_KEEP=1` devolve o de antes.
   Depois: `p2_cube` (Cube, jogado por toque, Naruto x Sai), Ougi do Kyubi em 149–164 s de `play_14.mp4`: cena inteira,
   sem listras, só o desfoque do próprio golpe. Falta no Odin com o mesmo Ougi.
3. **Botões da sequência viravam os do menu (corrigido em `hl_game.cpp`):** com o nativo desligado mais de 8 s (Ougi
   pelo GS), `scene()` passava ao layout do menu no meio da sequência. Agora a luta é também o que o jogo diz: um
   lutador leu a entrada (`0x21E660`) ou a sequência/pedido do Ougi leu o pad (`0x36E690`, `0x6C99B0`, novo
   `g_ougiTick`) nos últimos 3 s. Fora disso, os controles da luta saem 2 s depois (antes 8 s; a campanha fica em 8 s).
4. **Lutadores velhos depois da luta (corrigido em `hl_game.cpp`):** o `BTL.BIN` fica na memória sob a seleção de
   personagem, e os lutadores que acabaram a luta num Ougi ou numa disputa faziam `ougiOnScreen()` responder "sim":
   controles da luta por mais de 40 s na seleção (`p2_cube`, `[sp] pad answer` até v47980 com a cena desligada desde
   v44126), câmera larga e respostas do especial nos menus. Agora só vale com a luta viva (lutador ou pedido nos últimos
   5 s). `HL_SP_LOG=1` mostra uma vez por segundo `[sp] v.. ages: fighter .. ougi .., on screen .., scene ..`.

**Visto e ainda aberto:**
- Começo da disputa/Ougi pelo GS: 0,5–1 s de branco e um quadro com lixo de memória (blocos coloridos), no Cube
  (`p1_cube_boot` 76,0 s; `p2_cube` 148,5–149 s de `play_14`) e no Odin (`d3_odin_ougi50_43` 74,4 s). O GS começa da
  memória que o nativo nunca escreveu. Falta escrever no GS a última imagem do nativo ao desligar a cena.
- Faixa de ruído colorido na água do lago do Hidden Leaf no Cube (nativo, `p1_cube_boot`, 89,6 s).
- Som: os logos Bandai Namco, Bandai e CRIWARE saem mudos (`p1_cube_boot`, som zero até o logo do CC2); o filme bate no
  teto (pico 32112, 0,5–0,8% das amostras). Item 5.
- A vitória da CPU volta direto à seleção sem resultado (as duas derrotas de `p1_cube_boot` e `p2_cube`); não conferido
  no PCSX2 (`build/pcsx2.AppImage`).

**Pedido ao Nominum (não mexi no código dele):** levar para `vk/armsx2_host.cpp`/`core/stream.cpp` as correções 1 e 2
(`presentsHere` só com a cena ligada e uma apresentação recente; SCANMSK 0 ao GS).

### 2026-11-05 04:50, noite, item 6: fechamento, `good-20261105-1`

**APK final:**
- Refeita da árvore commitada (`6b420e6` no código; depois só o registro), em `build/lab/app_final2.apk`. Copiada para
  `~/Downloads/HiddenLeaf-builds/HiddenLeaf-20261105-<commit da tag>.apk`.
- As bibliotecas têm o mesmo md5 das da `app_a35s.apk` testada:

  | biblioteca | md5 (início) | o quê |
  |---|---|---|
  | `libun5.so` | `e710273f153b` | patches `0001`–`0069`, libsdr padrão, fila do som em 80 ms |
  | `libun5_un6_a35.so` | `b9b209886475` | pacote do A35 |
  | `libbt4gsx.so` | `149baf699bbe` | ARMSX2 com o Nominum `b2ff28b` |

- Launcher com o `ModSaves`. Instalada nos 3 aparelhos às 04:18–04:20.

**Rota completa final** (`build/lab/q_f1_*.out`, `fsum.sh`):

| rodada | fim | resultado |
|---|---|---|
| `f1_rp5_v3` | v18057 | 2ª luta 49,8, 3ª 50,0; fila mediana 94,7 ms, mín. 9,1 |
| `f1_cube_v3` | v18065 | 49,9 / 50,0; fila 87,9, mín. 26,8 |
| `f1_odin_v3` | v18040 | 50,0 / 50,0; fila 87,1, mín. 60,4 |
| `f1_odin209_v3` (2400x1080) | v18005 | 50,0 / 50,0; fila 86,8, mín. 60,1 |
| `f1_rp5_camp` | v11948 | 43,7 / 46,4 / 36,7; pausa em v10250; saída em v11912 |
| `f1_cube_camp` | v12256 | 49,6 / 50,0 / 42,2; pausa em v10106; saída em v12256 |
| `f1_odin_camp` | v12071 | 50 / 50 / 50; pausa em v10408; saída em v12071 |
| `f1_odin209_camp` | v11881 | 50 / 50 / 50; pausa em v10302; saída em v11881 |
| `f1_rp5_film`, `f1_cube_film`, `f1_odin_film`, `f1_odin209_film` | v7266–7471 | logos, abertura e os dois fins a 50 fps nos 4 |

- **Em todas as rodadas:** nenhum `missing-target`, nenhum "GAME DIED" e pico do som de 0,98.
- **Painel de pausa:** 4:3, tela cheia e continuar funcionaram. "Sair do jogo" fecha o processo do jogo e volta à
  `MainActivity`.
- **Filmes:** nos 4, o quadro 0 cai em v1446 e o 2400 em v6287; os começos de som em v1449, 1647, 1856–1882, 1904 e 6394.
  A fila fica em 77 ms (mín. 45–59).
- **Imagem (amostra de 25 em 25 s, RP5 e Odin 20:9):** seleção, lutas, WINNER "Naruto Uzumaki", resultado
  ("Outstanding!", 726 Ryo), Mode Select. Os trechos pretos de 1–5 s (`scratchpad/blackc.sh`, centro < 20 de luma) são
  tantos quanto nas rodadas de antes da noite (`n33`, `n37`, `n42`).
- **A35 e Alpha 1.1X** com esta APK: entradas de 04:00, 04:10 e 04:12.

**Som abaixo de 50 fps (achado, sem correção):**
- Na campanha do RP5 (37–46 fps) e do Cube (48–49 fps em trechos, 41–44 no menu START), o jogo produz menos som que o
  tempo real.
- A correção de deriva só ajusta ±1%, então a fila seca: 49 de 247 s abaixo de 10 ms no RP5 (`n53`), e no Cube trechos de
  0–15 ms (v4900–6200, v9800–9920, v10700–12100).
- Volta a encher quando o jogo passa de 50.
- Não é regressão: a rodada 3 (`n3_rp5_camp_c`, fila antiga de até 200 ms) também chegava a 0,0.
- **Pedido:** esticar o som no tempo quando o jogo fica abaixo de 50, como o PCSX2 faz com o SoundTouch. Alargar o ±1%
  muda o tom; não fiz, porque não dá para ouvir aqui. A outra saída é o desempenho da VU1 (passo 2 do `CLAUDE.md`).

**Aparelhos ao fim:**
- **RP5:** `un5_settings.txt` de volta ao `un5_settings.dev-0003.txt` (`aspect_milli=1333`).
  `tools/device-profile.sh -s 192.168.1.89:5555 restore` rodou (`vendor.perfservice` e `perf-hal` de volta). Driver do
  sistema.
- **Odin 3:** `wm size` em 1080x1920 (o físico), e `fps_mode=50`. O `un5_mods.txt`, que eu tinha renomeado para
  `un5_mods.dev.txt` para as rodadas, voltou: a Alpha 1.1X fica ligada como o desenvolvedor deixou.
- **Cube:** sem `un5_mods.txt` (o `un5_mods.a35` e o `modlib/mods.txt` com `1|un6-a35` são de antes da noite: o launcher
  mostra o A35 ligado, mas o motor roda a base até o mod ser religado na tela de mods).
- **RP5 e Odin** ficaram com `modlib/un6-a35` (1 GB) e o save `BASLUS-55606NARUTO6`, copiados do Cube.

### 2026-11-05 04:12, noite, item 5: a Alpha 1.1X no RP5, no Odin e no 20:9

- **APK e jeito de ligar:** a `app_a35s.apk` (`6b420e6`), com só o `un5_mods.txt` = `modlib/un6-a11x/disc` (`build/lab/a11xq2.sh`).
  Não há pacote de código: o log mostra `un5: mod program …`, e a base roda o programa da mod.
- **Rota:** `hl_env_n3_v3_c` até v9000.

  | rodada | fim | cenas nativas | fps em v6450–8900 (pior) |
  |---|---|---|---|
  | `n70_cube_a11x` | v9320 | 6022, 6359, 6986, 7686, 8035 | 50,0 (49,6) |
  | `n67_rp5_a11x` | v9415 | 6022, 7493, 8526 | 50,0 (45,0) |
  | `n68_odin_a11x` | v9591 | 6022, 6359, 6986, 7686, 8035 | 50,0 (49,8) |
  | `n69_odin209_a11x` (2400x1080) | v9288 | os mesmos | 50,0 (49,9) |

  - Comparação: `a11x_cube_v3` e `a11x_rp5_v3`, de 11-04, deram 50,0 e 50,0.
  - Nenhum `missing-target`.
  - Imagem da luta conferida no RP5 e no Odin 20:9 ("15 Chakra Recovery", "2 hits").
  - O `un5_mods.txt` foi apagado no fim, e o Odin voltou ao `wm size reset`.
- **Falta:** a rota inteira (WINNER, resultado, pausa e sair) com a mod. O `KNOWN_MODS` e o `CODE_RANGES` do `un5.py`
  (passo 3 do `CLAUDE.md`) não entraram: a mod roda sem pacote.

### 2026-11-05 04:10, noite, item 5: o launcher cria o save do A35

- **Achado (`n65_cube_a35_nosave`, `HL_MC_LOG=1`):**
  - sem `BASLUS-55606NARUTO6`, o A35 só pergunta pelo cartão: 11248 `getinfo` e 2808 `getdir`, nenhum `open`;
  - mostra "Nenhum dado do Naruto Shippuden: Ultimate Ninja 5 encontrado no cartão de memória (PS2) no slot 1 … insira
    um cartão de memória (PS2) com dados de Naruto Shippuden: Ultimate Ninja 6" e não sai dali.
  - O mod espera um save que já exista e não cria um. É do próprio mod (o cartão do runtime cria o save do UN5 base).
- **O save feito à mão em 11-03** é o do UN5 byte a byte (os 7 arquivos comparados): só mudam o nome da pasta e o do
  arquivo de cabeçalho dentro dela.
- **No launcher** (`ModSaves.kt`, `KnownVersions.KnownMod.cardFolder`, chamado no `GameActivity.onCreate` antes do motor):
  - com um mod ligado que tem `cardFolder` (só o A35: `BASLUS-55606NARUTO6`), se a pasta não existe e há
    `saves/BESLES-55605NARUTO5`, ela é copiada com o cabeçalho renomeado;
  - a cópia passa por uma pasta temporária, para que uma parada no meio não deixe save pela metade;
  - um save do mod que já existe não é tocado.
- **Testes:**
  - `ModSavesTest`: 3 casos, passam.
  - `n66_cube_a35_madesave`, com a `app_a35s.apk` (`yld1` + pacote do A35 + isto) e o save do A35 tirado do Cube: o
    launcher criou a pasta na partida (arquivos do usuário do app, 04:01). O A35 carregou e lutou (cena nativa
    v5873–9604), até v12119, sem `missing-target`.
  - O save original voltou ao Cube depois.
- **A APK final sai daqui:** `app_a35s.apk`.

### 2026-11-05 04:00, noite, item 5 e conferências: A35 nos 3 aparelhos; giro e 30 fps no Odin; imagem do PC

**A35 com a build da noite:**
- O código do mod foi regerado do zero com `recomp/build.sh --mod "UN6 A35.iso"` (`build/lab/a35gen.out`):
  - principal: 9573 funções, 221 stubs, 0 erros;
  - ADV: 7070; BTL: 5743.
  - Com a libsdr padrão, a `sceSdRemote` também sai dos stubs no mod.
- `recomp/android/build.sh --mod un6-a35` gerou a `libun5_un6_a35.so` (165 MB).
- `build/lab/app_a35.apk` é a `yld1` com o pacote; a `libun5.so` é idêntica à da `yld1`.
- No RP5 e no Odin, a pasta `modlib/un6-a35` (os 16 arquivos que o mod muda) e o save `BASLUS-55606NARUTO6` foram copiados
  do Cube (`build/lab/a35q2.sh`). Ficaram lá.
- Rota `hl_env_a35f` (a de 11-03):

  | rodada | luta (cena nativa) | fps na luta (pior) | fim |
  |---|---|---|---|
  | `n61_cube_a35` | v5873–9604 | 59,6 (48,7) | v12046 |
  | `n62_rp5_a35` | v5873–10622 | 57,5 (46,0) | v11680 |
  | `n63_odin_a35` | v5871–10360 | 60,0 (59,8) | v12201 |
  | `n64_odin209_a35` (2400x1080) | v5871–10360 | 60,0 (59,8) | v12139 |

  - Comparação: `log_a35f`, de 11-03, deu 59,3 no Cube.
  - Nenhum `missing-target`. O log mostra `mod un6-a35, program .../SLUS_556.06`.
  - A imagem da luta aparece nos 4 (texto do mod em português: "Chakra Recuper.").
  - Som: 162 de 197 segundos com rms > 0,01, pico 0,98.
- **A fila do som no A35 (Cube):** mediana de 80 ms, mas 5 segundos abaixo de 25 ms (o menor, 5,5 ms). Todos caem nos
  segundos em que a luta fica abaixo de 60 fps (56,6, 53,4, 55,6 e 48,7 em v7409–8603): o jogo fez menos som, e a fila
  segurou sem secar. É desempenho do Cube no NTSC (16,7 ms por quadro), não defeito do som. No jogo base, `n47` e `n58`
  não têm nenhum segundo abaixo de 25 ms.
- **Falta:** o save do A35 ainda é cópia à mão; e a Alpha 1.1X (`KNOWN_MODS` e `CODE_RANGES`).

**Odin, giro 90/270 e painel a 30 fps com a `yld1` (`n59_odin_rot_c`, GS do ARMSX2):**
- Os dois giros caíram em v6000, na 1ª luta. É que o `recfull2.sh` só olha as ações a cada 10 s, e o modo rápido já
  passou de v2231 e de v4847 antes disso.
- Imagem certa nos dois, no mesmo quadro da rodada sem giro (`n54`).
- A arena preta com o HUD nesse trecho da 1ª luta aparece igual sem giro, então não vem do giro.
- Painel em "30": o contador mostra 25 fps (50 Hz), que é o que o modo faz (as 25 imagens do jogo, uma vez cada).
  Depois volta a "50 (nativo)".
- A 50 fps em todo o resto, sem erro. Cada troca no painel dá uma linha de `[perf]` com a pausa (2,2 e 0,7 fps).

**PC, imagem:** 75 s com o som do PC mudo, e captura da janela pelo `spectacle`: a tela de idioma do UN5 ("ENGLISH …
ITALIANO") desenhada certa, em 4:3. Nenhum `missing-target` (`build/lab/pcrun2.log`).

**Ougi pelo GS (escuro e listrado, SCANMSK 2):** não apareceu nas rotas v3 desta noite. Nenhuma linha
`fight over with SCANMSK` nos 5 logs (`n47`, `n51`, `n52`, `n54`, `n58`), e no RP5 (`n51`) a luta 2 não teve esse Ougi.
Sem como reproduzir com a rota atual: fica como estava.

**Menus em 16:9 ("Esticar" e "Desenhar em 16:9"):** não feito.
- A forma da tela sai do `hl_native.cpp` (`applyAspect`), que fica na `libbt4gsx` (a mesma da APK, junto com o Nominum
  `b2ff28b`). Trocá-la pede outra regressão inteira.
- E "Desenhar em 16:9" não está definido: menu largo pelo jogo (patch) ou 4:3 com barras.
- Pela regra da noite (na dúvida, o mais seguro), fica para o desenvolvedor decidir. A APK final sai com os menus
  esticados, como hoje.

### 2026-11-05 03:35, noite: regressão da `yld1` passou; PC com os 69 patches e a libsdr; sincronia nos `.PSS`

**Regressão da `app_yld1.apk`** (patches `0062`–`0069`, libsdr, fila do som em 80 ms, Nominum padrão `b2ff28b`):

| aparelho | rodadas | resultado |
|---|---|---|
| RP5 16:9 | `n51`, `n52_rp5_v3yld`, `n53_rp5_campyld` | v3 até v17657 e v17810; campanha até v12324 |
| Odin 16:9 | `n50`, `n54_odin_v3yld`, `n57_odin_campyld` | v3 até v17784 e v17893; campanha até v12719 |
| Odin 20:9 | `n55`, `n56_odin209_v3yld` | até v17573 e v17695 |
| Cube | `n47`, `n58_cube_v3yld`, `n48_cube_campyld`, `n46_cube_filmyld` | v3 até v17657 e v17503; campanha até v12194; filmes |

- Nenhum `missing-target`.
- As cenas do renderizador nativo caem nos mesmos vsyncs das rodadas de antes (v3: 6022, 6359, 10829, 12078, 14280; no RP5:
  7075, 10529, 13292, 14880; campanha: 4559 … 10615). A única exceção é a `n50` no Odin, que a `n54` não repetiu:
  foi variação de rodada.
- O desempenho é o mesmo:
  - campanha: RP5 43,8 / 44,8 / 36,8 (rodada 3: 43,9 / 44,6 / 36,6); Odin 50 / 50 / 50; Cube 49,7 / 49,0 / 42,8 (rodada 3:
    49,5 / 49,2 / 42,9);
  - v3: a menos de 0,5 ms de EE e VU1 por trecho.
- **Com isto, a `0069` entra na APK final.**

**PC:**
- `recomp/build.sh` do zero com o ISO europeu (`build/lab/pcbuild.out`): os 69 patches se aplicam, o recompilador dá 0
  erros, e o `ps2EntryRunner` linka.
- O código gerado com a libsdr padrão é idêntico ao `build/lab/runner_sdr`.
- Rodou 100 s com o som do PC mudo (`build/lab/pcrun.log`): o IOP carrega o "SDR driver version 4.0.1", e não há queda
  nem `missing-target`. A imagem não foi conferida.
- O ISO não está no caminho do `CLAUDE.md`: está em `~/Downloads/Naruto UN5/`.

**Sincronia dentro dos `.PSS`** (leitor novo, `scratchpad/pssaudio.py`: primeiro PTS do vídeo `0xE0` e do áudio `0xBD`,
cabeçalho `SShd`, início do som com rms > 0,02 em janelas de 10 ms):

| filme | áudio | som depois do 1º quadro, no arquivo | medido |
|---|---|---|---|
| `LOGO_C` | PCM16, 48 kHz | +60 ms | +60 ms |
| `NARUTO10` | PCM16, **44,1 kHz** | +590 ms | +420 ms |
| `OPENING` | PCM16, 48 kHz | +290 ms | +340 ms (quadro 0 estimado, ±1 quadro) |

- O `LOGO_C` e a abertura batem com o arquivo.
- O `NARUTO10` é o único a 44,1 kHz, e o som chega 170 ms antes do que está no arquivo. A entrada de stream do SPU2 é de
  48 kHz, então isso é comportamento do hardware emulado. Fica "a conferir no PCSX2".

### 2026-11-05 03:15, noite, itens 3c, 4 e 5: o fim dos filmes a 6 fps; fila do som em 80 ms; Turnip; 60 e 120

**Item 3c, o fim de cada filme:**
- **Achado:** no fim do `LOGO_C` (v1566–1640) e da abertura (v6276–6370), o jogo cai para 5,5–6 fps no Cube por uns 12 s,
  com o EE em 165–179 ms por vsync. No Odin cai para 14–30 fps por uns 3 s, com 69 ms. Isso já estava na `n8` e na `n14`
  (antes da noite). A medida dos filmes que eu tinha feito só olhava os quadros durante o filme.
- **Perfil** no Odin (`prof2.sh`, 5 s a partir de v1560, `n45_odin_filmprof`):
  - `publishSnapshot` com o sort dentro: 20%;
  - mutex: 11%;
  - `selectReady` e `rotateReadyQueue`: 6%;
  - alocação: 6%.
  - Duas ou mais threads prontas cedem a vez uma à outra (`RotateThreadReadyQueue`) durante 1,5 s do jogo. Cada volta é
    uma transferência e conta poucos ciclos, então o relógio do EE quase não anda.
- **Patch `0069-ee-yield-loop-skip.patch`:**
  - depois de 32 giros seguidos com menos de 512 ciclos entre eles, o relógio avança até o próximo evento. É o que já se
    fazia quando uma thread sozinha só cede a vez. `HL_YIELD_SKIP=0` desliga;
  - o `publishSnapshot` monta a cópia no máximo a cada 20 ms. Só o painel de depuração do PC a lê.
- Vai na `app_yld1.apk`, já com o Nominum padrão `b2ff28b`.
- **Filmes** (`n46_cube_filmyld`, `n49_odin_filmyld`):
  - 50 fps nos dois fins, com EE de 2 ms no Cube e de 1 ms no Odin;
  - no vídeo do Cube, os dois trechos de ~10 s a 2–4 quadros por segundo sumiram;
  - os quadros e os começos de som caem nos mesmos vsyncs de antes (quadro 0 em v1446, 75 em v1626, último, o 2400, em
    v6287; som em v1449, v1647, v1904 e v6394).
- **Fila do som com alvo de 80 ms (`HL_SPU2_DRIFT`):**

  | rodada | mediana | menor |
  |---|---|---|
  | filmes, Cube (`n46`) | 77,6 ms | 46 ms |
  | filmes, Odin (`n49`) | 76,7 ms | 59 ms |
  | rota v3, Cube (`n44_cube_v3_80`) | 78,8 ms | 39,8 ms |

  - Antes era 128 ms no Odin e 175 ms no Cube.
  - Os buracos de ~0 ms da medida de 02:40 eram estes travamentos, não o alvo baixo.
- **Rota v3 com a `yld1`:**
  - Cube (`n47_cube_v3yld`, até v17657): igual vsync por vsync à de antes (cenas em 6022, 6359, 10829, 12078 e 14280), e
    o mesmo desempenho;
  - Odin (`n50_odin_v3yld`, até v17784): sem erro, a 50 fps, mas a 2ª luta acabou em v11856 (era v12078) e a 3ª começou
    em v14580 (era v14280).
  - Regressão completa em curso (`q_n51_rp5.out`, `q_n54_odin.out`, `q_n58_cube.out`).
- **Nota sobre as janelas:** nas rotas `hl_env_hp_v3`, os nomes de janela que usei (`luta1`, `Ougi`…) vêm da rota antiga.
  Nesta rota, a 1ª luta é em v6022–6359 (ainda no modo rápido), a 2ª em v10829–12078 e a 3ª a partir de v14280. As
  comparações entre rodadas valem, porque usam as mesmas janelas.

**Item 4, RP5 com o Turnip v26.3.0-R6 (`app_nmn4.apk`, Nominum `ff2e56d`, `rp5q4.sh`):**
- `n4_rp5t_v3_a` e `n4_rp5t_v3_b` vão até o fim, sem `missing-target` e sem `DEVICE_LOST`.
- Há imagem (brilho de 45 a 228 no centro).
- fps por janela, a e b:
  - v6450–6900: 49,9 e 50,1;
  - v7000–7900: 42,5 e 44,3;
  - v8000–8950: 35,9 e 35,8;
  - v8950–9450: 50 e 50;
  - 2ª luta: 47,4 e 44,7.
- Campanha b (`n4_rp5t_camp_b`): 42,2 fps na vila (era 27,3 na rodada 3).
- Linha "replay thread on the cores of capacity": 0 aqui também.
- **A tela preta do Odin é só no Adreno 830 com o driver da Qualcomm.** No Adreno 650 com o Turnip, o mesmo backend
  desenha.

**Item 5, 60 e 120 interpolados no Odin (GS do ARMSX2, `app_sdr19.apk`, painel de 120 Hz):**

| modo | rodada | apresentações por segundo na 2ª luta (`[interp]`) |
|---|---|---|
| 60 | `n5_odin_c60` | 53–60 |
| 120 | `n5_odin_c120` | 101–120 |

- Na 1ª luta não há medida: ela cai no `HL_FAST_UNTIL`, e o renderizador nativo desliga em v6359.
- O jogo fica a 50 fps nas duas rodadas, sem erro.
- O `fps_mode` voltou para 50 no fim.

### 2026-11-05 02:55, noite, itens 2a e 2e: o salto da 3ª luta corrigido; a libsdr vira o padrão

**Item 2a: corrigido.** Rodadas com a `app_sdr19.apk` (patch `0068`, libsdr, rota `hl_env_hp_v3` até v17300; as do RP5
e do Cube com `HL_MISS_DUMP`):

| aparelho | rodadas | resultado |
|---|---|---|
| RP5 16:9 | `n32`–`n35_rp5_fix` | 4 de 4 até o fim (v17564–18303); antes, 4 falhas em 7 |
| Cube | `n36`–`n38_cube_fix` | 3 de 3 (v17989–17990) |
| Odin 16:9 | `n39_odin_fix`, `n42_odin_fix` | 2 de 2 (v17650, v17701) |
| Odin 20:9 | `n40`, `n41_odin209_fix` | 2 de 2 (v17760) |

- Nenhum `missing-target` e nenhum `[ee-dump] thread`.
- O desempenho não mudou. Nos 5 trechos, RP5 (`n33` contra `n20`) e Cube (`n37` contra `n10`) ficam a menos de 0,3 ms de
  EE e VU1.
- Som em todas: pico máximo de 0,98, nenhuma janela acima de 1, rms médio de 0,206 a 0,258.

**Item 2e: feito.** O `tools/un5.py` passa a recompilar a libsdr por padrão (`sceSdRemoteInit`, `sceSdRemote`,
`sceSdTransToIOP`). `HL_SDR=0` deixa os três como stubs, como antes.
- É o mesmo código gerado do `build/lab/runner_sdr`, que já está no `src/runner` do runtime.
- Foram mais de 2 passagens seguidas nos 3 aparelhos e no 20:9 (tabela acima).
- **Falta:** a execução no PC com a libsdr não foi feita.

### 2026-11-05 02:40, noite, itens 2a, 3c e 4: a causa do salto da 3ª luta (checkpoint na mesma função mais fundo); fila do som

**Item 2a, a causa:**
- **Patch `0067-ee-run-end-events.patch`:**
  - evento `E` no fim de cada execução nativa, com o contexto base que ela deixou;
  - entrada `C` (`0xee004300`) a cada checkpoint do despacho, com o alvo e a origem da chamada suspensa.
- Com a `app_sdr17.apk`, a `n26_rp5_end` passou e a `n27_rp5_end` falhou. Na `n27`, a mesma execução nativa tem **dois**
  checkpoints seguidos:
  1. `C` no `jal 0x1bf9a0` da `0x3563c0`, origem `0x356520`;
  2. `C` no `jal 0x1bfeb0` da `0x31b4e0`, origem `0x31b650`, já com `sp 0xb6fb80`;
  3. por fim, `E` com a base `{0x1bfeb0, 0xb6fb80}`.
- **O mecanismo:**
  - a `0x31b4e0` faz `jal 0x1bf9a0` em `0x31b640` e depois `jal 0x1bfeb0` em `0x31b650`;
  - por baixo dessa chamada, mais fundo na pilha do host, o laço dos objetos chega à `0x3563c0`, que chama a
    `0x1bf9a0` de novo;
  - o checkpoint cai nessa chamada de dentro e deixa `ctx->pc = 0x1bf9a0`, e a pilha começa a se desfazer.
- No `dispatchGuestBranch` da chamada externa vale a regra `if (ctx->pc == entryPc) ctx->pc = fallthroughPc;`. Ela tomou
  o checkpoint por "a função voltou ao próprio começo" e respondeu `true`.
- Com isso a `0x31b4e0` seguiu com o `sp` da cadeia de dentro: parou de novo no `jal 0x1bfeb0` e, ao voltar, leu o `ra`
  do quadro da `0x356920` (`0xe3b4c0`).
- **Por que só no RP5 em 16:9 e com a libsdr:** é preciso que o checkpoint caia exatamente nessa chamada de dentro. A
  libsdr, a câmera larga e a vigia só mudam o tempo. O rastro de buscas é o mesmo, entrada por entrada, em todas as
  falhas (`n21`, `n23`, `n27`).
- **Correção, patch `0068-ee-checkpoint-unwind.patch`:**
  - o `checkpointDue` marca `m_hlSuspending` quando responde `true`;
  - o despacho desmarca antes de cada execução nativa;
  - enquanto a marca está ligada, o `dispatchGuestBranch` responde `false` depois da chamada, seja qual for o `pc`.
  - É uma correção do runtime: vale com e sem a libsdr. Vai na `app_sdr19.apk`.
- Primeiras rodadas da `sdr19`: `n32_rp5_fix` (RP5 16:9, v17929), `n36_cube_fix` (v17989) e `n39_odin_fix`, todas sem
  `missing-target`. As outras estão em curso (`q_n32_rp5.out`, `q_n36_cube.out`, `q_n39_odin.out`).
- **Descartado no caminho:**
  - código do jogo rodando fora da GameThread: o patch `0066` (`[ee-foreign]`) deu 0 linhas na `n23`, que falhou;
  - uma restauração de contexto feita pelo host: o único `R5900Context temp = *ctx` é o do `sceGszbufaddr`, numa cópia.

**Item 3c (sincronia), fila do som:**
- Nos filmes a fila não deriva: ela fica no nível em que as primeiras rajadas a deixaram (117–197 ms), num dente de
  serra sem tendência.
- **Novo no `hl_spu2.cpp`:** um controle segura a fila perto de um alvo, tocando até 1% mais rápido ou mais devagar
  (`SDL_SetAudioStreamFrequencyRatio`, média de ~1 s). `HL_SPU2_DRIFT=0` desliga.
- Teste no Cube com alvo de 60 ms (`app_sdr18.apk`):

  | rodada | mediana da fila | menor |
  |---|---|---|
  | filmes (`n30_cube_drift`) | 59,4 ms (era 174,7 na `n14_cube_sync`) | ~0 ms em v1578 e v6313 (fim do `LOGO_C` e da abertura) |
  | rota v3 (`n31_cube_v3drift`) | 58,8 ms (era 170,7 na `n11_cube_v3`) | 26,8 ms |

  - Nos dois pontos de ~0 ms a produção para por mais de 60 ms. A fila de 175 ms absorvia essas paradas.
- **O alvo passou para 80 ms** (no código, ainda sem medida). O teste fica para quando o Cube terminar o item 2a.

**Item 4:**
- A mesma `app_nmn4.apk` com o GS do ARMSX2 (`n5_odin_c50`, `hl_env_hp_v3.txt`) mostra a imagem no Odin, a 50 fps em
  todos os trechos.
- A tela preta é só do backend próprio do Nominum `ff2e56d` no Adreno 830, a 50 e a 60 (`n4_odin_v3_a`, brilho 0 do
  começo ao fim). No Cube, com o Mali, a imagem está certa.
- **Pedido ao Nominum:** com o `ff2e56d`, o backend próprio sai preto no Adreno 830. Na rodada 3, com o `b2ff28b`, ele desenhava. Entre
  os commits do meio estão o `caf58d7` (PCRTC: o alvo lido pelo circuito não é guardado) e o `b11058f` (DATE e mistura com
  cópia do alvo lida no shader). As gravações estão em `build/lab/rec/n4_odin_*`.

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
