# RefatoracaoSoftware-IES

Repositório da atividade de refatoração da disciplina Implantação e Entrega de Software (2026/2).
Parte do código legado `SubscriptionBillingService` da SaaS Billing Co. e chega a uma arquitetura
de portas e adaptadores, sem mudar as regras de negócio.

## Requisitos

- Java 21
- Maven 3.9 ou mais recente

## Testes

```bash
mvn test
```

Os testes não precisam de banco de dados, servidor SMTP nem variáveis de ambiente: o serviço é
testado com fakes em memória e com um `Clock` fixo. A CI roda o mesmo comando em cada PR.

No IntelliJ, basta abrir a pasta: o `pom.xml` é detectado automaticamente.

### Convenção de nomes dos testes

- Método: `metodo_oQueFaz_quandoCondicao`, em que `metodo` é o método de produção testado
  (`constructor` para construtores). Exemplo: `calculate_ignoraCupom_quandoBlackFridayForaDeNovembro`.
- `@DisplayName`: `"metodo: o que faz quando condição"`. Exemplo:
  `"calculate: ignora o cupom quando BLACKFRIDAY é usado fora de novembro"`.
- Testes que documentam um comportamento mantido do legado terminam o `@DisplayName` com
  `(como no legado)`.

## Configuração para rodar com a infraestrutura real

`BillingServiceFactory.fromEnvironment()` monta o serviço com PostgreSQL e SMTP e lê a
configuração destas variáveis de ambiente:

| Variável | O que guarda | Exemplo |
|---|---|---|
| `BILLING_DB_URL` | URL JDBC do banco | `jdbc:postgresql://localhost:5432/saas_db` |
| `BILLING_DB_USER` | Usuário do banco | `admin` |
| `BILLING_DB_PASSWORD` | Senha do banco | (não versionar) |
| `BILLING_SMTP_HOST` | Servidor SMTP | `smtp.empresa.com` |
| `BILLING_SMTP_PORT` | Porta SMTP | `587` |
| `BILLING_SMTP_FROM` | Remetente dos recibos | `billing@empresa.com` |

Se alguma estiver ausente, a criação do serviço falha com uma mensagem que diz qual variável falta.

**Linux/macOS** (vale para o terminal atual; para tornar permanente, coloque no `~/.bashrc`):

```bash
export BILLING_DB_URL=jdbc:postgresql://localhost:5432/saas_db
export BILLING_DB_USER=admin
export BILLING_DB_PASSWORD='sua-senha'
export BILLING_SMTP_HOST=smtp.empresa.com
export BILLING_SMTP_PORT=587
export BILLING_SMTP_FROM=billing@empresa.com
```

**Windows (PowerShell):** `$env:BILLING_DB_URL = "jdbc:postgresql://localhost:5432/saas_db"`, e
assim por diante.

**IntelliJ:** Run → Edit Configurations… → Environment variables.

## Uso

```java
SubscriptionBillingService service = BillingServiceFactory.fromEnvironment();

BillingResult result = service.processMonthlyBilling(
        BillingCommand.from("cus_123", 99.90, "2024-01-31", 0, "WELCOME10", false, "tok_visa"));

result.amount();    // valor cobrado
result.messages();  // mensagens do processamento
```

Falhas de banco ou de e-mail lançam `InfrastructureException`.
