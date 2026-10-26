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

## Estado do boot (2026-10-26, branch `lab/data-cvm-boot`)

### Onde o jogo está

- Boot → **tela de idioma** (✕ escolhe ENGLISH) → logo da BANDAI NAMCO → filme `\PSS\LOGO_C.PSS` (CRIWARE e
  CyberConnect2) → carga do `DATA.CVM` → `\PSS\NARUTO10.PSS` (o "10th") → abertura em anime `\PSS\OPENING.PSS` →
  **tela de título com a cena 3D** (a abertura em tempo real do Naruto no manto da Kyuubi, desde o patch 0025; ver
  "Tela de título" abaixo) → sem START, volta ao `NARUTO10.PSS` (modo de atração). Sem áudio.
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

### Tela de título (2026-10-26): resolvido pelo patch 0025

Sintoma era: a fase do título roda (carrega a cena, toca o som), mas **nenhum objeto entra na lista de desenho**; a
tela fica no último quadro do filme.

**Causa:** o escalonador do runtime ignorava o `Status.EIE`, então trocava de thread (interrupção, fatia de tempo)
dentro das regiões entre `DI` e `EI` do jogo. O heap do jogo (`0x118640`, lista livre em `0x618250`, entradas
`0x1176c0`/`0x1178e0`/`0x117c70`) se protege só com `DI` (`0x168b60`/`0x168bb0`); com threads entrando no meio dele
(retomadas em `0x1186cc` com pilhas de threads diferentes), saíram blocos sobrepostos: a câmera do título
(`0xbd5a40`, 0x2c0 bytes, criada por `0x10a360` ← `0x21a160`) caiu em cima da lista de tags DMA (REF, 0x100 × 16
bytes em `0xbd5a80`) que a troca de modo de vídeo (`0x1058f0` → `0x102220` → `0x1037a0`) aloca a cada cena. A lista
sobrescrevia as matrizes da câmera (`+0x40`, `+0x100`, `+0x140` com `0x30000080`…), a divisão de perspectiva
(`0x10ae58`, em `sub_0010A6B0`) dividia por zero e nada passava do recorte, então o VIF1 só recebia o `MPG` e nunca o
`MSCAL`. Com o `DI` respeitado (0025), a cena aparece. Itens 0 e 1 do "Falta" foram feitos antes (0021–0023, carga do
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
  cena 3D do título usa, desde o 0025). Por quadro: texturas
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
| `HL_FLOAT_LOG=1` | a cada 300 vsyncs, quantas diferenças de float PS2 × IEEE houve (expoente 255, divisão por zero, comparação, arredondamento do CVT.W.S, DIV/SQRT do VU0) e em que ponto do executável (deslocamento a partir de `__executable_start`, `addr2line`) (0024) |
| arquivos `hl_shot`, `hl_ram`, `hl_pad` | criados no diretório de execução enquanto o jogo roda (o runtime lê e apaga): `hl_shot` grava um quadro como o `HL_FRAME_DUMP`; `hl_ram` grava os 32 MB da RAM do EE em `hl_frames/ram_<tick>.bin`; `hl_pad` com botões (`UP UP CROSS`, `WAIT` = pausa) aperta cada um por 6 vsyncs e solta por 6 (0026) |

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
segura interrupções e trocas de thread, 0026 arquivos-gatilho `hl_shot`/`hl_ram`/`hl_pad`, 0027 vblank a 50 Hz com o modo PAL do `sceGsResetGraph`/`SetGsCrt` (também o relógio do stream de CD e o PTS dos filmes), 0028 vblank atrasado não adianta os próximos (sem ele o jogo ficava em ~53/s depois do boot);
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

- O menu principal com `HL_AUTOPRESS` (o memory card vazio pede para criar os dados).
- A fatia de tempo do escalonador (`checkpointDue`, `kDefaultTimeSliceCycles`) reveza threads de mesma prioridade; o
  kernel do PS2 não faz isso. Fica como suspeito se aparecer outra corrida.
- Semântica de float que o `HL_FLOAT_LOG` mostrou diferente do PS2, sem efeito visível por enquanto: `CVT.W.S` arredonda
  em vez de truncar (215 funções), `VDIV` por zero dá 0 (no PS2, ±máximo), `VSQRT` usa `max(0,x)` (no PS2, `|x|`),
  `VRSQRT` ignora `fs`. Os 29 div0 no início do título vêm da câmera montada antes de o jogo preencher os campos.
- A imagem sai com 512 de largura numa janela de 640 (faixa preta à direita).
- ~~O vsync é de 60 Hz, mas o jogo é PAL~~: feito no 0027 (50 Hz quando o jogo pede PAL).
- Os `(null)` dos menus.
- Áudio: SPU2 do ARMSX2 (ver Decisões).
- Desempenho do IOP (a IRQ do libsd dispara centenas de milhares de vezes por causa do autoDMA do ADX).

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
