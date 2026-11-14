# Changelog

[English](CHANGELOG.md) · **Português** · [Español](CHANGELOG.es.md)

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
