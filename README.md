# Hidden Leaf

**Naruto Shippuden: Ultimate Ninja 5** (PlayStation 2) recompilado para Android e PC, no estilo do
[Dragon Rage](https://github.com/dfdx047/Tenkaichi3DecompAndroid).

> Projeto de fã, sem ligação com a Bandai Namco ou a CyberConnect2. **Nenhum arquivo do jogo está neste
> repositório nem no app**: cada pessoa importa a própria ISO.

[English](README.en.md)

## Como funciona

- **Base:** a versão europeia, *Naruto Shippuden: Ultimate Ninja 5* (SLES-55605). O código dela (o ELF e os overlays de
  `PRG/`) é recompilado para código nativo com o [PS2Recomp](https://github.com/ran-j/PS2Recomp). Não é emulação.
- **Importar o jogo:** o app confere a ISO pelo SHA-256 e copia só os arquivos pequenos de boot. O resto é lido da ISO
  onde ela está.
- **Mods por cima:** na página Mods, você escolhe a ISO de um mod (o Ultimate Ninja 6, a Versão Brasileira, uma tradução).
  O app guarda só os arquivos que diferem da base.
  - **Mod de dados** (textos, vozes, modelos): roda direto.
  - **Mod que muda o código** (o UN6, por exemplo): precisa do **pacote de código** dele, com só as funções que o mod
    alterou ou acrescentou, recompiladas. O UN6 A35 muda cerca de 630 funções e acrescenta cerca de 770, num jogo de
    ~22 mil (`tools/un5.py diff`).

## Estado

| Parte | Estado |
|---|---|
| Análise do ELF, dos overlays (`MWo3`) e dos mods | feita ([docs/analise-elf.md](docs/analise-elf.md)) |
| Ferramentas (`tools/un5.py`): verificar ISO, extrair boot, overlays, diff por função, desempacotar SjCRUNCH | feitas |
| Launcher Android: tema, importador da base, mods por ISO, drivers, controles, texturas, depuração | feito (sem o motor) |
| Mapa de funções (Ghidra) | próximo |
| Recompilação e runtime no PC | depois |
| Motor no Android (host SDL3 + renderizador ARMSX2) | depois |
| Pacote de código do UN6 A35 | depois |

O plano completo está em [docs/plano.md](docs/plano.md).

## Ferramentas

```
python3 tools/un5.py verify  <iso>                  # é a base europeia? é um mod conhecido?
python3 tools/un5.py extract <iso> build/boot       # SYSTEM.CNF, ELF, PRG/, MODULES/
python3 tools/un5.py overlays build/boot/PRG        # id, endereço e tamanhos de cada overlay
python3 tools/un5.py diff <base.iso> <mod.iso> -o build/pack.json   # o que o mod muda no código
```

## Compilar o app

```
cd android
./gradlew assembleDebug
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

Os dados do app ficam em `Android/data/com.dfdx047.hiddenleaf/files/`.
