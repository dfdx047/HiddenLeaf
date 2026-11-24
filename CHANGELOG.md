# Changelog

**English** · [Português](CHANGELOG.pt-BR.md) · [Español](CHANGELOG.es.md)

## v0.2.0-beta (2026-11-24)

Major performance, graphics and Online update: **real Frame Generation (ZeroFG 1.2)** for offline play, **reorganized Online hub** with dedicated **Casual Mode** (including 2–8P Custom Lobby & Spectator) and **Ranked Mode** (Genin → Kage, Per-Character Mastery & Global Top 100), **GPU-aware Vulkan Drivers tab** (Turnip, PanVK, MaliSX2 & RADV), **100% Save with R1 transformations unlocked**, **Skip Company Logos + Intro**, and permanent fix for post-fight menu textures.

**Download:** `HiddenLeaf-v0.2.0-beta.apk` (Android 10+, 64-bit ARM).

### Graphics & Real Frame Generation (ZeroFG 1.2)

- **Real Frame Generation (ZeroFG 1.2 · Offline Only):** removed the non-functional interpolated FPS (60/90/120) option and integrated **ZeroFG 1.2** (dedicated Vulkan 1.3 Device B, ZeroGS mode with HUD-layer separation & content-break detection, **no external DLL required**).
  - Multipliers: **Off**, **2x**, **3x**, **4x** (in Settings and in-game menus), alongside native FPS cap (`30` or `50 original`).
  - **Strictly Offline Only:** `un5_main.cpp` automatically forces `ZeroFGEnabled = false` during Online matches (`hl_online::active()`) so online play stays fair at native 50 Hz.
  - Automatically requests the display's highest refresh rate (`preferRefresh`) when ZeroFG is enabled.
- **Real Hardware Benchmarks:**
  - **Retroid Pocket 5 (Snapdragon 865 / Adreno 650):** ZeroFG 1.2 active on Device B at `1920×1080` (`status=0`), smooth 2x + CAS.
  - **Anbernic RG Cube (Unisoc T820 / Mali-G57):** ZeroFG 1.2 active (`704×528`, `status=0`), doubling 25 FPS native to **50–59 FPS on display** (~13% GPU on GS with framebuffer fetch).
  - **Xiaomi Mi A3 (Snapdragon 665 / Adreno 610, stock Vulkan 1.1.128):** graceful fallback with zero crash (auto-activates when using a Vulkan 1.3 Turnip driver); Light Mode keeps fights and menus steady.
- **Post-fight menu textures fixed:** resolved inverted/corrupted menu textures when returning from a fight to Character Select or Mode Select (`hl_gsx.cpp`, `hl_native.cpp`), visually verified pre-fight vs post-fight on RP5, RG Cube and Mi A3.
- **Refined Light Mode:** auto-enabled only on weak GPUs (Tier 0–1: Adreno 3xx–620, Mali-G31/G51/G52/G57/G68/G71/G72, PowerVR, Xclipse 5xx) and kept off by default on mid/high GPUs (Tier 2–4) to preserve 100% of PS2 visual effects and filters. Expanded 2-thread GS SoC list (`trinket`, `bengal`, `khaje`, `ums*`, `mt676*`, `mt678*`, `mt683*`, `exynos*`, `sc9863*`).

### GPU-Aware Vulkan Drivers (Turnip, PanVK, MaliSX2 & RADV)

- **Automatic GPU family detection** in the **Drivers** tab with tailored sources:
  - **Adreno (Qualcomm):** Turnip (ARMSX2, Mr. Purple, StevenMXZ, K11MCH1, crueter 8 Elite, Banner's Turnip).
  - **Mali (ARM):** **PanVK** (`JimVulkan/mali-panvk`, `panvk-kbase-android`, `FristOneRR-Panvk-Driver`) and **MaliSX2** (`bmdhacks/malisx2`).
  - **Xclipse (Samsung / AMD RDNA):** **RADV** (`JimVulkan/radv-xclipse` and Paulo Wesley's `paulowesll/mesa`).
- **Fixed broken Drivers tab (GitHub HTTP 403):** added automatic fallback via GitHub Atom (`releases.atom` + `expanded_assets`) when anonymous REST API rate limits are hit.

### Reorganized Online Mode (Casual, Ranked, Mastery & Spectator)

- **Clean Online Hub:** Internet / Local Wi-Fi toggle, player name, **`Casual Mode`** button, **`Ranked Mode`** button, with only Server & Connection settings outside.
- **Per-mode Control Filter:** removed the global control filter from the main page and placed dedicated filters (`Touch Only` / `Controller Only` / `All`) inside **Casual** and **Ranked** separately, with a 15s **"Taking too long? Accept any controller"** fallback button.
- **Dedicated Casual Screen (`CasualDedicatedScreen`):** 2-column landscape layout grouping Control Filter, Quick Match, Train While Queuing, 1v1 Friend Rooms and **Custom Lobby & Live Spectator (2–8 players)** with real-time role switching (`P1` / `P2` / `Spectator`) and full-width room code input.
- **Dedicated Ranked Screen (`RankedDedicatedScreen`):** separate competitive queue (`#ranked`), **Ninja Info Card** with real extracted in-game character portraits, ranks (`Genin` → `Kage`), RP, W/L, win streak, **auto P1 character detection**, automatic HUD portrait export (`PixelCopy`), **per-character Mastery**, and **Global Top 100 Leaderboard**.
- **Cloudflare Free-Tier Backend Optimizations:** massive D1 write reduction (`TOUCH_EVERY=12s`, removed `sweep()` from poll/ws), Lazy WebSocket Connect (0 DO requests on direct P2P), IP rate limiting and UDP anti-spoof protection.

### Quality of Life, 100% Save, Mods & Controls

- **Skip Company Logos + Intro (`0111-skip-logos-and-safe-stop.patch`):** waits for Language Select, then fast-forwards past Bandai, CyberConnect2, CRIWARE and opening movies straight to **New Game / Load Game** in ~1–2 seconds.
- **Fixed phantom Save-button loop in UN6 A35 & mods:** hard stop added across all 4 native libraries (`libun5.so`, `libun5_v80.so`, `libun5_un6_a35.so`, `libun5_un6_a35_v80.so`).
- **100% Save & Unlock-All Cheat with R1 Transformations:** fixed progress flag (`save + 0x1C5C = 0x6E`) in both the built-in 100% save and Online/Cheat unlock so all R1 starting transformations are unlocked.
- **Touch Controls:** `↑↑○` and `↓↓○` special buttons now support **hold-to-charge** (`holdLast = true`); excluded `R2` (Guard/Kawarimi) from accidental slide activation and raised analog trigger deadzone to 50%; removed unused one-button special option; menu touch gestures remain optional with an animated guide.

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
- **Automatic updates:** the app looks for a new version on GitHub when it opens, shows what changed, downloads it
  (by itself on Wi-Fi, after asking on mobile data) and opens the installer; the game data, settings and saves stay.
- New icon: an ink ensō with a hidden leaf.
- Portuguese, English and Spanish.

### Known issues

- Some effects can still differ from the PS2; light mode can drop or flicker some effects.
- Texture packs install, but the game does not use them yet.
- Online: base game only; the same Hidden Leaf version on both phones.
- Mods that change code need a code pack (today: UN6 Alpha 3.5).
