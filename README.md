# Banking AI Platform

A Spring Boot multi-module implementation of the Java + AI banking architecture:
existing banking services on one plane, an AI intelligence plane (RAG, MCP,
multi-LLM gateway) on the other, connected only through controlled APIs.

> [!NOTE]
> 📖 **See [ARCHITECTURE.md](ARCHITECTURE.md) for a complete overview of the architecture, module breakdown, and design patterns.**


## What's actually implemented here

This is a **working scaffold**, not a claim that every one of the ~20 services
described in the full architecture plan is production-complete. Implemented
end-to-end, with real (compiling, tested) logic:

| Module | What it does |
|---|---|
| `common/common-api`, `common-exception`, `common-security`, `common-events` | Shared DTOs, error contract, correlation-ID propagation, PII masking, Kafka event envelope |
| `ai-platform/ai-model-gateway` | Provider-independent `AiModelClient`, real OpenAI/Claude/Gemini HTTP clients (fall back to a deterministic demo response if no API key is configured), `ModelRouter` with a task-based routing + fallback policy |
| `banking-services/account-service` | Full reference microservice: JPA entity, repository, service layer with real business rules (overdraft rejection, etc.), REST API, tests |
| `ai-platform/rag-service` | `VectorStore` abstraction + in-memory implementation, chunking/embedding ingestion pipeline, role-based (ACL) retrieval filtering, seeded demo policy docs |
| `ai-platform/knowledge-service` | Document metadata + full version history (immutable per-version content rows, DRAFT→ACTIVE→RETIRED lifecycle), separate from rag-service's embeddings. **Not yet integrated with rag-service** — see Not implemented below |
| `ai-platform/document-intelligence-service` | Keyword-based classification (KYC/loan/cheque/statement/report), regex-based structured field extraction per type, confidence scoring, low-confidence results routed to a human review queue instead of auto-accepted. **Scope note**: accepts already-OCR'd text, not raw file bytes — no virus scanning, object storage, or actual OCR in this environment |
| `ai-platform/report-insight-service` | Generates AI narrative summaries from account activity — but only from an aggregated `AccountMetrics` object, never raw transactions. `AiInsightService`'s method signature only accepts that aggregate type, so a raw transaction list structurally cannot reach the LLM prompt. Only `ACCOUNT_ACTIVITY_SUMMARY` has aggregation logic implemented |
| `ai-platform/mcp-gateway-service` | MCP-style tool registry + two tools (`getAccountBalance`, `getCustomerAccounts`) that call account-service's REST API — the LLM never touches a database directly, and financial *writes* are deliberately not exposed as tools |
| `ai-platform/ai-orchestrator-service` | The full section-5 flow: intent detection → RAG or MCP-tool routing → prompt construction → LLM call with fallback → PII/grounding response handling |
| `banking-services/customer-service` | Customer registration + KYC status lifecycle |
| `banking-services/transfer-service` | Transfer initiation with the **Transactional Outbox pattern**: the Transfer row and its outbox event commit in one local transaction; a scheduled poller publishes to Kafka afterwards |
| `banking-services/payment-service` | Payment lifecycle (INITIATED→AUTHORIZED→PROCESSING→COMPLETED/FAILED/REVERSED), **Idempotency-Key deduplication** (a retried request with the same key returns the original Payment instead of creating a duplicate), transactional outbox |
| `banking-services/transaction-service` | Immutable ledger: consumes `PAYMENT_COMPLETED`/`TRANSFER_COMPLETED` off Kafka and writes idempotent transaction records (dedup on referenceId), exposes a paginated statement API |
| `banking-services/card-service` | Card lifecycle (PENDING_ACTIVATION→ACTIVE→FROZEN→BLOCKED/CLOSED); tokenized — the full PAN is returned exactly once at issuance, only masked form + last4 ever persisted |
| `banking-services/loan-service` | Application with eligibility check (mirrors the demo loan policy seeded in rag-service), approve/reject/disburse, amortized monthly payment, repayment tracking down to auto-close |
| `banking-services/notification-service` | Kafka consumer on payment/transfer completion events, idempotent dispatch (dedup on source event id) — stands in for a real SMS/email/push provider |
| `banking-services/audit-service` | Append-only audit trail with automatic redaction of sensitive detail keys (password/token/card number/etc.) before persistence. **Not yet called by any other service** — see the Not implemented list below |
| `banking-services/fraud-service` | Deterministic business-rule fraud engine — threshold/blacklist/velocity checks via `POST /api/v1/fraud/evaluate`, each rule's pass/fail independently explained. Distinct from the Kafka-driven `fraud-ai-service` below. **Not yet called synchronously by payment-service or transfer-service** before they commit |
| `ai-platform/fraud-ai-service` | Kafka consumer implementing the fraud half of the transfer **saga**: consumes `TRANSFER_INITIATED`, runs a rule-based risk score, calls back into transfer-service to complete or fail it |
| `infrastructure/api-gateway` | Spring Cloud Gateway routing all of the above |
| `deployment/docker` | `docker-compose.yml` (Kafka + Zookeeper + Postgres + all 19 services) + a generic multi-stage `Dockerfile` |

**Not implemented** (documented as the intended shape, not built out):
Redis, real
vector-DB adapters (the `VectorStore` interface exists so adding
Pinecone/Weaviate/Milvus/Chroma is one new class each), real Keycloak/OIDC
(the gateway's demo JWT issuer is a stand-in, not this),
Kubernetes/Helm manifests, AWS wiring, and CI/CD. The `task.md` in this
repo tracks phase-by-phase status honestly, including partial/incomplete
items — read it before assuming something is done.

## Persistence: PostgreSQL per service + Flyway

Every one of the 13 stateful services (customer, account, transfer,
payment, transaction, card, loan, notification, audit, fraud, knowledge,
document-intelligence, report-insight) has its **own dedicated PostgreSQL
database** — never a shared instance — and Flyway-managed schema
migrations (`src/main/resources/db/migration/`). `docker-compose.yml`
spins up 13 separate `postgres:16-alpine` containers for this. Every
container runs Postgres on its own standard port, 5432 (that's what
"default port" means at the container level); host-side port mappings
differ per container (5432, 5433, 5434, ...) purely so you can also reach
each one individually from your laptop without a collision. Inside the
Docker network, every service reaches its database at
`<service>-db:5432` — see each service's `application.yml`.

`spring.jpa.hibernate.ddl-auto` is `validate` everywhere persistence
exists: Flyway owns schema creation (`V1__init_schema.sql` per service,
hand-verified column-for-column against its JPA entities) and Hibernate
only checks at startup that the entities match — it refuses to start
rather than silently altering a table if they've drifted.
`fraud-service`'s `V2__seed_reference_blacklist.sql` is a concrete example
of Flyway handling **data insertion**, not just schema DDL.

**Tests don't need a live Postgres**: `src/test/resources/application.yml`
overrides the Postgres config with per-service H2 in-memory databases and
disables Flyway (Spring Boot puts `src/test/resources` ahead of
`src/main/resources` on the test classpath). This is a documented
tradeoff — tests verify business logic against H2, not the literal schema
Flyway would create in Postgres. A stricter setup would use Testcontainers
to run integration tests against a real disposable Postgres; not
implemented here.

## Security: encryption, HMAC, and Keycloak

- **`common-crypto`** (new module, zero Spring dependencies) provides
  AES-256-GCM field encryption and HMAC-SHA256 signing — usable from any
  service, servlet or reactive, without the classpath conflicts
  `common-security` would introduce.
- **Encrypted PII at rest**: `customer-service` can store a `nationalId`
  encrypted; the default `GET /customers/{id}` response has no field
  capable of holding it, encrypted or plaintext. Decryption only happens
  via `GET /customers/{id}/national-id`, restricted to
  `COMPLIANCE_OFFICER`/`ADMIN`.
- **HMAC-signed callback**: fraud-ai-service's report back to
  transfer-service (`POST /transfers/{id}/fraud-decision`) now requires a
  valid `X-Signature` header, closing what was otherwise an open door to
  bypass fraud review entirely by calling that endpoint directly.
- **Keycloak**: `api-gateway` supports real OAuth2 resource-server JWT
  validation via `--spring.profiles.active=keycloak`, alongside (not
  replacing) the default demo-JWT mode. Bring up Keycloak with:
  ```bash
  docker compose -f deployment/docker/docker-compose.yml --profile keycloak up keycloak
  ```
  Then get a token directly from Keycloak (same 5 demo users/passwords as
  the default mode, for continuity):
  ```bash
  curl -X POST http://localhost:8180/realms/banking/protocol/openid-connect/token \
    -d "grant_type=password&client_id=banking-gateway&username=staff1&password=password"
  ```
  Run `api-gateway` with `SPRING_PROFILES_ACTIVE=keycloak` and use the
  returned `access_token` as your bearer token instead of the demo
  `/api/v1/auth/login` token. **This integration is hand-reviewed for
  correctness against Spring Security's documented API, but has not been
  run against a live Keycloak instance in this environment** — verify it
  yourself before relying on it in a real setting.

## Observability: Prometheus + Grafana + Zipkin

All 18 deployable services export Micrometer metrics at
`/actuator/prometheus` and distributed traces (via
`micrometer-tracing-bridge-otel` + `opentelemetry-exporter-zipkin`) to a
Zipkin container. `docker-compose.yml` brings up all three:

- **Prometheus** (`:9090`) scrapes every service every 15s — config in
  `deployment/docker/prometheus/prometheus.yml`, one job per service.
- **Zipkin** (`:9411`) collects traces; open it and search by service name
  to see a request's full path across the gateway and every service it hit.
- **Grafana** (`:3000`, anonymous admin access for local dev) has
  Prometheus pre-provisioned as a datasource — no manual setup — but ships
  with **no dashboards**; you'd build or import your own.

Every service's logs now include `traceId`/`spanId` alongside the existing
`correlationId`, so a request can be correlated across log lines, metrics,
and traces using the same identifiers. One exception: `api-gateway` never
picked up the `correlationId`-populating filter from `common-security`
(it can't depend on that module — see the Security section in `task.md`
for why), so `correlationId` is always empty in its logs specifically;
`traceId`/`spanId` still work there since Micrometer instruments it
independently.

**Not implemented**: custom AI-specific metrics (plan section 33 calls out
token usage, model latency, RAG retrieval latency — what exists is generic
HTTP/JVM auto-instrumentation only), Grafana dashboards, and structured
JSON log output (logs are plain-text patterns, not actual JSON).

## Running it locally

**Option A — plain Maven, one terminal per service (fastest to iterate on).**
Requires Postgres running for any service with persistence — either
`docker compose -f deployment/docker/docker-compose.yml up account-db
customer-db ...` (whichever DBs you need) or point each service's
`application.yml` datasource at your own local Postgres:
```bash
mvn -q -DskipTests install               # build all modules once
cd banking-services/account-service   && mvn spring-boot:run &   # :8081
cd ai-platform/rag-service            && mvn spring-boot:run &   # :8082
cd ai-platform/mcp-gateway-service    && mvn spring-boot:run &   # :8083
cd ai-platform/ai-orchestrator-service&& mvn spring-boot:run &   # :8080
cd infrastructure/api-gateway         && mvn spring-boot:run &   # :8000
```

**Option B — Docker Compose (recommended — brings up all 13 databases too):**
```bash
docker compose -f deployment/docker/docker-compose.yml up --build
```

### Try it

Open an account:
```bash
curl -X POST http://localhost:8081/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-1","accountType":"SAVINGS","openingBalance":1000.00,"currency":"USD"}'
```

Ask the AI assistant for a balance (use the `accountId` from the response above):
```bash
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"C1","userId":"CUST-1","userRoles":["CUSTOMER"],
       "query":"What is my account balance?","context":{"accountId":"<accountId>"}}'
```

Ask a policy question (RAG-grounded, no live LLM key needed — runs in demo mode):
```bash
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"C2","userId":"CUST-1","userRoles":["CUSTOMER"],
       "query":"What are the eligibility rules for personal loans?"}'
```

### Log in and call a role-protected endpoint (JWT + RBAC through the gateway)

The examples above call services directly on their own ports — that still
works with no token at all, since nothing enforces that only the gateway
can reach those ports (see the Security section in `task.md`). To actually
exercise JWT auth and RBAC, go through the gateway (`:8000`) instead:

```bash
# Get a token (demo users: customer1/staff1/analyst1/compliance1/admin1, password "password")
TOKEN=$(curl -s -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"staff1","password":"password"}' | jq -r .accessToken)

# A request through the gateway with no token is rejected before it's routed:
curl -i http://localhost:8000/api/v1/loans/some-id  # -> 401

# With a valid token it's routed through (loan must exist first via POST /api/v1/loans):
curl -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/loans/<loanId>

# staff1 has BANK_STAFF, so this succeeds:
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/loans/<loanId>/approve

# customer1 does NOT have BANK_STAFF -- the same call with their token gets a 403:
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer1","password":"password"}' | jq -r .accessToken)
curl -i -X POST -H "Authorization: Bearer $CUSTOMER_TOKEN" http://localhost:8000/api/v1/loans/<loanId>/approve  # -> 403
```

Only 3 endpoints across the whole platform currently enforce a role this
way (loan approve/reject/disburse, card block/close, fraud blacklist-add) —
see `task.md`'s Security section for the full picture of what is and isn't
covered.

### Try the transfer saga (outbox + Kafka + fraud-ai-service)

Requires Kafka running (`docker compose up kafka zookeeper` if not using
full Compose). Initiate a transfer:
```bash
curl -X POST http://localhost:8084/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountId":"ACC-1","destinationAccountId":"ACC-2","amount":250.00,"currency":"USD"}'
```
This writes the `Transfer` row and an outbox event in the same local
transaction (status: `PENDING_FRAUD_REVIEW`), then `OutboxPublisher` picks it
up within ~2s and publishes `TRANSFER_INITIATED` to
`banking.transfer.events`. `fraud-ai-service` consumes it, scores the amount
(>= 10,000 → HIGH risk, >= 5,000 → MEDIUM, else LOW), and calls back
`POST /api/v1/transfers/{id}/fraud-decision`. Poll the transfer to watch the
status move to `COMPLETED` (or `FAILED` for a high-value transfer):
```bash
curl http://localhost:8084/api/v1/transfers/<transferId>
```

### Using real LLM providers

By default all three providers run in **demo mode** (no network calls, no
cost) so the platform is runnable out of the box. To use a real model, in
`ai-orchestrator-service/src/main/resources/application.yml` (or via env
vars, e.g. `BANK_AI_PROVIDERS_OPENAI_ENABLED=true` /
`BANK_AI_PROVIDERS_OPENAI_API-KEY=...`), enable the provider and supply a key
— never commit a key to git; source it from Vault/Secrets Manager in real
deployments.

## Key architectural rules encoded in the code (not just docs)

- **AI never touches a banking database directly.** `mcp-gateway-service`'s
  tools call `account-service`'s REST API; `ai-orchestrator-service` never
  autowires a repository.
- **No financial write is exposed as an AI tool.** `McpTool` implementations
  in this repo are read-only by design (see the Javadoc on `McpTool`).
- **Provider independence.** Nothing outside `ai-model-gateway` imports an
  OpenAI/Claude/Gemini-specific type; everything else depends on
  `AiModelClient` / `ModelRouter`.
- **Guardrails are code, not just a diagram.** `AbstractAiModelClient` rejects
  a small set of prompt-injection patterns before any provider call;
  `ResponseHandlerService` masks PII and flags ungrounded answers.
