# "Naruto Online" no Mode Select do próprio jogo

Escrito em 2026-11-14, numa branch de agente (`worktree-agent-afb99d4aa54d02cd9`, a partir de `lab/bench-pcsx2` `371f44b`).
Estudo de viabilidade e protótipo no PC; nada testado em aparelho. Endereços do ELF europeu (SLES-55605). Nenhum dado nem
arte do jogo entra no repositório: só endereços, tamanhos e coordenadas de recorte; as imagens são montadas em tempo de
execução a partir do disco do jogador.

## Resposta curta

- **Dá, e barato.** O Mode Select tem uma sétima entrada escondida: o modo **3**, "Mini-Games" do original, cortado da
  versão europeia só por uma palavra (`-1`) numa tabela. Todas as peças dele continuam no arquivo do menu: a placa (sem
  nome), o diorama da direita (bandeirolas e o Naruto) e a linha de texto de baixo. Basta pôr o modo 3 na lista, logo
  abaixo do Free Battle, trocar os textos e escrever o nome na placa.
- **O protótipo funciona no PC** (`HL_MENU_ONLINE=1`): placa "Online" (com as letras do próprio jogo) logo abaixo do Free
  Battle, linha de baixo "Naruto Online: fight other players over the internet.", e ✕ abre a caixa de mensagem do próprio
  jogo com "Naruto Online / Quick Match, Create Room or Join Room.", com o som de "escolhido"; ✕ ou △ fecham a caixa e o
  menu segue normal. O log mostra `[online] menu chosen`. Desligado (o padrão), o menu fica igual ao de sempre.
- **Recomendação:** este caminho (a entrada escondida + ganchos em 5 funções do ELF), e não desenhar uma placa por fora.
  A passagem para o launcher é um contador (`hl_game::g_onlineMenu`, JNI `TouchBridge.nativeOnlineMenu()`), lido como o
  `nativeScene`; o launcher abre o lobby dele por cima do jogo (pausado). O lobby inteiro com gráficos do jogo não compensa
  agora (ver "O lobby").

## Como o Mode Select é feito (provado no PC)

O menu é do ELF (não de um overlay; o `BTL.BIN` está carregado, mas nenhuma função dele roda só no menu). O objeto do menu
fica no heap (`0xBD90F0` na rota de teste, `HL_FNCHG` abaixo):

| Campo | O quê |
|---|---|
| `+0x00` | estado: 0 entrando, 1 saindo para um modo, 2 parado esperando o jogador, 3 "Return to Title Screen?", 4 salvando, 5 caixa de mensagem, 6 escolhido |
| `+0x04` | quantas entradas há na lista |
| `+0x08` | os ids dos modos, até **7** (`+0x08..+0x23`) |
| `+0x24` | o cursor (índice na lista) |
| `+0x28` | a posição da rolagem (float) |
| `+0x30..+0x40` | o pad do quadro: `+0x3C` 1P apertado, `+0x40` 2P, `+0x30` os dois, `+0x38` com repetição |
| `+0x64..+0xD0` | arquivo e objetos do menu (`modesel1.ccs`), caixas de mensagem (`+0xC8`), id da mensagem (`+0xC4`) |

Os ids: **0 Master Mode, 1 Free Battle, 2 Practice, 3 Mini-Games (cortado), 4 Shop, 5 Collection, 6 Options.** A tabela
`0x5DC300` (um `int` por id) dá a cena seguinte: `4, 2, 3, -1, 5, 6, 7`; `-1` = o modo não existe.

| Função | O quê |
|---|---|
| `0x3953B0` | monta o menu (chamada de `0x1F0808`): `0x395580` carrega as peças, `0x395CE0` a lista, `0x395C60` põe o cursor no último modo escolhido |
| `0x395580` | carrega `modesel1.ccs` e os objetos: `ANM_modename0%da` (os nomes das placas, 1..7, **o 04 existe**), `ANM_mode_back`, `ANM_modemachine`, setas, tambor |
| `0x395CE0` | a lista: para cada id 0..6 com `0x5DC300[id] >= 0`, `ids[n++] = id` |
| `0x396B90` | o quadro do menu: copia o pad e despacha pelo estado (tabela `0x5DC340`) |
| `0x396020` | estado 2: `0x396340` lê o pad (1 ↑, 2 ↓, 3 ✕ do 1P, 4 ✕ do 2P, 5 △, 6 START); ↑/↓ andam o cursor (dá a volta), ✕ chama `0x395DC0` |
| `0x395DC0` | escolher: `0x5DC300[id] < 0` → `0x395F20(menu, 1)`; 2P fora do Free Battle e do Practice → `0x395F20(menu, 0)`; Master Mode com progresso → `0x396000`; senão grava o id em `gp-0x6518` (`0x6119D8`), estado 1, fade e som `0x58` |
| `0x395F20` | "modo indisponível": estado 5, mensagem 4 (com `a1 = 1`) ou 1, abre a caixa (`0x38F4F0`), som `0x3F` |
| `0x3972E0` | desenho do menu (chamada de `0x1F04C8`); a caixa de mensagem escreve `0x3D18A0(+0xC4)` com `0x393D30` |
| `0x3D1850` | o texto da linha de baixo de um modo: `tabela[idioma][id]` (`0x5BA810`, uma linha por idioma, apontando para o overlay `TEXT*.BIN`) |
| `0x3D18A0` | o texto de uma mensagem do menu (`0x5BA830`): 0 "Return to Title Screen?", 1 controle na porta 2, 2 "Select where to start Master Mode", **3 e 4 vazias** (apontam para uma string vazia do ELF) |
| `0x1DD470` | toca um efeito sonoro (`0x58` escolher, `0x33` cancelar, `0x3F` recusado, `0x57` mover) |

Estado 5 (caixa aberta, em `0x396D70`): ✕ fecha, △ fecha com o som de cancelar, e o menu volta ao estado 2.

### Os nomes das placas

São uma textura do `modesel1.ccs`, `TEX_modesel02`: 256x512, 8 bits com paleta, guardada de cabeça para baixo (a linha 0
é a de baixo na tela), uma faixa de 48 linhas por nome, de baixo para cima: Master Mode (464), Free Battle (416), Practice
(368), **a faixa 320, vazia** (o nome dos Mini-Games foi apagado na versão europeia), Shop (272), Collection (224), Options
(176), e acima "Mode Select" e "Press START button to save". O `MDL_modename04` aponta para a faixa vazia: escrever nela
aparece na placa, desenhada, iluminada e animada pelo próprio jogo. No PC a escrita feita ao montar o menu já aparece no
primeiro quadro (o momento exato do envio da textura ao GS não foi medido).

Achar a textura: na tabela de nomes do arquivo carregado, a entrada `TEX_modesel02` (+0x2C o objeto, que aponta de volta
para ela; +0x28 o bloco de envio; +4 os pixels; +8 o tamanho em quadwords, `0x2000`).

## O protótipo (`recomp/android/host/hl_game.cpp`, "Naruto Online in the Mode Select")

Ligado por `HL_MENU_ONLINE=1` (PC, `hl_env.txt`) ou `hl_game::g_menuOnline` (JNI `TouchBridge.nativeSetMenuOnline`,
recusado durante uma partida online, que liga os dois jogos em lockstep desde o boot). Ganchos por `registerFunction`, só
se seis palavras do código forem as do ELF europeu (o A35 tem outro código: lá não entra nada; a Alpha 1.1X roda o ELF
europeu e deve ganhar a entrada, não testado):

1. `0x395CE0`: depois da original, põe o id 3 logo depois do id 1 (a lista tem lugar para os 7) e escreve "Online" na
   faixa vazia da textura, só se ela estiver toda zerada.
2. `0x3D1850` (id 3) e `0x3D18A0` (mensagem 4): devolvem textos nossos, em 5 idiomas (pelo id do `TEXT*.BIN` carregado em
   `0x8F3D04`), só ASCII com as marcas do jogo (`<colorRRGGBB>`, `<WHITE>`, `<BLACK>`, `<br>`). Ficam em `0xFE000`, RAM
   abaixo do ELF que ninguém usa (a mesma ideia do rascunho do MPEG em `0x7C000`; os pools do runtime vão de `0xA0000` a
   `0xD1000`), conferida zerada na primeira vez.
3. `0x395F20`: com `a1 = 1` e o id 3 sob o cursor, é "Naruto Online escolhido": `[online] menu chosen`,
   `hl_game::g_onlineMenu++`, e o som `0x3F` (recusado) vira `0x58` (escolhido) no gancho de `0x1DD470`. O resto é o
   caminho do próprio jogo: estado 5, a caixa de mensagem, ✕/△ fecham.

O nome "Online" é montado das letras das outras placas (O de Options; n, l, i, e de Collection e Free Battle; o i é o
traço do l abaixo da altura das minúsculas mais o pingo do i de Collection), cada letra uma faixa inclinada (0,15 coluna
por linha, como a letra), com as coordenadas em `kOnlineLabel`. "Naruto" não dá para montar assim: não há N nem u em
nenhuma placa. A palavra "Online" serve aos 5 idiomas.

Capturas (PC, GS do ARMSX2, 4:3; a 07 com a tela em 20:9): `build/lab/menu/shots/` (`01_free_battle_with_online_below`,
`02_online_selected`, `03_cross_opens_message`, `04_cross_closes_back_in_menu`, `05_triangle_closes`, `06_cycled_back`,
`07_online_selected_20x9`). Rota: `build/lab/menu/route_online.txt`, rodada com `build/lab/menu/run.sh` (o `un5pc` desta
branch é refeito por `build/lab/menu/relink.py` em 5 s, reaproveitando os objetos do `build/pc`; o `run.sh` roda num disco
separado, `build/lab/menu/disc`, cópia por hard link do `build/disc` com `mc0/` e `mc1/` vazios: `cp -al` de novo, apagado
ao fim para não ficar lixo).

## As alternativas pesadas

| Caminho | Custo | Risco | Veredito |
|---|---|---|---|
| (a) Reaproveitar a placa de outro modo e trocar o nome | baixo | o nome seria o de outro modo (os nomes são imagem, não fonte) | descartado: a placa vazia do modo 3 é melhor |
| (b) Aumentar a lista e ganchar desenho e escolha | baixo, **com o modo 3** | a lista só tem lugar para 7 ids (`+0x24` é o cursor logo depois); um oitavo modo pediria mexer no objeto | **escolhido**, porque o modo 3 cabe nos 7 |
| (c) Desenhar a placa por fora (nativo ou camada do host) e interceptar o ✕ | alto | seguir a rolagem, a luz e a animação do menu 3D por fora, em 4:3, 16:9 e 20:9; duas vias de desenho (GS e nativo) | só se o (b) não existisse |

## O lobby

- **Agora (recomendado):** o launcher lê `TouchBridge.nativeOnlineMenu()` no mesmo laço que lê `nativeScene()`; quando
  sobe, pausa o jogo (`nativeSetPaused(true)`) e abre a tela Online dele (`data/Online.kt`, partida rápida, criar e entrar
  em sala, rede local) por cima, com o visual do jogo (fundo e cores do Mode Select desenhados pelo launcher, sem a arte do
  jogo). Achada a partida, o caminho é o que já existe: `online/session.txt` e o jogo reinicia em lockstep desde o boot
  (`hl_online.cpp`). Cancelar volta ao jogo (`nativeSetPaused(false)`), no Mode Select.
- **Telas do próprio jogo que dá para reaproveitar:** a caixa de mensagem do menu (já usada no protótipo; texto livre com
  `<br>`), a pergunta sim/não do estado 3 ("Return to Title Screen?", com cursor) para confirmações, e a seleção de
  personagem/arena normais do Free Battle durante a partida (o outro jogador na porta 2, como o `hl_online` já faz). Uma
  lista de salas ou digitar um código com as letras do jogo pediria uma tela nova inteira desenhada por nós: vários dias,
  e fica para depois.

## Riscos e o que não foi provado

- **Não testado em aparelho** (Mi A3, RG Cube), nem em 16:9 pelo caminho "Desenhar em 16:9" do Android; a placa é do
  próprio jogo, então deve seguir o mesmo caminho das outras.
- **Mods:** A35 fica sem a entrada (código diferente, conferido pelas palavras); Alpha 1.1X não testada (se a faixa 4 da
  textura dela não estiver vazia, o nome não é escrito).
- **Idiomas:** só o inglês foi visto; os outros 4 textos são traduções nossas, sem acentos (não conferi se a fonte do jogo
  tem acentos no mesmo código).
- O `0xFE000` livre foi conferido só no menu e no boot (a conferência de zeros na primeira vez protege contra outro uso
  anterior, não contra um uso posterior). Alternativa, se der problema: escrever no lugar do texto dos Mini-Games no
  `TEXT*.BIN` (64 bytes no inglês).
- O som trocado só vale no mesmo vsync do `0x395F20`; se a escolha atravessar uma troca de thread, sai o som de recusado.
- **Partida online:** os dois jogos passam pelo mesmo Mode Select em lockstep; a entrada fica desligada nessa hora
  (`nativeSetMenuOnline` recusa com `hl_online::active()`).

## Ferramentas novas (só para depuração, no `hl_game.cpp`)

- `HL_FNCOUNT=all:btl` ou `all:elf`: conta as funções do `BTL.BIN` e do ELF (antes só do `ADV.BIN`), uma por função (as entradas no
  meio de uma função dividem o mesmo contador).
- `HL_FNCHG=ADDR`: escreve `[fnchg] v<vsync> fn <função> (ra <de onde>)` a cada mudança da palavra, pela função mais
  interna que a mudou. Foi assim que o cursor (`0x1A9BF0` soma, `0x396020` dá a volta) e a lista (`0x395CE0`) apareceram.
- `HL_POKE=ADDR=VALOR,...`: palavras escritas a cada vsync (foi o `HL_POKE=5dc30c=3` que mostrou os Mini-Games).

## O que falta (estimativa)

1. Launcher: ler o contador, pausar, abrir o lobby por cima, voltar ou iniciar a partida; opção para ligar a entrada.
   Meio dia a um dia.
2. Do lobby para a partida: decidir se a partida online começa do boot (como hoje) ou se o Mode Select leva direto à
   seleção de personagem do Free Battle (`gp-0x6518 = 1` e estado 1, a partir de um pedido do launcher). Um a dois dias.
3. Placa: ajustar os recortes (o i e as emendas), talvez um "Naruto" em cima vindo de outra textura do jogo (o título).
   Meio dia.
4. Rotas nos aparelhos (4:3, 16:9, 20:9; base, Alpha 1.1X, A35). Meio dia.
