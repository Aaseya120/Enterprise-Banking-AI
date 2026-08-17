# Helm Deployment Guide

## Overview

One reusable chart (`banking-microservice`) drives all 19 deployable services.
Per-service customisation lives exclusively in `values/values-<service>.yaml`.
`helmfile.yaml` orchestrates the full stack with explicit dependency ordering.

## Prerequisites

```bash
brew install helmfile helm
helmfile init   # installs helm-diff and helm-secrets plugins
```

For AWS EKS:
```bash
aws eks update-kubeconfig --region us-east-1 --name banking-platform-dev
```

## Install the full stack

```bash
cd deployment/helm

# Development (local kind/minikube — or point kubeconfig at EKS dev cluster)
helmfile sync

# Production
helmfile -e prod sync
```

## Install / upgrade a single service

```bash
helmfile -l name=account-service apply
```

## Render templates without installing (dry-run)

```bash
helm template account-service ./banking-microservice \
  -f values/values-account-service.yaml \
  --namespace banking
```

## Rolling back a service

```bash
helm history account-service -n banking        # list revisions
helm rollback account-service 2 -n banking     # roll back to revision 2
```

## Secrets handling

In development, secret values can be passed directly:
```bash
helmfile sync --set "database.password=yourpassword"
```

In production, use [External Secrets Operator](https://external-secrets.io/) to
sync secrets from AWS Secrets Manager into Kubernetes Secrets, then set
`database.existingSecret` to the pre-created secret name. No secret values
ever appear in Helm values files committed to source control.

## Adding a new service

1. Create `values/values-<new-service>.yaml` (copy an existing file, adjust ports/DB/Kafka)
2. Add a release block to `helmfile.yaml` with appropriate `needs:`
3. Add the service to `infrastructure/terraform/modules/ecr/main.tf` (new ECR repo)
4. Add the service to `.github/workflows/ci.yml` path-filter list

No changes to the chart itself are needed.

## NetworkPolicy

The chart enables NetworkPolicy by default. This means:
- **Only the `api-gateway` pod** can reach a service on its app port (closes the
  direct-access hole documented in task.md)
- **Prometheus** (in the `monitoring` namespace) can scrape `/actuator/prometheus`
  on the management port
- **Kubelet health probes** are allowed on the management port
- **Egress** is allowed to DNS (port 53), same-namespace pods, monitoring namespace,
  RDS (5432), MSK Kafka (9092/9094), and AWS HTTPS endpoints (443)

To disable NetworkPolicy for debugging:
```bash
helm upgrade <service> ./banking-microservice \
  -f values/values-<service>.yaml \
  --set networkPolicy.enabled=false
```

## HPA and Pod Disruption Budget

Every service gets an HPA (CPU + memory) and a PDB (`minAvailable: 1`) by default.
Scale-down is conservative (1 pod per 60s, 5-minute stabilisation window) to avoid
traffic impact from autoscaler thrashing.
