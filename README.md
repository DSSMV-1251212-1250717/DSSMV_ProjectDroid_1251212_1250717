# Mapa do ISEP

Aplicação Android (Java) que ajuda os alunos do ISEP a orientarem-se no campus:
horário do dia, mapa com a sala da próxima aula e a biblioteca, posição dos amigos
e estimativa da ocupação da biblioteca.

Autores: Afonso Teixeira (1250717) e Marco Cardoso (1251212), DSSMV 2026/27, turma 2DB.

## Como correr
1. Abrir o projeto no Android Studio.
2. No ficheiro `local.properties` (não vai para o GitHub), acrescentar a chave do Google Maps:
   `MAPS_API_KEY=<chave do Google Maps>`
3. Correr a configuração `app` num telemóvel com Android 8.0 (API 26) ou mais recente.
4. Entrar com o número de estudante e a password registados no Firebase.

## Testes
- Unitários: `app/src/test` (correm no computador).
- Instrumentados: `app/src/androidTest` (correm no telemóvel ou no emulador).

## Relatório
[docs/DSSMV_ProjectDroid_report_1251212_1250717.pdf](docs/DSSMV_ProjectDroid_report_1251212_1250717.pdf)
