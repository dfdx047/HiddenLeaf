# Release (APK público e atualização automática)

Como lançar uma versão nova do Hidden Leaf para o app de quem já tem a anterior se atualizar sozinho. Primeiro release:
`v0.1.0-beta`, 2026-11-14.

## A chave de release (não trocar nunca)

- Arquivo: `~/keys/hiddenleaf-release.jks`. Senha, alias e caminho: `~/keys/hiddenleaf-release.properties`
  (`storeFile`, `storePassword`, `keyAlias=hiddenleaf`, `keyPassword`). Os dois ficam **fora do repositório** e não
  entram em commit, log nem documento.
- Certificado `CN=Hidden Leaf, O=dfdx047`, SHA-256 `8b3f4213556edbd018996833418f102f4769e418e2edab675a6e67b084990830`.
- **Todo release público é assinado com essa chave.** O Android só instala uma atualização por cima, mantendo os dados
  do jogo, os ajustes e os saves, se a assinatura for a mesma. Com outra chave, a pessoa precisa desinstalar e perde
  tudo, e a atualização automática falha.
- Se a chave sumir, não há como recuperar: o desenvolvedor mantém um backup. Não gerar uma chave nova sem ele pedir.
- A `android/app/hiddenleaf-test.jks` (no repositório, senha pública) é só para builds de teste (debug e CI). Um APK
  assinado com ela não instala por cima do release, nem o contrário.

## Versão

- `versionName` vem de `HL_VERSION_NAME` (padrão no `android/app/build.gradle.kts`, hoje `0.1.0-beta`). Subir o padrão
  no arquivo a cada release.
- `versionCode` sai do nome: `(maior*10000 + menor*100 + patch) * 100 + n`, com `n` = número da beta (`-beta` = 1,
  `-beta.2` = 2…) ou 99 sem sufixo. `0.1.0-beta` = 10001, `0.1.0-beta.2` = 10002, `0.1.0` = 10099. Tem que crescer a
  cada release, senão o Android recusa a atualização.
- O atualizador (`android/app/src/main/java/com/dfdx047/hiddenleaf/data/AppUpdates.kt`) compara os nomes do mesmo jeito:
  `0.1.0-beta` < `0.1.0-beta.2` < `0.1.0` < `0.1.1-beta`.

## Passo a passo

1. Motores atualizados em `android/app/src/main/jniLibs/arm64-v8a/`: `recomp/android/build.sh` e
   `HL_V80=1 recomp/android/build.sh` (e `--mod un6-a35` nos dois se o runtime mudou).
2. Versão nova no `build.gradle.kts` e uma seção nova no topo dos três changelogs (`CHANGELOG.md`,
   `CHANGELOG.pt-BR.md`, `CHANGELOG.es.md`), começando por `## vX.Y.Z… (data)`. O app mostra **a primeira seção `## `**
   do changelog no idioma do celular como as novidades da atualização.
3. Compilar assinado (TMPDIR dentro do projeto):
   ```sh
   cd android
   set -a; . ~/keys/hiddenleaf-release.properties; set +a
   HL_KEYSTORE=$storeFile HL_KEYSTORE_PASSWORD=$storePassword HL_KEY_ALIAS=$keyAlias HL_KEY_PASSWORD=$keyPassword \
     TMPDIR=$PWD/../build/tmp ./gradlew assembleRelease
   cp app/build/outputs/apk/release/app-release.apk ../build/release/HiddenLeaf-vX.Y.Z.apk
   ```
4. Conferir: `aapt2 dump badging` (versionName e versionCode novos) e `apksigner verify --print-certs` (o SHA-256 do
   certificado tem que ser o de cima).
5. `docs/registro.md`, commit, push da branch e da `main` (`git push origin lab/bench-pcsx2 lab/bench-pcsx2:main`).
6. Release com a tag `vX.Y.Z[-beta.N]` (o prefixo `v` é obrigatório para o atualizador achar), o APK como anexo
   `.apk`, pré-lançamento enquanto for beta:
   ```sh
   gh release create vX.Y.Z build/release/HiddenLeaf-vX.Y.Z.apk --target main --prerelease \
     --title "Hidden Leaf vX.Y.Z" --notes-file build/release/release-notes.md
   ```

## Como o app se atualiza

Ao abrir, o launcher lê `api.github.com/repos/dfdx047/HiddenLeaf/releases`, pega a maior versão com um `.apk` anexado e,
se for maior que a instalada: no Wi-Fi baixa sozinho e abre o instalador; nos dados móveis pergunta antes. O Android
sempre pede o toque em "Atualizar" no instalador (e, na primeira vez, a permissão de instalar apps desta fonte).
Com o repositório privado a busca falha calada; só funciona com ele público.
