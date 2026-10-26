# Relatório Técnico: Refatoração do SubscriptionBillingService

**Disciplina:** Implantação e Entrega de Software (2026/2), INF/UFG
**Dupla:** Omar Al Jawabri e Stephano Soares Viglio

## 1. Diagnóstico do débito técnico

As linhas citadas se referem ao código legado original (`SubscriptionBillingService.java`, commit `36d0970`).

### 1.1 Matriz de code smells e violações arquiteturais

| # | Trecho | Code smell / princípio violado | Juros (impacto na manutenção e na entrega contínua) | Onde foi tratado |
|---|---|---|---|---|
| 1 | L18–105 (`processMonthlyBilling`) | **Long Method / God Method**: 88 linhas que calculam o preço, consultam o banco, cobram, gravam a fatura e enviam o e-mail | Qualquer mudança de regra mexe no mesmo método, o que gera conflitos de merge e revisões longas. Não dá para testar uma regra isoladamente, então cada entrega depende de teste manual | Etapa 4: `PricingCalculator` e Extract Method (`bill`, `charge`) |
| 2 | L18–20 | **Long Parameter List** (8 parâmetros) e **parâmetro morto** (`planType` nunca é usado) | Chamadas frágeis: trocar de lugar dois `String` ou dois `int` compila sem erro. O parâmetro morto engana quem lê e obriga os chamadores a informar um valor inútil | Etapa 2: `BillingCommand` (Introduce Parameter Object, Remove Parameter) |
| 3 | L18, L29–57, L67, L86 | **Primitive Obsession**: `double` para dinheiro e `String` para data, cupom e status | `double` gera valores como `59.49999999999999`, que vão para o banco e para o e-mail. Validação e conversão ficam espalhadas e cada novo ponto de uso repete o problema | Etapa 2: `Money`, `CustomerStatus`, `InvoiceStatus`, `PromoCode` |
| 4 | L25, L42 | **Dependência oculta do relógio do sistema** (`LocalDate.now()`) | Os cálculos dependem do dia em que o código roda: o cupom `BLACKFRIDAY` só pode ser testado em novembro, e os testes ficam instáveis no CI | Etapa 3: `Clock` injetado |
| 5 | L14–16, L59 | **Violação do Dependency Inversion Principle / I/O direto** (`DriverManager`) e **credenciais fixas no código** | Não há teste unitário sem um PostgreSQL rodando, o que impede a CI de validar a regra de negócio. A senha fica versionada no Git, e trocar de banco ou de ambiente exige alterar e reimplantar a classe de negócio | Etapa 3: `CustomerRepository`, `InvoiceRepository` e variáveis de ambiente |
| 6 | L89–96 | **I/O direto com SMTP** (`new SimpleEmail()`) e **código morto** (`// emailSender.send();`) | O e-mail nunca é enviado, mas o log diz "E-mail disparado": um defeito silencioso em produção. Não há como testar a notificação sem um servidor SMTP | Etapa 3: `NotificationService`; a linha comentada foi removida |
| 7 | L100–102 | **Erro mascarado** (`catch (Exception e)` genérico) | Falhas de banco ou de e-mail viram uma linha de texto numa lista. Não há alerta, nova tentativa nem rollback, e o problema não aparece no monitoramento | Etapa 4: `InfrastructureException` propagada |
| 8 | L59–99 | **Violação do Single Responsibility Principle**: preço, persistência, pagamento e notificação na mesma classe | A classe tem quatro motivos para mudar. Uma alteração no e-mail exige reimplantar e testar de novo o cálculo de preço | Etapas 3 e 4: portas, adaptadores e `PricingCalculator` |
| 9 | L31–52, L75 | **Magic Numbers / Magic Strings** (`0.15`, `0.70`, `30.0`, `0.02`, `0.001`, `"tok_"`) | As regras comerciais ficam escondidas em literais. Mudar uma alíquota exige procurar números pelo código, com risco de alterar o errado | Etapa 4: constantes nomeadas no `PricingCalculator`; `"tok_"` no `TokenPrefixPaymentGateway` |
| 10 | L61, L83 | **Vazamento de recursos**: `PreparedStatement` e `ResultSet` nunca são fechados | Sob carga, os cursores do banco se esgotam e o sistema falha em produção de um jeito difícil de reproduzir em teste | Etapa 3: `try-with-resources` nos adaptadores JDBC |
| 11 | L18, L22, L104 | **Retorno com significado misturado**: um `List<String>` serve ao mesmo tempo de resultado e de log | Quem chama precisa comparar textos para saber se a cobrança deu certo, e mudar uma mensagem quebra os chamadores | Etapa 2: `BillingResult` (valor + mensagens) |

### 1.2 Regras de negócio preservadas

| # | Regra | Trecho do legado | Onde está hoje | Testes |
|---|---|---|---|---|
| R1 | Meses de assinatura = diferença de ano × 12 + diferença de mês, **sem olhar o dia** | L27 | `PricingCalculator.monthsActive` | `PricingCalculatorTest` |
| R2 | Fidelidade: mais de 24 meses → 15% sobre o preço base; mais de 12 meses → 10% | L31–35 | `PricingCalculator` | `PricingCalculatorTest`, `SubscriptionBillingServiceTest` |
| R3 | ONG: 30% de desconto sobre o preço **já com fidelidade** | L37–39 | `PricingCalculator` | `PricingCalculatorTest` |
| R4 | `BLACKFRIDAY`: menos R$ 30,00, só em novembro | L42–43 | `PricingCalculator` + `PromoCode` | `PricingCalculatorTest`, `SubscriptionBillingServiceTest` |
| R5 | `WELCOME10`: 10% de desconto com até 1 mês de assinatura | L44–45 | `PricingCalculator` + `PromoCode` | `PricingCalculatorTest` |
| R6 | No máximo um cupom; o código não diferencia maiúsculas; cupom nulo, vazio ou desconhecido é ignorado | L41–47 | `PromoCode.from` | `PromoCodeTest` |
| R7 | Atraso: multa de 2% + juros simples de 0,1% ao dia, sobre o preço já com descontos | L49–53 | `PricingCalculator` | `PricingCalculatorTest` |
| R8 | Piso zero, aplicado depois da multa | L55–57 | `PricingCalculator` | `PricingCalculatorTest` |
| R9 | Só o status `ACTIVE` (sem diferenciar maiúsculas) é ativo; cliente inativo tem a cobrança cancelada | L69–72 | `CustomerStatus.from`, `SubscriptionBillingService.bill` | `CustomerStatusTest`, `SubscriptionBillingServiceTest` |
| R10 | Pagamento aprovado quando o token existe e começa com `tok_` | L75–80 | `TokenPrefixPaymentGateway` | `TokenPrefixPaymentGatewayTest` |
| R11 | A fatura é gravada com `PAID` ou `FAILED` conforme o pagamento | L82–87 | `InvoiceStatus`, `InvoiceRepository` | `SubscriptionBillingServiceTest` |
| R12 | A notificação do recibo é solicitada depois da gravação da fatura | L89–98 | `NotificationService` | `SubscriptionBillingServiceTest` |

Além dos testes por regra, há um teste de comparação com o legado
(`PricingCalculatorLegacyComparisonTest`): ele calcula o preço com o `PricingCalculator` e com uma
cópia literal da fórmula do legado em 18.720 combinações de preço, meses de assinatura, ONG, cupom,
dias de atraso e data, e verifica se os dois resultados ficam a no máximo meio centavo um do outro.

### 1.3 Comportamentos questionáveis preservados de propósito

A refatoração não muda regra de negócio. Os comportamentos abaixo parecem defeitos, mas foram
mantidos e documentados em testes. Corrigi-los mudaria o comportamento do sistema e fica fora do
escopo desta atividade: eles permanecem como débito técnico remanescente.

| # | Comportamento atual | Trecho do legado | Teste que documenta |
|---|---|---|---|
| C1 | Meses de assinatura ignoram o dia: de 31/01 a 01/02 conta 1 mês | L27 | `PricingCalculatorTest.monthsActive_contaUmMes_quandoVaiDe31DeJaneiroA1DeFevereiro` |
| C2 | Cupom com espaços (`" BLACKFRIDAY"`) não é reconhecido, porque não há `trim` antes da comparação | L42–44 | `PromoCodeTest.from_retornaVazio_quandoCodigoTemEspacos` |
| C3 | `WELCOME10` vale para adesão com data futura (meses de assinatura negativos) | L44 | `PricingCalculatorLegacyComparisonTest` |
| C4 | Data de adesão inválida lança `DateTimeParseException` sem mensagem amigável | L24 | `BillingCommandTest` |
| C5 | Cliente inexistente não gera cobrança nem mensagem nenhuma | L65 | `SubscriptionBillingServiceTest.processMonthlyBilling_naoFazNada_quandoClienteInexistente` |
| C6 | O preço é calculado mesmo para cliente inativo | L29–72 | `SubscriptionBillingServiceTest.processMonthlyBilling_cancelaSemCobrar_quandoClienteInativo` |
| C7 | A fatura é gravada e o recibo é enviado mesmo com o pagamento recusado | L74–98 | `SubscriptionBillingServiceTest.processMonthlyBilling_gravaFailedEEnviaRecibo_quandoPagamentoRecusado` |
| C8 | O e-mail nunca é enviado de fato (`send()` estava comentado) | L96 | `CommonsEmailNotificationServiceTest` |

## 2. Refatoração

### 2.1 Arquitetura resultante

```
com.saas.billing
├── SubscriptionBillingService      orquestrador: calcula, cobra, grava e notifica
├── domain                          regras puras, sem I/O
│   ├── Money, BillingCommand, BillingResult
│   ├── Customer, CustomerStatus, Invoice, InvoiceStatus, PromoCode
│   └── PricingCalculator
├── port                            interfaces que o serviço usa (portas)
│   ├── CustomerRepository, InvoiceRepository, PaymentGateway, NotificationService
│   └── exception.InfrastructureException
├── adapter                         implementações com infraestrutura real
│   ├── jdbc     JdbcConnectionFactory, JdbcCustomerRepository, JdbcInvoiceRepository
│   ├── payment  TokenPrefixPaymentGateway
│   └── email    SmtpSettings, CommonsEmailNotificationService
└── config.BillingServiceFactory    monta o serviço com os adaptadores reais
```

As dependências apontam sempre para dentro: os adaptadores dependem das portas e do domínio, e o
serviço depende só do domínio e das portas. Nos testes, as portas são implementadas por fakes em
memória (`src/test/java/com/saas/billing/fake`), e o `Clock` é fixo.

O enunciado pede três portas. Criamos uma quarta, `InvoiceRepository`, porque colocar a gravação
da fatura dentro do `CustomerRepository` violaria o Single Responsibility Principle.

### 2.2 Refatorações do catálogo aplicadas

| Refatoração (catálogo de Fowler) | Onde | Ação equivalente no IntelliJ |
|---|---|---|
| Introduce Parameter Object | 8 parâmetros → `BillingCommand` | Refactor → Introduce Parameter Object |
| Remove Parameter | `planType`, que não era usado | Refactor → Change Signature (Ctrl+F6) |
| Replace Primitive with Object | `double` → `Money`; `String` → `LocalDate` | Refactor → Type Migration (Ctrl+Shift+F6) |
| Replace Type Code with Class | textos de status e cupom → `CustomerStatus`, `InvoiceStatus`, `PromoCode` | Refactor → Type Migration |
| Extract Interface | `CustomerRepository`, `InvoiceRepository`, `PaymentGateway`, `NotificationService` | Refactor → Extract Interface |
| Extract Class | `PricingCalculator` e os adaptadores JDBC, de pagamento e de e-mail | Refactor → Extract Delegate |
| Parameterize Function / injeção de dependência | `Clock` e portas recebidos no construtor | Refactor → Introduce Field / Change Signature |
| Replace Magic Literal | alíquotas, prazos e prefixo do token viraram constantes nomeadas | Refactor → Introduce Constant (Ctrl+Alt+C) |
| Extract Method | `bill` e `charge` no serviço; `apply*` no `PricingCalculator` | Refactor → Extract Method (Ctrl+Alt+M) |
| Rename Variable | `logs` → `messages`, `calculatedPrice` → `amount` | Refactor → Rename (Shift+F6) |
| Remove Dead Code | `// emailSender.send();` e as credenciais fixas | Safe Delete (Alt+Delete) |
| Replace Error Code with Exception | `catch (Exception)` + mensagem → `InfrastructureException` | — |

### 2.3 Histórico no Git

A dupla trabalhou em paralelo, com uma branch e uma PR por item, integradas na `main` com
*Rebase and merge* para manter o histórico linear. Por isso os commits aparecem na ordem em que
foram integrados, e não agrupados por etapa: o prefixo `tipo(etapa-N)` de cada mensagem identifica
a etapa.

| Etapa da atividade | Commits |
|---|---|
| 1. Código legado | `chore: adiciona código legado do SubscriptionBillingService` |
| 1. Base do projeto | `build(etapa-1): converte o projeto para Maven`, `ci(etapa-1): adiciona workflow de testes` |
| 2. Value objects e Primitive Obsession | `cria value object Money`, `cria BillingCommand`, `cria BillingResult`, `cria enum CustomerStatus`, `cria enum InvoiceStatus`, `cria enum PromoCode`, `introduz Money e BillingCommand no serviço`, `usa CustomerStatus/InvoiceStatus/PromoCode no serviço` |
| 3. Interfaces de infraestrutura e injeção de dependência | `cria InfrastructureException`, entidades `Customer` e `Invoice`, portas `CustomerRepository`, `InvoiceRepository`, `PaymentGateway`, `NotificationService`, adaptadores e fakes, `move credenciais do banco para variáveis de ambiente`, `injeta Clock/PaymentGateway/NotificationService/CustomerRepository/InvoiceRepository no serviço`, `cria BillingServiceFactory`, testes do serviço com fakes |
| 4. Motor de precificação e suíte verde | `extrai PricingCalculator`, `arredonda o preço só no final do cálculo`, `compara PricingCalculator com a fórmula do legado`, `delega o cálculo de preço ao PricingCalculator`, `extrai o método charge/bill`, `remove catch genérico e propaga falhas de infraestrutura`, `padroniza nomes dos testes de ...` (um por classe de teste) |

Toda PR passou na CI antes de ser integrada. A CI (`.github/workflows/ci.yml`) roda a suíte em cada
PR e em cada push na `main`, sem banco de dados nem servidor SMTP.

### 2.4 Mudanças intencionais de comportamento

Estas são as únicas diferenças observáveis em relação ao legado. Todas foram decididas de
propósito e estão cobertas por testes.

| # | Antes | Depois | Motivo |
|---|---|---|---|
| M1 | Valores em `double`, sem arredondamento: mensagens e banco recebiam algo como `59.49999999999999` | Valores com 2 casas (`HALF_EVEN`, o arredondamento bancário): mensagens no formato `R$ 59,50` e fatura gravada com `setBigDecimal` | Eliminar o erro de ponto flutuante em valores cobrados. O cálculo continua igual: o resultado fica a no máximo meio centavo do valor do legado, o que é verificado pelo teste de comparação com o legado |
| M2 | `processMonthlyBilling(String, String, double, String, int, String, boolean, String)` devolvendo `List<String>` | `processMonthlyBilling(BillingCommand)` devolvendo `BillingResult` | Remover a lista longa de parâmetros, o parâmetro morto `planType` e o retorno ambíguo. Quem chamava com texto usa `BillingCommand.from(...)`, que faz as mesmas conversões do legado |
| M3 | Credenciais e servidores fixos no código | Configuração por variáveis de ambiente (ver o README). Variável ausente gera `IllegalStateException` com o nome dela | Tirar a senha do Git e permitir trocar de ambiente sem recompilar |
| M4 | Uma conexão aberta para a consulta e o `INSERT`, com `PreparedStatement` e `ResultSet` sem fechar | Cada repositório abre e fecha a própria conexão e todos os recursos | Corrigir o vazamento de recursos. Como o legado usava autocommit, não havia transação compartilhada a perder |
| M5 | Qualquer exceção virava a mensagem `"Erro fatal no processamento: ..."` e o método terminava normalmente | Falhas de banco, pagamento ou e-mail são convertidas em `InfrastructureException` e chegam a quem chamou | Exigência do enunciado: não esconder falhas de infraestrutura. Quem chama pode registrar, alertar ou tentar de novo |
| M6 | Endereço de e-mail inválido virava a mensagem `"Erro fatal..."` | Vira `InfrastructureException` com a `EmailException` como causa | Consequência de M5 |

O que **não** mudou de propósito: o e-mail continua sem ser enviado (comportamento C8); a linha
comentada só foi removida do código. O `created_at` da fatura continua vindo do `NOW()` do banco.

### 2.5 Métricas antes × depois

Contagem manual sobre o código do commit `36d0970` (antes) e da `main` ao fim da etapa 4 (depois).
A complexidade ciclomática conta 1 + cada `if`, `else if`, `for`, `case`, `&&`, `||`, `?:` e `catch`.

| Métrica | Antes | Depois |
|---|---|---|
| Linhas do método principal (`processMonthlyBilling`) | 88 | 13 |
| Parâmetros do método principal | 8 (1 sem uso) | 1 (`BillingCommand`) |
| Complexidade ciclomática do método principal | 18 | 1 |
| Maior complexidade ciclomática entre os métodos de produção | 18 | 5 (`PricingCalculator.applyPromoCode`) |
| Responsabilidades da classe de serviço | 4 (preço, banco, pagamento, e-mail) | 1 (orquestração) |
| Dependências concretas de infraestrutura no serviço | 3 (JDBC, SMTP, relógio do sistema) | 0 |
| Credenciais e endereços fixos no código | 6 (URL, usuário e senha do banco; host, porta e remetente do SMTP) | 0 |
| Literais de regra de negócio sem nome no cálculo | 13 | 0 |
| Blocos `catch (Exception)` genéricos | 1 | 0 |
| Recursos JDBC sem fechar | 2 | 0 |
| Testes automatizados | 0 | 114 execuções (93 métodos em 18 classes) |
| Testes que exigem banco de dados ou SMTP | — | 0 |
| Classes de produção | 1 | 22 |
