# 🏦 Financial Ledger Distribuído
> Sistema de ledger financeiro distribuído de nível mundial, implementando padrões avançados de engenharia para garantir integridade total de dados em transações entre microserviços.

## 🎯 O Problema que Este Sistema Resolve
Em sistemas financeiros distribuídos, como garantir que o dinheiro saiu de uma conta **e** entrou na outra, mesmo que a rede caia ou um servidor exploda no meio do processo?

Este projeto responde a essa pergunta com uma arquitetura robusta e battle-tested.

---

## 🏗️ Arquitetura
```
┌─────────────────────────────────────────────────────┐
│                    API GATEWAY                       │
│         POST /transfers {idempotencyKey}             │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│           TRANSFER SERVICE (Orquestrador)            │
│  • Verifica idempotência (Redis)                     │
│  • Inicia SagaExecution                              │
│  • Persiste evento no Outbox (mesma TX)              │
└──────────┬──────────────────────────────────────────┘
           │ Kafka
     ┌─────┴──────┐
     ▼            ▼
┌─────────┐  ┌──────────┐
│ ACCOUNT │  │ RESERVE  │
│ SERVICE │  │ SERVICE  │
│ (débito)│  │(crédito) │
└─────────┘  └──────────┘
```

---

## Padrões e Conceitos Implementados

### 🔄 Saga Pattern (Orquestrado)
Gerencia transações distribuídas garantindo que o dinheiro nunca "desapareça". Se o crédito falhar, o débito é estornado automaticamente.
```
HAPPY PATH:
STARTED → DEBIT_REQUESTED → DEBIT_PERFORMED → CREDIT_REQUESTED → COMPLETED ✅

FAILURE PATH:
STARTED → DEBIT_REQUESTED → DEBIT_PERFORMED → CREDIT_FAILED → COMPENSATING → COMPENSATED ↩️
```

### 📬 Outbox Pattern
Garante que eventos **nunca se percam** entre o banco e o Kafka. O evento é salvo na mesma transação do negócio e publicado por um worker assíncrono.

### 🔑 Idempotência Absoluta
Se o cliente enviar a mesma requisição duas vezes, o sistema reconhece e retorna a resposta original — sem cobrar duas vezes.

### 📋 Audit Log Imutável
Cada centavo movimentado gera um registro com checksum SHA-256. Qualquer adulteração é detectada instantaneamente.

### ⚡ Resiliência
- **Circuit Breaker** — para de tentar quando o sistema está sobrecarregado
- **Retry com Exponential Backoff** — tenta novamente com intervalos crescentes
- **Dead Letter Queue** — mensagens problemáticas são isoladas sem parar o sistema
- **Saga Timeout Handler** — Sagas presas há mais de 30 minutos são finalizadas automaticamente

---

## 🛠️ Stack Tecnológica

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.2.5 | Framework |
| PostgreSQL | 16 | Banco de dados |
| Apache Kafka | 3.6 | Mensageria |
| Redis | 7 | Idempotência |
| Resilience4j | 2.2 | Circuit Breaker / Retry |
| OpenTelemetry | 1.31 | Distributed Tracing |
| Zipkin | 3 | Visualização de traces |
| Flyway | 9.22 | Migrations |
| Testcontainers | - | Testes de integração |

---

## 🚀 Como Executar

### Pré-requisitos
- Java 21
- Maven 3.9+
- Docker Desktop

### 1. Subir a infraestrutura
```bash
docker-compose up -d
```

### 2. Compilar o projeto
```bash
mvn clean install -DskipTests
```

### 3. Executar os testes
```bash
mvn test
```

### 4. Iniciar os serviços
```bash
# Terminal 1
java -jar account-service/target/account-service-1.0.0-SNAPSHOT.jar

# Terminal 2
java -jar transfer-service/target/transfer-service-1.0.0-SNAPSHOT.jar

# Terminal 3
java -jar reserve-service/target/reserve-service-1.0.0-SNAPSHOT.jar
```

---

## 📡 Endpoints

### Transfer Service (8080)
```bash
# Iniciar uma transferência
POST /transfers
Headers: X-Idempotency-Key: {uuid}
Body: {
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 100.00,
  "currency": "BRL"
}
```

### Account Service (8081)
```bash
# Histórico de uma conta
GET /audit/accounts/{accountId}

# Histórico de uma transferência
GET /audit/sagas/{sagaId}

# Verificar integridade do audit log
GET /audit/accounts/{accountId}/integrity
```

---

## 🔍 Observabilidade

| Ferramenta | URL | Descrição |
|---|---|---|
| Kafka UI | http://localhost:8090 | Monitorar tópicos e mensagens |
| Zipkin | http://localhost:9411 | Distributed tracing |
| Actuator | http://localhost:808x/actuator | Métricas e saúde |

---

## 🗄️ Decisões de Arquitetura

### Por que SERIALIZABLE e não REPEATABLE READ?
O isolamento SERIALIZABLE com SSI (Serializable Snapshot Isolation) do PostgreSQL detecta conflitos de escrita concorrente e aborta a transação perdedora. É o único nível que garante que dois débitos simultâneos na mesma conta não resultem em saldo negativo. Custo: ~10-15% de overhead. Cada centavo vale em Fintech.

### Por que Outbox Pattern em vez de publicar direto no Kafka?
Sem o Outbox, existe uma janela de falha entre salvar no banco e publicar no Kafka. Com o Outbox, ambos acontecem na mesma transação ACID — ou os dois persistem, ou nenhum persiste.

### Por que Redis para idempotência e não só o banco?
Redis garante atomicidade via `SETIFABSENT` em O(1). Dois requests idênticos chegando simultaneamente — apenas um registra a chave. O banco é o fallback caso o Redis esteja fora.

---

## 👩‍💻 Autora

**Hellen** [@hlyncode](https://github.com/hlyncode)

> Projeto desenvolvido como peça de portfólio, demonstrando domínio de arquitetura distribuída, padrões de resiliência e práticas de engenharia de nível sênior em sistemas financeiros críticos.

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.