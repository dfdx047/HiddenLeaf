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
- [ ] `tools/un5.py diff` dá a lista; o pacote leva as funções alteradas e novas, recompiladas, e os hashes dos arquivos
      que elas exigem.
- [ ] O runtime liga o pacote quando o mod ativo tem esses hashes; sem pacote, o jogo abre como a base (o launcher já
      avisa).
- [ ] Depois: interpretador para as funções de um mod desconhecido.

### 5. Ajustes do jogo
- [ ] Widescreen: procurar patch 16:9 da comunidade (pnach) e aplicar no runtime.
- [ ] 60 fps: o jogo europeu é PAL (25/50); o A35 já é NTSC. Preferir quadros intermediários no renderizador.

## Estado do boot (2026-10-25, branch `lab/data-cvm-boot`)

### Onde o jogo está

- Boot → **tela de idioma** (✕ escolhe ENGLISH) → logo da BANDAI NAMCO → filme `\PSS\LOGO_C.PSS` (CRIWARE e
  CyberConnect2) → carga do `DATA.CVM` → `\PSS\NARUTO10.PSS` (o "10th") → abertura em anime `\PSS\OPENING.PSS` →
  tela de título → (START) carrega o **`BTL.BIN`** (`[un5] overlay btl (id 1) resident`) e mostra os menus em janela
  ("No Naruto Shippuden: Ultimate Ninja 5 data found on memory card…", "Return to title screen?"). Tudo com imagem
  certa, sem áudio.
- **A tela de título não é desenhada** (com a abertura pulada por START ou terminando sozinha por volta do vsync 6500):
  o jogo lê ~388 KB do `DATA.CVM` (`lbn 0x131ffd`, em anel de 4 × 64 KB em `0xbee700`, parece arquivo comprimido),
  toca o som do título (`SOUND.AFS` de `lbn 0x100004` a `0x10013c`, 3 buffers em `0x63ad40`) e, ~1000 vsyncs depois,
  volta ao `NARUTO10.PSS` (modo de atração). Enquanto isso a tela fica no último quadro do filme. A cadeia DMA do
  título (13 tags, 0x2c48 bytes) é: 8 × `MPG` (microcódigo do VU1 em 0x300–0x791, sem `MSCAL`), `BASE`/`OFFSET`/
  `MSKPATH3`, o blit final (`DIRECTHL` 0x33 qwords) e as 8 camadas da lista de desenho, cada uma só com
  `FLUSHA` + `DIRECT` de `SCISSOR_1`: **nenhum objeto entra nas camadas**. O VIF e o GS fazem o que recebem; o
  problema é do lado do jogo. É a primeira cena 3D (CCS) do boot (filmes e menus são 2D). Suspeitas: o `title.ccs`
  (tabela de cena em `0x5c78a0`, tratadores de estado `0x1e47d4`/`0x1e4854`/`0x1e48d4` sobre o objeto em `+0x1c`)
  carregado errado, ou o recorte de visibilidade descartando tudo (contas do VU0).
- No ritmo atual (~10–12 vsyncs/s com dumps) a abertura leva ~9 min; `HL_AUTOPRESS="15:CROSS,130:START"` cai no meio
  dela e pula para o título.
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

### Gráficos (o que já funciona)

- O jogo usa só a cadeia DMA do VIF1 (CHCR 0x145, TTE=1), PATH2 DIRECT/DIRECTHL; sem VU1. Por quadro: texturas
  (512x512 PSMT8 dbp 0x3c00 + CLUT 0x3be0, 256x256 T4 dbp 0x3800), contexto (FRAME_1 fbp 0, ALPHA_1=0x44) e o blit final
  (8 sprites de 64 px, tbp 0 → FRAME_2 fbp 0x70). DISPFB1 = 0x1070 (fbp 0x70, 512 de largura, CT32).
- Patch 0011 corrige o VIF1: o IMAGE que fica devendo dados é completado pelo próximo DIRECT, não pelos bytes crus.

### Variáveis de depuração

| Variável | O quê |
|---|---|
| `HL_FRAME_DUMP=N` | a cada N vsyncs grava `hl_frames/frame_*.ppm` (quadro exibido), `_vram.ppm` (VRAM inteira 1024x1024 CT32) e `_ctx0/1.ppm` (o FRAME de cada contexto), mais a linha `[frame-dump]` no log (0012) |
| `HL_GIF_LOG=N` | os N primeiros pacotes GIF (caminho, tamanho, GIFtag; A+D com reg=valor) (0012); `N@S` começa S segundos depois do primeiro pacote e grava os pacotes inteiros em `hl_gif.bin` (u32 caminho, u32 tamanho, bytes) (0020) |
| `HL_VIF_LOG=N` | os N primeiros códigos VIF1 (`[vif1] +off/size code op num imm`) (0012); `N@S` começa S segundos depois (0020) |
| `HL_DMA_LOG=1` | cada cadeia DMA (`[dma-chain] ch chcr tags bytes next lastTag`) (0012) |
| `HL_RPC_LOG=1` | cada chamada SIF RPC (0008) |
| `HL_IOP_TRACE=pc,pc` | registradores do IOP nesses PCs, em hexa (0009) |
| `HL_AUTOPRESS="15:CROSS,140:START"` | aperta botões sozinho (segundos desde a 1ª leitura do controle:botão, 0,25 s cada) (0014) |
| `HL_CD_LOG=1` | cada `sceCdRead` (lbn, setores, buffer, ra) (0004) |
| `HL_EE_DUMP=1` | a cada 5 s de EE ocioso, o que cada thread espera (0005) |

Depurar com gdb: `rdram = $rdi`, `ctx = $rsi`, `r[n]` em `$rsi+n*16`, `pc` em `$rsi+512`; rodar sob gdb (ptrace_scope
bloqueia attach), interromper com `pkill -INT -x ps2EntryRunner`. Watchpoint de hardware em `$rdi+endereço` acha quem
escreve; watchpoint condicional em `ctx->pc` é lento demais. Patches novos: gerar com um worktree do PS2Recomp em
`e563698` + os patches anteriores, nunca editar `build/PS2Recomp` sem gerar o patch (o `build.sh` faz `checkout -f`).

### Commitado

`b3c382b` fnmap (delay slot do `jr ra`); patches 0004 log do sceCdRead, 0005 dump do EE, 0006 pilha do main abaixo das
pilhas de callback, 0007 leitura de CD do IOP em qualquer RAM, 0008 log de RPC, 0009 trace do IOP, 0010 DMA do SPU2
antes de WaitEventFlag sem thread, 0011 VIF1 DIRECT, 0012 dumps de depuração, 0013 vf0 fixo, 0014 HL_AUTOPRESS,
0015 GS sem Z com ZTE=0, 0016 ID 0x79 do controle, 0017 callbacks de stream do MPEG, 0018 MADR no fim da cadeia DMA, 0019 fim dos filmes,
0020 logs de GIF e VIF com atraso;
overlays (addr+0x40, construtores);
`un5.py stubfix` (sceVu0 recompiladas, MPEG por endereço); build com FFmpeg.

### Falta

1. A tela de título vazia (ver "Onde o jogo está"): achar por que nenhum objeto da cena entra nas camadas; depois o
   PNG do título e o menu principal com `HL_AUTOPRESS` (o memory card vazio pede para criar os dados).
2. Os `(null)` dos menus.
3. Áudio: SPU2 do ARMSX2 (ver Decisões).
4. Desempenho do IOP (a IRQ do libsd dispara centenas de milhares de vezes por causa do autoDMA do ADX).

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
- **Contrato do motor:** `TouchBridge.nativeScene()` → 0 desconhecido, 1 menu, 2 luta, 3 campanha. O motor sabe
  pelo overlay carregado (`BTL.BIN` luta, `ADV.BIN` aventura/campanha) e pelo estado do jogo.

## Launcher (feito)

Do template do Dragon Rage: tema (laranja do Naruto, índigo e roxo do Sasuke, verde do Rock Lee), botão Jogar com a
cena do campo de treino (troncos, shurikens que cravam, folhas caindo), importador da base com SHA-256 do ELF e dos
overlays, mods por ISO (compara com a base, guarda só o que difere, um mod de código por vez), drivers, controles,
texturas, depuração e atualização. Sem o online e as trapaças do BT3.
