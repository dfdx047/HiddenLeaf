# Plano

Do Dragon Rage (BT3 descompilado, BT4 recompilado) para o Naruto Ultimate Ninja 5. A cadeia é a mesma do BT4:

```
ISO do jogador → ELF + overlays exatos (SHA-256) → mapa de funções (Ghidra) → PS2Recomp → C++ gerado
                                                                                  ↓
   runtime (sem janela) ── stream do GS ao vivo (formato de dump do PCSX2, num anel em memória) ──┐
          ↑ pad (anel)                                                                             ↓
   host Android (SDL3): janela, eventos, pad, menu de toque ── renderizador ARMSX2 (libgsx)
```

## Decisões

- **Base: a versão europeia (SLES-55605).** Traz cinco idiomas e é de onde os mods partem.
- **Mods por cima, sem mexer na base** (launcher: página Mods). Mod de dados roda direto; mod de código precisa do
  pacote de código dele. Primeiro pacote: **UN6 Alpha 3.5**.
- **Nada do jogo no repositório nem no APK.** O código é gerado a partir do ELF do jogador; o repositório guarda só
  ferramentas, runtime, configuração e hashes.
- **Overlays escolhidos pelo hash do arquivo**, não pelo id (o `MOD.BIN` do UN6 reusa o id 8 do `TEXTSPA.BIN`).
- **O jogo roda pelo recomp estático.** Do ARMSX2 (fork do PCSX2, GPL-3) entram só componentes: o **SPU2** (PC e
  Android) e o **renderizador do GS** (Android). Nada do JIT nem dos recompiladores dinâmicos do ARMSX2. O PS2Recomp
  já é GPL, então o código do ARMSX2 entra como patch em `recomp/patches/` sem conflito de licença.
- **Áudio:** o jogo usa CRI ADX (`CRI_ADXI.IRX`) e o sintetizador da Sony (`SNDBASE.IRX`, `modhsyn`/`modmidi`); os
  dois saem pelo SPU2 via libsd, que já roda de verdade no ps2xIOP. Falta o SPU2: registradores em `0x1F900000`, DMA
  dos canais 4 e 7, ADPCM das 48 vozes, ADSR, as duas cores e depois o reverb, mixados em 48 kHz para o áudio do
  raylib que o runtime já abre. Primeiro objetivo: a música do menu.

## Fases

### 1. Mapa de funções (Ghidra)
- [ ] Projeto Ghidra com o ELF europeu (MIPS R5900, little endian) e os overlays `BTL`, `ADV`, `ETC`, `TEXT*`,
      cada um como bloco de memória sobreposto no endereço de carga (`0x6c6d00` e `0x8f3d00`).
- [ ] Exportar com `ps2xRecomp/tools/ghidra/ExportPS2Functions.java` (melhor que o analisador do PS2Recomp em jogo
      comercial sem símbolos).
- [ ] Identificar as funções da SDK da Sony (assinaturas), da CRI (ADX, Sofdec: `PSS/`) e do CodeWarrior.
- [ ] O mesmo para o A35: ELF, overlays alterados, `MOD.BIN` (`0x940000`) e o código do `TEXTBRA.BIN`
      (`0x936fb4–0x93ffb8`).

### 2. Recompilação no PC
- [ ] PS2Recomp de cima (ran-j) e, do BT3-Recomp, só o genérico: correções de tradução VU0/VR*, o caminho headless
      (`PS2X_HEADLESS_HOST`), o anel ao vivo (`ps2_gs_live.cpp`, `ps2_live_ring.h`), os fibers no Android, o readback.
- [ ] Runtime: carregar os overlays `MWo3` sob demanda, trocando a tabela de funções da região pelo hash do arquivo.
- [ ] Disco: a ISO lida no lugar; `un5_mods.txt` com os mods ligados, em ordem, cada um com arquivos por cima da ISO.
- [ ] Rodar no PC com o `pcsx2-gsrunner` ao vivo, como o dbz-bt4-recomp, até dar boot, menus e uma luta.
- [ ] Vídeos (`PSS/`, Sofdec/MPEG-2) e áudio (`CRI_ADXI.IRX`, AFS): IPU e SPU2 do runtime.

### 3. Android
- [ ] Host do BT4 (`bt4_main.cpp` + `bt4_shim.c`) como host genérico; motor `libun5.so`.
- [ ] Renderizador ARMSX2 (`libgsx`) com o `.ini` do UN5 (o serial e os ajustes de GS do jogo).
- [ ] Argumentos: pasta do jogo, pasta das `.so`, driver (pasta e arquivo), ISO como `/proc/self/fd/N`
      (`GameActivity` já passa os cinco).
- [ ] Páginas de 4 KB no build do ARMSX2 (com 16 KB o núcleo recusa iniciar).

### 4. Pacote de código do UN6 A35
- [x] O pacote é o jogo do mod inteiro recompilado (`recomp/build.sh --mod`, `recomp/android/build.sh --mod`), não só
      as funções alteradas: `libun5_<id>.so`, carregada pelo launcher com o mod ligado (2026-11-03).
- [x] A35 até a luta no Cube (com o save copiado à mão, 2026-11-03).
- [ ] Save do A35 pelo launcher; Alpha 1.1X.
- [ ] Depois: interpretador para as funções de um mod desconhecido.

### 5. Ajustes do jogo
- [ ] Widescreen: procurar patch 16:9 da comunidade (pnach) e aplicar no runtime.
- [ ] 60 fps: o jogo europeu é PAL (25/50); o A35 já é NTSC. Preferir quadros intermediários no renderizador.

## Estado do boot (2026-10-26, branch `lab/data-cvm-boot`)

### Onde o jogo está

- Boot → **tela de idioma** (✕ escolhe ENGLISH) → logo da BANDAI NAMCO → filme `\PSS\LOGO_C.PSS` (CRIWARE e
  CyberConnect2) → carga do `DATA.CVM` → `\PSS\NARUTO10.PSS` (o "10th") → abertura em anime `\PSS\OPENING.PSS` →
  tela de título, **ainda sem a cena 3D** (ver "Tela de título" abaixo): com START no filme, só o efeito de transição
  (zoom radial) roda sobre o último quadro do filme por ~12–27 s e o jogo volta ao `NARUTO10.PSS` (modo de atração).
  O Naruto no manto da Kyuubi e o logo no fim são do próprio `OPENING.PSS`, não da cena em tempo real. Sem áudio.
- Com START de novo durante a transição (`hl_pad`), o jogo faz a carga automática do memory card (modo 1, ver
  "Memory card") e mostra "No … data found … Please insert memory card (PS2) with … data" → ✕ → "Return to title
  screen? (null)Yes(null)". → (o texto fica vermelho) e ✕ escolhem continuar: outra transição parada e, ~27 s depois,
  o modo de atração. O menu principal também não desenha.
- Numa execução de 2026-10-25 (`build/runs/run_2315.log`) o jogo carregou o **`BTL.BIN`** (`[un5] overlay btl (id 1)
  resident`) e mostrou os menus em janela ("No Naruto Shippuden: Ultimate Ninja 5 data found on memory card…",
  "Return to title screen?") e depois voltou ao "10th". O log só tem `HL_AUTOPRESS` com CROSS aos 15 s, então ou
  houve tecla na janela, ou entrada fantasma. Não reproduzido em 2026-10-26: o START só foi apertado na abertura.
- Nos menus aparecem `(null)` em volta de "Yes": provavelmente ícones de botão que o texto procura por nome.
- gp = `0x617ef0`. Laço principal `0x1C5DA0`; tarefas = threads (8 = sequência de boot `0x1E6B20`, 13 = loader).
- **Filmes (libmpeg da Sony, stubs do runtime em `Stubs/MPEG.cpp`).** Thread de stream `sub_00104020` (lê o CD em
  anel, `sceMpegDemuxPssRing`), thread de vídeo `sub_00101C00` (buffer livre `1015B0` na fila gp-0x3C8 →
  `sceMpegGetPicture` → envia `101620`), exibição no vsync `sub_001052E0` (só com gp-0x3B0 ligado, que `1044A0` liga
  quando o áudio pré-carregado em obj+0x54 chega a obj+0x48 = 24576). O quadro sai num pacote DMA do VIF1 com o
  callback `105120` (`109090`), chamado pelo tratador de fim de DMA `108D10` → `108F00`, que acha o pacote em
  `D1_MADR - 0x10`; `105120` tira o quadro da fila. Fim: `sceMpegIsEnd` (`0x159748`) e `sceMpegIsRefBuffEmpty`
  (`0x159758`), sem nome no ELF.
- Corrigido nesta etapa: VU0 (`sceVu0*` recompiladas, vf0 fixo em todas as threads), blit com ZTE=0 apagando a origem
  (0015), ID 0x79 do controle em modo de pressão (0016), callbacks de stream perdidos porque o heap do runtime está
  cheio (0017), MADR no fim da cadeia DMA (0018), `sceMpegIsEnd`/`IsRefBuffEmpty` ligados por endereço (`un5.py
  stubfix`, `STUB_AT`), fim dos filmes sem o EOF do stream (0019: o jogo nunca lê o último setor do PSS, só com o
  código de fim de programa), overlays com o código em addr+0x40 e os construtores estáticos (código dentro dos dados,
  tabela nos dois últimos campos do cabeçalho `MWo3`) marcados como código.

### Noite de 2026-11-01: título, menu e velocidade

**"Malha ampliada" era Z e quadro sem limpar (0032).** O título parecia ampliado por uma malha de pós-efeito, mas a causa
é outra: o stub de `sceGsSetDefDBuff` lia os argumentos da pilha (na ABI do EE, a0–a3 e t0–t3 vêm em registradores) e
ignorava `clear`. O fim do pacote de cada quadro (`0x9391a0`: TEST, PRIM, RGBAQ, XYZ2×2, TEST) ficava zerado no recomp, e
os pixels da animação de abertura (logo gigante, Z alto) ficavam na tela para sempre. Comparado entre a RAM do PCSX2 e a
do recomp. Com o 0032 o NOW LOADING e o **Mode Select** batem com o vídeo de referência.

**Caminho até o menu:** START pula os filmes; o título abre com o cursor em Load Game; sem memory card, Load Game dá
"No data… / Return to title". **New Game = UP, CROSS** → NOW LOADING → Mode Select. Em Mode Select, `DOWN WAIT WAIT CROSS`
(arquivo `hl_pad`) entra em Free Battle.

**Velocidade (0034).** Perfil antes: 85% do tempo no GS, 20 vsyncs/s no menu. `src/lib/gs/gs_cpu_fast.inl` refaz
triângulo e sprite com tudo decodificado por primitiva: tabela de swizzle lida direto, paleta decodificada em cache,
filtro bilinear em inteiros, spans calculados por linha. **Menu e filmes agora a 50 vsyncs/s** (com folga de CPU por
medir); título com a cena completa ~37 vsyncs/s. O caminho antigo continua como referência: formato desconhecido ou
textura lida das páginas sendo desenhadas (a textura e o quadro se sobrepõem) vão por ele. `HL_GS_VERIFY=N` desenha uma
primitiva em N pelos dois caminhos e reporta bytes que diferem mais de 3. No título/menu: ~10% das primitivas conferidas
diferem em 1–64 bytes (pixels de borda de triângulo, arredondamento); nada visível.

**Funções do ELF principal só alcançadas por overlays:** `un5.py fnmap --also PRG/*.BIN` varre JAL/J e ponteiros dos
overlays (602 funções a mais, entre elas a `0x3d1f60` que o BTL chama e que dava `missing-target` e tela branca depois de
Free Battle).

**Da seleção à luta (0035–0037 e `fnmap`):** o BTL está residente e a luta roda (HUD, cronômetro, câmera). No caminho:
pilhas de invocação do EE esgotadas (uma por id de thread; o loader cria uma por arquivo: 0035 recicla as de threads
apagadas); `SNDBASE.IRX` em laço com `ioman` sem implementação (`open/read/lseek/close` no IOP, 0036; `close` tem de
devolver o descritor); `_strtod_r` em laço infinito porque `_Balloc` devolvia 0 (o jogo toma o heap inteiro no boot: o
0037 dá a libc blocos pequenos da área de pilhas); e funções sem nome: `fnmap` agora marca prólogos depois de retorno
e folhas alinhadas a 16 bytes (métodos virtuais vazios como `0x6ea220`), o BTL foi de 2810 para ~5700 funções.
**Rota de teste:** `HL_AUTOPRESS="15:CROSS,35:START,50:START,58:START,64:UP,68:CROSS"`, aos 92 s `DOWN WAIT WAIT CROSS`,
aos 110 s `CROSS WAIT WAIT CROSS`, aos 118 s `CROSS`, depois CROSS a cada 6 s; a luta começa perto de ~210 s.

**Rota por vsync (substitui a de cima):** `HL_PADROUTE="tick:BOTÕES,..."` anexa ao `hl_pad` por contagem de vsync, sem
depender da velocidade do PC. Rota completa até a luta (começa em ~5200 vsyncs, ~200 s):
`650:CROSS,1560:START,2080:START,2330:START,2520:UP,2650:CROSS,3179:DOWN WAIT WAIT CROSS,3587:CROSS WAIT WAIT CROSS,`
`3827:CROSS,4059:CROSS,4233:CROSS,4391:CROSS,4545:CROSS,4715:CROSS,4893:CROSS,5083:CROSS,5226:CROSS,5306:CROSS,5386:CROSS,5468:CROSS,5548:CROSS,5630:CROSS`.

**Luta (0038–0040):**
- 0038, ponto flutuante do EE: `SQRT.S` lia `fs` em vez de `ft` (49 instruções erradas); `CVT.W.S` trunca e satura; divisão
  e raiz do VU0 com a semântica do PS2. O cenário passou a sair inteiro e estável.
- 0039, sem fatia de tempo entre threads de mesma prioridade: era por isso que a luta nunca começava (estado dos
  personagens em `0x8ed3f0` zerado). Agora o cronômetro conta, o HUD aparece, sem travar.
- 0040, opcodes do EFU do VU1 pela tabela do manual (`0x78` ESQRT, `0x79` ERSQRT, `0x7A` ERCPR, `0x7C` ESIN, `0x7D` EATAN,
  `0x7E` EEXP), também na tabela de latência. O interpretador rodava EEXP onde o jogo pedia EATAN/ERSQRT.

**Aberto: personagens invisíveis.** As matrizes dos ossos explodem (1e35, nan). A origem está nas chaves de rotação da
animação (tipo eixo+ângulo, 32 bytes: x, y, z, ângulo, duração; lidas em `0x1a8128`–`0x1a81f8`, `func_10C160` faz
eixo+ângulo → matriz): no recomp o ângulo é sempre 0 (803 chaves) ou ±`0x7f800000` (75); no savestate do PCSX2 são normais
(1,17; −2,13; π/2). O eixo está certo; só o ângulo é errado, e essas chaves não existem cruas no disco, logo são
calculadas ou descomprimidas na carga. `sin(inf)` dá nan e contamina tudo daí em diante. **Quadros alternam** HUD (sem
cenário) e cenário (sem HUD): só olhar de novo quando os personagens saírem. Endereços são determinísticos entre execuções
com a rota por vsync (chave em `0x1c391f0`), o que permite watchpoint por `mprotect`.

**Perfil da luta:** ~13 vsyncs/s; interpretador do VU1 ~55% (`commitReadyPipelines` 22%, `run` 7%,
`calculatePairReadyCycle` 6%), `DrawSpriteFast` 15,6%, `DrawTriangleFast` 3%; o EE quase não aparece. Para 50 vsyncs/s falta
um caminho rápido do VU1 (~4×).

**Regressão aparente depois do `8a3902d` e causa real (0041):** a luta parecia não começar (cenário estável, sem START
BATTLE, personagens ou HUD). Não era lógica: a máquina de fluxo `0x1f5eb0` passa por 0 → 2 → 3 → 4 no tempo certo (estado 2
é o banner, ~100 vsyncs; 4 é a luta correndo). O runtime parava a cadeia de DMA em `kMaxChainTags = 4096`, e a lista de um
quadro de luta tem ~11700 tags (11642 no savestate): o cenário cabia, o resto nunca chegava ao VIF. Com a matemática do
runtime a câmera virava NaN, o cenário sumia da lista e o HUD cabia nas 4096 tags; por isso parecia que o `8a3902d` tirara o
HUD. Ele só deixou o cenário correto. O 0040 não tem relação (reverter não muda nada). Como se achou: o fluxo do GIF de três
quadros tinha o mesmo tamanho exato e terminava no meio do cenário.

**Com o 0041:** banner START BATTLE, personagens, HUD, cronômetro e contador de golpes aparecem (vsync 5264 e 5358 em diante;
`build/runs/fight_2026-11-01.png`). A luta caiu para ~5 vsyncs/s, porque agora o quadro inteiro é processado. Nos dumps a
imagem ocupa só os 512 pixels da esquerda de 640 (a causa não foi investigada).

**Laboratório:** os scripts (`route.sh`, `fightram.sh`, `bannerdump.sh`, `dl.py`, `gifwin.py`…) e os dumps ficam em
`build/lab/` (ignorado). Nada em `/tmp`: a queda de energia apagou o scratchpad.

**Pendências vistas:** "(null)" no menu de memory card segue sem causa (fora do caminho New Game); filmes têm quadros
pretos em alguns dumps desde o 0032 (o stub de `sceGsSwapDBuff` limpa o quadro); um osso do objeto do título (`+1c`, osso 5)
com matriz errada (R -3e38/nan contra P 0.425,-0.425,-0.85), provavelmente o cursor.

### Tela de título (2026-10-26, histórico: a malha era o 0032)

**0030, cor e alpha zerados:** com o 0029 a cena saía escura e falhada. No log das primitivas do GS, ~18 mil dos
triângulos que o VU1 manda por quadro chegavam com RGBA = 0 (invisíveis, alpha 0). O microprograma do VU1 que desenha a
cena (o de `0x000`, por lote a partir de `0x049`/`0x066`) lê a cor da entrada como inteiros (`LQ.xyzw vf7, 1(vi2)`,
0..255) e a copia para a saída com `MAX.xyzw vf10, vf7, vf7`. Inteiros pequenos são denormais como float; o
interpretador do VU1 passava os operandos de `MAX`/`MINI` pelo `normalizeOperand` (denormal vira 0). A VU compara as
palavras cruas (o PCSX2 também: `fp_max`/`fp_min` com inteiros). O 0030 faz `MAX`, `MAXbc`, `MAXi`, `MINI`, `MINIbc` e
`MINIi` compararem as palavras como o PCSX2, com um teste no `ps2_vu1_tests.cpp` (falha nos 5 casos de denormal sem a
correção). O `VMAX`/`VMINI` do VU0 recompilado usa `_mm_max_ps`, que já preserva denormais.

**Câmera `+0x274` (0,75 no PCSX2, 1,0 no recomp) não é bug:** é o patch de widescreen do próprio PCSX2
(`resources/patches.zip`, `SLES-55605_C071D4C1.pnach`: `patch=1,EE,20B3EF14,extended,3F400000`, com
`EnableWideScreenPatches = true` no `PCSX2.ini`). Para comparar câmeras, desligar os patches de widescreen no PCSX2.

**Agora:** personagens, fundo, botões e textos com as cores do PCSX2. O que ainda difere: todo quadro termina com um
pós-efeito (copia o quadro para fbp 384 com 17 sprites, máscaras, um quad preto de tela cheia cujo alpha cai de 128 a
0 em ~5 s depois que o título aparece, e por fim uma malha de 288 triângulos com a cópia como textura). Mesmo depois do
fade, essa malha fica maior que a tela (de (-9, -78) a ~(521, 530) em vez de (0, 0)–(512, 448)), então a imagem sai
ampliada e o logo fica grande, com rastros. Próximo passo: de onde vêm os vértices dessa malha (lote do VU1 com a
textura tbp 12288) e o que o PCSX2 tem nesse ponto (com o widescreen desligado). Velocidade: a lógica roda a ~9
quadros/s (contador do título), o rasterizador do GS continua sendo o gargalo, e o título cai no modo de atração em
~100 s.

### 0029, matriz de mundo zerada

**Causa (achada comparando com o PCSX2):** o recompilador traduzia `VSQI`/`VSQD` do VU0 com os campos trocados (o VF
vinha de `it` e o VI de `fs`). `0x19fec0` monta a matriz de mundo de cada objeto da cena (`+0x00`) a partir da local
(`+0x40`) subindo pelos pais (`+0x80`), com uma pilha na memória de dados do VU0: empilha os nós com
`vsqi $vf1,($vi1++)` e fecha com `vsqi $vf0,($vi1++)`. Com os campos trocados, o terminador ia para `vi0` (que ainda
era incrementado), a leitura seguinte pegava um ponteiro velho como pai e a matriz de mundo saía zerada (com
`8b06af3a` em `+0x30`): os objetos do título não tinham tamanho e nada passava do recorte. O 0029 corrige os campos,
faz os quatro (`VLQI`/`VSQI`/`VLQD`/`VSQD`) usarem a memória de dados do VU0 (4 KB, não a RAM do EE em `vi*16`) e
mantém `vi0` em 0 depois de qualquer instrução, como o 0013 faz com o `vf0`.

**Método:** savestate do PCSX2 na tela de título (mesma ISO do Redump; o `.p2s` é um zip, `7z x estado.p2s
eeMemory.bin`), RAM do recomp gravada com `hl_ram` depois do START, e `tools/ramdiff.py` andando pelas estruturas a
partir do objeto do título (`*(gp-0x22c)`, raízes com nome no próprio script). Os heaps não têm o mesmo layout (a
câmera da cena está em `0xb3eca0` no PCSX2 e em `0xb3ec90` no recomp), então a comparação segue os ponteiros dos
dois lados. A primeira diferença foi `title/30+0` (matriz de mundo: escala 1,075 e posição (-3, 10) no PCSX2, zeros
no recomp); o watchpoint nela levou a `0x19fec0` ← `0x194390` ← `0x1bff50` ← `0x1e5740`.

Depois do 0029 a cena do título passou a ser desenhada, mas escura (ver 0030 acima) e lenta: ~85 % do tempo no
rasterizador de software do GS (`GSCpuBackend::DrawTriangle`, `SampleTexture`, `LookupCLUT`, `WritePixel`; amostragem
com o gdb, sem `perf` instalado), com os triângulos texturizados de tela cheia que o VU1 manda.

### Antes do 0029 (histórico)

Sintoma: a fase do título roda (carrega a cena, toca o som), mas **nenhum objeto entra na lista de desenho**; a
tela fica no último quadro do filme.

**Correção de 2026-10-26 (tarde):** o primeiro relatório dizia que o 0025 resolvia o título. Errado: os PNGs daquele
relatório (`build/runs/title_2026-10-26.png`, ticks 3536–4689 do `di2.log`) eram do `OPENING.PSS` tocando de novo no
modo de atração. Com o 0025, o VIF1 recebe a cena **uma vez**: logo depois do START, um quadro sai com 0xc2e40 bytes
(33 `MSCAL`, 186 `MSCNT`, `UNPACK` V2/V3/V4) e deixa traços escuros da geometria no quadro; todos os outros ficam
em 0x2c48 bytes (13 tags, só `MPG`, nada para desenhar), como antes do 0025.

O que se sabe do título depois do 0025 (gdb, 2026-10-26):

- `0x21a160` cria um contexto de desenho (`0xbd8330`) com uma câmera padrão (`0x10a360` → `0x10f4f0`, câmera em
  `0xbd5a40`, `contexto+0x3c` aponta para ela e nada muda isso depois). `0x10f4f0` monta as matrizes (`0x10dd00`)
  **antes** de `0x10f640` → `0x10e670` preencher os campos `+0x26c…+0x290`: as matrizes `+0x40`, `+0x100`, `+0x140`
  saem da sobra do heap (palavras `00c80005 30000080`, da lista de tags DMA liberada que ocupava o bloco) e nunca são
  refeitas. No PS2 o código é o mesmo; a diferença deve estar no que havia antes nesse bloco (ordem das alocações).
- `0x10a6b0(objeto, contexto)` usa essa câmera: projeta (0, 0, z, 1) pela matriz `+0x140` e divide por w = 0
  (`0x10ae58`). Roda 29 vezes em ~1,1 s depois do START e para; são os 29 `div0` do `HL_FLOAT_LOG`.
- A câmera da cena (`0xb3ec90`, e a cópia `0xbf5260` via `0x1c01b0`) é atualizada todo quadro por `0x1e5740` →
  `0x10e430` → `0x10dd00`, com campos válidos, mas o desenho não a usa.
- Rodízio de threads de mesma prioridade desligado (teste): nada muda. Primeiro `GetInfo` = -1: nada muda.
- Com a ISO do Redump no lugar da que tinha o patch de DVD para CD (`docs/analise-elf.md`): igual, só a transição
  sobre o último quadro do filme. A ISO não era a causa.

Próximo passo: achar por onde os objetos da cena (câmera `0xb3ec90`) deveriam entrar nas 8 camadas (item 3 do
"Falta") e se a câmera padrão com lixo é que barra (testar zerando o bloco da câmera em `0x10a360`).

**Causa:** o escalonador do runtime ignorava o `Status.EIE`, então trocava de thread (interrupção, fatia de tempo)
dentro das regiões entre `DI` e `EI` do jogo. O heap do jogo (`0x118640`, lista livre em `0x618250`, entradas
`0x1176c0`/`0x1178e0`/`0x117c70`) se protege só com `DI` (`0x168b60`/`0x168bb0`); com threads entrando no meio dele
(retomadas em `0x1186cc` com pilhas de threads diferentes), saíram blocos sobrepostos: a câmera do título
(`0xbd5a40`, 0x2c0 bytes, criada por `0x10a360` ← `0x21a160`) caiu em cima da lista de tags DMA (REF, 0x100 × 16
bytes em `0xbd5a80`) que a troca de modo de vídeo (`0x1058f0` → `0x102220` → `0x1037a0`) aloca a cada cena. A lista
sobrescrevia as matrizes da câmera (`+0x40`, `+0x100`, `+0x140` com `0x30000080`…), a divisão de perspectiva
(`0x10ae58`, em `sub_0010A6B0`) dividia por zero e nada passava do recorte, então o VIF1 só recebia o `MPG` e nunca o
`MSCAL`. Com o `DI` respeitado (0025), a cena passa a ser enviada, mas só num quadro (ver acima). Itens 0 e 1 do "Falta" foram feitos antes (0021–0023, carga do
`title.ccs` conferida: o trecho descomprimido bate 100 % com o arquivo).

Notas da investigação (para referência):

- **Sequência no log** (`HL_CD_LOG=1`), logo depois do fim/pulo da abertura: 7 leituras do `DATA.CVM` (`lbn 0x131ffd`
  a `0x1320bd`, 0x20 setores cada, ~388 KB, em anel de 4 × 64 KB em `0xbee700`/`0xbfe700`/`0xc0e700`/`0xc1e700`;
  parece arquivo comprimido sendo lido em pedaços, provavelmente o `title.ccs`); depois o som do título do
  `SOUND.AFS` (`lbn 0x100004` a `0x10013c`, pedaços de 0xe/0xb setores em 3 buffers `0x63ad40`/`0x641d40`/`0x648d40`,
  um a cada ~60 vsyncs); quando o som acaba, ~500 vsyncs depois, `NARUTO10.PSS`. Toda leitura sai de `ra 0x141338`.
  Nenhum erro, nenhum overlay carregado.
- **GS** (`HL_GIF_LOG=3000@145`, decodificado com `tools/gifdec.py`): por quadro, 8 pacotes só com
  `SCISSOR_1=0x1bf000001ff0000` (as 8 camadas, vazias) e o blit final (`TEX0_2=0x268020000`, `FRAME_2` fbp 0x70, 8
  sprites de tbp 0). Nenhuma primitiva da cena, nenhuma textura nova. A VRAM e os dois contextos (`HL_FRAME_DUMP`)
  só têm o último quadro do filme.
- **VIF1** (`HL_VIF_LOG=400@150`, `HL_DMA_LOG=1`): a cadeia do título tem 13 tags e 0x2c48 bytes (pacotes alternando
  em `0x938b90`/`0x938bf0`): 8 × `MPG` (op 0x4a, microcódigo do VU1 em 0x300–0x791, num 0x5c…0xfc), `BASE` (0x03),
  `OFFSET` 0x200 (0x02), `MSKPATH3` (0x13), `DIRECTHL` 0x33 qwords (o blit), e 8 × (`FLUSHA` 0x11 + `DIRECT` 2 qwords,
  o `SCISSOR_1`). **Nunca há `MSCAL` nem `UNPACK`**: o VU1 recebe o programa, mas nada para desenhar. O runtime tem
  interpretador de VU1 (`ps2_vu1_core.cpp`, XGKICK), então se o jogo mandasse, seria executado.
- **Threads** (`HL_EE_DUMP=1`): todas esperando no escalonador de tarefas do jogo (`pc 0x15e9c8`, `ra 0x1d55ec` ou
  `0x1d523c`); a 8 (sequência de boot `0x1E6B20`) também. Nada travado; o EE fica ocioso.
- **Código do título:** a string `title.ccs` está em `0x5c78a0` (outra em `0x3e8f90`, lista de `.ccs`); logo depois,
  em `0x5c78b0`, a tabela de saltos de `0x1e4790` (`switch` de 7 estados sobre `a1`; `a0` = objeto da cena):
  `0x1e47d4` e `0x1e4854` tocam a animação `+0x20`/`+0x24` do objeto CCS em `+0x1c` (`0x1be060`, `0x1bf9a0`,
  `0x1bfeb0`, `0x1c0680`, `0x1c0550`), `0x1e48d4` idem pelo quadro em `+0x90→+0xc` (`0x1bfd70`); `0x1e494c` = nada.
  Grava o estado em `+0x18`. As tabelas em `0x5c78e0`… (`0x1e5354`…) e `0x5c7940`… (`0x1e6554`…) parecem ser os
  `switch` da sequência de boot. Strings `ANM_title_menu01` em `0x417bf4`.
- **`DATA.CVM`:** cabeçalho `CVMH` com `ROFSROFSBLD`; o volume ISO dentro dele não está em claro em 0x1800/0x9800
  (provavelmente cifrado). `un5.py` ainda não lê o CVM: o teste 1 abaixo precisa disso.
- **Velocidade:** ~10–12 vsyncs/s (2130 vsyncs em 170 s com `HL_FRAME_DUMP=30`; 6840 em 720 s com 120). A abertura
  leva ~9 min; `HL_AUTOPRESS="15:CROSS,130:START"` cai no meio dela e pula para o título (~vsync 1750). Ainda sem
  perf.
- **Execuções guardadas** em `build/runs/` (ignorado): `run_long.log` (START aos 130 s, 10 min), `run_dma.log`
  (+ `HL_DMA_LOG`/`HL_EE_DUMP`), `run_gif.log` + `hl_gif_title.bin` (pacotes GIF do título), `run_cd.log`
  (+ `HL_CD_LOG`/`HL_RPC_LOG`), `run_vif.log` (VIF1 do título), `run_nat12.log`/`run_nat20.log` (abertura até o fim,
  12 e 20 min), e os quadros em `hl_frames_run1…8`.
- Rodar sem o `build.sh` (o executável já compilado; `hl_frames/` e `hl_gif.bin` saem no diretório atual):
  `cd build/disc && HL_AUTOPRESS="15:CROSS,130:START" HL_FRAME_DUMP=60 timeout 190
  ../PS2Recomp/build-rt/ps2xRuntime/ps2EntryRunner SLES_556.05 > ../run.log 2>&1`. Mudança só no runtime:
  `cmake --build build/PS2Recomp/build-rt` (segundos); o `build.sh` recopia o código gerado e recompila tudo.

### Gráficos (o que já funciona)

- O jogo usa a cadeia DMA do VIF1 (CHCR 0x145, TTE=1), PATH2 DIRECT/DIRECTHL; os filmes e os menus não usam o VU1 (a
  cena 3D do título usa, e desde o 0025 é enviada num quadro só). Por quadro: texturas
  (512x512 PSMT8 dbp 0x3c00 + CLUT 0x3be0, 256x256 T4 dbp 0x3800), contexto (FRAME_1 fbp 0, ALPHA_1=0x44) e o blit final
  (8 sprites de 64 px, tbp 0 → FRAME_2 fbp 0x70). DISPFB1 = 0x1070 (fbp 0x70, 512 de largura, CT32).
- Patch 0011 corrige o VIF1: o IMAGE que fica devendo dados é completado pelo próximo DIRECT, não pelos bytes crus.

### Variáveis de depuração

- `HL_GS_VERIFY=N`: compara o caminho rápido do GS com o antigo em uma primitiva a cada N (0034).
- Arquivo `hl_steps`: despeja o FRAME do contexto 1 nos próximos 40 TEXFLUSH em `hl_frames/step_NNN.ppm` (0033).

| Variável | O quê |
|---|---|
| `HL_FRAME_DUMP=N` | a cada N vsyncs grava `hl_frames/frame_*.ppm` (quadro exibido), `_vram.ppm` (VRAM inteira 1024x1024 CT32) e `_ctx0/1.ppm` (o FRAME de cada contexto), mais a linha `[frame-dump]` no log (0012) |
| `HL_GIF_LOG=N` | os N primeiros pacotes GIF (caminho, tamanho, GIFtag; A+D com reg=valor) (0012); `N@S` começa S segundos depois do primeiro pacote e grava os pacotes inteiros em `hl_gif.bin` (u32 caminho, u32 tamanho, bytes) (0020) |
| `HL_VIF_LOG=N` | os N primeiros códigos VIF1 (`[vif1] +off/size code op num imm`) (0012); `N@S` começa S segundos depois (0020) |
| `HL_DMA_LOG=1` | cada cadeia DMA (`[dma-chain] ch chcr tags bytes next lastTag`) (0012) |
| `HL_RPC_LOG=1` | cada chamada SIF RPC (0008) |
| `HL_IOP_TRACE=pc,pc` | registradores do IOP nesses PCs, em hexa (0009) |
| `HL_PADROUTE="650:CROSS,3179:DOWN WAIT WAIT CROSS"` | a mesma coisa do arquivo `hl_pad`, por vsync: a rota não depende da velocidade do PC (0039) |
| `HL_AUTOPRESS="15:CROSS,140:START"` | aperta botões sozinho (segundos desde a 1ª leitura do controle:botão, 0,25 s cada) (0014) |
| `HL_CD_LOG=1` | cada `sceCdRead` (lbn, setores, buffer, ra) (0004) |
| `HL_EE_DUMP=1` | a cada 5 s de EE ocioso, o que cada thread espera (0005) |
| `HL_FLOAT_LOG=1` | a cada 300 vsyncs, quantas diferenças de float PS2 × IEEE houve (expoente 255, divisão por zero, comparação, arredondamento do CVT.W.S, DIV/SQRT do VU0) e em que ponto do executável (deslocamento a partir de `__executable_start`, `addr2line`) (0024) |
| `HL_GS_DUMP=cena,...` (Android) | dump do GS do PCSX2 (`.gs.zst`) de `HL_GS_DUMP_FRAMES` vsyncs (300) em cada cena da rota, em `<pasta do jogo>/gsdumps/<HL_GS_DUMP_DEVICE>_<cena>.gs.zst`; `cena@vsync` muda o vsync; cenas e gatilhos em `hl_game.cpp` (`kDumpScenes`) e no `docs/registro.md` (11-03 22:05); `tools/gsdump.py info/shot` confere no PC |
| arquivos `hl_shot`, `hl_ram`, `hl_pad` | criados no diretório de execução enquanto o jogo roda (o runtime lê e apaga): `hl_shot` grava um quadro como o `HL_FRAME_DUMP`; `hl_ram` grava os 32 MB da RAM do EE em `hl_frames/ram_<tick>.bin` e os 4 MB da VRAM do GS, no layout do GS, em `vram_<tick>.bin` (0031; o `GS.bin` do savestate do PCSX2 tem a VRAM no mesmo layout a partir do byte 425, mas no renderizador de hardware os framebuffers não estão nela); `hl_pad` com botões (`UP UP CROSS`, `WAIT` = pausa) aperta cada um por 6 vsyncs e solta por 6 (0026) |

Depurar com gdb: `rdram = $rdi`, `ctx = $rsi`, `r[n]` em `$rsi+n*16`, `pc` em `$rsi+512`; rodar sob gdb (ptrace_scope
bloqueia attach), interromper com `pkill -INT -x ps2EntryRunner`. Watchpoint de hardware em `$rdi+endereço` acha quem
escreve; watchpoint condicional em `ctx->pc` é lento demais. Patches novos: gerar com um worktree do PS2Recomp em
`e563698` + os patches anteriores, nunca editar `build/PS2Recomp` sem gerar o patch (o `build.sh` faz `checkout -f`).

### Commitado

`b3c382b` fnmap (delay slot do `jr ra`); patches 0004 log do sceCdRead, 0005 dump do EE, 0006 pilha do main abaixo das
pilhas de callback, 0007 leitura de CD do IOP em qualquer RAM, 0008 log de RPC, 0009 trace do IOP, 0010 DMA do SPU2
antes de WaitEventFlag sem thread, 0011 VIF1 DIRECT, 0012 dumps de depuração, 0013 vf0 fixo, 0014 HL_AUTOPRESS,
0015 GS sem Z com ZTE=0, 0016 ID 0x79 do controle, 0017 callbacks de stream do MPEG, 0018 MADR no fim da cadeia DMA, 0019 fim dos filmes,
0020 logs de GIF e VIF com atraso, 0021 thread ociosa pula para o próximo evento, 0022 tempo do autoDMA do SPU2, 0023
bilinear sem texels de peso zero (0021–0023: de ~11 para ~52 vsyncs/s no título), 0024 `HL_FLOAT_LOG`, 0025 `DI`
segura interrupções e trocas de thread, 0026 arquivos-gatilho `hl_shot`/`hl_ram`/`hl_pad`, 0027 vblank a 50 Hz com o modo PAL do `sceGsResetGraph`/`SetGsCrt` (também o relógio do stream de CD e o PTS dos filmes), 0028 vblank atrasado não adianta os próximos (sem ele o jogo ficava em ~53/s depois do boot), 0029 `VSQI`/`VSQD` do VU0 com os campos certos e na memória de dados do VU0 (`vi0` fixo; a cena do título passa a desenhar), 0030 `MAX`/`MINI` do VU1 comparam as palavras cruas (cores da cena), 0031 `hl_ram` grava também a VRAM do GS;
overlays (addr+0x40, construtores);
`un5.py stubfix` (sceVu0 recompiladas, MPEG por endereço); build com FFmpeg.

### Falta

**Feito em 2026-10-26** (a lista abaixo fica como registro; o 2 explicou, ver "Tela de título"):

0. **Velocidade primeiro.** 1000 vsyncs levando minutos é lento demais. `perf record -g` por 30 s na tela de título e
   dizer onde está o tempo (logs ligados? lookup de função lento? GS? IOP?). Corrigir o que for barato antes de
   seguir: desligar logs que não estejam atrás de `HL_*_LOG`, cache no resolver de overlay
   (`recomp/runtime/un5_overlays.cpp`, gancho `0001-overlay-resolver.patch`).
1. **Carga do `title.ccs`:** logar cada arquivo aberto dentro do `DATA.CVM` (nome, LSN, tamanho no diretório) e
   quantos bytes cada leitura devolveu. Extrair o arquivo real com `un5.py` (precisa ler o CVM, ver acima) e comparar
   o hash. Se veio curto ou diferente, o problema é a leitura do CVM/`sceCdRead`.
2. **NaN/infinito:** contar os NaN/Inf gerados em COP1, VU0 macro e nas `sceVu0*` durante o título, com o endereço de
   quem gerou. Se aparecerem, aplicar o tratamento do PS2 de forma global no runtime (resultado limitado a ±FLT_MAX,
   denormal vira 0), não caso a caso.
3. Se 1 e 2 estiverem limpos: achar a função que põe objetos nas 8 camadas da lista de desenho (watchpoint no
   ponteiro de escrita da camada) e seguir para trás a condição que impede de pôr.

Parar no primeiro que explicar. Depois: o PNG do título e o menu principal com `HL_AUTOPRESS` (o memory card vazio
pede para criar os dados).

Depois disso:

- O menu principal: bloqueado pela mesma falta de cena 3D (ver "Onde o jogo está").
- **Memory card** (achado em 2026-10-26): depois do título, a máquina de estados `0x1efe60` chama `0x1e9d00(menu, 1)`
  e o 1 põe o gerenciador (`*(gp-0x1fc)`) no modo de carga (`+0x54 = 1`). A checagem `0x1c6c10` (`sceMcGetDir` de
  `/BESLES-55605NARUTO5`) devolve 4 (sem dados; 3 se faltar espaço) e, no modo 1, as duas viram a mensagem 41 ("Please
  insert … with … data"); fora do modo 1 seria a 12 ("Create new … data?"). O texto vem de `0x3d3800(id)`: tabela
  `0x6c6890 + idioma*0xc0 + id*4` (montada pelo construtor `0x5e6810` a partir do `TEXTENG.BIN`). Ou seja, a mensagem
  é a do jogo para carga sem dados, não erro do runtime. O `GetInfo` do runtime nunca devolve -1 (cartão novo);
  testado, não muda nada aqui.
- A fatia de tempo do escalonador (`checkpointDue`, `kDefaultTimeSliceCycles`) reveza threads de mesma prioridade; o
  kernel do PS2 não faz isso. Fica como suspeito se aparecer outra corrida.
- Semântica de float que o `HL_FLOAT_LOG` mostrou diferente do PS2, sem efeito visível por enquanto: `CVT.W.S` arredonda
  em vez de truncar (215 funções), `VDIV` por zero dá 0 (no PS2, ±máximo), `VSQRT` usa `max(0,x)` (no PS2, `|x|`),
  `VRSQRT` ignora `fs`. Os 29 div0 no início do título vêm da câmera montada antes de o jogo preencher os campos.
- A imagem sai com 512 de largura numa janela de 640 (faixa preta à direita). O jogo monta o `DISPLAY1` com MAGH = 0
  e DW = 511 (`gfx+0x2c8`, `0x0002027c`/`0x001ff1ff`); no PCSX2 sai MAGH = 4 e DW = 2559 (`0x02048290`/`0x001ff9ff`) e
  o `PMODE` com MMOD/AMOD (`0x7f67` contra `0x7f07`). Ainda não investigado (suspeita: o parâmetro de modo de saída
  que a libgraph guarda no `sceGsResetGraph`).
- ~~O vsync é de 60 Hz, mas o jogo é PAL~~: feito no 0027 (50 Hz quando o jogo pede PAL).
- ~~Os `(null)` dos menus.~~ Resolvido em 2026-11-02 (`0052`): o `va_list` do EE tem slots de 8 bytes, o stub andava de 4 em 4.
- Áudio: SPU2 do ARMSX2 (ver Decisões).
- Desempenho do IOP (a IRQ do libsd dispara centenas de milhares de vezes por causa do autoDMA do ADX).

## Fase Android (desde 2026-11-01)

**60 Hz:** o jogo não oferece. Options tem Difficulty, Control, Screen (só posição X/Y), Music e Reset; a meta é 50 fps (PAL).

**VuWorker (`hl_vu_worker.cpp`, patch 0044):** o disparo de DMA de VIF1/GIF era 60% da thread do jogo. A cadeia já era copiada no disparo,
então o resto (VIF1, VU1, XGKICK, anel do GS) foi para uma thread, em fila. O jogo vê o DMA terminar na hora, como antes. Ele só alcança o que
é da thread (memória da VU1, registradores VIF1/GIF, escrita no CSR, `readFifo`, A+D dos stubs) depois de `hl_vuworker::sync()`. O vsync entra
na fila com os registradores lidos na hora, e o jogo fica no máximo 1 quadro à frente. `HL_VUWORKER=0` desliga.

**Divergência aberta do caminho rápido (`HL_VUVERIFY`):** o teste de recorte da VU0 em 0xC00 recebe palavras `0xFFFFFFFF` como entrada (não são
floats). Os resultados saem a 1 ulp um do outro perto de fmax, e o sinal do teste seguinte pode divergir do interpretador. Não está explicado por
completo, e nenhum dos dois é o hardware. Nenhum defeito visível nos prints. As 60 divergências registradas nessa rodada são todas dessa classe
(o log só guarda as 60 primeiras); falta a contagem por unidade.
`HL_VUVERIFY=vu0` verifica só a VU0, e a primeira divergência vai para `hl_vu_mismatch.bin`.

Arquitetura: `libun5.so` (runtime sem janela, `PS2X_HEADLESS_HOST`, código gerado, `recomp/android/host/`) escreve o fluxo do
GS no formato de dump do PCSX2 num anel em memória (`ps2_live_ring.h`), lido por `libbt4gsx.so` (GS do ARMSX2 90126bdfea com
Vulkan, o mesmo do Dragon Rage: o PCSX2 upstream não tem build Android). A ISO é lida pelo fd do launcher (`hl_image.cpp`).
`recomp/android/build.sh` depende das saídas do build do Dragon Rage (libbt4gsx, SDL3, FFmpeg). O APK de teste tem o código do
jogo em `jniLibs/` (ignorado pelo git), então fica local; a decisão "nada do jogo no APK" ainda precisa de resposta.

**Cena de medida fixa:** Naruto x Naruto, cenário do lamen, pela rota `HL_PADROUTE` de `build/lab/hl_env.txt`.
`build/lab/measure.sh SERIAL TAG`: `HL_PERF=1 HL_PERF_FROM=5900` (10 s de aquecimento, 30 s de medida, linha `[perf] RESULT`).
`HL_FAST_UNTIL=5800` tira a espera do relógio até lá. O Retroid (perfil `sd480p`, não calibrado) e o Cube (sem limite) chegam à
luta em uns 12 e 8 min com o interpretador de VU.

**Colunas:** tempo por quadro em ms. EE, VU0, VU1, IOP, parse (parser do próprio runtime) e ring (escrita do fluxo) somam o
tempo da thread do jogo. GS é o tempo de CPU das threads `GS` e `CPU Thread` do PCSX2, em paralelo: conta contra o orçamento dele.

| data | build | aparelho | perfil | fps | 1% low | EE | VU0 | VU1 | GS | IOP | parse | ring | MB/q | o que mudou |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-01 | 8fac981+HL_PERF | Retroid Pocket 5 | sd480p | 1,5 | 0,7 | 32,8 | 31,8 | 608,5 | 55,1 | 20,1 | 3,1 | 8,7 | 3,22 | primeira medida, VU por interpretador |
| 2026-11-01 | 8fac981+HL_PERF | RG Cube | sem limite | 1,9 | 0,8 | 19,9 | 30,0 | 473,1 | 77,1 | 10,3 | 2,0 | 5,5 | 3,28 | primeira medida, VU por interpretador |
| 2026-11-01 | +VU rápida 1 | RG Cube | sem limite | 5,1 | 2,2 | 18,7 | 0,3 | 167,2 | 31,5 | 10,6 | 2,0 | 3,5 | 3,13 | caminho rápido de VU (ps2_vu_fast.cpp); o laço de vértices com FSAND ainda no interpretador |
| 2026-11-01 | +VU rápida 1 | Retroid Pocket 5 | sd480p | 3,9 | 1,5 | 32,6 | 0,4 | 194,2 | 33,1 | 21,9 | 2,8 | 6,9 | 3,12 | idem |
| 2026-11-01 | +flags de status | RG Cube | sem limite | 22,9 | 6,1 | 17,8 | 0,3 | 10,1 | 19,1 | 10,4 | 1,8 | 1,8 | 3,25 | FSAND/FSSET no caminho rápido (waitGS 2,7) |
| 2026-11-01 | +flags de status | Retroid Pocket 5 | sd480p | 14,7 | 4,5 | 29,3 | 0,4 | 12,5 | 23,2 | 20,2 | 2,4 | 5,9 | 2,99 | idem |
| 2026-11-01 | +lotes | RG Cube | sem limite | 23,8 | 5,8 | 15,9 | 0,3 | 10,6 | 17,3 | 4,8 | 1,9 | 1,0 | 3,25 | futex do anel a cada 128 KB, IOP em lotes de 1024 ciclos, sem limpar 64 KB por execução de VU; waitGS 8,5: o GS segura o Cube |
| 2026-11-01 | +lotes | Retroid Pocket 5 | sd480p | 24,9 | 8,8 | 17,5 | 0,4 | 13,1 | 11,1 | 5,3 | 2,6 | 1,6 | 3,17 | idem; waitGS 0 |
| 2026-11-01 | +sem desperdício no DMA | RG Cube | sem limite | 24,1 | 5,4 | 13,3 | 0,3 | 10,6 | 16,7 | 4,7 | 0,1 | 0,9 | 3,22 | buffer de cadeia reaproveitado, sem cópia no árbitro, parse do GS só SIGNAL/FINISH/LABEL, relógio só com evento vencido; waitGS 12,4: o GS limita o Cube em ~41 ms |
| 2026-11-01 | +sem desperdício no DMA | Retroid Pocket 5 | sd480p | 28,9 | 10,4 | 14,7 | 0,4 | 12,9 | 11,5 | 5,5 | 0,6 | 1,6 | 3,26 | idem; waitGS 0 |
| 2026-11-01 | +thread VuWorker | Retroid Pocket 5 | sd480p | 44,0 | 16,9 | 11,9 | 0,4 | 16,2 (VuWorker: VIF+VU1+GIF+anel) | 16,8 | 5,5 | - | - | 3,20 | VIF1/VU1/GIF numa thread (hl_vu_worker.cpp); waitVU 5,5; picos de 48, efeitos pesados caem a 31 (4,5 MB/f) |
| 2026-11-01 | +thread VuWorker | RG Cube | sem limite | 24,1 | 4,9 | 12,1 | 0,4 | 16,4 (idem) | 17,8 | 5,1 | - | - | 3,25 | waitGS 23,4, waitVU 25,1: quem limita é a GPU: PerfLog do PCSX2: GPU 74%, GS 30% |
| 2026-11-01 | +ForceMaliFramebufferFetch | RG Cube | sem limite | 47,4 | 19,5 | 10,3 | 0,3 | 14,9 (idem) | 11,1 | 4,9 | - | - | 3,14 | barreiras 1878 → 0, passes 519 → 22, GPU 75% → 13%; imagem certa; waitVU 4,8 |
| 2026-11-01 | +IOP e timers em lote, Bsymbolic, LTO no IOP | RG Cube | sem limite | 48,7 | 20,5 | 9,0 | 0,3 | 15,0 | 11,2 | 3,6 | - | - | 3,21 | waitVU 6,3, idle 1,9 |
| 2026-11-01 | idem | Retroid Pocket 5 | sd480p | 45,8 | 16,5 | 10,9 | 0,5 | 15,8 | 16,0 | 4,7 | - | - | 3,14 | waitVU 5,7 |
| 2026-11-01 | +vsync recupera até 4 atrasados | RG Cube | sem limite | 49,4 | 20,5 | 9,0 | 0,3 | 14,9 | 11,2 | 3,6 | - | - | 3,19 | maioria dos segundos em 50; quedas em efeitos pesados (38 no pior segundo, 4,5 MB/f) |
| 2026-11-01 | idem | Retroid Pocket 5 | sd480p | 46,6 | 15,4 | 10,7 | 0,5 | 15,8 | 15,7 | 4,6 | - | - | 3,14 | três threads pesadas (jogo, VuWorker, GS) em 2 núcleos grandes; pior segundo 31 fps |
| 2026-11-01 | +um salto indireto por operação da VU (clang `tail-dup`) | RG Cube | sem limite | 49,6 | 21,0 | 8,9 | 0,3 | 14,3 | 11,0 | 3,6 | - | - | 3,14 | hipótese (-10% na VU1) refutada: -0,1 ms; pares > 44 ms 11,8% → 8,3% |
| 2026-11-01 | idem | Retroid Pocket 5 | sd480p | 46,9 | 16,5 | 10,7 | 0,4 | 15,5 | 15,8 | 4,6 | - | - | 3,12 | -0,4 ms na VU1; pares > 44 ms 33,3% → 30,0% |
| 2026-11-01 | +UNPACK direto | RG Cube | sem limite | 49,7 | 22,1 | 8,8 | 0,3 | 13,8 | 10,8 | 3,5 | - | - | 3,13 | esperava -1,5 ms, deu -0,5; pares > 44 ms 7,9% |
| 2026-11-01 | idem | Retroid Pocket 5 | sd480p | 47,4 | 18,1 | 10,8 | 0,4 | 15,2 | 16,2 | 4,7 | - | - | 3,20 | pares > 44 ms 29,2% |
| 2026-11-01 | +MAC morto não calculado (`stickyLive`) | RG Cube | sem limite | 49,8 | 22,7 | 9,3 | 0,3 | 12,7 | 11,1 | 3,7 | - | - | 3,17 | pares > 44 ms 4,1% |
| 2026-11-01 | idem | Retroid Pocket 5 | sd480p | 48,2 | 17,0 | 10,6 | 0,4 | 14,3 | 16,3 | 4,8 | - | - | 3,19 | pares > 44 ms 21,5% |
| 2026-11-01 | +`sceGsSyncPath` não esvazia a fila; escrita no GS vai pela fila | RG Cube | sem limite | **50,0** | 26,1 | 8,9 | 0,3 | 13,1 | 10,6 | 3,4 | - | - | 3,14 | waitVU 5,3 → 0,1, idle 7,5; pares > 44 ms 0,9%, pior 1% 43,8 ms. **Velocidade cheia (sem áudio)** |
| 2026-11-01 | idem | Retroid Pocket 5 | sd480p | 49,3 | 17,7 | 10,9 | 0,4 | 15,2 | 17,3 | 4,8 | - | - | 3,20 | waitVU 1,1, idle 3,7; pares > 44 ms 22,2%, quedas a 39 fps nos efeitos pesados. GS é o maior custo |
| 2026-11-01 | +nice -10 nas 3 threads pesadas, IOP sem chamada por acesso à RAM | RG Cube | sem limite | 50,0 | 27,3 | 8,9 | 0,3 | 13,3 | 11,3 | 2,7 | - | - | 3,17 | idle 8,6; pares > 44 ms 0,7%, pior 1% 42,0 ms |
| 2026-11-01 | idem | Retroid Pocket 5 | sd480p | 49,3 | 16,3 | 11,2 | 0,4 | 14,8 | 17,3 | 3,7 | - | - | 3,21 | idle 4,5; pares > 44 ms 11,6%, pior 1% 64 ms. Só o nice (medido antes do IOP): 22,2% → 14,5%. `texture_preloading=1;hw_mipmap=false` não ajudou (GS 18,0, 25,1%) |
| 2026-11-01 | +SPU2 do ARMSX2, heap do IOP, `PHMADH`/`PMADDH` | RG Cube | sem limite | 50,0 | 27,7 | 8,8 | 0,3 | 13,3 | 11,1 | 2,2 | - | - | 3,19 | idle 9,2; pares > 44 ms 0,4%, pior 1% 41,8 ms. **Velocidade cheia com som**. O relógio e a mixagem do SPU2 entram na coluna IOP |
| 2026-11-01 | idem | Retroid Pocket 5 | sd480p | 49,5 | 15,3 | 11,9 | 0,5 | 14,9 | 17,3 | 3,0 | - | - | 3,21 | idle 4,3, waitVU 1,2; pares > 44 ms 11,3%, pior 1% 67,9 ms. Som não custou fps. EE+VU1+GS = 44 ms/vsync contra 40 ms dos 2 núcleos grandes do perfil |
| 2026-11-01 | LTO + armv8.2 no `libbt4gsx` (build à parte, não adotado) | Retroid Pocket 5 | sd480p | 49,7 | 17,7 | 11,4 | 0,5 | 14,6 | 16,9 | 3,0 | - | - | 3,19 | hipótese (GS -5 a -10%) refutada: -2%, pares > 44 ms 10,8%, dentro do ruído. Voltou ao `libbt4gsx` do Dragon Rage |
| 2026-11-01 | +vértices nas formas com laço próprio do GS (`compactVertices`) | Retroid Pocket 5 | sd480p | 49,7 | 17,3 | 11,3 | 0,5 | 14,4 | 15,8 | 3,0 | - | - | 3,15 | idle 5,1, waitVU 0,7; pares > 44 ms 8,1%, pior 1% 61,6 ms; imagem igual |
| 2026-11-01 | idem | RG Cube | sem limite | 50,0 | 28,3 | 8,9 | 0,3 | 13,5 | 10,3 | 2,2 | - | - | 3,20 | idle 9,3; pares > 44 ms 0,4%, pior 1% 41,6 ms |
| 2026-11-01 | renderizador nativo (`HL_NATIVE=1`), silhuetas vértice a vértice | Retroid Pocket 5 | sd480p | 50,0 | 20,7 | 10,8 | 0,4 | 13,8 | 14,3 | 2,8 | - | - | 3,12 | idle 5,7; pares > 44 ms 3,0%, pior 1% 52,4 ms. **Imagem congelada (ver a correção abaixo): só carga de CPU** |
| 2026-11-01 | nativo, tags sem troca de alvo inteiros, só os uploads que o desenho lê, thread de replay nos núcleos pequenos | Retroid Pocket 5 | sd480p | 50,0 | 20,0 | 11,0 | 0,4 | 13,7 | 14,1 | 2,8 | - | - | 3,11 | idle 5,4; pares > 44 ms 4,2%, pior 1% 52,0 ms. **Imagem congelada: só carga de CPU.** As rodadas intermediárias saíram pretas (tag inteiro levava a limpeza do FB 0 ao GS) e não valem, mesmo com GS 12,6 ms |
| 2026-11-01 | idem, Turnip v26.3.0-R6 | Retroid Pocket 5 | sd480p | 50,0 | 20,9 | 11,5 | 0,4 | 14,2 | 15,4 | 2,9 | - | - | 3,19 | idle 5,5; pares > 44 ms 3,3%, pior 1% 51,5 ms. **Imagem congelada: não prova nada sobre o Turnip**, GPU não caiu |
| 2026-11-01 | sem o nativo, Turnip v26.3.0-R6 | Retroid Pocket 5 | sd480p | 49,8 | 18,5 | 11,5 | 0,5 | 14,5 | 15,5 | 3,0 | - | - | 3,19 | idle 5,0, waitVU 0,7; pares > 44 ms 9,3%, pior 1% 55,4 ms. Igual ao driver do sistema sem o nativo (15,8 ms; 8,1%) |
| 2026-11-01 | nativo, Turnip Rutabaga v3 | Retroid Pocket 5 | sd480p | 50,0 | 20,3 | 11,5 | 0,4 | 14,0 | 15,8 | 2,8 | - | - | 3,18 | idle 5,4; pares > 44 ms 4,1%, pior 1% 53,4 ms. **Imagem congelada: não prova nada sobre o Turnip** |
| 2026-11-01 | nativo com imagem viva: sombra, bloom, efeitos e cópia para `0x70` nativos, texturas da memória do GS, EE e VU1 nos núcleos grandes, vértices do GS religados | Retroid Pocket 5 | sd480p | **50,0** | 22,7 | 11,2 | 0,5 | 13,8 | 11,7 | 2,8 | - | - | 3,20 | replay 10,1 (thread GS ~1,6), idle 6,0; pares > 44 ms 2,0%, pior 1% 48,1 ms, pior 59,1 ms. 0 quadros pretos em 404 gravados. Primeira medida do nativo que vale |
| 2026-11-01 | idem | RG Cube | sem limite | **50,0** | 30,6 | 8,5 | 0,3 | 13,2 | 10,6 | 2,1 | - | - | 3,19 | replay 6,8, idle 9,7; pares > 44 ms 0,1%, pior 1% 41,5 ms, pior 48,1 ms |
| 2026-11-01 | idem, Turnip v26.3.0-R6 | Retroid Pocket 5 | sd480p | 50,0 | 22,1 | 11,1 | 0,5 | 13,9 | 12,2 | 2,8 | - | - | 3,18 | replay 9,8, idle 5,8; pares > 44 ms 2,6%, pior 1% 48,5 ms, pior 59,7 ms. 0 pretos, GPU não caiu. Sem ganho |
| 2026-11-01 | idem, Turnip Rutabaga v3 | Retroid Pocket 5 | sd480p | 50,0 | 22,1 | 11,0 | 0,4 | 13,7 | 12,4 | 2,8 | - | - | 3,14 | replay 9,9, idle 5,6; pares > 44 ms 3,2%, pior 1% 47,5 ms, pior 58,5 ms. Sem ganho. O driver do sistema continua o padrão |
| 2026-11-01 | nativo padrão, liga sozinho pela luta (sem nenhuma variável) | Retroid Pocket 5 | sd480p | **50,0** | 22,9 | 10,8 | 0,4 | 13,7 | 11,5 | 2,8 | - | - | 3,16 | replay 9,9, idle 6,1; pares > 44 ms 2,0%, pior 1% 48,2 ms, pior 61,4 ms |
| 2026-11-01 | idem | RG Cube | sem limite | **50,0** | 30,3 | 8,4 | 0,3 | 12,9 | 10,4 | 2,1 | - | - | 3,13 | replay 6,6, idle 9,4; pares > 44 ms 0,0%, pior 1% 41,6 ms, pior 42,9 ms |

**Como ler a tabela desde 2026-11-01.** O jogo desenha um quadro a cada 2 vsyncs, com um vsync pesado e um leve, então o
`1% low` por vsync fica perto de 20 fps mesmo com a imagem sem falhas: um vsync de 45 ms seguido de um curto ainda fecha o
par no prazo. Por isso a medida também registra `PAIRS`: o tempo de cada par de vsyncs (alvo 40 ms; acima de 44 ms é falha
visível). A cena de medida não é determinística entre rodadas: é a mesma luta e os mesmos personagens, mas os golpes variam.
Variações de até uns 3% no fps e de 0,3 ms nas colunas estão dentro do ruído.

**Decisões desta rodada.**
- `HL_STATSYNC=1` volta ao comportamento antigo (ler `VIF1_STAT`/`GIF_STAT` esvazia a fila), para comparar. A/B no Retroid
  com 12 prints de cada modo: 48,4 fps sem a espera, 45,2 com ela.
- Um print do Retroid mostrou o cenário em dupla exposição (relógio 86). Não reproduziu em 24 prints (12 em cada modo).
  Causa não determinada; pode ser efeito do jogo. Se voltar a aparecer, comparar com o PCSX2.
- `kMaxFramesAhead` 1 → 2 não mudou nada enquanto a espera real era o `SyncPath`; ficou em 1, com menos latência.
- O verificador da VU no PC continua aceitando: 1,2M execuções com `stickyLive`, 2 divergências na VU1, ambas da classe já
  conhecida em `0x1820` (`vf16.x`), nenhuma de status. Rodada anterior (`verify8`, sem `stickyLive`): 5,84M execuções, 13 na
  VU1, 9 em `0x1820` e 4 em `0xf0` (bytes de um XGKICK). Essas duas classes **não foram explicadas**.
- `stickyLive` assume que nenhum programa lê sticky sem antes limpar com `FSSET`. Vale para as imagens que conferi; um
  programa que não faça isso veria stickies a menos, e o verificador só compara os bits baixos nas imagens sem leitor de sticky.

A primeira tentativa (mesmo build, sem o escopo no `VU1Interpreter::run`) dava "EE 600 ms": era a VU contada como EE.

### O que uma luta manda ao GS (2026-11-01, `HL_GSCAP` + `tools/gscap.py`)

Um quadro (2 vsyncs): ~61 mil vértices, ~4000 pacotes, 6 MB, 1,9 MB de textura reenviada, ~1800 grupos de estado, 700 trocas de
estado reais; tudo no framebuffer 0. Quase tudo vem do VU1 em tiras: `FOG/STQ/RGBA/XYZ2` (28,5 mil), `NOP/STQ/RGBA/XYZ2` (14,7 mil),
`STQ/NOP/NOP/XYZ2` (7 mil), `NOP/NOP/NOP/XYZ2` (6,8 mil). O GS do ARMSX2 tem laços NEON só para as formas sem NOP, então cada
registro era uma chamada por ponteiro. `compactVertices` (`hl_gs_live.cpp`) reescreve os tags no anel: fog vai em `XYZF2` e os NOPs saem.
`HL_GSCOMPACT=0` desliga. Um tag com Z acima de 24 bits fica como veio.

### Renderizador nativo da luta (2026-11-01, `recomp/android/gsx/`, em construção)

**Correção (2026-11-01):** as primeiras linhas da tabela do nativo (marcadas "imagem congelada") estavam erradas ao dizer
"imagem certa": a tela mostrava um quadro do GS anterior ao `HL_NATIVE_FROM`, com flashes pretos (cronômetro parado em 96, contra
86 sem o nativo no mesmo vsync). Dois defeitos, ambos resolvidos:
1. **Congelada:** a cópia final FB 0 → `0x70` pelo GS não lia o alvo do nativo (o quadrado vermelho de teste na cena nunca
   aparecia; o motivo exato não foi determinado). Com a cópia também nativa, a imagem anda.
2. **Preto em quadros alternados (Adreno, Turnip e Mali):** o GS liga o buffer de vértices **uma vez por command buffer**
   (`GSDeviceVK::SetInitialState`), fora das flags de estado sujo, então `InvalidateCachedState` não o traz de volta. O `Merge` do
   vsync pesado desenhava com os vértices do nativo. O vsync leve estava em outro command buffer e saía certo. Agora o nativo
   chama `SetInitialState` depois dos seus passes. Preto gravado: 165 de 403 quadros antes, 0 de 404 depois.

**Como conferir:** print isolado engana, nos dois sentidos. Usar `build/lab/rec.sh SERIAL TAG V [ENV...]`, que grava 8 s e dá o
brilho de cada quadro, e conferir se o cronômetro anda. Um teste que mudava o tempo (`Flush` de leitura de pixel) também
"curava" o preto e quase apontou para a causa errada; o que separou foi submit sem espera, que curou igual.

Mesma ideia do pedido ("personagens e cenário direto em Vulkan"), com um desvio: em vez de ler os modelos CCS e a saída da
VU1, ele lê o fluxo do GS na thread de replay do `libbt4gsx` (o nosso, `recomp/android/build_gsx.sh`, sem tocar a árvore do
Dragon Rage) e decide **por desenho**: o que entende vai direto (`hl_native.cpp`), o resto segue para o GS do ARMSX2, na ordem.
Assim o HUD também sai por aqui quando o desenho é simples; a contrapartida é que menus e casos raros continuam corretos
sem lista de cenas. Desenha nas mesmas imagens Vulkan dos alvos do GS (FB 0 e Z em `0x1E00`), então o GS continua de onde
parou, e marca os alvos como válidos (RGB e alfa) para a cópia final FB 0 → página `0x70` não ler a memória do GS.

- Padrão ligado (`HL_NATIVE=0` desliga). Liga sozinho depois de 8 quadros seguidos com a assinatura da luta no fluxo, a cópia do Z
  (`FRAME` com FBP `0x1E0`, FBW 4, Z24); desliga depois de 30 vsyncs sem ela e devolve registradores e uploads ao GS. Testado no RP5:
  entrada na luta (v5236), pausa e seus painéis (continua nativo, imagem certa), saída para o Mode Select pelo GS (v7563) e segunda
  luta na mesma sessão (v9825), 0 pretos. A campanha (`ADV`) **não foi testada**. `HL_NATIVE_FROM=<vsync>` só impede
  de ligar antes do vsync dado. A thread de replay só fica nos núcleos pequenos com o nativo ligado. `HL_NATIVE_WHOLE=0`,
  `HL_NATIVE_LITTLE=0` e `HL_NATIVE_CAP=<arquivo>:<vsync>:<n>` (o que foi ao GS, `HL_GSCAP` com um registro `0xFE` por quadro nativo).
- Nativo na luta inteira: cena, sombras (volumes em `R` contra o Z da cena, sem a cópia do Z), bloom, efeitos, HUD e a cópia final
  para `0x70`. Texturas que não vieram por upload (efeitos nos bits altos, CLUTs em `0x2Cxx`) são lidas da memória do GS.
  Ao GS só sobra o present e um desenho raro (`prim 154`, que lê `0x70`): 12 a 64 tags por 500 vsyncs.
- `HL_PIN=0` tira EE e VU1 dos núcleos grandes; esse fixar foi o maior ganho no Retroid (pares > 44 ms de 8,1% para 1,6%).
- Um tag inteiro pode conter vários desenhos: só vai inteiro ao GS se não troca alvo nem textura no caminho. Foi isso que deixou
  a tela preta com números ótimos: confira sempre o print, não só a tabela.
- Turnip: R6 e Rutabaga funcionam no RP5 com a imagem viva (0 pretos no R6 gravado, sem queda da GPU), mas sem ganho: pares
  > 44 ms 2,6% e 3,2%, contra 2,0% do driver do sistema, dentro do que a cena varia. O `v863.1` é da Adreno 863 e não se aplica. Para testar: a pasta do driver em `files/drivers/<id>/` e o id em `files/drivers/selected`.
- Limites conhecidos: o alfa do FB não é escrito pelo nativo; `As > 128` é limitado; sem despejo do cache de texturas (248 depois
  de duas lutas, limite de 16384 conjuntos de descritor; as texturas nunca são liberadas); pipelines criados na primeira vez que
  aparecem, o que pode causar pico; leitura pelo GS de um alvo privado do nativo (`target read`, 32 tags por 500 vsyncs em
  um trecho da segunda luta) lê dado velho.
- Falta: comparar quadro a quadro com o GS (a cena varia entre rodadas, comparei só a olho); os 2% de pares acima de 44 ms no RP5.

### Áudio (2026-11-01)

SPU2 do ARMSX2 (`recomp/android/spu2/hl_spu2.cpp`, os fontes de `pcsx2/SPU2` da árvore do Dragon Rage, sem cópia) sob o IOP
do runtime: registros `0x1F900000`, DMA 4 e 7 e IRQs por `Spu2Hooks` (`iop_memory.h`). Saída por um stream SDL, 48 kHz,
com 70 ms de folga. Só no Android: no PC `g_spu2` fica nulo e o som continua desligado. `HL_SPU2=0` desliga,
`HL_SPU2_DUMP=<arquivo>` grava os últimos 120 s (f32, 48 kHz, estéreo), `HL_SPU2_LOG=N` loga N acessos. O som não foi
**ouvido** por mim: conferi envelope e espectro do dump; falta o desenvolvedor ouvir no aparelho.

Três defeitos que estavam escondidos pela falta de som:
1. `sceSdVoiceTrans` fora de thread de IOP: `WaitEventFlag` não bloqueia ali, então a espera agora anda o relógio do SPU2
   até o DMA acabar. Sem isso, `sceHSyn_VoisTrans Error` e o jogo parava antes da luta.
2. `sceSifFreeIopHeap` (`0x166f40`, sem nome no ELF) não estava ligado ao stub e não liberava nada: cada filme vazava
   `0x2E810` bytes, o heap enchia, a alocação devolvia 0, o jogo não confere e mandava o som do filme para o endereço 0 do
   IOP. O driver ADX lê esse endereço no fim de uma lista, pulava para o lixo e a thread morria (`execution outside RAM`),
   deixando o último buffer em laço: um tom contínuo. `un5.py` (`STUB_AT`) corrige na próxima geração. O alocador
   também passou a reaproveitar o espaço liberado (primeiro encaixe).
3. `PHMADH` e `PMADDH` estavam errados no gerador (`mmi_translation_helpers.cpp`): `PHMADH` é o filtro de síntese do AHX, e
   toda fala de personagem (`AHX/PS2EE`) saía como ruído branco em volume máximo. `PMADDH` só aparece em `0x19af84`, com
   `PMTHI`/`PMTLO` (que só gravavam a metade baixa). Corrigi os três; `PMSUBH` e `PHMSBH` continuam errados, mas o jogo não os usa.

Como o código gerado foi editado só nesses 3 arquivos (`sub_00166F40`, `sub_0013BAA0`, `sub_0019AA00`), não houve build
completo; a próxima geração sai igual pelo gerador corrigido.

Sobra: o `libbt4gsx` ainda abre um stream Oboe próprio, mudo (`[SPU2/Output] Backend = Null` no ini não mudou isso).

Com `HL_PERF`, o SPU2 loga uma linha por segundo: `[snd] rms .. peak .. queued .. playing` (nível da mistura e fila do
stream). Em 2026-11-03, no Cube: som no título, seleção de modo, Options, Shop, Collection (com o tocador de músicas),
seleção de personagem e de cenário, VS e luta. Logos (Bandai, CRIware, 10º aniversário) e telas de carregamento ficam em
silêncio. O pico passa um pouco de 1,0 depois do filtro de DC (um pouco de clipping). Master Mode não conferido.

### Noite de 2026-11-03

**Controles (itens 1 e 2):** o botão grande e a detecção pelo GS saíram. "Especial com um botão" (aba Controles: liga,
botão, multiplicador 1x a 2x) põe a sequência na entrada do lutador e responde aos avisos do Ougi pelo gancho `0x6C99B0`
na tabela do BTL (`hl_game.cpp`, log `[sp]`). Visto na rota fixa: um toque dá o Ougi ("Unleash Ultimate Jutsu"), a cena
do Ougi roda, o "Press!" é respondido e termina em "Critical Damage". Uma etapa mostrou "ERROR". **Não visto:** efeito
separado de △△ (Ougi pela sequência longa), a disputa de Ougi entre os dois e o multiplicador num trecho de apertar repetido.

**Widescreen (item 3):** a proporção alternava porque o nativo e o GS usavam formas diferentes, e a câmera larga ficava
ligada nas telas sem lutador. Agora os dois usam a mesma forma (`s_shape`), e a câmera larga (`+0x274`, escala horizontal)
vale só na campanha (`ADV`) e na luta enquanto houve entrada de lutador nos últimos 6 vsyncs ou há Ougi na tela; fora disso
volta a 1,0. A tela de resultado montava as matrizes uma vez, na transição, com a câmera ainda larga: daí o resultado
apertado. Por gravação: 808 quadros com barra lateral antes, 0 depois. Menus e filmes são esticados para a mesma forma.
**Falta:** as opções "Esticar" e "Desenhar em 16:9" para os menus. As listras da tela de resultado vêm do conteúdo
(desentrelaçamento 1 e 8 não mudaram).

**Tela de vitória (item 4):** o personagem não chega ao GS por nenhum dos dois caminhos: a tela de WINNER só manda camadas
2D. O desenho se perde antes, no EE ou na VU1. Não resolvido.

**Mods (item 6):** `HL_MODS` (pastas, uma por linha; o host lê `un5_mods.txt`) sobrepõe os arquivos dos mods à imagem
(`0053`, `hl_image.cpp`): o arquivo que cabe nos setores do original entra no lugar, o maior vai depois do fim da imagem,
alinhado a 16 setores, e o diretório aponta para lá. O último mod da lista ganha. Para os mods de código,
`recomp/build.sh --mod <iso>` extrai o disco para `build/mods/<id>/`, acha o id pelo sha do ELF (`un5.KNOWN_MODS`) e gera o
código; `recomp/android/build.sh --mod <id>` faz `jniLibs/libun5_<id>.so` (com `HL_PACK_ID`: o host carrega o ELF do mod e
recusa abrir sem o mod ligado). O launcher carrega essa biblioteca quando o mod está ligado (`EngineBridge.libraryFor`).
O MOD.BIN e o TEXTBRA.BIN do A35 guardam o código no meio dos dados: `un5.py` acha as ilhas de código pelos prólogos e
retornos (`data_code_islands`), junta a primeira ao texto (a função de 0x940070 começa no texto e segue nos dados) e conta
como entrada todo `J` de outro ELF para o meio de uma função (o MOD.BIN volta ao código do jogo depois do seu gancho:
0x941838 → 0x10bd74), além das chamadas para código depois de preenchimento (0x9476c0). No jogo base isso só acrescenta
a entrada 0x1d3950. `build/lab/reovl.sh <id> <overlay>...` refaz só esses overlays.

No Cube, o A35 roda com o código próprio: logos, abertura, título "Ultimate Ninja 6 3.0", menus em português. **O cartão
precisa de um save na pasta do mod, `BASLUS-55606NARUTO6`:** sem ela, "Novo jogo" e "Carregar jogo" dizem que não há dado
do UN6 no slot 1, e "Não" em "Voltar à Tela Inicial?" volta à mesma mensagem. Com o save do UN5 copiado para essa pasta (e o
arquivo de cabeçalho renomeado), o A35 carrega ("Carregado com sucesso"), abre o Combate Livre e a seleção com a grade do UN6
e chega ao "Lutem!". Cada travada seguinte foi um endereço que faltava, achado e corrigido nos mapas (`ovlelf --from`: código
onde outros ELFs pulam ou chamam dentro dos dados do MOD.BIN; os overlays passam a ver as chamadas dos outros que não ocupam o
mesmo endereço). **Às 05:30 o A35 lutou no Cube:** "Carregando", "Lutem!", luta Naruto x Naruto com o HUD
do UN6 e o "Rasengan Gigante", renderizador nativo ligado (v5873 a v9850, 66 s), e depois a volta à seleção de personagem.
A rota para de apertar em v6500, então a CPU deve ter vencido; a tela de resultado não foi conferida. Sem medida de fps.
Os passos novos do `recomp/build.sh` (`ovlelf --from`, os outros overlays no `fnmap`) só rodaram pelo
`build/lab/reovl.sh`, que faz o mesmo; um `--mod` do zero não foi repetido. No jogo base, os mapas dos overlays ganham
entradas (BTL +4, ADV +63, ETC +3, o principal +1) na próxima geração, sem perder nenhuma; isso não foi testado.

Para o jogador: falta o launcher criar o save do mod (copiar o do UN5 para `BASLUS-55606NARUTO6`, como fiz à mão) ou
descobrir por que o A35 não cria um save novo.

**Opções do launcher (itens 9 e 13):** saíram efeitos do PS2, volumes, renderizador, profundidade precisa, ASTC, pular
quadros, present mode e a orientação retrato (o jogo é só paisagem). Testados: tema claro, escuro e do sistema, atalho na tela
inicial (abre o pedido do sistema), buscar atualização (responde; hoje 404, sem release publicada). O 120 interpolado só
aparece em tela de 100 Hz ou mais (`DisplayCaps`); um 120 salvo vira 60 numa tela de 60 Hz.

**Campanha no sd480p (item 11):** rota fixa `HL_PADROUTE=<rota até 2650:CROSS>,3179:CROSS,4000:START,4400:START,4900:CROSS`
(Master Mode, `FROM=5900`). 43,7 fps, 83% de pares acima de 44 ms, EE 12,4, VU1 20,8, GS 13,5, waitVU 5,6. Hipótese: a
thread GS do ARMSX2 dividia o núcleo grande com a VuWorker (estava no cpu4). Prendê-la nos núcleos pequenos (como root,
pelo hlprofile) deu 43,5 fps e 82%: refutada. O limite é a VU1 (interpretador) num A77 a 1,77 GHz. Não otimizado.

## Correto primeiro: bancada contra o PCSX2 (desde 2026-11-08)

O dono testou o `app_d26` e achou o jogo muito quebrado (defeitos diferentes por personagem, cenários com erro, câmera
doida, tela preta até o próximo especial). Duas fontes de defeito estavam misturadas: o recomp do EE e o caminho nativo da
luta. A fase atual é só no PC, sem o nativo (no PC ele nem existe: o `HL_NATIVE` é da `libbt4gsx` do Android; no PC a
imagem sai pelo GS de software do runtime). O Android fica parado até a tabela de lutas do registro estar toda verde.

### O que os outros recomps do dono fazem diferente (2026-11-08, só leitura)

Lidos: BT4 (`DragonRage/port/build/bt4/BT3-Recomp` e `port/bt4/patches/`, mais o `ps2recomp-dbz.patch` do dbz-bt4-recomp) e
BT2 (`DragonRage/bt2/runtime`, `bt2/docs`), e o modelo de float do BT3 descompilado (`Tenkaichi3Decomp/port/src/softfloat_ps2.c`).
O BT3-Recomp parte de um PS2Recomp mais velho que o nosso `e563698`; o BT2 parte de `c5a9d02` (4 commits antes) e reescreve o
código gerado com regex (`bt2/tools/ps2recomp/prepare_runtime.py`) num runtime escrito à mão. Nos dois, várias áreas já estão
atrás do nosso (expoente 255, divisão por zero, `CVT.W.S` truncando, VDIV/VSQRT/VFTOI, CLIP, MMI, escalonador por prioridade).

Por ordem de chance de explicar defeito de jogo:

| # | Diferença | Nos falta? | Pode explicar? | Testado por eles |
|---|---|---|---|---|
| 1 | **`rand` (0x17C558) era um stub do host**: `std::rand() & 0x7FFF`, sem a semente do `srand` do jogo (0x17C540). O do jogo é o LCG de 64 bits do newlib (`0x5851F42D4C957F2D`, estado em `*(0x412DD8)+0xA8`), 31 bits | sim | **sim**: as escolhas da CPU no `BTL` (`0x6F0E60`, `0x6F1260`, `0x6F1720`), `rand()/2^31` sempre perto de 0 (`0x192830`), `(rand()>>19)%101` sempre 0 (`0x359D80`), direções pela metade (`0x743720`) | BT3 `616e85e`, BT2 roda o do jogo |
| 2 | **FPU e VU0 macro arredondam ao mais próximo**; o PCSX2 corta para zero (MXCSR do EE com RZ+DAZ+FTZ), soma e subtração **sem bits de guarda** (o operando menor perde os bits que cairiam além de 1 bit de guarda: `iFPU.cpp` `FPU_ADD_SUB`), `DIV.S` ao mais próximo (`FPUDivFPCR`), resultado preso em ±FLT_MAX (`fpuFloat`) | sim | sim: 1 ulp em metade das contas, acumulado em física, câmera e limiares | BT3 só bateu tick a tick com o PCSX2 depois desse modelo (`f6438a5`, `d7dbf9c`); o BT3-Recomp viu RZ+FTZ+DAZ **global na thread** quebrar menus e carga (`97ad839`): fazer por operação, não no MXCSR |
| 3 | VU0 e VU1 **sem clamp**; o GameDB do PCSX2 pede para este jogo `vu0ClampMode: 3` ("fundos de diálogo errados") e `vu1ClampMode: 3` ("SPS nos personagens") | a conferir | imagem dos personagens e diálogos | GameDB do PCSX2 |
| 4 | `ChangeThreadPriority` devolve 0 em vez da prioridade antiga; o ADXM_Lock/Unlock da CRI (`0x12E4B8`/`0x12E540`) guarda o retorno em `0x3F6180` e restaura, pedindo prioridade 0 (recusada) | sim | ordem das threads, som (é o "todos na prioridade 1" do `0039`) | dbz |
| 5 | Tempo do host: o VBLANK precisa do prazo em ciclos **e** do relógio do host; T0–T3 e IOP em ciclos fictícios; `0021`/`0069` adiantam o relógio | sim | não determinístico entre aparelhos; o jogo mede o quadro pelo T0 (`0x1061a4` zera, `0x108748` lê, guarda em `*(gp-0x3F4)+4`) | BT2: VBLANK em pontos fixos |
| 6 | `BLTZ/BGEZ/BLEZ/BGTZ` testam 32 bits; 36 sítios do UN5 testam um valor de `ld`/`lq` (`0x102620`, `0x159f18`, ADV `0x816390`) | sim | pouco provável | BT2, 491 sítios sem regressão |
| 7 | Callbacks do IOP para o EE (sifcmd 15/16/20/21/23) descartados; VBLANK do IOP em NTSC | sim | som | BT2 |
| 8 | INTC: causa mascarada por `DisableIntc` descartada em vez de guardada; a cadeia de tratadores não para em retorno negativo | sim | tempo | BT2, contra capturas do PCSX2 |
| 9 | `SuspendThread`/`ResumeThread` devolvem 0 (o BT3 devolve o tid) | conferir a ABI | pouco | BT3 |
| 10 | Pad lido ao vivo a cada `scePadRead` (o BT2 trava um bloco por VBLANK); pressão só 0 ou 0xFF | sim | entrada | BT2 |
| — | Flags de STATUS da VU0 macro nunca calculadas; `VCLIP`/`VRNEXT`; `LQ`/`SQ` sem alinhar a 16; bit SPR da tag DMA; VU0 por thread; `malloc`/`free` em stub (o heap não fica onde fica no PS2) | sim | baixo: o UN5 não chama os leitores (`sceVu0ClipScreen*`), não usa `vclip`/`vrnext`, 1 `lq` estático | BT3, BT2 |
| — | Som: o dbz arredonda `AllocSysMemory` e `sceSifAllocIopHeap` a 256 bytes (com menos, o ADX estalava a cada 8192 amostras); o nosso usa 16 (IOP) e 64 (EE) | sim | estalos | dbz |

Já certos no nosso (não regredir): `MFC1` com extensão de sinal (a "câmera maluca" do BT2), SQRT/RSQRT, `DIV.S` x/0,
VF0/VI0, VLQI/VSQI, PMADDH/PHMADH, `-ffp-contract=off`, prioridade estrita sem fatia de tempo (`0039`).

Relatórios completos dos dois agentes: copiados em `build/lab/bench/bt4-vs-hl.md` e `bt2-vs-hl.md` (fora do git).

### A bancada (CPU contra CPU, sem savestate)

- **PCSX2 da bancada** (fora do repositório): `~/build/pcsx2-bench/src` (PCSX2 `v2.9.162`) com o gancho
  `pcsx2/HLBench.cpp`, ligado por `HLB_OUT`. Usa o mecanismo de ponto de parada do recompilador do EE para chamar o gancho
  (sem pausar) na entrada de `0x21E660`. Configuração portátil em `b/bin` (`portable.ini`): BIOS `scph39001`, renderizador
  por software, sem patches nem cheats, cartão em pasta formatado e vazio no slot 1 (o recomp tem um de 8 MB formatado e
  vazio), slot 2 vazio. Rodar: `~/build/pcsx2-bench/run.sh <prefixo>` (Qt em `xcb`, sem MangoHud).
- **Recomp do PC:** `recomp/runtime/hl_bench.cpp` (só fora do Android), mesmo gancho por `registerFunction`, os mesmos
  registros; rodar com `~/build/pcsx2-bench/runrc.sh <prefixo>`. Os botões vêm do `HL_PADROUTE` (no PCSX2, `HLB_ROUTE`, mesma sintaxe).
- **Registro** (`<prefixo>.bin`): a cada chamada de `0x21E660` (a entrada de cada lutador, uma vez por quadro do jogo, 25 por
  segundo): vsync, luta, número da chamada, endereço do lutador, `ra`, índice do Mersenne Twister, hash do estado dos dois
  geradores, a câmera (`*(0x617ACC)`), os 0xC00 bytes do lutador e os 0x280 da câmera. `<prefixo>.txt` tem os eventos.
- **Geradores aleatórios do jogo:** um Mersenne Twister de 624 palavras de 64 bits (estado em `0x627D40`, índice em
  `gp-0x7FA8` = `0x60FF48`, gerador `0x180BA0`, tabela `mag01` em `0x413254`) e o `rand` do newlib (acima). Com `HLB_RNG=1`, na
  primeira entrada de lutador de cada luta os dois lados põem o índice em `0x271` (a próxima chamada ressemeia com a semente
  padrão do jogo) e o estado do `rand` em 1.
- **Comparação:** `tools/hlbench.py diff <pcsx2.bin> <recomp.bin>`: casa por luta e número de chamada, o lutador pela ordem
  em que aparece; ponteiros que diferem pelo mesmo deslocamento dos lutadores contam como iguais. Mostra a primeira chamada
  com diferença, os campos (deslocamento, os dois valores, como float) e como a diferença se espalha.
- **Luta de referência** (`build/lab/hl_env.txt` cortada antes da luta, `runs/route_idle.txt`): Naruto x Naruto no lamen,
  o 1P parado contra a CPU. No PCSX2 a luta começa no v5306 nas duas rodadas.

## Teste nos aparelhos

- **AYN Odin 2 Mini** (Snapdragon 8 Gen 2, Adreno 740), mais os aparelhos do Dragon Rage: RG Cube (Mali-G57, o alvo
  difícil) e Retroid Pocket 5 (Adreno 650).
- Instalar: `adb -s <serial> install -r android/app/build/outputs/apk/debug/app-debug.apk`.
- Medir como no Dragon Rage: luta gravada com a entrada repetida, FPS, pior quadro, tempo de CPU+GPU, comparação de
  pixels contra a referência.

## Controles na tela

Três cenas, cada uma com o próprio layout (editável) e trocadas sozinhas pelo motor:

| Cena | Movimento | Botões |
|---|---|---|
| Luta (2D) | direcional | ○ Ataque, ✕ Pular, △ Chakra, □ Item, Jutsu (△ → ○), Ougi (△ △ → ○), R2 Defesa, R3 Despertar, L1/R1 item |
| Campanha (mapa do Master mode) | analógico | ○, ✕, △, □, L1/R1, Start/Select |
| Menus | direcional | o losango do PS2, L1/R1, Start/Select |

- As funções vêm do manual do Ultimate Ninja 2 (○ ataque, ✕ pulo, △ prepara a técnica secreta, □ item,
  L1/R1 trocam o item, L2/R2 defesa) e das entradas da série (△ ○ jutsu, △ △ ○ jutsu supremo). **Conferir no UN5
  quando o jogo rodar**, inclusive o tempo entre os toques do Jutsu e do Ougi (hoje 70/50/90 ms).
- Depois: variantes de direção dos jutsus e os minijogos do Ougi num toque.
- [ ] **Modo "Ougi de um botão" (pedido do desenvolvedor, 2026-11-09):**
  - **Onde se escolhe:** no menu de configuração da luta (Battle Settings, antes da luta) entra uma opção a mais para
    o Ougi. Ela vale para as lutas seguintes.
  - **O que muda:** o especial (△ ○, e o △ △ ○) começa como hoje. Os minijogos do Ougi, de quem faz e de quem leva,
    trocam as sequências de botões por **um único botão, o ○** (o de bater), apertado várias vezes.
  - **Quem ganha:** conta-se quantas vezes cada lado apertou na janela do minijogo:
    - se quem fez apertou mais, o especial segue;
    - se quem leva apertou mais, o especial é cancelado.
  - **CPU:** a CPU também precisa de uma contagem (uma taxa de toques por dificuldade). Assim, contra a CPU, o
    resultado depende de quem toca mais rápido.
  - **Base que já existe** (`hl_game.cpp`):
    - `special_one`: um toque faz o Jutsu ou o Ougi;
    - os passos de apertar repetido do Ougi e das disputas são lidos em `0x7AF440`, `0x819D10` e `0x820FD0`;
    - o `mash_percent` diz quanto vale cada toque.
  - **Falta achar no jogo:**
    - onde cada minijogo decide sucesso ou falha (resultado por lado), para trocar pela comparação das contagens;
    - a opção do Battle Settings (o texto e o valor gravado), para acrescentar o modo novo;
    - se acrescentar uma linha ao menu do jogo custar demais, a opção vai para a aba Controles do menu do Hidden
      Leaf, e o desenvolvedor decide.
- **Contrato do motor:** `TouchBridge.nativeScene()` → 0 desconhecido, 1 menu, 2 luta, 3 campanha. O motor sabe
  pelo overlay carregado (`BTL.BIN` luta, `ADV.BIN` aventura/campanha) e pelo estado do jogo.

## Launcher (feito)

Do template do Dragon Rage: tema (laranja do Naruto, índigo e roxo do Sasuke, verde do Rock Lee), botão Jogar com a
cena do campo de treino (troncos, shurikens que cravam, folhas caindo), importador da base com SHA-256 do ELF e dos
overlays, mods por ISO (compara com a base, guarda só o que difere, um mod de código por vez), drivers, controles,
texturas, depuração e atualização. Sem o online e as trapaças do BT3.
