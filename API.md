# Integração da app com o Back4App (Parse)

## Âmbito

Este documento descreve a comunicação que a aplicação Android **MapaISEP** implementa com o Back4App através do Parse SDK Android (`com.github.parse-community.Parse-SDK-Android:parse:4.4.0`). A app é cliente do serviço Parse alojado em `https://parseapi.back4app.com/`; este repositório não contém um servidor REST próprio nem controllers de servidor.

O SDK constrói e envia os pedidos HTTP internamente. Por isso, os métodos abaixo documentam as operações da app, não endpoints HTTP implementados manualmente por este projeto. Os caminhos REST internos podem variar com a operação/versão do Parse; consultar o painel Back4App ou a documentação Parse para os detalhes de baixo nível.

## Configuração da ligação

- **Servidor:** `https://parseapi.back4app.com/`
- **SDK:** Parse SDK Android 4.4.0
- **Inicialização:** `App.onCreate()` chama `Parse.initialize(...)` antes das Activities.
- **Identificadores:** `applicationId` e `clientKey` identificam a app Parse e são configurados na inicialização. Não são credenciais de utilizador; não publicar chaves privilegiadas (por exemplo, master key) na app cliente.
- **Acesso à rede:** a app declara a permissão Android `INTERNET`.
- **Execução:** as operações assíncronas do SDK devolvem o resultado através de callbacks.

## Operações usadas pela aplicação

### 1. Iniciar sessão

- **Código:** `AuthService.login(...)`
- **Operação Parse:** `ParseUser.logInInBackground(username, password, callback)`
- **Dados enviados:** número de estudante usado como `username` e palavra-passe. A autenticação é feita pelo Parse; a app não constrói manualmente o URL nem o corpo JSON.
- **Sucesso:** o callback recebe o `ParseUser` autenticado; `LoginActivity` prossegue para o menu.
- **Erro:** o callback recebe `ParseException`; a interface apresenta a mensagem apropriada.
- **Método HTTP, headers e formato exato do corpo:** geridos pelo SDK, não definidos explicitamente na app.

### 2. Obter os dados atualizados do aluno

- **Código:** `HorarioService.obterAulasHoje(...)`
- **Operação Parse:** `ParseUser.fetchInBackground(...)` para atualizar o utilizador autenticado.
- **Campo lido:** `TurmaAluno`, uma referência Parse para a turma do aluno.
- **Falha:** se não existir utilizador autenticado, a referência à turma estiver vazia, ou a consulta falhar, o serviço devolve uma mensagem de erro pelo callback.

### 3. Consultar as aulas de hoje

- **Código:** `HorarioService.obterAulasHoje(...)`
- **Classe Parse consultada:** `Aula`
- **Filtros:** `turma` igual à turma do aluno e `diaSemana` igual ao dia atual.
- **Ordenação:** `horaInicio` ascendente.
- **Campos lidos pela interface:** `nome`, `horaInicio` e `horaFim`.
- **Sucesso:** o callback recebe uma lista de `ParseObject`; `MenuPrincipal` apresenta as aulas ou a mensagem de que não existem aulas.
- **Erro:** o callback recebe a mensagem da falha e a interface apresenta o erro.

#### Esquema de dados esperado (inferido do código cliente)

| Classe Parse | Campo | Uso no cliente |
|---|---|---|
| `User` (aluno) | `TurmaAluno` | Referência para a turma do aluno autenticado |
| `Aula` | `turma` | Referência usada para filtrar pela turma |
| `Aula` | `diaSemana` | Número do dia: segunda-feira = 1, ..., domingo = 7 |
| `Aula` | `horaInicio` | Ordenação e apresentação |
| `Aula` | `horaFim` | Apresentação |
| `Aula` | `nome` | Apresentação |

Os tipos exatos dos campos e a configuração de ACL/permissões têm de ser confirmados no painel Back4App; o código cliente permite confirmar os nomes e usos acima, mas não todo o esquema nem as regras do servidor.

### 4. Terminar sessão

- **Código:** `MenuPrincipal.terminarSessao()`
- **Operação Parse:** `ParseUser.logOut()`
- **Efeito:** limpa a sessão local e a app regressa ao ecrã de login. O fluxo atual não envia um pedido de logout remoto explícito.

## Contrato de resposta observado

O Parse SDK converte as respostas recebidas em `ParseUser` e `ParseObject`. A aplicação não lê diretamente o código HTTP nem o corpo bruto da resposta. Em caso de falha, o SDK fornece um `ParseException`/erro ao callback; em caso de sucesso, fornece o objeto ou a lista de objetos correspondente.

## Sequência simplificada

```text
LoginActivity -> AuthService -> Parse SDK -> Back4App
                               <- ParseUser / erro

MenuPrincipal -> HorarioService -> Parse SDK -> Back4App
                                  1) atualizar ParseUser
                                  2) consultar Aula por turma e dia
                                  <- lista de ParseObject / erro
```

## Fora do âmbito desta integração

- O mapa usa o Google Maps SDK e não é uma operação de dados do Back4App.
- Este projeto não declara endpoints próprios como `/aulas` nem implementa um servidor REST/Swagger.
- Não há DTOs próprios nesta comunicação: o cliente usa diretamente `ParseUser` e `ParseObject`.
- Não são especificados aqui pedidos HTTP manuais, headers ou corpos JSON porque a app delega esses detalhes ao Parse SDK.

## Ficheiros relacionados

- `app/src/main/java/pt/isep/dssmv/mapaisep/controllers/App.java` — configuração do Parse.
- `app/src/main/java/pt/isep/dssmv/mapaisep/services/AuthService.java` — autenticação.
- `app/src/main/java/pt/isep/dssmv/mapaisep/services/HorarioService.java` — atualização do aluno e consulta das aulas.
- `app/src/main/java/pt/isep/dssmv/mapaisep/controllers/LoginActivity.java` — interface e tratamento do resultado de login.
- `app/src/main/java/pt/isep/dssmv/mapaisep/controllers/MenuPrincipal.java` — apresentação do horário e logout.
