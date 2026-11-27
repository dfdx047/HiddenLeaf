# Changelog

**English** · [Português](CHANGELOG.pt-BR.md) · [Español](CHANGELOG.es.md)

## v0.2.3 (2026-11-27)

Critical bugfix update focused on **fixing the v0.2.2 offline black screen and frozen frames with running audio**, **guaranteeing Ranked W.O. points against ragequitters**, **fixing Ranked scoring & Global Top 100**, and **moving About into More with guaranteed in-app Changelog display**.

**Download:** `HiddenLeaf-v0.2.3.apk` (Android 10+, 64-bit ARM — installs cleanly over `v0.2.2`, `v0.2.1`, and `v0.2.0-beta`, keeping all game data, saves, drivers, and mods).

### Offline Black Screen & Frozen Display Fixes

- **Stable Light Mode on Mali, PowerVR, MediaTek, Exynos, and Unisoc GPUs:** removed the experimental `v0.2.2` `un5.ini` overrides (`accurate_blending_unit=0`, `texture_preloading=1`) and global 6+ core `GSBackThreadMode=1` that caused offline black screens or frozen rendering while audio kept playing. 2-thread GS is again strictly limited to verified Qualcomm Adreno 610 SoCs (and disabled whenever ZeroFG is on), while preserving the real CPU speedup via `NOMINUM_VU1=1`.
- **Clean Offline Startup:** the **Play** button and home-screen shortcut now clear any leftover `online/session.txt` before launch and enforce a fresh `:game` process lifecycle (`onNewIntent` + `onDestroy`), preventing black screens after exiting online matches.

### Ranked Mode: Anti-Ragequit (W.O.), Ougi KO Detection & Global Top 100

- **Fast, Reliable W.O. Scoring Against Ragequitters:**
  - Reduced P2P disconnect timeout from **30s to 12s** when an opponent force-closes the app or cuts Wi-Fi.
  - Quitting via the in-game drawer during a Ranked match now submits an immediate `"forfeit"` report, awarding RP to the winner and deducting RP from the quitter.
  - The post-match modal keeps **Back to menu** disabled (*"Registrando..."*) until the server confirms the KO or W.O. result (`🏆 VITÓRIA POR W.O.! +X RP`), preventing early exits from cancelling the HTTP report.
- **No More Voided `KO + ragequit` Matches & Ougi / Time-Over KO Detection:**
  - Fixed the Cloudflare Worker so a real KO claim wins over a trailing peer disconnect instead of marking the match disputed (which previously voided ~17% of Ranked matches).
  - Engine HP tracking now polls every `onVsync()` and ignores CPU assist fighters, accurately detecting **Ultimate Jutsu (Ougi) KOs** and **Time Overs**.
- **Full Auto-Refreshing Global Top 100:** the Ranked screen now refreshes your profile and leaderboard automatically on `ON_RESUME` after every match, displays all **100 players** (instead of clipping at 20), and highlights your own row.

### Launcher, About Tab & Guaranteed In-App Changelog

- **About Moved to `More → About`:** version + build number, game folder path, GitHub link, update check, and a **View changelog** button now live in a dedicated **More → About** page.
- **Guaranteed In-App Update Changelog:**
  - Added automatic GitHub Atom fallback when GitHub's REST API returns `HTTP 403`, ensuring updates and localized changelogs (EN, PT-BR, ES) always load.
  - Shows a **What's new** dialog once after upgrading to a new version (even for manual APK installs or upgrades from `v0.2.0-beta`), also accessible anytime from **More → About**.
  - Pre-validates APK `versionCode` and signing certificate before opening Android's installer so updates always install cleanly over existing versions.

## v0.2.2 (2026-11-26)

Critical update focused on **fixing Online & Local Wi-Fi (LAN) multiplayer desync**, **automatic 1-match Ranked room disconnect**, and **extra entry-level device optimizations in Light Mode**.

**Download:** `HiddenLeaf-v0.2.2.apk` (Android 10+, 64-bit ARM — installs cleanly over `v0.2.1` and `v0.2.0-beta`, keeping all game data and saves).

### Online & Local (LAN) Desync Fixes

- **Cycle-Exact Synchronous IOP in Multiplayer (`HL_IOP_THREAD=0`):** previously, the asynchronous IOP worker thread could drift several VBlanks behind on slower devices, causing CDVD/SIF loads, Ultimate Jutsu (Ougi) audio, and post-match transitions to finish on different frames between peers — leading to mismatched Ougi button prompts and diverging match outcomes. Multiplayer (Online and LAN) now runs IOP synchronously by EE cycle so both devices stay locked to the exact same frame.
- **Deterministic EE Scheduler during Lockstep (`HL_ONLINE_DETERMINISTIC=1`):** patched `EeScheduler` so internal game alarms and `VBlankEnd` are dispatched strictly by EE cycle and never gated or reordered by host wall-clock delays during network waits; wall-clock pacing applies only to `VBlankStart`.
- **Deterministic Memory Card Timestamps (`HL_FIXED_CLOCK`) & Unified Boot (`HL_SKIP_INTRO=0`):** `sceMcGetDir` now returns deterministic timestamps instead of leaking real host clock values, and multiplayer sessions use identical boot flow across both peers.
- **Updated Online Fingerprint (`a20299`):** ensures `v0.2.2` players only match with other `v0.2.2` deterministic clients.

### Ranked Mode: 1 Match & Auto-Disconnect

- **Automatic Room Close After 1 Ranked Match:** once a Ranked match ends by KO, the app submits and confirms the bilateral ELO/RP report (`/v1/ranked/report`), displays the RP banner (`+RP` / `-RP`), automatically disconnects the room, and opens the post-match summary modal to return to the menu. Players simply queue again for their next match — preventing rematch farming and accumulated post-match drift.

### Extra Entry-Level Optimizations (Light Mode)

- **Automatic 2-Thread Vulkan GS (`GSBackThreadMode=1`) on Any 6+ Core Device in Light Mode:** expanded platform detection (`mt67*`, `mt68*`, `s5e*`) plus automatic enablement on any 6+ core CPU when Light Mode is active.
- **Lighter Vulkan Pipeline in Light Mode:** disables `accurate_blending_unit`, `mipmapping`, `trilinear_filtering`, and `dithering_ps2`, and enables `texture_preloading = 1` to improve FPS and reduce frame-time spikes on budget phones while leaving mid/high GPUs (Tier 2–4) untouched.

## v0.2.1 (2026-11-26)

Update focused on **Ranked Mode**, **Character Mastery**, **Live Spectator**, **Inspectable Ninja Profiles**, and **Touch Controls** — resolving the top community feedback reports from `v0.2.0-beta`.

**Download:** `HiddenLeaf-v0.2.1.apk` (Android 10+, 64-bit ARM — installs cleanly over `v0.2.0-beta`, keeping all game data and saves).

### Ranked Mode, ELO, Mastery & Anti-Grief

- **Working Ranked ELO / RP Progression:** the engine now deterministically detects KO winners directly from RDRAM (`fighter+0x6C`) at the end of every battle (including rematches in the same room), submits bilateral reports to Cloudflare (`/v1/ranked/report`), updates RP, Wins, Losses, Streak, and displays an in-game RP banner (`+RP` / `-RP`). Automatically awards victory if the opponent ragequits or drops mid-fight.
- **Anti-Grief START Button Block in Ranked Fights:** deterministic engine-level guard (`hl_game::inFightDeterministic()`) blocks **START** for both P1 and P2 during active Ranked battles (touch, gamepad, and macros) with zero desync. START continues working normally on character select and post-match screens.
- **Character Mastery Fixed:** XP is now awarded on **every single fight** (even when playing the same character repeatedly) plus bonus win/loss XP on the server.
- **Clickable Global Top 100 Profiles + Privacy Toggle:**
  - Tap any player on the leaderboard to open their **Ninja Profile** showing their photo/avatar, rank, tier, RP, total matches, wins, losses, winrate, streak, and full **per-character Mastery breakdown**.
  - Added a **Public / Private Profile** toggle under your Ninja Info Card so players can hide their custom photo, detailed stats, and mastery list if desired.
- **Custom Gallery Avatar + 20-Character Art Bank:** replaced broken screen-crop portraits with a unified picker supporting **custom images from your gallery** (synced to public profiles) and a **20-character cached art bank**.
- **Fixed Ranked & Casual Screen Layouts:** full-width uncached (`no-store`) **Refresh Ranking** button and `FlowRow` filter chips that never clip on any screen size (Odin 2 Mini, RG Cube 1:1, portrait or landscape phones).

### Live Spectator, Online, Controls & Diagnostics

- **Real In-Game Live Spectator Playback:** in Custom Lobbies (2–8 players), spectators now watch fights live inside the engine via host frame streaming + late-join replay over the WebSocket relay.
- **Desync Indicator (`⚠ Desync`):** re-enabled the online HUD desync warning so players immediately know if two games diverge.
- **Fixed `↑ ↑ ○` & `↓ ↓ ○` Touch Macro Buttons:** fixed double-tap timing (removed dead gap before `○`), auto-centered left analog stick during directional macros, canceled overlapping macros, and prevented accidental slide activation onto `Jutsu (△ + ○)` / `Ougi`.
- **In-App Update Changelog Always Shown:** removed silent Wi-Fi auto-download skipping so every player sees the full localized changelog inside the app before and during updates.
- **Fixed Bug-Report Log Export (`un5_log.txt`):** updated `LogExport.kt`, `LastRun.kt`, and `PerfReport.kt` from legacy `bt3_log.txt` to `un5_log.txt` so player logs include the real engine log and settings.

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
