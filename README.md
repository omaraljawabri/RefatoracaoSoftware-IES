# RefatoracaoSoftware-IES

Repositório da atividade de refatoração da disciplina Implantação e Entrega de Software (2026/2),
INF/UFG. Parte do código legado `SubscriptionBillingService` da SaaS Billing Co. e chega a uma
arquitetura de portas e adaptadores, sem mudar as regras de negócio.

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
