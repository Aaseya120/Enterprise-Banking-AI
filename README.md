# 🏦 Enterprise Banking AI Platform

> **A modern, event-driven banking architecture seamlessly integrated with Generative AI.**

Welcome to the **Enterprise Banking AI Platform**! This project is a comprehensive, Spring Boot-based multi-module reference architecture. It demonstrates how to safely build and integrate an intelligent AI layer (RAG, multi-LLM routing, Document Intelligence) alongside a secure, highly-regulated traditional banking services plane.

---

## ✨ Key Features

- **🧠 AI Intelligence Plane**: Integrates OpenAI, Claude, and Gemini securely. Features an AI orchestrator, Retrieval-Augmented Generation (RAG) for internal policies, and intelligent document processing.
- **🛡️ Secure by Design**: The AI *never* touches the banking databases directly. Interactions happen strictly through an API Gateway and controlled Model Context Protocol (MCP) tools.
- **🏛️ Microservices Architecture**: 13 stateful, decoupled microservices. Each service owns its dedicated PostgreSQL database, enforcing strict data isolation.
- **⚡ Event-Driven**: Leverages Apache Kafka and the Transactional Outbox pattern to guarantee eventual consistency across payments, transfers, and fraud checks.
- **🔐 Enterprise Security**: Implements JWT-based Role-Based Access Control (RBAC), end-to-end correlation tracking, and AES-256-GCM encryption for Personally Identifiable Information (PII) at rest.

---

## 🚀 Quick Start (Local Development)

The fastest way to get everything running is using Docker Compose. This will bring up all 13 databases, Kafka, Zookeeper, Zipkin (for tracing), Prometheus, Grafana, and all 19 microservices!

### 1. Start the Platform
```bash
docker compose -f deployment/docker/docker-compose.yml up --build
```
*(Wait a few minutes for all services to initialize and flyway migrations to complete).*

### 2. Try it out!

**Open a new bank account:**
```bash
curl -X POST http://localhost:8081/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-1","accountType":"SAVINGS","openingBalance":1000.00,"currency":"USD"}'
```

**Chat with the AI Assistant:**
*(No API keys needed out-of-the-box! It runs in a deterministic "demo mode" for easy local testing).*
```bash
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
        "conversationId":"C1",
        "userId":"CUST-1",
        "userRoles":["CUSTOMER"],
        "query":"What is my account balance?",
        "context":{"accountId":"<INSERT_ACCOUNT_ID_HERE>"}
      }'
```

---

## 📂 Project Structure

This repository is split into distinct domains:

| Directory | Description |
|-----------|-------------|
| 🌐 **`ai-platform/`** | The "brain". Includes the AI Model Gateway, RAG, Document Intelligence, and the MCP Gateway. |
| 🏦 **`banking-services/`** | The "core". Traditional ledgers, accounts, transfers, payments, cards, and loan services. |
| 🛠️ **`common/`** | Shared libraries for DTOs, custom exceptions, events, crypto, and security filters. |
| 🚪 **`infrastructure/`** | The API Gateway (Spring Cloud Gateway) and Terraform IaC definitions for AWS. |
| 🚢 **`deployment/`** | Docker Compose configurations and Helm charts for Kubernetes deployments. |

---

## 📖 Documentation

Want to understand how all the pieces fit together? 

👉 **Check out the [Architecture Guide](ARCHITECTURE.md)** for a deep dive into the system design, the Saga patterns we use for fraud checks, and our database-per-service approach.

---

*Note: This is a reference scaffold architecture designed to showcase modern integration patterns between Core Banking and AI. While fully compiling and tested, it serves as a foundation for enterprise teams rather than a turn-key banking product.*
