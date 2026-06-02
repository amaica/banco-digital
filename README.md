# desafio tecnico - banco digital

Api rest do desafio java — transferir entre contas, extrato, notificacao.

https://github.com/amaica/banco-digital

Spring Boot 3.2, MySQL 8, Flyway. Swagger: http://localhost:8080/swagger-ui.html

## como subir

Java 17, maven, docker ou podman.

Terminal 1:

```
./init.sh
```

```
./init.sh reset   # zera
./init.sh
```

Nao use `./init.sh docker`.

Mysql 3309, api 8080. Espera `Started BancoDigitalApplication`.

Parar: ctrl+c ou `./init.sh stop`

## como testar

Terminal 2:

```
./init.sh test
```

Esperado: `ok — api, transferencia e swagger funcionando`

### curls

Ja vem 5 contas: Alice 1 com 10000, Bruno 2 com 5000, Carla 3 com 7500, Diego 4 com 3200, Elena 5 com 15000.

**curl 1** — listar contas

```
curl http://localhost:8080/api/accounts
```

**curl 2** — Alice

```
curl http://localhost:8080/api/accounts/1
```

**curl 3** — transferir 100, Alice → Bruno

```
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":100}'
```

**curl 4** — extrato Alice

```
curl http://localhost:8080/api/accounts/1/movements
```

**curl 5** — notificacao Alice

```
sleep 2
curl http://localhost:8080/api/accounts/1/notifications
```

**curl 6** — criar conta

```
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"Joao","initialBalance":500}'
```

**curl 7** — Bruno

```
curl http://localhost:8080/api/accounts/2
```

**curl 8** — saldo insuficiente

```
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":99999999}'
```

Cabeçalho opcional na transferência: `X-Chave-Retry`

```
mvn test
```

## rotas

GET /api/accounts
GET /api/accounts/{id}
POST /api/accounts
GET /api/accounts/{id}/movements
GET /api/accounts/{id}/notifications
POST /api/transfers

## deliberações

- camadas: controlador / serviço / repositório. Data Transfer Object na API, Entidade no JPA
- MySQL 8, Flyway V1–V3, Hibernate validate, CHECK saldo >= 0
- transferência: transação única, bloqueio pessimista, menor ID primeiro, débito/crédito na Conta, @Version
- notificação: TransferCompletedEvent, ouvinte AFTER_COMMIT + @Async
- tentar novamente: cabeçalho X-Chave-Retry
- tratamento de erros: exceções de domínio + GlobalExceptionHandler
- local: init.sh + docker/podman, mysql 3309, api 8080
- stack: Spring Boot 3.2, Java 17, Springdoc
