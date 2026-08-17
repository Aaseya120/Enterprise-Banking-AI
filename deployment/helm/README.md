# ☸️ Helm Deployment Guide

Welcome to the Kubernetes deployment guide! We use **Helm** and **Helmfile** to manage the deployment of all 19 microservices in a clean, declarative way.

## 🌟 Overview

Instead of maintaining 19 different Helm charts, we use **one reusable chart** called `banking-microservice`. 
Each individual service simply provides its own custom configuration values in the `values/values-<service>.yaml` file. 

The entire stack, along with its dependency ordering, is orchestrated by `helmfile.yaml`.

---

## 🛠️ Prerequisites

Before you start, make sure you have Helm and Helmfile installed:

```bash
brew install helmfile helm
helmfile init   # Installs required helm-diff and helm-secrets plugins
```

*If deploying to AWS EKS, configure your kubeconfig:*
```bash
aws eks update-kubeconfig --region us-east-1 --name banking-platform-dev
```

---

## 🚀 Quick Start: Deploying the Stack

Navigate to the Helm directory:
```bash
cd deployment/helm
```

**For Local Development** *(minikube/kind or EKS dev)*:
```bash
helmfile sync
```

**For Production**:
```bash
helmfile -e prod sync
```

---

## 🎯 Common Commands

### Deploy or Upgrade a Single Service
```bash
helmfile -l name=account-service apply
```

### Dry-Run (See what will be generated without deploying)
```bash
helm template account-service ./banking-microservice \
  -f values/values-account-service.yaml \
  --namespace banking
```

### Rollback a Service
```bash
helm history account-service -n banking        # List all revisions
helm rollback account-service 2 -n banking     # Roll back to revision 2
```

---

## 🔐 Secrets Management

We take security seriously. **No passwords or secret values are ever committed to source control.**

- **Development**: Pass secrets securely via command line:
  ```bash
  helmfile sync --set "database.password=your_secure_password"
  ```
- **Production**: We use the [External Secrets Operator](https://external-secrets.io/) to seamlessly sync secrets from AWS Secrets Manager directly into Kubernetes Secrets.

---

## 🛡️ Security & Reliability Policies

By default, our Helm charts enforce strict policies:

1. **Network Policies**:
   - Only the `api-gateway` pod can communicate with microservices on their main application ports.
   - Egress (outbound traffic) is strictly limited to necessary endpoints (DNS, Kafka, RDS, etc.).
2. **High Availability**:
   - Every service is configured with a Horizontal Pod Autoscaler (HPA) and a Pod Disruption Budget (PDB) to ensure zero-downtime during node upgrades.

*To temporarily disable Network Policies for local debugging:*
```bash
helm upgrade <service> ./banking-microservice \
  -f values/values-<service>.yaml \
  --set networkPolicy.enabled=false
```
