# Changelog

**English** · [Português](CHANGELOG.pt-BR.md) · [Español](CHANGELOG.es.md)

## v0.1.0-beta (2026-11-14)

The first public release. *Naruto Shippuden: Ultimate Ninja 5* (PS2, Europe, SLES-55605) recompiled to native ARM64
code for Android, with a launcher, touch controls, mods and online play. You need your own ISO of the game.

**Download:** `HiddenLeaf-v0.1.0-beta.apk` (Android 10+, 64-bit ARM).

### Game engine

- The game's ELF and every `PRG/` overlay (fight, adventure, menus, texts) statically recompiled with PS2Recomp:
  8158 ELF functions, 2832 in the fight overlay, 3679 in the adventure overlay, 253 in ETC, 0 errors.
- Overlays found at run time by their bytes in memory, with code islands extended to their own branch targets.
- Two engines in one APK: ARMv8.2-A and ARMv8.0-A (older chips such as the Snapdragon 625/665).
- Real IOP modules (`libsd`, `sdr_driver`, `modmidi`, `modhsyn`, `cdvdstm`, the recompiled `libsdr`) with SIF DMA and
  RPC; the IOP runs in exact pre-decoded blocks, on its own thread together with the SPU2.
- The disc is read straight from your ISO, with each file at its real sector and the drive's timing.
- PS2 behaviour matched against PCSX2: EE FPU and VU0 rounding, `SQRT.S`, the game's own `rand`, 64-bit branches, the
  PS2's heap layout, thread priorities, idle loops and `DI`.
- VIF1, VU1 and GIF on their own thread; V2/V3 unpack like the hardware; PATH2 and PATH3 transfers kept whole;
  Hinata's awakening drawn right.
- MPEG films with their sound in order, ended without the stream's EOF; 50 fps to the last frame.
- Threads pinned by core speed; lighter scheduler, address translation and thread lookup.

### Graphics

- ARMSX2/PCSX2's GS as the default renderer, over Vulkan or OpenGL ES; native fight renderer as an option.
- Internal resolution from 0.5x to 8x.
- **Widescreen (16:9)** and **Ultrawide (20:9)** in fights, the campaign and the menus: the scene widens, the HUD and
  portraits keep their shape; films stay 4:3. The wide camera turns off for the round's end panel (no black bars).
- 30 and 50 fps, plus 60, 90 and 120 fps by interpolation on screens that support them.
- **Light mode** for Snapdragon 665 class phones: Nominum-Render's VU1 and the GS split in two threads.
- Custom Adreno drivers (Turnip) downloaded and installed from the launcher.
- Fixes: Neji's Ougi hang, Sakura's Ougi scanlines, the Ougi freeze, the black screen after an Ougi clash, the
  victory screen ghost, the lake's broken reflection, missing smoke textures, PAL display window and SMODE1.

### Sound

- SPU2 sound with the audio queue held near 80 ms and no clipping peaks; film sound packets kept in order.
- The menus are muted while an online match is being set up, and the sound comes back at the character select.

### Controls

- On-screen controls per scene: fight, campaign (with conversations) and menus.
- One-touch specials **↑↑○** and **↓↓○**; Jutsu and Ougi on one button.
- **Toque Pro**: a large, fixed D-pad with a wide touch area; icon-only buttons; button names; ink and glass styles.
- Macro editor, button guide and a layout editor for every button's size, position and opacity.
- Item and guard buttons above the game's item bar; the scene button is back on screen (fight, campaign, menu).
- Physical controllers: detected by themselves, with their own mapping; the touch buttons hide while a controller is
  connected and come back when it disconnects.

### In-game menu

- A side drawer menu over the paused game, by touch or controller: Game, Graphics, Controller, Touch and Help tabs.
- Opens with the shortcut you choose (SELECT + START, L3 + R3 or Back alone); "Quit game" returns to the launcher.

### Online

- **Naruto Online** inside the game's own Mode Select, under Free Battle. The panel opens to the left of the animated
  Naruto, with the music still playing, by touch or controller, in the game's look.
- **Quick match:** anyone else with the same Hidden Leaf version, no code.
- **Create room** and **Enter code** (6 digits).
- **Local network (Wi-Fi):** host and search on the same Wi-Fi or hotspot, found by broadcast, no internet needed.
- Both phones go straight to the character select (1P and 2P), fast-forwarded and muted through the boot and the menus
  (12–30 s on a phone). The host controls the menus and the stage; the guest controls 2P.
- Lockstep with input delay; direct link through STUN with a relay as fallback; full fights in sync on LAN and the
  internet.
- Rooms service on a Cloudflare Worker with a D1 database. When the free daily quota runs out, the home page says when
  it resets.
- An end-of-match dialog in the game's style when the other player leaves or the link drops.
- Base game only for now (no mods); both phones need the same version.

### Mods

- Import a mod's ISO: the app keeps only the files that differ from the base game.
- **Ultimate Ninja 6 (Alpha 3.5)** with its recompiled code pack (about 630 changed and 770 new functions); its
  memory card folder made from the base game's save.
- **Ultimate Ninja 6 (Alpha 1.1X)** and the **Versão Brasileira** on the base engine.
- Known mods get a badge and a language hint.

### Launcher

- Five pages: Home, Online, Controls, Settings and More (Mods, Textures, Drivers, Cheats, Debug).
- Base ISO checked by SHA-256 (Redump); only the small boot files are copied.
- Memory card manager: `.ps2` import and export with a backup; folder import.
- Cheats: unlocks written to the game's save.
- Skip intro (by START), home screen shortcut, portrait and landscape layouts.
- New icon: an ink ensō with a hidden leaf.
- Portuguese, English and Spanish.

### Known issues

- Some effects can still differ from the PS2; light mode can drop or flicker some effects.
- Texture packs install, but the game does not use them yet.
- Online: base game only; the same Hidden Leaf version on both phones.
- Mods that change code need a code pack (today: UN6 Alpha 3.5).
