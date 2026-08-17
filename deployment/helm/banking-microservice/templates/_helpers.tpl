{{/*
_helpers.tpl — name and label helpers for the banking-microservice chart.

All templates are prefixed "banking-microservice." so they don't collide if
this chart is used as a dependency of an umbrella chart.
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "banking-microservice.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because Kubernetes DNS label limit is 63 characters.
If fullnameOverride is set, use it directly.
Otherwise combine release name + chart name, deduplicating if the release
name already contains the chart name (common Helm convention).
*/}}
{{- define "banking-microservice.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart label "chart: <name>-<version>".
*/}}
{{- define "banking-microservice.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to every resource.
These are the four recommended Kubernetes well-known labels
(https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)
plus the chart/managed-by pair.
*/}}
{{- define "banking-microservice.labels" -}}
helm.sh/chart: {{ include "banking-microservice.chart" . }}
{{ include "banking-microservice.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels — used in both the Deployment selector and the Service selector.
Must be stable across upgrades (never change after first deploy) so Kubernetes
can match existing pods to the updated ReplicaSet.
*/}}
{{- define "banking-microservice.selectorLabels" -}}
app.kubernetes.io/name: {{ include "banking-microservice.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
ServiceAccount name.
*/}}
{{- define "banking-microservice.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "banking-microservice.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Database JDBC URL built from values.database.* fields.
Used in the configmap to avoid repeating the JDBC URL template everywhere.
*/}}
{{- define "banking-microservice.jdbcUrl" -}}
{{- if .Values.database.enabled -}}
jdbc:postgresql://{{ .Values.database.host }}:{{ .Values.database.port }}/{{ .Values.database.name }}
{{- end }}
{{- end }}

{{/*
Container port name — used consistently across Deployment, Service, and probes.
*/}}
{{- define "banking-microservice.portName" -}}
http
{{- end }}
