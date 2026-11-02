# Online: estudo de viabilidade (sem implementação)

Escrito em 2026-11-02. Nada aqui foi implementado. As medidas são da luta fixa (Naruto × Naruto, rota de botões fixa,
`build/lab/d2/det.sh`), no Retroid Pocket 5 (RP5, SD865) e no RG Cube (Mali), os dois com Android 13.

## Resposta curta

- **Rollback entre aparelhos diferentes ainda não dá, mas está perto.** No mesmo aparelho o jogo repete exatamente. Entre aparelhos a RAM difere em
  cerca de 400 bytes de 32 MB, em posições que parecem de partículas, e isso não cresceu em 112 s. A causa não foi achada. Ver "Determinismo".
- **Salvar a RAM é barato; restaurar tudo não foi testado.** Copiar custa pouco, e a troca de thread já não depende da pilha
  nativa, o que ajuda. Falta o estado do escalonador, dos stubs, das VUs, do GS, do IOP e do SPU2. Ver "Custo do estado".
- **O melhor primeiro passo é lockstep com atraso de entrada**, não rollback, com a detecção de dessincronia ligada.
- **O servidor pode ser o mesmo do Dragon Rage** (Cloudflare Worker para salas, STUN público e conexão direta). O relay é
  a única peça que pede uma máquina de verdade.

## Determinismo

"Mesma entrada → mesma RAM depois de N quadros", com hashes de toda a RAM do EE (32 MB) nos vsyncs 6100, 6102, 6400 e 6700 e um
despejo no 6400.

| Comparação | v6400 | v9000 | v12000 | Leitura |
|---|---|---|---|---|
| Cube, 2 execuções | 0 bytes | não medido | não medido | Idêntico (mesmo minuto do relógio) |
| RP5, 2 execuções | 8 bytes, 1 página | não medido | não medido | Só o relógio (`0x632241`) |
| RP5 normal × RP5 limitado a `sd480p` | 8 bytes, 1 página | 28 bytes, 3 páginas | 36 bytes, 3 páginas | Relógio e duas listas em `0x406000` e `0x40d000` |
| Cube × RP5 | 439 bytes, 14 páginas | 390 bytes, 9 páginas | 393 bytes, 9 páginas | Diferença que **não cresce** em 112 s |

Para dar a medida: entre os vsyncs 9000 e 12000 do mesmo aparelho mudam 2637 páginas (5,6 MB), então o jogo segue ativo e esses
400 bytes são muito pouco.

O que isso diz:

1. **O mesmo aparelho é determinístico**, até com a CPU limitada: tempo de CPU não muda a lógica.
2. **A velocidade não é a causa da diferença entre aparelhos,** porque o RP5 limitado continua igual ao RP5 normal nas páginas de efeitos.
3. **As diferenças entre aparelhos ficam em poucas páginas** e todo o resto da RAM é igual byte a byte. Nas amostras que li são floats de posição
   (x perto de -1080, z perto de 150) e vetores, que parecem de partículas. "Parecem" é leitura minha dos valores: **não provei** que nada de
   lógica lê essas posições, e não localizei os endereços do cronômetro e da vida para conferir direto. A entrada só existe no começo da luta,
   e não conferi se a luta já tinha acabado no v12000 (a tela não foi registrada nesse ponto).
4. As listas em `0x406000` e `0x40d000` mudam de ordem com o tempo (blocos de 0x200 e 0x400 bytes trocados), então parecem de alocação ou de
   streaming. Precisam ficar de fora do hash, ou ter a ordem fixada.

**A causa da diferença entre aparelhos não foi encontrada.** Com a libm e a velocidade descartadas, sobra algo estável em cada aparelho.

### O que já foi descartado

| Hipótese | Teste | Resultado |
|---|---|---|
| libm diferente (`sinf`, `cosf`, `tanf`, `atan2f`, `powf`, `expf`, `logf`), `rand`, multiplicar e somar | `build/lab/d2/mathck/m.c`, 4 milhões de valores nos dois aparelhos | **Iguais** nos dois: não é a libm |
| Configurações de imagem (filtro, modo de fps) mudam a lógica | Cube com filtro 3 e modo 30 contra o padrão | **Não**: só a página do relógio e a da estrutura de vídeo (`0x938000`) mudam, nenhuma de lógica |

### O que precisaria ser feito antes do online

1. **Relógio fixo:** o jogo guarda a data e a hora em `0x632241`. Em partida online o RTC tem de ser o mesmo nos dois lados.
2. **Estrutura de vídeo:** tirar `0x938000` da comparação, ou igualar os campos que dependem das opções de imagem.
3. **Achar a origem da diferença nos efeitos** (ver "Próximo teste").
4. **Uma mesma versão:** o serviço só junta quem tem a mesma versão de simulação, como o `DRNET_SIM` do Dragon Rage.

## Custo do estado

| Medida | RP5 | Cube |
|---|---|---|
| Copiar os 32 MB da RAM do EE | 2,6 a 2,9 ms | 5,4 a 6,0 ms |
| Hash dos 32 MB (64 bits, por software) | 4,5 ms | 8,6 ms |
| Tempo do EE por quadro de luta (medida `[perf]`) | 7,0 ms | 8,8 ms |
| Tempo da VU1 por quadro de luta | 10 ms | 13 ms |

Páginas de 4 KB que mudam entre quadros (Cube, luta, de 8192):

| Intervalo | Páginas | Volume |
|---|---|---|
| 2 vsyncs (1 quadro de lógica) | 216 | 0,9 MB |
| 4 vsyncs (2 quadros de lógica) | 233 | 1,0 MB |
| 50 vsyncs (25 quadros de lógica) | 405 | 1,7 MB |

Com rastreio de páginas sujas (`mprotect` ou comparação), salvar um quadro custaria uma fração da cópia inteira: cerca de
1/38 dos 32 MB, ou seja, algo perto de 0,1 a 0,2 ms em memória. Isso é uma estimativa a partir das páginas, não uma medida.

### O que esses números não contam

Não foi medido nem implementado:

- RAM do IOP (2 MB), memória e registradores das VU0 e VU1, VIF, DMA, memória do GS (4 MB) e registradores, RAM do SPU2 (2 MB).
- **Estado do escalonador e dos stubs.** Este é o maior risco, mas parece tratável. A troca de thread do EE já volta ao despachante
  (`EeScheduler::jumpToDispatcher`, por `longjmp`) e retoma a thread por `ctx->pc`, então ela não depende da pilha nativa de quem foi
  interrompido. Isso favorece o restore: bastaria voltar ao despachante com os contextos salvos. Falta guardar os contextos de todas as
  threads, semáforos, event flags, alarmes e o estado dos stubs (`resumeCompletion` é um closure C++, não dá para copiar como bytes), além do
  IOP, que roda de verdade em parte. Isso vem da leitura do código: nenhum restore foi testado.
- O tempo de re-simular. No Cube um quadro de lógica custa cerca de 9 ms de EE e 13 ms de VU1 em threads paralelas, e o orçamento é de 40 ms.
  Voltar 2 quadros já ocupa boa parte da folga, e o Cube na campanha está no limite (17% dos pares acima de 44 ms).

## Qual netcode

| Opção | Precisa de | A favor | Contra |
|---|---|---|---|
| **Lockstep com atraso** | Determinismo e entrada dos dois lados | Não precisa salvar nem restaurar estado | O atraso sente-se. A lógica roda a 25 Hz, então cada quadro de atraso vale 40 ms |
| **Rollback** | Determinismo, salvar e restaurar todo o estado e re-simular com folga | A melhor sensação, como no Dragon Rage | Restore nunca testado. O Cube tem pouca folga |
| **Streaming do host** | Vídeo e rede bons | Dispensa determinismo | Latência alta para luta. Não recomendado |

**Recomendação:** lockstep com 2 quadros de atraso (80 ms) e hash de uma região de lógica a cada segundo para detectar
dessincronia. Rollback só depois, se o teste de restore passar. Como o jogo pede uma entrada só a cada 40 ms, o atraso em
quadros é maior em tempo do que num jogo de 60 Hz, e isto precisa ser avaliado jogando.

## O que precisa do servidor

O Dragon Rage já mostra o caminho (`docs/DRAGON_NET.md` e `port/src/gs/net_match.c`, no repositório dele):

1. **Salas e partida rápida** (HTTP): `host`, `join` com código, `quick`, sempre com a versão de simulação no pedido. Lá é um
   Cloudflare Worker com D1.
2. **Descoberta de endereço:** STUN público (`stun.cloudflare.com:3478`, `stun.l.google.com:19302`), sem servidor próprio.
3. **Conexão direta por UDP** (hole punching). O servidor só troca os endereços.
4. **Relay UDP** para quem não conecta direto. É a única parte com custo de banda.
5. **Opcional:** classificatória e chat, como no Dragon Net. Fora do primeiro passo.

### Banda de um relay

Só entrada por quadro: com 25 quadros por segundo, cerca de 8 bytes de entrada mais 28 de cabeçalho UDP/IP e alguma
redundância, dá uns 60 bytes por pacote em cada sentido, ou **3 KB/s por partida** (11 MB/hora). É uma estimativa, não uma
medida. Em 10 TB por mês caberiam centenas de milhares de horas de partida.

## Hospedagem gratuita

Consultado em 2026-11-02. Estes limites mudam, então confira antes de decidir.

| Opção | Para quê | Limite encontrado | Fonte e confiança |
|---|---|---|---|
| **Cloudflare Workers + D1** | Salas e partida rápida | Já usado pelo Dragon Rage. Não achei confirmação de UDP de saída, então não serve de relay | Uso próprio no Dragon Rage; limites atuais não conferidos |
| **Oracle Always Free (Ampere A1)** | Relay UDP | Teria caído de 4 OCPU/24 GB para 2 OCPU/12 GB por volta de junho de 2026; a doc lista 1.500 OCPU-horas e 9.000 GB-horas por mês. Os 10 TB de saída vêm só de um guia de terceiros. UDP não confirmado na doc | [InfoQ](https://infoq.com/news/2026/07/oracle-cloud-free-tier-limits/), [Linuxiac](https://linuxiac.com/oracle-quietly-cuts-free-tier-ampere-a1-resources-in-half/), [doc da Oracle](https://docs.oracle.com/iaas/Content/FreeTier/resourceref.htm), [guia](https://space-node.net/blog/oracle-cloud-always-free-limits-2026) |
| **Google Cloud e2-micro** | Relay pequeno | Always Free em us-west1, us-central1 e us-east1, 30 GB de disco, 1 GB de saída e depois US$ 0,12/GB. Longe do Brasil e da Europa | [agentdeals](https://agentdeals.dev/gcp-free-tier-2026), [agentdeals](https://agentdeals.dev/vendor/google-compute-engine) (terceiros) |
| **Fly.io** | Relay UDP | Situação do plano gratuito conflitante entre as fontes. UDP pede IPv4 dedicado (cerca de US$ 2/mês, guia de 2024) | [costbench](https://costbench.com/software/cloud-infrastructure/fly-io/free-plan/), [zenn](https://zenn.dev/submax/articles/fly-udp-game-server-deployment-2024-01-08) (terceiros) |

**Recomendação:** Worker para salas, conexão direta como padrão, e a Oracle como relay, se o UDP se confirmar ao criar a
máquina. O risco da Oracle é o histórico de cortes e de contas gratuitas recuperadas, então o relay não pode ser peça obrigatória.

## Menu online com os gráficos do próprio jogo

Como no Dragon Rage, o online entra no menu do jogo, não só no launcher. Só desenho, nada implementado.

```
Mode Select (placas de madeira do jogo)
  Options / Master Mode / Free Battle / [Online]   ← nova placa, mesma textura, texto "Online"
        │
        ▼
Online (mesmo fundo de engrenagens e nuvens do Mode Select; faixa de texto embaixo explica cada item)
  Quick Match      partida rápida
  Create Room      mostra um código de 4 letras
  Join Room        código com as letras na grade de seleção
  Settings         conexão: automática / só direta / relay
        │
        ▼
Regras (o anfitrião escolhe; painéis da tela de opções de luta)
  Modo:            1 luta / melhor de 3 / melhor de 5
  Tempo de luta:   60 / 99 / sem limite
  Personagens:     todos / os que os dois têm
  Atraso:          automático / 1 / 2 / 3 quadros
        │
        ▼
Select Character → Stage Select (telas normais do jogo; o outro jogador no lugar de "COM")
        │
        ▼
Luta; canto com ping e nome no HUD
        │
        ▼
Fim: Revanche / Trocar personagem / Sair (caixa de diálogo do jogo, a mesma de "Save data?")
```

Pontos de atenção:

- **Textos:** o jogo desenha com as próprias fontes, então as palavras novas precisam de glifos existentes. Palavras com letras já
  usadas nos menus saem sem desenhar nada novo. As placas grandes do Mode Select são imagens, então "Online" precisa de
  arte nova no mesmo estilo.
- **Como entrar no menu:** por hook no menu recompilado (`registerFunction`), como já fazemos em outros pontos. O nome e o ponto exato das
  funções do Mode Select não foram levantados.
- **Desbloqueio:** "Desbloquear tudo" muda o save. Para os dois lados terem os mesmos personagens, a sala precisa comparar a
  matriz de desbloqueio, ou liberar tudo só durante a partida.

## Próximo teste, quando o online voltar à pauta

1. Achar a origem da diferença entre aparelhos (ver "Determinismo") e achar quem escreve essas posições de partículas.
2. Parar num vsync, salvar RAM, contextos e escalonador, restaurar e ver se o jogo segue igual: é o teste que decide se rollback é possível.
3. Lockstep local: dois aparelhos na mesma rede, só com o hash, sem rede de verdade no caminho do jogo.
