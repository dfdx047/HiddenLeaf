# Hidden Leaf: instruções para o Claude Code

Recompilação de **Naruto Shippuden: Ultimate Ninja 5** (PS2, versão europeia SLES-55605) para PC e Android, com o
PS2Recomp, mais o launcher Android (`android/`, a partir do template do Dragon Rage).

## Regras

- **Nenhum arquivo do jogo entra no repositório** (ISO, ELF, `PRG/`, `DATA/`, código gerado a partir do ELF). Tudo que
  vem do disco fica em `build/` (ignorado). Antes de commitar: `git status` e conferir.
- Conversa com o desenvolvedor em **português do Brasil**. Comandos git **em inglês**, simples e curtos.
- Commits só locais, cada mudança testada; o desenvolvedor publica (ou pede o push).
- Experimentos em branch de laboratório, nunca direto na `main`.

## Onde está cada coisa

| Caminho | O quê |
|---|---|
| `docs/analise-elf.md` | ELF, overlays (`MWo3`), mods (UN6 A35, Versão Brasileira), hashes |
| `docs/plano.md` | plano, decisões, controles na tela, contrato do motor |
| `tools/un5.py` | `verify`, `extract`, `overlays`, `diff`, `unpack`, `prepelf`, `ovlelf` |
| `recomp/build.sh` | ISO → disco extraído → ELF dividido → PS2Recomp → código gerado → runtime (→ `--run`) |
| `android/` | launcher (Kotlin/Compose): importador da base, mods por ISO, controles do UN5 por cena |

## Rodar no PC (CachyOS)

```
recomp/build.sh ~/Downloads/"Naruto UN5"/"Naruto Shippuden - Ultimate Ninja 5 (Europe) (En,Fr,De,Es,It).iso" --run
```

Dependências (Arch/CachyOS): `base-devel cmake ninja p7zip libx11 libxrandr libxinerama libxcursor libxi mesa alsa-lib ffmpeg`
(o FFmpeg decodifica os filmes PSS).
O log da execução vai para `build/run.log`.

## Estado (2026-10-26)

- ELF europeu: o código está em `0x100000–0x3d5760` e `0x5e3a00–0x5e7208` (construtores estáticos); o resto da seção
  única é dado, e `0x40f848–0x410d68` é código de kernel copiado pelo jogo. `un5.py prepelf` divide as seções para o
  recompilador: **8158 funções, 0 erros** no PS2Recomp `e563698`.
- O runtime do PS2Recomp compila no Linux com GCC 13 desde que tenha `-msse4.2 -mavx` (o script usa `-march=native`).
- **Overlays (implementado, ainda não testado com o jogo):** cada overlay de `PRG/` (`BTL` luta, `ADV` aventura,
  `ETC`, `TEXT*`) é recompilado à parte (BTL 2832 funções, ADV 3679, ETC 253, 0 erros), renomeado num módulo com
  tabela própria (`recomp/overlays.py`) e achado em tempo de execução pelo conteúdo da memória
  (`recomp/runtime/un5_overlays.cpp`), via um gancho no despacho do runtime (`recomp/patches/0001-overlay-resolver.patch`).
- **Primeira execução no PC:** a janela abre e o jogo roda até o boot do IOP. Corrigido em seguida:
  - `MODULES.BIN` (9 IRX) é copiado para a RAM do IOP com `sceSifSetDma` e carregado com `sceSifLoadModuleBuffer`
    pelo endereço do IOP; o runtime lia esse endereço na RAM do EE e falhava calado. `0003-iop-module-buffers.patch`
    lê da RAM do IOP, roda de verdade `libsd`, `sdr_driver`, `modmidi`, `modhsyn`, `cdvdstm` e deixa `sio2man`,
    `padman`, `mcman`, `mcserv` no HLE do EE. Com isso o driver ADX (CRI) registra o RPC `0x90000200`.
  - O EE ficava preso em `0x173460` esperando o RPC `0x80000597` (busca de arquivo do `cdvdfsv`):
    `sceCdLayerSearchFile` (`0x1733c0`) não tinha tratador. `un5.py stubfix` o liga ao `sceCdSearchFile` do runtime.
  - LTO desligado por padrão (`HL_LTO=1` liga).

## Próximos passos

O estado detalhado do boot (onde o jogo está, endereços, variáveis `HL_*` de depuração) fica em `docs/plano.md`,
seção "Estado do boot". Em resumo: o jogo passa da tela de idioma, toca os filmes (FFmpeg) e desenha a cena 3D da
tela de título (patch 0025: o `DI` do jogo agora segura interrupções e trocas de thread). ~52 vsyncs/s. Sem áudio.

1. Menu principal com `HL_AUTOPRESS` (ver "Falta" em `docs/plano.md`).
2. Áudio: SPU2 do ARMSX2 num patch (ver `docs/plano.md`, Decisões).
3. Gráficos no Android: o renderizador do GS do ARMSX2.
4. `TouchBridge.nativeScene()` no host Android: 1 menu, 2 luta (`BTL.BIN` residente), 3 campanha (`ADV.BIN`).

## Aparelhos de teste

- AYN Odin 2 Mini: `adb -s 9cf62eef`. Instalar o launcher:
  `cd android && ANDROID_SERIAL=9cf62eef ./gradlew installDebug`.
- Dados do app: `/sdcard/Android/data/com.dfdx047.hiddenleaf/files/`.
