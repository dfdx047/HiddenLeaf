# Changelog

[English](CHANGELOG.md) · [Português](CHANGELOG.pt-BR.md) · **Español**

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
- Icono nuevo: un ensō de tinta con una hoja escondida.
- Portugués, inglés y español.

### Problemas conocidos

- Algunos efectos todavía pueden diferir de PS2; el modo ligero puede perder o hacer parpadear algunos efectos.
- Los paquetes de texturas se instalan, pero el juego todavía no los usa.
- En línea: solo el juego base; la misma versión de Hidden Leaf en los dos móviles.
- Los mods que cambian el código necesitan un paquete de código (hoy: UN6 Alpha 3.5).
