# Enterprise Banking + AI Platform Tasks

Tracking against the master prompt in task.md. Status reflects what is
actually implemented as real, compiling code in this repo — not aspirational.

- [x] Phase 1: Project structure, Common modules
      (common-api, common-exception, common-security, common-events)
- [x] Phase 2: API Gateway, routing, JWT authentication
      (Spring Cloud Gateway; JwtAuthenticationGlobalFilter now enforces
      "Authorization: Bearer <jwt>" on every route except /api/v1/auth/**
      and /actuator/health -- see Security section below for what this
      does and doesn't cover)
- [x] Phase 3: Customer Service, Account Service
  - [x] Account Service
  - [x] Customer Service
- [x] Phase 4: Payment, Transfer, Transaction Services
  - [x] Payment Service — lifecycle (INITIATED->AUTHORIZED->PROCESSING->COMPLETED/FAILED/REVERSED), Idempotency-Key dedup, transactional outbox
  - [x] Transfer Service — initiate/get + fraud-decision callback endpoint
  - [x] Transaction Service — ledger consuming PAYMENT_COMPLETED and TRANSFER_COMPLETED off Kafka, idempotent on referenceId, statement query API
- [x] Phase 5: Kafka, Outbox, Saga, Idempotency
  - [x] Transactional Outbox (transfer-service AND payment-service: OutboxEvent + OutboxPublisher poller)
  - [x] Saga (partial): transfer-service publishes TRANSFER_INITIATED ->
        fraud-ai-service consumes, scores, calls back -> transfer-service
        marks COMPLETED/FAILED. Compensation/reversal step is a documented
        no-op (account-service isn't wired to actually debit yet).
  - [x] Idempotency-Key header handling — implemented in payment-service (dedup on the key); transaction-service consumers are separately idempotent on referenceId
- [x] Phase 6: Card, Loan, Notification, Audit, Fraud (business-rule fraud-service, distinct from fraud-ai-service)
  - [x] Card Service — issue (tokenized: full PAN returned once, only masked+last4 stored), activate/freeze/unfreeze/block/close lifecycle
  - [x] Loan Service — apply with eligibility check (mirrors the demo loan policy seeded in rag-service), approve/reject/disburse, amortized monthly payment, repayment down to auto-close
  - [x] Notification Service — Kafka consumer on payment/transfer completion events, idempotent dispatch (dedup on source event id), query API
  - [x] Audit Service — append-only audit trail; AuditEventKafkaConsumer now consumes from
        banking.audit.events; AuditEventPublisher (common-events) fires on every key action
        in payment-service, transfer-service, customer-service, account-service, card-service.
  - [x] Fraud Service (business-rule engine) — now wired into payment-service and transfer-service
        as a synchronous pre-check before transactions complete; fail-open when fraud-service is
        unreachable, with FRAUD_CHECK_SKIPPED audit note. The async fraud-ai saga for transfers
        remains the deeper second line of defence.
- [x] Phase 7: AI Model Gateway (OpenAI, Claude, Gemini) — real HTTP clients + demo-mode fallback + router
- [x] Phase 8: RAG Service, Vector DB abstraction — in-memory VectorStore implemented; Pinecone/Weaviate/Milvus/Chroma adapters documented as stubs, not implemented
  - [x] Knowledge Service — document metadata + full version history; lifecycle (DRAFT→ACTIVE→RETIRED).
        **Gap filled**: publish()/publishNewVersion() now calls RagIngestionClient to chunk and POST
        the document content to rag-service's vector store; retire() deletes the chunks.
        Ingestion failure is logged but does NOT roll back the publish transaction.
- [/] Phase 9: Document Intelligence — classification (keyword-scoring), structured field extraction (regex-based, per document type: KYC/loan/cheque/statement), confidence scoring, and low-confidence-to-human-review routing are all real and tested. NOT implemented: virus scanning, object storage, actual OCR/parsing of raw files (this service accepts already-extracted text, not file bytes -- see its pom.xml scope note), and the chunking/embedding/vector-DB tail of the pipeline (that would hand off to rag-service, not wired up).
- [x] Phase 10: MCP Gateway, Banking Tools — tool registry + 2 read-only tools (getAccountBalance, getCustomerAccounts)
- [x] Phase 11: AI Banking Assistant, Fraud AI, Report/Insight AI
  - [x] AI Banking Assistant (ai-orchestrator-service: intent detection -> RAG or MCP tool -> response handling)
  - [x] Fraud AI (fraud-ai-service: consumes transfer events, rule-based risk scoring, saga callback)
  - [x] Report/Insight AI (report-insight-service: MetricsAggregationService reduces transaction-service's raw statement rows to an AccountMetrics aggregate FIRST -- AiInsightService's method signature only accepts that aggregate type, so a raw transaction list structurally cannot reach the LLM call; ai-model-gateway used as a library, same as ai-orchestrator-service). Only ACCOUNT_ACTIVITY_SUMMARY has aggregation logic; CUSTOMER_BEHAVIOR_ANALYSIS and RISK_SUMMARY report types are defined in the enum but rejected with a clear error, not silently accepted.
- [x] Phase 12: Observability (Prometheus, Grafana, OpenTelemetry) — every one of the 18 deployable services now exports Micrometer metrics via `/actuator/prometheus` (Prometheus scrapes all 18, config in `deployment/docker/prometheus/prometheus.yml`) and distributed traces via `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-zipkin` to a Zipkin container; Grafana is wired up with Prometheus pre-provisioned as a datasource (no manual UI setup needed) but ships with zero dashboards -- you'd build/import your own. Every service's log pattern now includes `traceId`/`spanId` alongside the existing `correlationId`. **Known gap**: `api-gateway` never got the `CorrelationIdFilter` from `common-security` (it can't depend on that module -- see the Security section above, servlet-vs-reactive-stack conflict), so `correlationId` is always empty in its logs specifically; `traceId`/`spanId` still populate there via Micrometer's own instrumentation, which is independent of that filter.
- [/] Phase 13: Testing — unit/integration tests exist for account-service, customer-service, transfer-service (outbox write), payment-service (idempotency dedup), transaction-service (idempotent ledger write), card-service (PAN masking + lifecycle), loan-service (eligibility + repayment), notification-service (idempotent dispatch), audit-service (redaction + query), fraud-service (threshold/blacklist/velocity rules), knowledge-service (versioning + lifecycle), document-intelligence-service (classification + extraction + confidence routing + review), report-insight-service (aggregation math + prompt-only-contains-aggregate verification); no contract/security/AI-eval/performance tests
- [/] Phase 14: Docker — docker-compose + generic Dockerfile covering all 19 running services; not yet built/verified in this environment (no network access to Maven Central)
- [x] Phase 15: Kubernetes + Helm
  - [x] Single reusable `banking-microservice` Helm chart (deployment/helm/banking-microservice/)
  - [x] Chart templates: Deployment (rolling update, dual-port, checksums), Service, ConfigMap,
        Secret, HPA (autoscaling/v2), PodDisruptionBudget, ServiceAccount
        (automountServiceAccountToken=false), NetworkPolicy (closes the "only gateway
        can reach services directly" gap from prior phases), Ingress
  - [x] Per-service values files (19 files in deployment/helm/values/)
  - [x] helmfile.yaml orchestrating full stack with dependency ordering (needs:)
  - [x] deployment/helm/README.md
- [x] Phase 16: AWS Deployment Scripts (Terraform)
  - [x] Root module: versions.tf, variables.tf, main.tf (for_each over 13 RDS instances), outputs.tf
  - [x] modules/vpc: VPC, public/private subnets, IGW, NAT, VPC endpoints (S3/ECR/Secrets Manager)
  - [x] modules/eks: EKS cluster + KMS envelope encryption + OIDC + 2 node groups + CloudWatch logs
  - [x] modules/rds: PostgreSQL 16 with SSL enforcement, storage encryption, Enhanced Monitoring
  - [x] modules/msk: MSK Kafka with JMX/node exporter (Prometheus scraping), CloudWatch logs
  - [x] modules/ecr: 19 repos, immutable tags, scan-on-push, lifecycle policy (keep 30 images)
  - [x] modules/secrets: Secrets Manager — one secret per credential, sensitive=true
  - [x] modules/alb-ingress: ALB Controller IRSA role + Helm release + optional ACM/Route53
  - [x] environments/dev/terraform.tfvars and environments/prod/terraform.tfvars
  - [x] infrastructure/terraform/README.md
  - Caveat: not `terraform apply`'d against a live account; treat as reviewed blueprint.
- [x] Phase 17: CI/CD Pipelines (GitHub Actions)
  - [x] .github/workflows/ci.yml — Maven build+test all, path-filter change detection,
        per-service conditional Docker build+ECR push (push to main only)
  - [x] .github/workflows/cd.yml — workflow_run trigger after CI, per-service Helm deploy
        with dependency ordering, smoke tests
  - [x] .github/workflows/infra.yml — Terraform plan on PR (comments output), apply on merge
  - [x] .github/workflows/reusable-build-push.yml — OIDC AWS auth, BuildKit cache, Trivy scan
  - [x] .github/workflows/reusable-helm-deploy.yml — helm upgrade --atomic, rollout status check,
        port-forward health probe
  - [x] .github/CODEOWNERS — security-team gates on crypto/security changes
  - [x] .github/pull_request_template.md


## Security (this turn's addition)

- **JWT issuance**: `POST /api/v1/auth/login` on api-gateway (handled by a
  local controller, not routed) authenticates against `DemoUserStore` (5
  fixed demo users: customer1/staff1/analyst1/compliance1/admin1, password
  `password` for all) and issues an HS256 JWT via a small in-gateway
  `JwtSupport` class. This is a deliberate stand-in for Keycloak/OIDC (plan
  section 16), not a replacement — see `DemoUserStore`/`JwtSupport` javadocs
  for exactly what changes to point this at real Keycloak.
- **JWT enforcement**: `JwtAuthenticationGlobalFilter` runs on every request
  through the gateway. Public paths (`/api/v1/auth/**`, `/actuator/health`)
  pass through; everything else needs a valid, unexpired bearer token or
  gets a 401 before it's routed anywhere.
- **Identity propagation**: the filter strips any `X-User-Id`/`X-User-Roles`
  the caller sent themselves (anti-spoofing, plan section 15: "Do not trust
  user identity headers from external clients"), then sets them from the
  verified JWT claims before forwarding downstream.
- **Downstream RBAC**: `TrustedIdentityFilter` (new, in `common-security`)
  turns those headers into a Spring Security context in each service, so
  `@PreAuthorize` works. Applied to exactly 3 endpoints as a demonstration:
  `loan-service` approve/reject/disburse (`BANK_STAFF`/`ADMIN`),
  `card-service` block/close (`BANK_STAFF`/`ADMIN`), `fraud-service`
  blacklist-add (`COMPLIANCE_OFFICER`/`ADMIN`). Every other endpoint across
  all 16 services remains unprotected by role — reaching a service directly
  (bypassing the gateway, as every curl example in this README does) still
  works with no token at all, since nothing stops direct access to a
  service's own port. Real deployments need NetworkPolicy/mTLS so only the
  gateway can reach these services' ports directly; this repo doesn't
  configure that.
- **Bug found and fixed along the way**: `common-security` has had
  `spring-boot-starter-security` on its classpath since it was first
  created, but until this turn no `SecurityFilterChain` bean was ever
  defined. That means every service depending on `common-security`
  (account, transfer, customer, payment, mcp-gateway, ai-orchestrator, rag) was
  silently getting Spring Boot's *default* security auto-configuration when
  actually run (not in `@SpringBootTest`, which doesn't start a real HTTP
  listener) — HTTP Basic auth against a single generated user with a random
  password printed to the console log at startup. So the earlier claim in
  this file and the README that those specific services were simply "wide
  open" was wrong; they were more likely broken/unreachable without that
  logged password. `DownstreamSecurityConfig`'s explicit permissive chain
  now fixes this for every service that depends on `common-security`.

## Database-per-service + Flyway (this turn's addition)

Every one of the 13 stateful services now has its own dedicated PostgreSQL
database (not a shared instance -- see `deployment/docker/docker-compose.yml`,
13 separate `postgres:16-alpine` containers) and a Flyway-managed schema
(`src/main/resources/db/migration/V1__init_schema.sql` per service, hand-
verified column-for-column against each JPA entity -- see the verification
script output from this turn). `spring.jpa.hibernate.ddl-auto` is now
`validate` everywhere it used to be `update`: Flyway owns schema creation
and changes; Hibernate only checks at startup that the entities match what
Flyway created, and refuses to start if they don't (catches drift instead
of Hibernate silently altering a production table). `fraud-service` also
has a `V2__seed_reference_blacklist.sql` as a concrete example of Flyway
handling data insertion, not just DDL, seeding two demo sanctioned-entity
references at deploy time.

Because a real Postgres server per service can't be started inside this
tool environment, tests still run against per-service H2 in-memory
databases (`src/test/resources/application.yml` overrides the main
Postgres config -- Spring Boot puts `src/test/resources` ahead of
`src/main/resources` on the test classpath) with Flyway disabled and
`ddl-auto: update`. This means the schema Flyway would create in
production was verified by hand against the entities, not by an actual
migration run -- a stricter setup would use Testcontainers to run
integration tests against a real disposable Postgres instance; not
implemented here.

## Loose coupling, encryption, HMAC, and Keycloak (this turn's addition)

- **`common-crypto`**: new module, deliberately zero Spring dependencies
  (pure JDK `javax.crypto`) -- unlike `common-security`, which pulls in
  `spring-boot-starter-web` and therefore can't be used by api-gateway's
  reactive stack. This is loose coupling made concrete: a module carries
  only the dependencies its actual job needs. `CryptoUtil` provides
  AES-256-GCM field encryption (`encrypt`/`decrypt`) and HMAC-SHA256
  message signing (`hmacSha256`/`verifyHmac`, constant-time comparison via
  `MessageDigest.isEqual` to avoid a timing side-channel) -- two
  deliberately separate capabilities documented as such in the class
  javadoc (encryption protects data at rest; HMAC protects a message in
  transit between two services that share a secret; GCM's built-in auth
  tag is not a substitute for the latter). 5 unit tests: round-trip,
  IV-randomness (same plaintext encrypts differently each time), wrong-key
  rejection, tamper detection, HMAC accept/reject.

- **Encrypted PII at rest**: `customer-service`'s `Customer` entity gained
  `nationalIdEncrypted` (AES-256-GCM ciphertext only, column named to make
  that explicit). `CustomerResponse` (the default read) has no field
  capable of holding the value at all -- structurally impossible to leak
  through that DTO, not just "happens not to be populated". The only
  decrypted read path is `GET /{id}/national-id`, gated
  `@PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','ADMIN')")` on the
  service method (so the restriction holds even for in-process callers,
  not just HTTP). 2 new tests cover the full round trip and the RBAC
  denial.

- **HMAC-signed inter-service callback**: `POST /transfers/{id}/fraud-decision`
  (fraud-ai-service's callback into transfer-service, closing the transfer
  saga) now requires an `X-Signature` header -- HMAC-SHA256 over a
  canonical string, using a secret only the two services share (config
  key `bank.crypto.fraud-decision-hmac-secret`, identical in both
  services' `application.yml`, same demo-secret caveat as everything
  else). Without this, the endpoint was callable by anyone who could reach
  transfer-service's port, bypassing fraud review entirely -- a much
  larger hole than a missing role check. 3 new MockMvc tests: missing
  signature rejected (403), wrong signature rejected (403), valid
  signature accepted (200). **Known fragility, documented in code**: the
  canonical-string format is duplicated (not shared) between
  `TransferServiceClient.canonicalize()` and `TransferController`'s
  reconstruction of it -- the two services share only the secret and this
  string shape, not code, so they can drift silently if either side
  changes independently.

- **Keycloak + real OAuth2 resource server**: `api-gateway` now supports
  two auth modes, switched by Spring profile (default runs exactly as
  before -- nothing broke): the existing demo HS256 issuer, or
  `--spring.profiles.active=keycloak` for real Keycloak-issued-and-verified
  JWTs via `spring-boot-starter-oauth2-resource-server`'s reactive
  `ReactiveJwtDecoder`, auto-configured from
  `spring.security.oauth2.resourceserver.jwt.issuer-uri` (no shared secret
  to manage on this side -- Spring Boot fetches Keycloak's public signing
  keys from its JWKS endpoint). `deployment/docker/keycloak/banking-realm.json`
  defines a `banking` realm with the same 5 roles and 5 demo users
  (username/password) as `DemoUserStore`, for continuity between the two
  modes. `KeycloakIdentityPropagationFilter` translates Keycloak's JWT
  claim shape (`realm_access.roles`, `preferred_username`) into the same
  `X-User-Id`/`X-User-Roles` headers the demo filter produces, so every
  downstream service's `TrustedIdentityFilter` and `@PreAuthorize` work
  identically regardless of which auth mode issued the token -- that
  header contract is the loose-coupling seam between the gateway and
  everything behind it.
  **A second silent-lockdown bug was caught and fixed while wiring this
  up**: adding `spring-boot-starter-oauth2-resource-server` pulls
  `spring-boot-starter-security` onto api-gateway's classpath, which
  (exactly like the earlier `common-security` incident) auto-configures a
  default reactive security chain -- HTTP Basic against a random generated
  password -- for ANY profile that doesn't explicitly define its own
  `SecurityWebFilterChain`. `DemoModeSecurityConfig` (`@Profile("!keycloak")`)
  exists specifically to prevent that, by explicitly permitting everything
  at the Spring Security layer in demo mode (JwtAuthenticationGlobalFilter,
  a Gateway filter independent of Spring Security, does the actual demo
  enforcement).
  **Verification caveat, stated plainly**: this Keycloak integration is
  reviewed carefully by hand against Spring Security's documented reactive
  resource-server API and follows the standard, widely-used Spring Cloud
  Gateway + Keycloak pattern, but has NOT been run against a live Keycloak
  instance in this environment -- no network access here to pull the
  Keycloak image or start a JVM. Treat it as carefully-written, plausible
  code to verify yourself before relying on it, not as tested-and-confirmed.

## Circuit breaker (this turn's addition)

`ai-orchestrator-service`'s two synchronous downstream calls (rag-service,
mcp-gateway-service) now go through a Resilience4j circuit breaker
(`spring-cloud-starter-circuitbreaker-resilience4j`, configured in
`CircuitBreakerConfiguration`: 10-call sliding window, 50% failure-rate
threshold, 15s wait-in-open, 3s call timeout). The two fallbacks are
deliberately different: a RAG failure degrades to an empty context (the
model still answers, just ungrounded -- `ResponseHandlerService` already
adds a disclaimer for that case), while an MCP tool-call failure surfaces a
clear "temporarily unavailable" message rather than fabricating a balance
-- see `AiOrchestratorService`'s javadoc for why those two failure modes
are handled differently on purpose. This is the ONLY circuit breaker in
the platform so far; the other ~18 services' inter-service HTTP calls
(mcp-gateway-service -> account-service, report-insight-service ->
transaction-service, fraud-ai-service -> transfer-service, etc.) have no
resilience wrapping yet.

## Explicitly not attempted (scope note)

Keycloak is now real (see above) but unverified against a live instance;
`@PreAuthorize` on the other ~50+ endpoints across the platform (5 are
protected now: 3 from the security phase, 2 new -- national-id decrypt and
the fraud-decision HMAC check technically isn't @PreAuthorize but is an
equivalent access control), NetworkPolicy/mTLS to actually enforce that
only the gateway can reach a service directly, encryption key rotation or
a real KMS/HSM/Vault-backed key (customer-service's encryption key and the
fraud-decision HMAC secret are both config-sourced demo passphrases, same
class of gap as the JWT signing secret), PCI-DSS scope beyond the two
concrete fields this turn touched (a real PCI-DSS program covers network
segmentation, key management procedures, access logging, and much more
than field-level encryption alone), Redis
caching/idempotency store (payment-service's idempotency check is DB-backed,
not Redis), Testcontainers-based integration testing against real Postgres
(tests currently run against H2, not the Postgres schema Flyway would
actually create -- see the Database-per-service section above), circuit
breakers on any inter-service call other than ai-orchestrator-service's two,
custom AI-specific metrics (plan section 33 calls out token usage, model
latency, RAG retrieval latency specifically -- what's implemented is
generic Micrometer HTTP/JVM auto-instrumentation only, no custom
`@Timed`/`Counter` business metrics anywhere), Grafana dashboards (the
Prometheus datasource is provisioned automatically, but zero dashboards
ship -- you'd build or import your own), structured JSON log output (logs
are still plain-text patterns with traceId/spanId/correlationId
interpolated in, not actual JSON -- see the Observability section above),
ai-model-gateway as its own deployable service (it's
currently a shared library — see note below), Kubernetes/Helm manifests, AWS
wiring, and CI/CD pipelines. See README.md for what IS implemented and why
a full 20-service platform wasn't attempted as one delivery.

Note: the master prompt lists `ai-model-gateway-service` as a standalone
microservice (#12). This repo implements it as a shared library
(`ai-model-gateway`) consumed by ai-orchestrator-service, so every AI-facing
service gets the same OpenAI/Claude/Gemini clients without an extra network
hop. Splitting it into its own deployable service is a small, mechanical
change (wrap the existing beans in a thin REST controller) if you need it
deployed independently for scaling/versioning reasons.
