# MercadoVendas

## Integrantes
- Fernando Araújo Maia Machado

## Descrição do Projeto
Marketplace simples dividido em dois microservices: catálogo de produtos
(`product-service`) e pedidos de compra (`order-service`). O `order-service` valida
cada item do pedido em tempo real junto ao `product-service`, via comunicação
síncrona (OpenFeign) resolvida por Service Discovery (Eureka), com proteção de
resiliência (timeout + circuit breaker + fallback) para o caso do catálogo ficar
indisponível. Todo acesso externo passa por um API Gateway único.

Documentos técnicos completos em [docs/](docs/).

## Arquitetura
Ver [docs/arquitetura.md](docs/arquitetura.md) para o diagrama completo.

- **Config Server**: configuração centralizada de todos os serviços.
- **Eureka Server (Discovery Server)**: registro e descoberta dos serviços.
- **API Gateway**: ponto único de entrada, roteia por nome lógico via Eureka.
- **product-service**: dono do catálogo (produtos, categorias).
- **order-service**: dono dos pedidos; chama `product-service` via Feign.

## Microservices

| Serviço | Responsabilidade | Porta | Banco |
|---|---|---|---|
| `config-server` | Configuração centralizada | 8888 | — |
| `discovery-server` (Eureka) | Registro/descoberta de serviços | 8761 | — |
| `api-gateway` | Ponto único de entrada / roteamento | 8080 | — |
| `product-service` | Catálogo de produtos e categorias | 8091 | PostgreSQL (`product_db`) |
| `order-service` | Pedidos de compra | 8082 | PostgreSQL (`order_db`) |

Detalhamento de cada serviço em [docs/microservices.md](docs/microservices.md).

## Tecnologias utilizadas
- Java 17+ / Spring Boot
- Spring Cloud Config Server
- Spring Cloud Eureka (Discovery Server)
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Resilience4j (Circuit Breaker / Timeout / Fallback)
- PostgreSQL (via Docker)
- Maven
- Java 21 / Spring Boot 4

## Como executar

Pré-requisitos: Docker, Java 21+, Maven.

1. Subir o PostgreSQL (cria automaticamente os databases `product_db` e `order_db`,
   cada um com seu próprio usuário — ver [docker/postgres-init/init.sql](docker/postgres-init/init.sql)):
```bash
docker compose up -d
```

2. Subir os serviços **nesta ordem**, cada um em um terminal (cada um só fica
   pronto de fato depois de aparecer "Started ... Application" no log):
```bash
cd config-server && mvn spring-boot:run
```
```bash
cd discovery-server && mvn spring-boot:run
```
```bash
cd product-service && mvn spring-boot:run
```
```bash
cd order-service && mvn spring-boot:run
```
```bash
cd api-gateway && mvn spring-boot:run
```

## Portas utilizadas
| Serviço | Porta |
|---|---|
| config-server | 8888 |
| discovery-server (Eureka) | 8761 |
| api-gateway | 8080 |
| product-service | 8091 |
| order-service | 8082 |

## Exemplos de endpoints
Contratos completos em [docs/contratos-api.md](docs/contratos-api.md).

```bash
# via API Gateway
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Eletrônicos","description":"Periféricos e acessórios"}'

curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Teclado mecânico","description":"ABNT2","price":349.90,"stock":12,"categoryId":"<id-da-categoria>"}'

curl http://localhost:8080/api/products
curl http://localhost:8080/api/products/{id}

curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cliente-123","items":[{"productId":"<id-do-produto>","quantity":2}]}'
```

## Discovery Server
- URL do painel Eureka: `http://localhost:8761`
- Após subir `product-service`, `order-service` e `api-gateway`, todos devem
  aparecer registrados no painel (`PRODUCT-SERVICE`, `ORDER-SERVICE`,
  `API-GATEWAY`).

## API Gateway
| Rota externa | Microservice destino |
|---|---|
| `/api/products/**` | `product-service` |
| `/api/categories/**` | `product-service` |
| `/api/orders/**` | `order-service` |

Teste rápido pelas rotas do Gateway:
```bash
curl http://localhost:8080/api/products
curl http://localhost:8080/api/orders/{id}
```

## Resiliência
Ver plano completo, incluindo como simular a falha, em
[docs/resiliencia.md](docs/resiliencia.md). Resumo: `order-service → product-service`
protegida por timeout + circuit breaker + fallback (Resilience4j).

## Evidências

Fluxo completo validado manualmente de ponta a ponta (subindo os 5 serviços na ordem
acima e chamando tudo através do Gateway):

1. **Eureka com os serviços registrados**: `PRODUCT-SERVICE`, `ORDER-SERVICE` e
   `API-GATEWAY` aparecem com status `UP` em `http://localhost:8761`.
2. **Chamada via Gateway**: `POST /api/categories`, `POST /api/products` e
   `GET /api/products` respondendo corretamente através da porta 8080 (roteado para
   `product-service`).
3. **Chamada Feign funcionando (caminho feliz)**: `POST /api/orders` retorna
   `status: "CONFIRMED"` com o preço/nome do produto obtidos em tempo real do
   `product-service` via Feign.
4. **Fallback acionando**: com `product-service` derrubado, `POST /api/orders`
   retorna `status: "AWAITING_VALIDATION"` (em vez de erro 500), com o log
   `Fallback acionado: product-service indisponível ao consultar produto ...`
   em `order-service`.
5. **Recuperação**: religando `product-service`, o próximo `POST /api/orders`
   volta a responder `CONFIRMED` normalmente.

_(Prints de tela ainda a serem capturados para anexar à documentação da entrega —
o roteiro acima reproduz exatamente o que deve aparecer em cada print.)_
