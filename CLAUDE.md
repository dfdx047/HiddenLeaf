# Hidden Leaf: instruções para o Claude Code

Recompilação de **Naruto Shippuden: Ultimate Ninja 5** (PS2, versão europeia SLES-55605) para PC e Android, com o
PS2Recomp, mais o launcher Android (`android/`, a partir do template do Dragon Rage).

## Regras

- **Nenhum arquivo do jogo entra no repositório** (ISO, ELF, `PRG/`, `DATA/`, código gerado a partir do ELF). Tudo que
  vem do disco fica em `build/` (ignorado). Antes de commitar: `git status` e conferir.
- Conversa com o desenvolvedor em **português do Brasil**. Comandos git **em inglês**, simples e curtos.
- Commits só locais, cada mudança testada; o desenvolvedor publica (ou pede o push).
- Experimentos em branch de laboratório, nunca direto na `main`.
- **Antes de começar qualquer trabalho:** ler `docs/registro.md` inteiro (estado das funções e histórico), este arquivo
  e o `docs/plano.md`. **Ao terminar cada item** (e antes de qualquer `/compact`): atualizar o `docs/registro.md` e
  commitar junto com o código. Nunca apagar entradas antigas; corrigir com uma entrada nova.
- Nada é dado como corrigido sem a rota completa gravada nos 3 aparelhos e no 20:9 (método no fim do
  `docs/registro.md`).

## Onde está cada coisa

| Caminho | O quê |
|---|---|
| `docs/analise-elf.md` | ELF, overlays (`MWo3`), mods (UN6 A35, Versão Brasileira), hashes |
| `docs/registro.md` | **estado das funções e histórico por build (ler primeiro)** |
| `docs/plano.md` | plano, decisões, controles na tela, contrato do motor |
| `tools/un5.py` | `verify`, `extract`, `overlays`, `diff`, `unpack`, `prepelf`, `ovlelf` |
| `tools/vifdump.py`, `docs/vifdump.md` | dumps do VIF1 e do GIF por cena (`HL_VIF_DUMP`, patch `0058`) para a Fase 3 do Nominum: leitor e formato |
| `tools/ramdiff.py` | compara estruturas do jogo entre a RAM do PCSX2 (savestate) e a do recomp (`hl_ram`), seguindo ponteiros |
| `recomp/build.sh` | ISO → disco extraído → ELF dividido → PS2Recomp → código gerado → runtime (→ `--run`); `--mod <iso>` gera o código de um mod em `build/mods/<id>/` |
| `recomp/android/build.sh` | `libun5.so` do Android; `--mod <id>` faz `libun5_<id>.so`, carregada pelo launcher quando o mod está ligado |
| `android/` | launcher (Kotlin/Compose): importador da base, mods por ISO, controles do UN5 por cena |

## Rodar no PC (CachyOS)

```
recomp/build.sh ~/Downloads/Minerva_Myrient/Redump/"Sony - PlayStation 2"/"Naruto Shippuden - Ultimate Ninja 5 (Europe) (En,Fr,De,Es,It).iso" --run
```

Dependências (Arch/CachyOS): `base-devel cmake ninja p7zip libx11 libxrandr libxinerama libxcursor libxi mesa alsa-lib ffmpeg`
(o FFmpeg decodifica os filmes PSS).
O log da execução vai para `build/run.log`.

## Estado (2026-10-26; o de hoje está em `docs/plano.md`)

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

Fase Android (estado, medidas e a noite de 2026-11-03 em `docs/plano.md`, "Fase Android"). A luta roda a 50 fps nos dois
aparelhos, com o renderizador nativo (`recomp/android/gsx/hl_native.cpp`), que liga sozinho ao detectar a luta; menus e o resto seguem
pelo GS do ARMSX2 (`libbt4gsx`).

1. Tela de vitória: o personagem não chega ao GS (só camadas 2D); achar onde o EE ou a VU1 perde o desenho.
2. Campanha no `sd480p` (43,7 fps) e no Cube (17% de pares lentos): o limite é o interpretador da VU1.
3. Mods de código: o A35 luta no Cube, mas só com um save em `BASLUS-55606NARUTO6` (copiei o do UN5 à mão); o
   Alpha 1.1X (sha `20872b06...`) precisa de entrada em `KNOWN_MODS` e `CODE_RANGES`, e a ISO dele só está no Odin 3.
4. Menus em 16:9 ("Esticar" e "Desenhar em 16:9"), packs de textura, os `?` da grade de personagem ligado.
5. Odin 3: 60 e 120 interpolados num painel de 120 Hz, resolução 1x a 4x.

## Aparelhos de teste

- Retroid Pocket 5 (SD865): `192.168.1.89:5555`. Medir com `HL_STOP_PERF=1 tools/device-profile.sh -s 192.168.1.89:5555 limit sd480p`.
- Anbernic RG Cube (Mali): `192.168.1.183:5555`, sem limite.
- AYN Odin 3 (tela de 120 Hz): `192.168.1.134:5555`. Tem padrão de bloqueio: sem o desenvolvedor desbloquear, não dá
  para usar.
- AYN Odin 2 Mini: `adb -s 9cf62eef`. Instalar o launcher:
  `cd android && ANDROID_SERIAL=9cf62eef ./gradlew installDebug`.
- Dados do app: `/sdcard/Android/data/com.dfdx047.hiddenleaf/files/`.
