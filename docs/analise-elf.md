# Análise do ELF, dos overlays e dos mods

Data: 2026-10-25. Nenhum arquivo do jogo entra no repositório; só hashes, endereços e tamanhos. As contagens de
funções são aproximadas (fim de função por `jr ra`); o mapa real sai do Ghidra.

## Versões

| Versão | ELF | SHA-256 | Situação |
|---|---|---|---|
| Europa (original) | `SLES_556.05` | `20a43677397731a2a20899336d1165ace5b436906b9b89be90fb10f4558dd19d` | **base** |
| Versão Brasileira | `SLES_556.05` | `7c3517aa09c0c3e34742b89ee19a2de5f71753380ee198e8861a8030cfcf8e51` | ELF empacotado com SjCRUNCH v0.3 (LZO, blocos de 256 KB, `tools/un5.py unpack`); descomprimido, difere do europeu em 446 bytes (4 correções do próprio empacotador e textos): é praticamente um mod só de dados |
| UN6 Alpha 1.1X | `SLES_556.05` | `20872b06f4ef8b5ff49291b297a97941bc9131525b79e1cab2b1717156ca0056` | só dados: 606 palavras do ELF fora do código e 13 dos dados do `BTL.BIN` (2026-11-04); a base roda o programa dela |
| UN6 Alpha 3.5 | `SLUS_556.06` | `47c40141a3e1aeb0c96bc28e8dc311938b284d54fd21f4d8ba953c2e16234809` | primeiro pacote de código |

`SYSTEM.CNF` do A35: `59aecaa95b057f204f51b380b8fdccb50b8713ed10c986f38316a53d3d2c072c`
(`BOOT2 = cdrom0:\SLUS_556.06;1`, `VMODE = NTSC`). O A35 é só português do Brasil: a tabela de textos tem só
`TextBra.bin`.

### A ISO base (Redump)

A base é a ISO europeia do Redump: 1926234112 bytes, SHA-1 `50eedd72f0a8ee95b16c6e4e7a6b259743925012`.
`tools/un5.py verify` (e, por ele, o `build.sh`) e o importador do launcher conferem esse SHA-1 depois do ELF e dos
overlays, e recusam uma imagem que não bate.

Até 2026-10-26 usamos outra ISO com o mesmo nome e o mesmo tamanho (SHA-1 `1e56bddfb2536741152bd88348340a9caa9fd3e8`).
`un5.py diff` entre ela e a do Redump: ELF e os 8 overlays iguais (0 funções mudadas). Comparando setor a setor, só
2 setores diferem, os dois em `MODULES/IOPRP300.IMG`, no módulo `CDVDMAN` da imagem IOPRP (+0x73d0 e +0x9ca0):
onde o original lê o tipo do disco do registrador do CDVD (`lui v0,0xbf40; ori v0,0x200f; lbu v0,0(v0)`), a ISO ruim
tem `li v0,0x12` (sempre "CD de PS2"). É o patch de DVD para CD, para gravar a imagem numa mídia CD-R. Todos os
outros arquivos (`DATA/DATA.CVM` incluído) são iguais. O runtime não carrega a IOPRP (`sceSifRebootIop` só reinicia
o transporte do IOP, e o CDVD é HLE), então o código recompilado e os dados que o jogo lê são os mesmos com as duas
ISOs.

| Arquivo | SHA-256 Redump | SHA-256 com o patch de DVD para CD |
|---|---|---|
| `MODULES/IOPRP300.IMG` | `8e11fc76bbdd5770a47a56549d861c0caebc9f9190a1cf936990dcff9e5a86c5` | `f329ac928b49f203626f5271c91f420591eeabc26988f0f3dff068f49f3ef2bb` |

## Layout de memória

- ELF: carrega em `0x100000`, 0x517a80 bytes no arquivo (memsz 0x5c6d00); código até ~`0x5e71fc`, depois dados.
  Compilado com o Metrowerks CodeWarrior (MW MIPS C Compiler 2.4.1.01).
- Fim do BSS: europeu `0x928900`; A35 `0xa28900` (1 MB a mais, para o código novo).
- **Overlays em `cdrom0:\PRG\`**, formato `MWo3` do CodeWarrior. Cabeçalho de 0x40 bytes: magic, id, endereço de
  carga, tamanho do texto, tamanho dos dados, BSS, início e fim da tabela de construtores estáticos; o corpo começa em
  0x40 (o jogo carrega o arquivo inteiro em addr, então o código fica em addr+0x40). Os construtores são código dentro
  dos dados (BTL `0x8ea080`…, ADV `0x8aa680`…): `un5.py ovlelf` os marca como seções de código (`ctor_islands`).

| Arquivo | id | Carga | SHA-256 europeu | SHA-256 A35 |
|---|---|---|---|---|
| `BTL.BIN` (batalha) | 1 | `0x6c6d00` | `7e8518da7bd4957af18cb0ababe67f0e9b37c42c6551375201b15997f0a3dfe3` | `d9c05e13b772a44e4a8fef1e5101966c2748545a122a5f219d8aa992f88758c6` |
| `ADV.BIN` (aventura/RPG) | 2 | `0x6c6d00` | `7e2af55362141bb1b055247cd7ef7edae290f3c0095701bc51467f096a2d00b8` | `5fa4c6ecfa5bc98416a61c7e25b86f71f4fb4b37b1764c6e3996467279df37d4` (mesmo cabeçalho; +203 KB de enchimento no fim) |
| `ETC.BIN` | 3 | `0x6c6d00` | `bdb6bda1f9d335047586a263e478486c8e7924b91fa972b6f3e58caec5ea0778` | `478178c332b68451fa6d4c4308d5700e652c8c35cc59503b8d8acec68c3e1894` |
| `TEXTENG.BIN` | 4 | `0x8f3d00` | `3e42d2ddffe770b05dd41e2c5937380133e255c9ce32ca2f037e34c65a8e571e` | — |
| `TEXTFRN.BIN` | 5 | `0x8f3d00` | `485d44d2866e9e985d2d84b4796bd8daa573567d44b8913b9fbccffaafc836b0` | — |
| `TEXTGER.BIN` | 6 | `0x8f3d00` | `84de40f3def485b80863370723e462a7b23ebb6a4b5e6a088b4897f78005222d` | — |
| `TEXTITA.BIN` | 7 | `0x8f3d00` | `110a915c3043214a6c2a9bd2ca3cb33284629126f91b76db6e525a9fd7b20b03` | — |
| `TEXTSPA.BIN` | 8 | `0x8f3d00` | `72ec1bb3aead2764bdc096a1a757c6969d1573c99954df6711e0a0495418e927` | — |
| `TEXTBRA.BIN` | 4 | `0x8f3d00` | — | `07e30831dc9e88ba4e0ddb1b4f3fd8edd0d8c4d1cf170bd59bfcb17c09e256bf`; textos + ~167 funções em `0x936fb4–0x93ffb8` |
| `MOD.BIN` | **8** | `0x940000` | — | `6eab9760d2bd6583630d096eb08fb7f09e299f5e2fb64df2413e5dc2ed182998`; ~600 funções |

O `MOD.BIN` reusa o **id 8**, que no europeu é o `TEXTSPA.BIN`: o runtime escolhe o código de um overlay pelo hash do
arquivo carregado, nunca pelo id ou pelo endereço.

## O que o A35 muda no código (`tools/un5.py diff`)

| Onde | Funções | Alteradas | Novas |
|---|---|---|---|
| ELF | ~9244 | ~312 | |
| `BTL.BIN` | ~5665 | ~219 | |
| `ADV.BIN` | ~6918 | ~97 | |
| `ETC.BIN` | ~314 | ~3 | |
| `MOD.BIN` | | | ~599 |
| `TEXTBRA.BIN` | | | ~167 |

Total: ~631 alteradas e ~766 novas, contra ~22 mil funções do jogo (~6%). O A35 também injeta 132 chamadas, em 60
funções do jogo, para o código novo em `0x93xxxx–0x94xxxx`.

Outros: serial `SLUS-55606` / `NARUTO6`; lê saves de UN1–UN5 (`BASLUS-21358` … `BESLES-55605`); personagens novos
prováveis, pelos nomes dos materiais: Karin, Jugo, Tobi, corpos do Pain, Konan, Kakuzu, Hidan. A ISO do A35 tem um
`CHANGELOG.TXT` na raiz.

## Disco do A35 (para referência)

`SYSTEM.CNF`, `SLUS_556.06`, `CHANGELOG.TXT`, `FLIST.DIR`, `OUT1M.BIN` (enchimento de 1 MB),
`DATA/` (`DATA.CVM` 605 MB, `PLVOICE.AFS`, `RPGVOICE.AFS`, `SNDDATA.BIN`, `SOUND.AFS`, `STREAM.AFS`, `GF4.BIN`,
`GF4C.BIN`, `GRF4.BIN`, `SF1.BIN`, `SF1C.BIN`), `MODULES/` (`CRI_ADXI.IRX`, `IOPRP300.IMG`, `MODULES.BIN`,
`SNDBASE.IRX`), `PRG/` (acima), `PSS/` (`LOGO_C.PSS`, `NARUTO20.PSS`, `OPENING.PSS`).
