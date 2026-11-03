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
| Som dos logos e da abertura (filme) | quebrado | `2fee126` | Cube | linha `[snd]` do `HL_PERF` | logos em silêncio; o filme de abertura nunca foi conferido (a rota pula com START). Relato do desenvolvedor: piorou desde 11-01. Sem bisect ainda |
| Título e menus | funciona | `2fee126` | RP5, Cube | rota fixa, prints, `[snd]` | som em título, Mode Select, Options, Shop, Collection, seleções, VS |
| Luta | parcial | `2fee126` | RP5, Cube | rota fixa, `measure.sh` | 50 fps nos dois (11-01). Relato: personagens piscando e sombra preta nova. Nunca contados por gravação |
| Tela de vitória / resultado | quebrado | `2fee126` | RP5, Cube | rota fixa | relato: fica presa numa imagem parada, o jogo segue por trás e não dá para sair. O personagem não chega ao GS na tela de WINNER |
| Campanha (Master Mode) | parcial | `2fee126` | RP5 | rota de campanha (`plano.md`) | 43,7 fps no `sd480p`, 83% de pares lentos; Cube 17% de pares lentos (VU1) |
| Widescreen na luta | parcial | `2fee126` | RP5 | gravação de 11-03: 808 → 0 quadros com barra | câmera larga presa à entrada do lutador nos últimos 6 vsyncs (não ao estado da cena). Relato de regressão |
| Widescreen na campanha | não testado | | | | câmera larga ligada pelo `ADV` |
| Widescreen nos menus | parcial | `2fee126` | RP5 | prints | esticados; "Esticar" e "Desenhar em 16:9" não existem |
| Widescreen em 20:9 | não testado | | | | |
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
