# Changelog

[English](CHANGELOG.md) · [Português](CHANGELOG.pt-BR.md) · **Español**

## v0.2.1 (26/11/2026)

Actualización enfocada en el **Modo Clasificatorio**, **Maestría de Personajes**, **Espectador en Vivo**, **Perfiles Ninja** y **Controles Táctiles** — corrigiendo los principales reportes de la comunidad en `v0.2.0-beta`.

**Descarga:** `HiddenLeaf-v0.2.1.apk` (Android 10+, ARM de 64 bits — se instala directamente encima de `v0.2.0-beta` conservando todos tus datos y guardados).

### Modo Clasificatorio, ELO, Maestría y Anti-Grief

- **Puntuación Clasificatoria (ELO / RP) funcionando de extremo a extremo:** el motor ahora detecta automáticamente al ganador por KO directo en la RDRAM (`fighter+0x6C`) al finalizar cada combate (incluyendo revanchas en la misma sala), envía reportes bilaterales a Cloudflare (`/v1/ranked/report`), actualiza RP, Victorias, Derrotas, Racha y muestra un aviso en pantalla (`+RP` / `-RP`). También otorga victoria automática ante desconexión/ragequit en pleno combate.
- **Bloqueo del botón START durante combates Clasificatorios:** protección determinista en el motor (`hl_game::inFightDeterministic()`) que bloquea **START** para P1 y P2 durante las peleas clasificatorias (táctil, mando y macros) sin causar desincronización. En selección de personajes y menús posteriores, START sigue funcionando normal.
- **Maestría de Personajes corregida:** ahora suma XP en **cada combate jugado** (incluso repitiendo el mismo personaje) y añade XP extra de victoria/derrota en el servidor.
- **Perfiles clicables en el Top 100 Global + Control de Privacidad:**
  - Tocar cualquier jugador en el ranking abre el modal **Perfil Ninja** con foto/avatar, rango, RP, partidas jugadas, victorias, derrotas, porcentaje de victoria, racha y la lista completa de **personajes jugados con su maestría individual**.
  - Nuevo interruptor **Perfil público / Perfil privado** bajo tu Tarjeta Ninja para ocultar foto personalizada, estadísticas detalladas y maestrías si lo prefieres.
- **Avatar personalizado (Galería) + Banco de Artes (20 Ninjas):** eliminado el recorte roto de pantalla; ahora puedes usar **una imagen de tu galería** (sincronizada en tu perfil público) o elegir entre **20 personajes del banco de artes** en caché local.
- **Diseño de Clasificatorio y Casual corregido:** botón **Actualizar ranking** a ancho completo sin caché (`no-store`) y chips de filtro con ajuste automático (`FlowRow`) sin cortes en ninguna pantalla.

### Espectador en Vivo, En Línea, Controles y Diagnóstico

- **Modo Espectador en Vivo real dentro del juego:** en Salas Personalizadas (2–8 jugadores), los espectadores ahora ven el combate en tiempo real dentro del motor mediante retransmisión de cuadros por WebSocket.
- **Indicador de desincronización (`⚠ Desync`):** reactivado el aviso en el HUD en línea cuando dos partidas divergen.
- **Botones táctiles `↑ ↑ ○` y `↓ ↓ ○` corregidos:** mejorado el tiempo de doble toque (sin pausa muerta antes de `○`), centrado del stick izquierdo durante macros direccionales, cancelación de macros superpuestos y bloqueo de deslizamiento accidental hacia `Jutsu (△ + ○)` / `Ougi`.
- **Changelog siempre visible dentro de la app al actualizar:** eliminada la descarga silenciosa en Wi-Fi que ocultaba las novedades; ahora todos los jugadores ven el changelog completo dentro de la app antes y durante la actualización.
- **Exportación de logs corregida (`un5_log.txt`):** actualizados `LogExport.kt`, `LastRun.kt` y `PerfReport.kt` del antiguo `bt3_log.txt` a `un5_log.txt` para que los reportes incluyan el log real del motor y los ajustes.

## v0.2.0-beta (24/11/2026)

Gran actualización de rendimiento, gráficos y Modo En Línea: **Frame Generation real (ZeroFG 1.2)** para el modo sin conexión, **menú En Línea reorganizado** con pantallas dedicadas para **Modo Casual** (incl. Sala Personalizada y Espectador 2–8J) y **Modo Clasificatorio** (Genin → Kage, Maestría por Personaje y Top 100 Global), **pestaña de Drivers** con detección automática de GPU (Turnip, PanVK, MaliSX2 y RADV), **Guardado 100% con transformaciones R1**, **Saltar Logos + Introducción** y corrección definitiva de texturas de menús tras los combates.

**Descarga:** `HiddenLeaf-v0.2.0-beta.apk` (Android 10+, ARM de 64 bits).

### Gráficos y Frame Generation real (ZeroFG 1.2)

- **Frame Generation real (ZeroFG 1.2 · Solo Offline):** eliminada la antigua opción de FPS interpolados (60/90/120) e integrado el motor **ZeroFG 1.2** (dispositivo Vulkan 1.3 dedicado en Device B, modo ZeroGS con separación de HUD y detección de cortes de escena, **sin necesidad de DLL externa**).
  - Multiplicadores: **Apagado**, **2x**, **3x** y **4x** (en Ajustes y en los menús dentro del juego), junto al límite nativo (`30` o `50 original`).
  - **Exclusivo del modo Offline:** `un5_main.cpp` fuerza `ZeroFGEnabled = false` automáticamente en partidas en línea (`hl_online::active()`) para mantener el juego competitivo justo a 50 Hz nativos.
  - Solicita automáticamente la máxima tasa de refresco de la pantalla (`preferRefresh`) al activar ZeroFG.
- **Resultados medidos en hardware real:**
  - **Retroid Pocket 5 (Snapdragon 865 / Adreno 650):** ZeroFG 1.2 activo en Device B a `1920×1080` (`status=0`), 2x + CAS fluido.
  - **Anbernic RG Cube (Unisoc T820 / Mali-G57):** ZeroFG 1.2 activo (`704×528`, `status=0`), duplicando de 25 FPS nativos a **50–59 FPS reales en pantalla** (~13% de GPU en GS con framebuffer fetch).
  - **Xiaomi Mi A3 (Snapdragon 665 / Adreno 610, Vulkan 1.1.128 de fábrica):** respaldo seguro sin cierres (se activa automáticamente usando un driver Turnip Vulkan 1.3); el Modo Ligero mantiene combates y menús estables.
- **Corrección de texturas de menús tras combates:** solucionadas las texturas invertidas/corruptas al volver de un combate a Selección de Personaje o Mode Select (`hl_gsx.cpp`, `hl_native.cpp`), verificado en RP5, RG Cube y Mi A3.
- **Modo Ligero refinado:** activado automáticamente solo en GPUs modestas (Tier 0–1: Adreno 3xx–620, Mali-G31/G51/G52/G57/G68/G71/G72, PowerVR, Xclipse 5xx) y apagado por defecto en GPUs medias/altas (Tier 2–4) para conservar el 100% de los efectos y filtros de PS2. Lista ampliada de SoCs con GS en 2 hilos (`trinket`, `bengal`, `khaje`, `ums*`, `mt676*`, `mt678*`, `mt683*`, `exynos*`, `sc9863*`).

### Drivers Vulkan por GPU (Turnip, PanVK, MaliSX2 y RADV)

- **Detección automática de familia de GPU** en la pestaña **Drivers** con fuentes recomendadas:
  - **Adreno (Qualcomm):** Turnip (ARMSX2, Mr. Purple, StevenMXZ, K11MCH1, crueter 8 Elite, Banner's Turnip).
  - **Mali (ARM):** **PanVK** (`JimVulkan/mali-panvk`, `panvk-kbase-android`, `FristOneRR-Panvk-Driver`) y **MaliSX2** (`bmdhacks/malisx2`).
  - **Xclipse (Samsung / AMD RDNA):** **RADV** (`JimVulkan/radv-xclipse` y `paulowesll/mesa` de Paulo Wesley).
- **Solución definitiva a la pestaña de Drivers (HTTP 403):** añadido respaldo automático mediante GitHub Atom (`releases.atom` + `expanded_assets`) cuando la API REST anónima alcanza el límite.

### Nuevo Modo En Línea (Reorganizado, Clasificatorio, Maestría y Espectador)

- **Hub En Línea limpio:** selector `En línea (internet)` / `Red local (Wi-Fi)`, nombre del jugador, botón **`Modo Casual`**, botón **`Modo Clasificatorio`** y solo ajustes de servidor/conexión fuera.
- **Filtro de Control separado por modo:** eliminado el filtro global de la pantalla principal y añadido filtro dedicado (`Solo Pantalla` / `Solo Mando` / `Todos`) dentro de **Casual** y de **Clasificatorio**, con botón automático **"¿Tardando? Aceptar cualquier control"** tras 15s.
- **Pantalla dedicada — Modo Casual (`CasualDedicatedScreen`):** diseño de 2 columnas (horizontal) con Filtro de Control, Partida Rápida, Entrenar en Cola, Salas 1v1 y **Sala Personalizada y Espectador (2–8 jugadores)** con cambio de rol en vivo (`P1` / `P2` / `Espectador`) y campo de código sin cortes.
- **Pantalla dedicada — Modo Clasificatorio (`RankedDedicatedScreen`):** cola competitiva separada (`#ranked`), **Tarjeta Ninja** con retratos reales extraídos del juego, rangos (`Genin` → `Kage`), RP, V/D, racha de victorias, **detección automática de personaje**, exportación automática del retrato del HUD (`PixelCopy`), **Maestría individual por personaje** y **Top 100 Global**.
- **Optimización del servidor Cloudflare Free-Tier:** fuerte reducción de escrituras D1 (`TOUCH_EVERY=12s`, eliminado `sweep()` en poll/ws), Lazy WebSocket Connect (0 peticiones DO en P2P directo), límite de tasa por IP y protección anti-spoof UDP.

### Calidad de vida, Guardado 100%, Mods y Controles

- **Saltar Logos + Introducción (`0111-skip-logos-and-safe-stop.patch`):** espera la selección de idioma y salta automáticamente Bandai, CyberConnect2, CRIWARE y la intro directo a **New Game / Load Game** en ~1–2 segundos.
- **Corrección del bucle de guardado automático en UN6 A35 y mods:** parada rígida permanente en las 4 bibliotecas nativas (`libun5.so`, `libun5_v80.so`, `libun5_un6_a35.so`, `libun5_un6_a35_v80.so`).
- **Guardado 100% y truco "Desbloquear todo" con Transformaciones R1:** corregido el indicador de progreso (`save + 0x1C5C = 0x6E`) tanto en el guardado 100% integrado como en el modo En Línea/Trucos para desbloquear todas las transformaciones iniciales con **R1**.
- **Controles Táctiles:** los botones especiales `↑↑○` y `↓↓○` ahora permiten **mantener pulsado para cargar** (`holdLast = true`); eliminada la activación accidental de **R2 (Kawarimi)** por deslizamiento y zona muerta de gatillos ajustada al 50%; eliminada la opción sin uso de especial con un botón; gestos táctiles en menús opcionales con guía animada.

## v0.1.0-beta (14/11/2026)

La primera versión pública. *Naruto Shippuden: Ultimate Ninja 5* (PS2, Europa, SLES-55605) recompilado a código ARM64
nativo para Android, con launcher, controles táctiles, mods y juego en línea. Necesitas tu propia ISO del juego.

**Descarga:** `HiddenLeaf-v0.1.0-beta.apk` (Android 10+, ARM de 64 bits).

### Motor del juego

- El ELF del juego y todos los overlays de `PRG/` (combate, aventura, menús, textos) recompilados estáticamente con
  PS2Recomp: 8158 funciones en el ELF, 2832 en el overlay de combate, 3679 en el de aventura, 253 en ETC, 0 errores.
- Overlays encontrados en tiempo de ejecución por sus bytes en memoria, con las islas de código extendidas hasta sus
  propios destinos de salto.
- Dos motores en un solo APK: ARMv8.2-A y ARMv8.0-A (chips más antiguos, como el Snapdragon 625/665).
- Módulos reales del IOP (`libsd`, `sdr_driver`, `modmidi`, `modhsyn`, `cdvdstm`, la `libsdr` recompilada) con DMA y
  RPC del SIF; el IOP corre en bloques predecodificados exactos, en un hilo propio junto con el SPU2.
- El disco se lee directo de tu ISO, con cada archivo en su sector real y los tiempos de la unidad.
- Comportamiento de PS2 igualado al de PCSX2: redondeo de la FPU del EE y de la VU0, `SQRT.S`, el `rand` del propio
  juego, saltos de 64 bits, organización del heap de PS2, prioridades de hilos, bucles ociosos y `DI`.
- VIF1, VU1 y GIF en un hilo propio; UNPACK V2/V3 como en el hardware; envíos de PATH2 y PATH3 completos; el despertar
  de Hinata dibujado correctamente.
- Vídeos MPEG con el sonido en orden, terminados sin el EOF del flujo; 50 fps hasta el último fotograma.
- Hilos fijados según la velocidad de los núcleos; planificador, traducción de direcciones y búsqueda de hilos más
  ligeros.

### Gráficos

- El GS de ARMSX2/PCSX2 como renderizador por defecto, sobre Vulkan u OpenGL ES; renderizador nativo de combate como
  opción.
- Resolución interna de 0,5x a 8x.
- **Widescreen (16:9)** y **Ultrawide (20:9)** en combates, campaña y menús: la escena se amplía, el HUD y los retratos
  mantienen su forma; los vídeos quedan en 4:3. La cámara ancha se apaga en el panel de fin de ronda (sin barras negras).
- 30 y 50 fps, más 60, 90 y 120 fps por interpolación en las pantallas que lo admiten.
- **Modo ligero** para móviles de la clase Snapdragon 665: la VU1 de Nominum-Render y el GS dividido en dos hilos.
- Drivers Adreno personalizados (Turnip) descargados e instalados desde el launcher.
- Correcciones: bloqueo en el Ougi de Neji, líneas en el Ougi de Sakura, congelación del Ougi, pantalla negra tras un
  choque de Ougi, fantasma en la pantalla de victoria, reflejo roto del lago, texturas de humo que faltaban, ventana de
  imagen PAL y SMODE1.

### Sonido

- Sonido del SPU2 con la cola de audio cerca de 80 ms y sin picos saturados; paquetes de sonido de los vídeos en orden.
- Los menús quedan en silencio mientras se prepara la partida en línea, y el sonido vuelve en la selección de personaje.

### Controles

- Controles en pantalla por escena: combate, campaña (con las conversaciones) y menús.
- Especiales de un toque **↑↑○** y **↓↓○**; Jutsu y Ougi en un solo botón.
- **Toque Pro**: cruceta grande y fija con área táctil amplia; botones solo con icono; nombres de los botones; estilos
  tinta y cristal.
- Editor de macros, guía de botones y editor de diseño con tamaño, posición y opacidad de cada botón.
- Botones de objeto y de defensa por encima de la barra de objetos del juego; el botón de escena vuelve a la pantalla
  (combate, campaña, menú).
- Mandos físicos: detectados solos, con su propio mapeo; los botones en pantalla se ocultan mientras el mando está
  conectado y vuelven cuando se desconecta.

### Menú dentro del juego

- Un menú lateral sobre el juego en pausa, con toque o mando: pestañas Juego, Gráficos, Mando, Táctil y Ayuda.
- Se abre con el atajo que elijas (SELECT + START, L3 + R3 o solo Atrás); "Salir del juego" vuelve al launcher.

### En línea

- **Naruto Online** dentro del propio Mode Select del juego, debajo de Free Battle. El panel se abre a la izquierda del
  Naruto animado, con la música sonando, con toque o mando, con el aspecto del juego.
- **Partida rápida:** cualquier otro jugador con la misma versión de Hidden Leaf, sin código.
- **Crear sala** y **Entrar con código** (6 números).
- **Red local (Wi-Fi):** crear y buscar partida en el mismo Wi-Fi o punto de acceso, encontrada por broadcast, sin
  internet.
- Los dos móviles van directo a la selección de personaje (1P y 2P), acelerados y sin sonido por el arranque y los
  menús (12–30 s en un móvil). Quien crea controla los menús y el escenario; quien entra controla al 2P.
- Lockstep con retardo de entrada; conexión directa por STUN con relay de respaldo; combates completos sincronizados en
  red local y por internet.
- Servicio de salas en un Cloudflare Worker con base de datos D1. Cuando se agota la cuota diaria gratuita, la página
  de inicio avisa cuándo vuelve.
- Aviso de fin de partida con el estilo del juego cuando el otro jugador sale o se cae la conexión.
- Solo el juego base por ahora (sin mods); los dos móviles necesitan la misma versión.

### Mods

- Importar la ISO de un mod: la app guarda solo los archivos que difieren del juego base.
- **Ultimate Ninja 6 (Alpha 3.5)** con su paquete de código recompilado (unas 630 funciones cambiadas y 770 nuevas); su
  carpeta de tarjeta de memoria creada a partir de la partida guardada del juego base.
- **Ultimate Ninja 6 (Alpha 1.1X)** y la **Versão Brasileira** en el motor base.
- Los mods conocidos reciben una insignia y una sugerencia de idioma.

### Launcher

- Cinco páginas: Inicio, En línea, Controles, Ajustes y Más (Mods, Texturas, Drivers, Trucos, Depuración).
- ISO base comprobada por SHA-256 (Redump); solo se copian los archivos pequeños de arranque.
- Gestor de tarjeta de memoria: importar y exportar `.ps2` con copia de seguridad; importar carpeta.
- Trucos: desbloqueos escritos en la partida guardada del juego.
- Saltar la intro (con START), acceso directo en la pantalla de inicio, diseños en vertical y horizontal.
- **Actualización automática:** la app busca una versión nueva en GitHub al abrirse, muestra qué cambió, la descarga
  (sola en Wi-Fi, tras preguntar con datos móviles) y abre el instalador; los datos del juego, ajustes y partidas se
  mantienen.
- Icono nuevo: un ensō de tinta con una hoja escondida.
- Portugués, inglés y español.

### Problemas conocidos

- Algunos efectos todavía pueden diferir de PS2; el modo ligero puede perder o hacer parpadear algunos efectos.
- Los paquetes de texturas se instalan, pero el juego todavía no los usa.
- En línea: solo el juego base; la misma versión de Hidden Leaf en los dos móviles.
- Los mods que cambian el código necesitan un paquete de código (hoy: UN6 Alpha 3.5).
