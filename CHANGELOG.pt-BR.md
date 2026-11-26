# Changelog

[English](CHANGELOG.md) · **Português** · [Español](CHANGELOG.es.md)

## v0.2.2 (26/11/2026)

Atualização crítica focada na **correção definitiva de dessincronização (desync) no Online e Rede Local**, **fim automático de sala após 1 partida no Modo Ranqueado** e **otimizações extras para celulares de entrada no Modo Leve**.

**Download:** `HiddenLeaf-v0.2.2.apk` (Android 10+, ARM de 64 bits — instala diretamente por cima da `v0.2.1` e da `v0.2.0-beta` mantendo todos os dados e saves).

### Correção de Dessincronização (Online & Rede Local)

- **IOP síncrono e determinístico no multiplayer (`HL_IOP_THREAD=0`):** anteriormente, a thread assíncrona do IOP podia ficar vários quadros atrasada em celulares mais lentos, fazendo leituras de CDVD/SIF, falas de Especial (Ougi) e carregamentos pós-luta concluírem em VBlanks diferentes entre os dois jogadores — causando botões diferentes no Especial e telas divergentes ("aqui eu ganhei, lá ele ganhou"). Agora o IOP roda de forma síncrona por ciclo no Online e na Rede Local (LAN), mantendo os dois aparelhos no mesmo quadro exato.
- **Agendador da EE 100% determinístico em partidas online (`HL_ONLINE_DETERMINISTIC=1`):** corrigido o `EeScheduler` para que alarmes internos do jogo e o `VBlankEnd` nunca sejam reordenados ou segurados pelo relógio real do celular durante esperas de rede (lockstep). Somente o `VBlankStart` controla o ritmo real de quadros.
- **Relógio fixo no Memory Card (`HL_FIXED_CLOCK`) e boot unificado (`HL_SKIP_INTRO=0`):** timestamps de diretório do Memory Card (`sceMcGetDir`) agora usam relógio determinístico em vez do horário do sistema, e o fluxo de inicialização no multiplayer é idêntico para todos os jogadores.
- **Fingerprint atualizado (`a20299`):** garante que jogadores na `v0.2.2` pareiem apenas entre si, evitando desync contra versões antigas.

### Modo Ranqueado: 1 Partida por Sala

- **Desconexão automática após 1 luta no Modo Ranqueado:** ao final de uma luta ranqueada, o jogo envia e confirma o relatório bilateral de ELO/RP (`/v1/ranked/report`), exibe o resultado (`+RP` / `-RP`), encerra a sala automaticamente e mostra o resumo da partida para voltar ao menu. Para jogar novamente, basta buscar uma nova partida — evitando farm de revanche na mesma sala e desync acumulado entre lutas seguidas.

### Otimizações Extras para Celulares de Entrada (Modo Leve)

- **GS em 2 threads (`GSBackThreadMode=1`) automático em qualquer dispositivo 6+ núcleos no Modo Leve:** além dos SoCs já listados (incluindo expansão para `mt67*`, `mt68*` e `s5e*`), qualquer aparelho com 6 ou mais núcleos rodando no Modo Leve agora paraleliza o renderizador Vulkan automaticamente.
- **Perfil Vulkan mais leve no Modo Leve:** desativa `accurate_blending_unit`, `mipmapping`, `trilinear_filtering` e `dithering_ps2`, além de habilitar `texture_preloading = 1` para reduzir travadinhas (stutters) em celulares de entrada, mantendo GPUs intermediárias e fortes (Tier 2–4) intactas.

## v0.2.1 (26/11/2026)

Atualização focada no **Modo Ranqueado**, **Maestria de Personagens**, **Espectador ao Vivo**, **Perfis Ninja** e **Controles Touch** — corrigindo os principais relatos da comunidade na `v0.2.0-beta`.

**Download:** `HiddenLeaf-v0.2.1.apk` (Android 10+, ARM de 64 bits — instala diretamente por cima da `v0.2.0-beta` mantendo todos os dados e saves).

### Modo Ranqueado, ELO, Maestria & Anti-Grief

- **Pontuação Ranqueada (ELO / RP) funcionando de ponta a ponta:** a engine agora detecta automaticamente o vencedor por KO direto na RDRAM (`fighter+0x6C`) ao fim de cada luta (inclusive revanches na mesma sala), reporta bilateralmente para o servidor Cloudflare (`/v1/ranked/report`), atualiza RP, Vitórias, Derrotas, Sequência e exibe banner na tela com o resultado (`+RP` / `-RP`). Também contabiliza vitória automática caso o adversário dê ragequit/caia no meio da luta.
- **Bloqueio do botão START durante lutas Ranqueadas:** proteção determinística na engine (`hl_game::inFightDeterministic()`) que bloqueia o botão **START** de P1 e P2 durante combates ranqueados (toque, controle e macro), impedindo spam de pause com zero dessincronização. Fora do combate (seleção de personagens e tela pós-luta), o START funciona normalmente.
- **Maestria de Personagens corrigida:** agora soma XP em **toda luta jogada** (mesmo repetindo o mesmo personagem várias vezes seguidas) e credita XP extra de vitória/derrota no servidor.
- **Perfis clicáveis no Top 100 Global + Chave de Privacidade:**
  - Tocar em qualquer jogador no ranking abre o modal **Perfil Ninja** com foto/avatar, patente, RP, partidas jogadas, vitórias, derrotas, winrate, sequência e a lista completa de **personagens jogados com maestria individual**.
  - Nova opção **Perfil público / Perfil privado** abaixo do seu Cartão Ninja para quem prefere ocultar foto personalizada, estatísticas detalhadas e maestrias.
- **Avatar personalizado (Galeria) + Banco de Artes (20 Ninjas):** removida a captura quebrada de retratos da tela; agora você pode escolher **uma foto da sua galeria** (sincronizada para o seu perfil público) ou escolher entre **20 personagens do banco de artes** cacheados localmente.
- **Layout da Ranqueada e Casual corrigido:** botão **Atualizar ranking** em largura total sem cache (`no-store`) e chips de filtro com quebra automática (`FlowRow`), sem cortes em nenhuma tela (Odin 2 Mini, RG Cube 1:1, celulares em pé ou deitados).

### Espectador ao Vivo, Online, Controles & Diagnóstico

- **Modo Espectador ao Vivo real dentro do jogo:** nas Salas Personalizadas (2–8 jogadores), espectadores agora entram na partida em tempo real com replay automático dos quadros recebidos do host pelo relay WebSocket.
- **Aviso de dessincronização (`⚠ Desync`):** reativado o indicador no HUD online quando duas partidas divergem, evitando lutas silenciosamente dessincronizadas.
- **Botões touch `↑ ↑ ○` e `↓ ↓ ○` corrigidos:** ajustada a cadência de duplo toque (sem pausa morta antes do `○`), centralização do analógico durante macros direcionais, cancelamento de macros concorrentes e bloqueio de deslize acidental para `Jutsu (△ + ○)` / `Ougi`.
- **Changelog sempre exibido dentro do app em updates:** removido o download silencioso no Wi-Fi que pulava a tela de novidades; agora todos os jogadores veem o changelog completo no app antes e durante a atualização.
- **Exportação de logs corrigida (`un5_log.txt`):** corrigidos [LogExport.kt], [LastRun.kt] e [PerfReport.kt] que ainda buscavam o nome antigo `bt3_log.txt`, garantindo que relatórios de jogadores incluam o log real da engine e as configurações.

## v0.2.0-beta (24/11/2026)

Grande atualização de desempenho, gráficos e Modo Online: **Frame Generation real (ZeroFG 1.2)** para o modo offline, **Modo Online reorganizado** com telas dedicadas para **Modo Casual** (incl. Sala Personalizada & Espectador 2–8P) e **Modo Ranqueado** (Genin → Kage, Maestria por Personagem e Top 100 Global), **aba de Drivers** com detecção automática de GPU (Turnip, PanVK, MaliSX2 e RADV), **Save 100% com transformações R1**, **Pular Logos + Abertura** e correção definitiva das texturas de menu pós-luta.

**Download:** `HiddenLeaf-v0.2.0-beta.apk` (Android 10+, ARM de 64 bits).

### Gráficos & Frame Generation real (ZeroFG 1.2)

- **Frame Generation real (ZeroFG 1.2 · Somente Offline):** removida a antiga opção de FPS interpolado (60/90/120) e substituída pelo motor **ZeroFG 1.2** (Vulkan 1.3 dedicado em Device B, modo ZeroGS com separação de HUD e detecção de corte de cena, **sem precisar de DLL externa**).
  - Multiplicadores disponíveis: **Desligado**, **2x**, **3x** e **4x** (em Ajustes e no menu dentro do jogo), além do limite nativo (`30` ou `50 original`).
  - **Exclusivo do modo Offline:** desativado automaticamente em partidas Online (`hl_online::active()`) para manter justiça competitiva a 50 Hz nativos.
  - Seleção automática da maior taxa de atualização da tela (`preferRefresh`) quando o ZeroFG está ligado.
- **Números medidos em hardware real:**
  - **Retroid Pocket 5 (Snapdragon 865 / Adreno 650):** ZeroFG 1.2 ativo no Device B em `1920×1080` (`status=0`), 2x/CAS liso a 50 Hz base.
  - **Anbernic RG Cube (Unisoc T820 / Mali-G57):** ZeroFG 1.2 ativo (`704×528`, `status=0`), dobrando o jogo de 25 FPS para **50–59 FPS reais na tela** (~13% de GPU no GS com framebuffer fetch).
  - **Xiaomi Mi A3 (Snapdragon 665 / Adreno 610, Vulkan 1.1.128 stock):** fallback gracioso sem travamento (ativa automaticamente se usar driver Turnip Vulkan 1.3); Modo Leve mantém lutas e menus estáveis.
- **Correção das texturas de menu pós-luta:** corrigida a corrupção/inversão de texturas ao voltar de uma luta para a Seleção de Personagem ou Mode Select (`hl_gsx.cpp`, `hl_native.cpp`), verificada antes e depois de lutas no RP5, RG Cube e Mi A3.
- **Modo Leve refinado:** ativado automaticamente apenas em GPUs fracas (Tier 0–1: Adreno 3xx–620, Mali-G31/G51/G52/G57/G68/G71/G72, PowerVR, Xclipse 5xx) e mantido desligado por padrão em GPUs intermediárias/fortes (Tier 2–4) para preservar 100% dos efeitos e filtros do PS2. Lista de SoCs com GS em 2 threads expandida (`trinket`, `bengal`, `khaje`, `ums*`, `mt676*`, `mt678*`, `mt683*`, `exynos*`, `sc9863*`).

### Drivers Vulkan por GPU (Turnip, PanVK, MaliSX2 & RADV)

- **Detecção automática da família de GPU** na aba **Drivers** com filtro e fontes recomendadas:
  - **Adreno (Qualcomm):** Turnip (ARMSX2, Mr. Purple, StevenMXZ, K11MCH1, crueter 8 Elite, Banner's Turnip).
  - **Mali (ARM):** **PanVK** (`JimVulkan/mali-panvk`, `panvk-kbase-android`, `FristOneRR-Panvk-Driver`) e **MaliSX2** (`bmdhacks/malisx2`).
  - **Xclipse (Samsung / AMD RDNA):** **RADV** (`JimVulkan/radv-xclipse` e `paulowesll/mesa` do Paulo Wesley).
- **Correção definitiva da aba parada (HTTP 403):** adicionado fallback automático via GitHub Atom (`releases.atom` + `expanded_assets`) quando a API REST anônima atinge o limite de requisições.

### Novo Modo Online (Reorganizado, Ranqueada, Maestria & Espectador)

- **Hub Online reorganizado:** tela principal limpa com seletor `Online (internet)` / `Rede local (Wi-Fi)`, nome do jogador, botão **`Modo Casual`**, botão **`Modo Ranqueado`** e apenas as opções de servidor/conexão fora deles.
- **Filtro de Controle separado por modo:** removido o filtro global da tela principal e adicionado filtro independente (`Só Tela` / `Só Controle` / `Todos`) dentro do **Modo Casual** e dentro do **Modo Ranqueado**, com botão automático **"Demorando? Aceitar qualquer controle"** após 15s de busca.
- **Tela dedicada — Modo Casual (`CasualDedicatedScreen`):** reúne em 2 colunas (paisagem) Filtro de Controle, Partida Rápida, Treinar na Fila, Criar/Entrar Sala 1v1 e **Sala Personalizada & Espectador (2–8 jogadores)** com troca de papéis ao vivo (`P1` / `P2` / `Espectador`) e código de sala em largura total sem corte.
- **Tela dedicada — Modo Ranqueado (`RankedDedicatedScreen`):** fila competitiva separada (`#ranked`), **Cartão Ninja** com retratos reais dos personagens do jogo, patente (`Genin` → `Kage`), RP, V/D, sequência de vitórias, **detecção automática do personagem escolhido**, exportação automática do retrato do HUD (`PixelCopy`), **Maestria individual por personagem** e **Top 100 Global** com filtro por controle.
- **Otimização do servidor Cloudflare Free-Tier:** redução drástica de escritas D1 (`TOUCH_EVERY=12s`, remoção de `sweep()` no poll/ws), Lazy WebSocket Connect (0 requisições DO em P2P direto), rate limit por IP e proteção anti-spoof UDP.

### Qualidade de vida, Save 100%, Mods & Controles

- **Pular Logos + Abertura (`0111-skip-logos-and-safe-stop.patch`):** aguarda a escolha de idioma e pula automaticamente Bandai, CyberConnect2, CRIWARE e abertura direto para **New Game / Load Game** em ~1–2 segundos.
- **Correção do loop de salvar sozinho no UN6 A35 e mods:** limite rígido permanente no skip de abertura nas 4 bibliotecas nativas (`libun5.so`, `libun5_v80.so`, `libun5_un6_a35.so`, `libun5_un6_a35_v80.so`), eliminando botões fantasmas nos menus.
- **Save 100% & Trapaça "Desbloquear tudo" com Transformações R1:** corrigido o flag de progresso (`save + 0x1C5C = 0x6E`) tanto no save 100% embutido quanto no modo Online/Trapaça, liberando todas as transformações iniciais apertando **R1** na seleção de personagem.
- **Controles Touch:** botões de especial `↑↑○` e `↓↓○` agora permitem **segurar para carregar** (`holdLast = true`); removido o acionamento acidental de **R2 (Kawarimi)** por deslize e zona morta dos gatilhos ajustada para 50%; opção antiga de "Especial com um botão" removida; toque nos menus mantido como opcional com guia animado.

## v0.1.0-beta (14/11/2026)

A primeira versão pública. *Naruto Shippuden: Ultimate Ninja 5* (PS2, Europa, SLES-55605) recompilado para código ARM64
nativo no Android, com launcher, controles por toque, mods e jogo online. Você precisa da sua própria ISO do jogo.

**Download:** `HiddenLeaf-v0.1.0-beta.apk` (Android 10+, ARM de 64 bits).

### Motor do jogo

- O ELF do jogo e todos os overlays de `PRG/` (luta, aventura, menus, textos) recompilados estaticamente com o
  PS2Recomp: 8158 funções no ELF, 2832 no overlay de luta, 3679 no de aventura, 253 no ETC, 0 erros.
- Overlays achados em tempo de execução pelos bytes na memória, com as ilhas de código estendidas até os próprios
  destinos de desvio.
- Dois motores num APK só: ARMv8.2-A e ARMv8.0-A (chips mais antigos, como o Snapdragon 625/665).
- Módulos reais do IOP (`libsd`, `sdr_driver`, `modmidi`, `modhsyn`, `cdvdstm`, a `libsdr` recompilada) com DMA e RPC do
  SIF; o IOP roda em blocos pré-decodificados exatos, numa thread própria junto com o SPU2.
- O disco é lido direto da sua ISO, com cada arquivo no setor real e o tempo do leitor.
- Comportamento do PS2 igualado ao do PCSX2: arredondamento da FPU do EE e da VU0, `SQRT.S`, o `rand` do próprio jogo,
  desvios de 64 bits, organização do heap do PS2, prioridades de thread, laços ociosos e `DI`.
- VIF1, VU1 e GIF numa thread própria; UNPACK V2/V3 como no hardware; envios de PATH2 e PATH3 inteiros; o despertar da
  Hinata desenhado certo.
- Filmes MPEG com o som em ordem, terminados sem o EOF do fluxo; 50 fps até o último quadro.
- Threads presas pela velocidade dos núcleos; escalonador, tradução de endereços e busca de threads mais leves.

### Gráficos

- O GS do ARMSX2/PCSX2 como renderizador padrão, sobre Vulkan ou OpenGL ES; renderizador nativo da luta como opção.
- Resolução interna de 0,5x a 8x.
- **Widescreen (16:9)** e **Ultrawide (20:9)** nas lutas, na campanha e nos menus: a cena alarga, o HUD e os retratos
  mantêm a forma; os filmes ficam em 4:3. A câmera larga desliga no painel do fim do round (sem barras pretas).
- 30 e 50 fps, mais 60, 90 e 120 fps por interpolação nas telas que suportam.
- **Modo leve** para celulares da classe do Snapdragon 665: a VU1 do Nominum-Render e o GS dividido em duas threads.
- Drivers Adreno personalizados (Turnip) baixados e instalados pelo launcher.
- Correções: travamento no Ougi do Neji, listras no Ougi da Sakura, congelamento do Ougi, tela preta depois de uma
  disputa de Ougi, fantasma na tela de vitória, reflexo quebrado do lago, texturas de fumaça faltando, janela de imagem
  PAL e SMODE1.

### Som

- Som do SPU2 com a fila de áudio perto de 80 ms e sem picos estourados; pacotes de som dos filmes em ordem.
- Os menus ficam sem som enquanto a partida online é montada, e o som volta na seleção de personagem.

### Controles

- Controles na tela por cena: luta, campanha (com as conversas) e menus.
- Especiais com um toque **↑↑○** e **↓↓○**; Jutsu e Ougi num botão só.
- **Toque Pro**: direcional grande e fixo com área de toque larga; botões só com ícone; nomes dos botões; estilos
  tinta e vidro.
- Editor de macros, guia de botões e editor de layout com tamanho, posição e opacidade de cada botão.
- Botões de item e de defesa acima da barra de itens do jogo; o botão de cena voltou à tela (luta, campanha, menu).
- Controles físicos: detectados sozinhos, com mapa próprio; os botões na tela somem enquanto o controle está conectado
  e voltam quando ele desconecta.

### Menu dentro do jogo

- Um menu lateral sobre o jogo pausado, por toque ou controle: abas Jogo, Gráficos, Controle, Toque e Ajuda.
- Abre com o atalho que você escolher (SELECT + START, L3 + R3 ou só o Voltar); "Sair do jogo" volta ao launcher.

### Online

- **Naruto Online** dentro da própria Mode Select do jogo, abaixo do Free Battle. O painel abre à esquerda do Naruto
  animado, com a música tocando, por toque ou controle, no visual do jogo.
- **Partida rápida:** qualquer outro jogador com a mesma versão do Hidden Leaf, sem código.
- **Criar sala** e **Entrar com código** (6 números).
- **Rede local (Wi-Fi):** criar e procurar partida no mesmo Wi-Fi ou hotspot, achada por broadcast, sem internet.
- Os dois celulares vão direto para a seleção de personagem (1P e 2P), acelerados e sem som pelo boot e pelos menus
  (12–30 s num celular). Quem criou controla os menus e o cenário; quem entrou controla o 2P.
- Lockstep com atraso de entrada; conexão direta por STUN com relay de reserva; lutas inteiras em sincronia na rede
  local e na internet.
- Serviço de salas num Cloudflare Worker com banco D1. Quando a cota diária gratuita acaba, a página inicial avisa
  quando volta.
- Aviso de fim de partida no estilo do jogo quando o outro jogador sai ou a conexão cai.
- Só o jogo base por enquanto (sem mods); os dois celulares precisam da mesma versão.

### Mods

- Importar a ISO de um mod: o app guarda só os arquivos que diferem do jogo base.
- **Ultimate Ninja 6 (Alpha 3.5)** com o pacote de código recompilado (cerca de 630 funções mudadas e 770 novas); a
  pasta do cartão de memória dele feita a partir do save do jogo base.
- **Ultimate Ninja 6 (Alpha 1.1X)** e a **Versão Brasileira** no motor base.
- Mods conhecidos ganham um selo e uma dica de idioma.

### Launcher

- Cinco páginas: Início, Online, Controles, Ajustes e Mais (Mods, Texturas, Drivers, Cheats, Depuração).
- ISO base conferida por SHA-256 (Redump); só os arquivos pequenos de boot são copiados.
- Gerenciador de cartão de memória: importar e exportar `.ps2` com backup; importar pasta.
- Cheats: desbloqueios gravados no save do jogo.
- Pular a abertura (pelo START), atalho na tela inicial, layouts em retrato e paisagem.
- **Atualização automática:** o app procura uma versão nova no GitHub ao abrir, mostra o que mudou, baixa (sozinho no
  Wi-Fi, depois de perguntar nos dados móveis) e abre o instalador; os dados do jogo, ajustes e saves ficam.
- Ícone novo: um ensō de tinta com uma folha escondida.
- Português, inglês e espanhol.

### Problemas conhecidos

- Alguns efeitos ainda podem diferir do PS2; o modo leve pode perder ou piscar alguns efeitos.
- Pacotes de textura instalam, mas o jogo ainda não os usa.
- Online: só o jogo base; a mesma versão do Hidden Leaf nos dois celulares.
- Mods que mudam o código precisam de pacote de código (hoje: UN6 Alpha 3.5).
