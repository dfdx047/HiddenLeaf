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
