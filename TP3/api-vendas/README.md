# TP3 — Autenticação JWT e Microsserviços Reativos com Spring

## Integrantes
- Fernando Araújo Maia Machado

Projeto construído sobre a base do exercício de Spring Cloud da disciplina
(`config-server`, `eureka-server`, `produtos-service`), com:

- um **microsserviço de autenticação independente** (`auth-service`) que emite
  tokens **JWT** (login + refresh), com persistência via **Spring Data JDBC**;
- o `produtos-service` reescrito como servidor **reativo** (**Spring WebFlux** +
  **Spring Data R2DBC**) com rotas protegidas por JWT;
- um novo `vendas-service` reativo que consulta o `produtos-service` via
  **WebClient** (não bloqueante, resolvido pelo Eureka) para registrar vendas;
- **bancos de dados separados** por serviço (PostgreSQL);
- **testes automatizados com Testcontainers** (PostgreSQL real em Docker).

## Arquitetura

```
                     ┌──────────────────┐          ┌──────────────────┐
                     │  config-server   │          │  eureka-server   │
                     │      :8888       │          │      :8761       │
                     │ (lê config-repo/)│          │(service registry)│
                     └────────┬─────────┘          └────────┬─────────┘
                configurações │ (porta, banco, jwt.secret…) │ registro / descoberta
          ┌───────────────────┼─────────────────────────────┼──────────────────┐
          ▼                   ▼                             ▼                  │
┌───────────────────┐ ┌─────────────────────┐   ┌─────────────────────┐        │
│   auth-service    │ │  produtos-service   │   │   vendas-service    │◄───────┘
│      :9000        │ │       :8082         │   │       :8083         │
│ Spring MVC        │ │ WebFlux (reativo)   │   │ WebFlux (reativo)   │
│ Spring Data JDBC  │ │ Spring Data R2DBC   │   │ Spring Data R2DBC   │
│                   │ │                     │◄──┤ WebClient + Eureka  │
│ POST /auth/login  │ │ GET  /produtos   🔒 │   │ POST /vendas     🔒 │
│ POST /auth/refresh│ │ POST /produtos 🔒ADM│   │ GET  /vendas     🔒 │
└────────┬──────────┘ └──────────┬──────────┘   └──────────┬──────────┘
         ▼                       ▼                         ▼
   ┌──────────┐           ┌─────────────┐           ┌───────────┐
   │ auth_db  │           │ produtos_db │           │ vendas_db │   PostgreSQL :5434
   └──────────┘           └─────────────┘           └───────────┘   (usuário próprio por banco)
```

| Serviço | Responsabilidade | Stack | Porta | Banco |
|---|---|---|---|---|
| `config-server` | Configuração centralizada (backend *native*, pasta `config-repo/`) | Spring Cloud Config | 8888 | — |
| `eureka-server` | Service Discovery | Spring Cloud Netflix Eureka | 8761 | — |
| `auth-service` | **Autenticação**: valida usuário/senha e emite/renova tokens JWT | Spring MVC + **Spring Data JDBC** | 9000 | `auth_db` |
| `produtos-service` | Catálogo de produtos — rotas protegidas | **WebFlux + R2DBC** | 8082 | `produtos_db` |
| `vendas-service` | Registro de vendas — consulta o produto via **WebClient** | **WebFlux + R2DBC + WebClient** | 8083 | `vendas_db` |

### Bancos de dados separados (database per service)

Um único servidor PostgreSQL hospeda **três bancos isolados**, cada um com seu
**próprio usuário** ([docker/postgres-init/init.sql](docker/postgres-init/init.sql)).
Um serviço não tem permissão de acesso ao banco de outro:

```bash
$ docker compose exec postgres psql -U vendas_user -d produtos_db
FATAL:  permission denied for database "produtos_db"
DETAIL:  User does not have CONNECT privilege.
```

O `vendas-service` **não** lê a tabela de produtos: ele obtém o preço chamando a
API do `produtos-service`.

## Requisitos de Reactive Spring — onde cada um é atendido

| Requisito | Onde |
|---|---|
| Persistência com **Spring Data JDBC** em banco separado | `auth-service`: [Usuario.java](auth-service/src/main/java/com/exemplo/authservice/model/Usuario.java) (`@Table`, sem JPA), [UsuarioRepository.java](auth-service/src/main/java/com/exemplo/authservice/repository/UsuarioRepository.java) (`ListCrudRepository`), [schema.sql](auth-service/src/main/resources/schema.sql), banco `auth_db` |
| **Testes da persistência com Testcontainers** | [UsuarioRepositoryTest](auth-service/src/test/java/com/exemplo/authservice/repository/UsuarioRepositoryTest.java) (`@DataJdbcTest`), [ProdutoRepositoryTest](produtos-service/src/test/java/com/exemplo/produtosservice/repository/ProdutoRepositoryTest.java) e [VendaRepositoryTest](vendas-service/src/test/java/com/exemplo/vendasservice/repository/VendaRepositoryTest.java) (`@DataR2dbcTest` + `StepVerifier`) |
| Servidor **WebFlux** com acesso a dados **R2DBC** | `produtos-service` e `vendas-service`: controllers retornam `Mono`/`Flux`, repositórios `ReactiveCrudRepository`, servidor Netty, segurança reativa (`SecurityWebFilterChain`) |
| **WebClient** entre microsserviços | `vendas-service`: [WebClientConfig](vendas-service/src/main/java/com/exemplo/vendasservice/config/WebClientConfig.java) (LoadBalancer/Eureka + repasse do JWT) e [ProdutoClient](vendas-service/src/main/java/com/exemplo/vendasservice/client/ProdutoClient.java) (timeout, tratamento de 404 e falhas) |
| **Testes das aplicações reativas** com Spring e Testcontainers | [ProdutoControllerIntegrationTest](produtos-service/src/test/java/com/exemplo/produtosservice/controller/ProdutoControllerIntegrationTest.java) e [VendaControllerIntegrationTest](vendas-service/src/test/java/com/exemplo/vendasservice/controller/VendaControllerIntegrationTest.java) (`@SpringBootTest` + `WebTestClient` + PostgreSQL no Testcontainers; o `produtos-service` é simulado com MockWebServer) |

## Tecnologia de autenticação: JWT

- **JWT (JSON Web Token)** assinado com **HMAC-SHA256 (HS256)**.
- Emissão: `spring-security-oauth2-jose` (Nimbus JOSE + JWT) no `auth-service`.
- Validação: **Spring Security OAuth2 Resource Server** (versão reativa) no
  `produtos-service` e no `vendas-service`. Cada serviço valida o token
  **localmente** (assinatura, expiração, emissor e tipo), sem chamar o
  `auth-service`, o que mantém os serviços desacoplados.
- A chave (`jwt.secret`) e o emissor (`jwt.issuer`) são distribuídos pelo
  `config-server` (`config-repo/application.properties`).
- Senhas armazenadas com hash **BCrypt**.

| Token | Validade | Uso | Claims |
|---|---|---|---|
| **Access token** | 5 min | Header `Authorization: Bearer` nas rotas protegidas | `sub`, `roles`, `token_type=access`, `iss`, `iat`, `exp`, `jti` |
| **Refresh token** | 24 h | **Somente** no `POST /auth/refresh` | `sub`, `token_type=refresh`, `iss`, `iat`, `exp`, `jti` |

Regras aplicadas:
- **Refresh token não é aceito** como access token (e vice-versa) → 401.
- Assinatura inválida, token expirado ou de outro emissor → **401**.
- Perfis relidos do banco a cada refresh.
- `POST /produtos` exige `ROLE_ADMIN` → usuário comum recebe **403**.
- **Propagação do token**: ao registrar uma venda, o `vendas-service` repassa o
  JWT recebido na chamada ao `produtos-service` (`ServerBearerExchangeFilterFunction`).

## Como executar

Pré-requisitos: **Docker** (e, para a opção 2 e para os testes, **Java 17+** e **Maven**).

### Opção 1 — Docker Compose (tudo de uma vez)

```bash
docker compose up --build
```

Aguarde os três serviços (`auth`, `produtos`, `vendas`) registrarem no Eureka
(http://localhost:8761, ~15 s). Para encerrar: `docker compose down`
(`docker compose down -v` apaga também os dados do PostgreSQL).

### Opção 2 — Maven (um terminal por serviço, **nesta ordem**)

```bash
docker compose up -d postgres          # só o banco
cd config-server    && mvn spring-boot:run
cd eureka-server    && mvn spring-boot:run
cd auth-service     && mvn spring-boot:run
cd produtos-service && mvn spring-boot:run
cd vendas-service   && mvn spring-boot:run
```

> O `config-server` deve ser iniciado **de dentro da pasta `config-server/`**,
> pois lê `../config-repo/`.

### Executando os testes

Com o Docker em execução (o Testcontainers sobe um PostgreSQL descartável):

```bash
cd auth-service     && mvn test   # 11 testes (JDBC + fluxo de login/refresh)
cd produtos-service && mvn test   # 18 testes (R2DBC + WebFlux/segurança)
cd vendas-service   && mvn test   # 12 testes (R2DBC + WebClient + WebFlux)
```

> `src/test/resources/docker-java.properties` fixa `api.version=1.44`, necessário
> para o Testcontainers 1.19 funcionar com Docker Engine 29+.

### Usuários de teste (criados na inicialização do `auth-service`)

| Usuário | Senha | Perfis |
|---|---|---|
| `admin` | `admin123` | `USER`, `ADMIN` |
| `usuario` | `usuario123` | `USER` |

## Endpoints

### Públicos (sem token)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `http://localhost:9000/auth/login` | Autentica e retorna access + refresh token |
| `POST` | `http://localhost:9000/auth/refresh` | Troca um refresh token válido por um novo par de tokens |
| `GET` | `/actuator/health` (9000, 8082, 8083) | Health check |

### Protegidos (exigem `Authorization: Bearer <accessToken>`)

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| `GET` | `http://localhost:8082/produtos` | autenticado | Lista produtos (`?nome=` filtra) |
| `GET` | `http://localhost:8082/produtos/{id}` | autenticado | Busca produto |
| `GET` | `http://localhost:8082/produtos/me` | autenticado | Dados do token recebido |
| `POST` | `http://localhost:8082/produtos` | **ADMIN** | Cria produto |
| `POST` | `http://localhost:8083/vendas` | autenticado | Registra venda (consulta o produto via WebClient) |
| `GET` | `http://localhost:8083/vendas` | autenticado | Vendas do usuário autenticado |
| `GET` | `http://localhost:8083/vendas/{id}` | autenticado | Busca venda |

## Como realizar a autenticação

```bash
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Resposta (200):

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 300
}
```

Credenciais inválidas (401):

```json
{"status":401,"erro":"Unauthorized","mensagem":"Usuario ou senha invalidos","timestamp":"..."}
```

## Acessando rotas protegidas

```bash
# Sem token -> 401
curl -i http://localhost:8082/produtos

# Com token -> 200
curl http://localhost:8082/produtos -H "Authorization: Bearer <accessToken>"

# Registrar venda -> 201
curl -X POST http://localhost:8083/vendas \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"idProduto":1,"quantidade":2}'
```

Resposta da venda (o `valorProduto` veio do `produtos-service`):

```json
{"idVenda":1,"idProduto":1,"quantidade":2,"valorProduto":3500.00,"valorTotal":7000.00,
 "usuario":"usuario","dataVenda":"2026-10-01T12:18:43.403988"}
```

| Situação na venda | Resposta |
|---|---|
| Produto inexistente | **422** |
| `produtos-service` fora do ar, com erro ou lento (timeout de 3 s) | **503** |
| `quantidade` < 1 ou campos ausentes | **400** |

## Como utilizar o endpoint de refresh

Quando o access token expira (5 min), as rotas protegidas passam a responder
**401**. Envie o refresh token para obter um novo par:

```bash
curl -X POST http://localhost:9000/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

A resposta tem o mesmo formato do login, com um **novo** `accessToken` (e um novo
`refreshToken`). Refresh token inválido, expirado ou um access token no lugar
dele → **401**.

## Exemplos de requisições para teste

- **`demo.sh`**: roteiro completo da apresentação (requer `curl` e `jq`):
  acesso sem token (401), login inválido (401), login válido, acesso com token
  (200), token adulterado (401), refresh, acesso com o novo token (200), refresh
  inválido (401), refresh token usado como access (401), autorização por perfil
  (403/201) e vendas (401, 201, 422, listagem).

  ```bash
  ./demo.sh
  ```

- **`requests.http`**: as mesmas requisições para o *REST Client* do VS Code ou
  o *HTTP Client* do IntelliJ.

## Estrutura do projeto

```
api-vendas/
├── config-repo/                 # configurações servidas pelo config-server
│   ├── application.properties   #   compartilhadas (eureka, jwt, sql.init)
│   ├── auth-service.properties
│   ├── produtos-service.properties
│   └── vendas-service.properties
├── docker/postgres-init/init.sql  # cria auth_db, produtos_db, vendas_db e usuários
├── config-server/
├── eureka-server/
├── auth-service/                # login/refresh JWT — Spring Data JDBC
├── produtos-service/            # WebFlux + R2DBC — rotas protegidas
├── vendas-service/              # WebFlux + R2DBC + WebClient — rotas protegidas
├── docker-compose.yml
├── demo.sh
└── requests.http
```

## Observações

- Em produção, a chave `jwt.secret` deve vir da variável de ambiente
  `JWT_SECRET` (já suportada) e nunca ser versionada.
- Os tokens são *stateless*: não há lista de revogação. Um refresh token antigo
  continua válido até expirar mesmo após ser usado. Uma evolução seria persistir
  os `jti` dos refresh tokens para permitir rotação com revogação e logout.
- Os intervalos do Eureka/LoadBalancer foram reduzidos para 5 s
  (`config-repo/application.properties`) para que serviços recém-iniciados sejam
  encontrados rapidamente durante a demonstração.
