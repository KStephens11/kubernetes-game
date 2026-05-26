{{/*
Expand the name of the chart.
*/}}
{{- define "k8s-game.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "k8s-game.fullname" -}}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "k8s-game.backend.fullname" -}}
{{- printf "%s-backend" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "k8s-game.frontend.fullname" -}}
{{- printf "%s-frontend" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "k8s-game.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "k8s-game.backend.labels" -}}
{{ include "k8s-game.labels" . }}
app.kubernetes.io/name: {{ include "k8s-game.name" . }}-backend
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: backend
{{- end }}

{{- define "k8s-game.frontend.labels" -}}
{{ include "k8s-game.labels" . }}
app.kubernetes.io/name: {{ include "k8s-game.name" . }}-frontend
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: frontend
{{- end }}

{{/*
Selector labels
*/}}
{{- define "k8s-game.backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "k8s-game.name" . }}-backend
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "k8s-game.frontend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "k8s-game.name" . }}-frontend
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
ServiceAccount name for the backend
*/}}
{{- define "k8s-game.serviceAccountName" -}}
{{- if .Values.backend.serviceAccount.name }}
{{- .Values.backend.serviceAccount.name }}
{{- else }}
{{- include "k8s-game.backend.fullname" . }}
{{- end }}
{{- end }}

{{/*
Full image reference helper
*/}}
{{- define "k8s-game.backend.image" -}}
{{- printf "%s%s:%s" .Values.image.registry .Values.backend.image.repository .Values.backend.image.tag }}
{{- end }}

{{- define "k8s-game.frontend.image" -}}
{{- printf "%s%s:%s" .Values.image.registry .Values.frontend.image.repository .Values.frontend.image.tag }}
{{- end }}
