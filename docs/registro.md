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

## Histórico (mais novo primeiro)

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
