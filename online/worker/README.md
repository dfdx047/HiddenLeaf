# Hidden Leaf Online: o servidor das salas

Um Cloudflare Worker (`worker.js`) com um banco D1 (`schema.sql`) e um Durable Object de relay. Ele dá a cada sala um
código de 6 dígitos, conta a cada jogador os endereços do outro e, quando os dois celulares não conseguem se falar
direto, repassa os pacotes da partida pela própria sala (o relay, `HiddenLeafRelay`). A partida em si vai de celular
para celular por UDP (`recomp/android/host/online/`); o servidor só entra no caminho quando precisa.

Feito a partir do servidor do Tag Team Online do Dragon Rage (`port/tagnet-worker` lá), com salas de dois jogadores,
o jogo `un5` e a versão de rede do Hidden Leaf.

## O que você vai precisar

- Uma conta gratuita da Cloudflare (não pede cartão para o plano gratuito).
- O Node.js 22 ou mais novo no PC (no CachyOS: `sudo pacman -S nodejs npm`). Confira com `node --version`.
- Este diretório (`online/worker/`) do repositório.

O `wrangler` é a ferramenta de linha de comando da Cloudflare. Não precisa instalar: os comandos abaixo usam
`npx wrangler`, que baixa e roda na hora.

## Passo a passo

### 1. Criar a conta

1. Abra <https://dash.cloudflare.com/sign-up>, cadastre e-mail e senha e confirme o e-mail.
2. No painel, abra **Workers & Pages** uma vez. A Cloudflare pede para escolher um subdomínio
   `<algo>.workers.dev`: escolha um nome (por exemplo `hiddenleaf`). O endereço do servidor vai terminar nele.

### 2. Entrar com o wrangler

No terminal, dentro de `online/worker/`:

```
npx wrangler login
```

Abre o navegador; clique em **Allow**. Para conferir: `npx wrangler whoami`.

### 3. Criar o banco D1

```
npx wrangler d1 create hiddenleaf-online
```

A saída mostra um `database_id` (um código longo). Copie o arquivo de configuração e cole esse código nele:

```
cp wrangler.example.jsonc wrangler.jsonc
```

Abra `wrangler.jsonc` e troque `<the id wrangler d1 create printed>` pelo `database_id`. O `wrangler.jsonc` fica fora
do git (`.gitignore`), porque é da sua conta.

### 4. Criar as tabelas

```
npx wrangler d1 execute hiddenleaf-online --remote --file schema.sql
```

### 5. Publicar

```
npx wrangler deploy
```

No fim aparece o endereço, algo como `https://hiddenleaf-online.<seu-subdominio>.workers.dev`. Para conferir se está
no ar:

```
curl -s -X POST https://hiddenleaf-online.<seu-subdominio>.workers.dev/v1/hello -d '{"v":1,"game":"un5"}'
```

A resposta tem `"ok":true` e o `colo` (o centro da Cloudflare que atendeu, por exemplo `GRU` em São Paulo).

### 6. Colocar o endereço no app

No launcher: **Online > Servidor**, cole o endereço do passo 5 (com `https://`, sem barra no fim) e toque em
**Testar conexão**. O teste mostra o tempo de resposta e a região. Os dois jogadores precisam usar o mesmo servidor.

Para deixar esse endereço como padrão de todos, troque `DEFAULT_SERVER` em
`android/app/src/main/java/com/dfdx047/hiddenleaf/data/Online.kt` e gere o app de novo.

### 7. Opcional: chave TURN

Hoje o app não precisa dela: quando a conexão direta falha, a partida passa pelo relay da própria sala (o Durable
Object, incluído no passo 5). O Worker já sabe entregar logins do TURN da Cloudflare (`/v1/relay`) para uma versão
futura com relay UDP, que tende a ter menos atraso. Para ligar:

1. No painel: **Realtime > TURN Server > Create**. Anote o **Turn Token ID** e o **API Token** (mostrado uma vez só).
2. No terminal:
   ```
   npx wrangler secret put TURN_KEY_ID
   npx wrangler secret put TURN_KEY_TOKEN
   ```
   (cada comando pede o valor).

Sem os dois segredos, o Worker funciona igual, só sem o TURN.

## Atualizar depois

Mudou o `worker.js`: `npx wrangler deploy` de novo. Mudou o `schema.sql`: rode o passo 4 de novo antes (as tabelas
usam `IF NOT EXISTS`, nada se perde).

Quando a versão de rede sobe (`HL_NET_VERSION` em `recomp/android/host/online/net_version.h` e `NET_VERSION` em
`data/Online.kt`), ponha a nova em `ALLOWED_VERSIONS` no `wrangler.jsonc` (por exemplo `"1,2"`) e publique. Jogos de
versões diferentes nunca caem na mesma sala.

## Limites do plano gratuito

Segundo o que o Dragon Rage anotou ao usar o mesmo tipo de servidor (confira na Cloudflare, os limites mudam):

- O plano gratuito de Workers aceita **100.000 pedidos por dia**. Quem cria uma sala pergunta a cada 2 s enquanto
  espera: 10 minutos de espera são 300 pedidos. Quem entra faz poucos pedidos.
- O TURN da Cloudflare é gratuito até **1.000 GB por mês** (só conta se você ligar o passo 7).

Quando o limite do dia acaba, a Cloudflare responde 429 e o app mostra "Servidor ocupado".

## Testes

```
node test.mjs
```

Roda o Worker contra um SQLite em memória (Node 22, `node:sqlite`), com o Durable Object e o TURN simulados. Nada é
enviado para a internet.

## Os pedidos

| Pedido | Corpo | Resposta |
|---|---|---|
| `POST /v1/hello` | `{v, game}` | `{ok, v, game, colo, country, time}` |
| `POST /v1/host` | `{v, game, fp, name, addrs, opts}` | `{code, key, you}` |
| `POST /v1/join` | `{v, game, fp, code, name, addrs}` | `{key, you, host, opts}`; erros `no room`, `version`, `game`, `different`, `full` |
| `POST /v1/poll` | `{code, key}` | quem criou: `{guest, opts}`; quem entrou: `{host, opts}` |
| `POST /v1/addrs` | `{code, key, addrs}` | `{}` |
| `POST /v1/relay` | `{code, key}` | `{turn}` ou `{noRelay}` (só quem criou) |
| `POST /v1/leave` | `{code, key}` | `{}` |
| `GET /v1/ws?code=&key=` | WebSocket | o relay da sala: o que um manda, o outro recebe |

- `v`: a versão de rede; `game`: sempre `un5`.
- `fp`: o que tem de ser igual nos dois celulares para os dois jogos continuarem iguais (a versão do app e os mods
  ligados). A sala só aceita quem tem o mesmo `fp` de quem criou (erro `different`).
- `opts`: as escolhas de quem criou, `{delay: 0 (automático) a 4 quadros, relay: true/false}`.
- Uma sala sem pedidos por 3 minutos some.
