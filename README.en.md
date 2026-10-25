# Hidden Leaf

**Naruto Shippuden: Ultimate Ninja 5** (PlayStation 2) recompiled for Android and PC, in the style of
[Dragon Rage](https://github.com/dfdx047/Tenkaichi3DecompAndroid).

> A fan project, not affiliated with Bandai Namco or CyberConnect2. **No game file is in this repository or in the
> app**: everyone imports their own ISO.

[Português](README.md)

## How it works

- **Base:** the European release, *Naruto Shippuden: Ultimate Ninja 5* (SLES-55605). Its code (the ELF and the `PRG/`
  overlays) is recompiled to native code with [PS2Recomp](https://github.com/ran-j/PS2Recomp). It is not emulation.
- **Importing the game:** the app checks the ISO by SHA-256 and copies only the small boot files. The rest is read from
  the ISO where it is.
- **Mods on top:** on the Mods page you pick a mod's ISO (Ultimate Ninja 6, the Brazilian version, a translation). The
  app keeps only the files that differ from the base.
  - **Data mods** (text, voices, models) run as they are.
  - **Mods that change the code** (UN6, for example) need their **code pack**: only the functions the mod changed or
    added, recompiled. UN6 A35 changes about 630 functions and adds about 770, in a game of ~22 thousand
    (`tools/un5.py diff`).

See [docs/plano.md](docs/plano.md) for the plan (in Portuguese) and the README in Portuguese for the status.

## Building the app

```
cd android
./gradlew assembleDebug
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```
