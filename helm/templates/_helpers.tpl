{{/*
Expand the name of the chart.
*/}}
{{- define "kubernetes-game.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "kubernetes-game.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Backend selector labels
*/}}
{{- define "kubernetes-game.backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "kubernetes-game.name" . }}-backend
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Frontend selector labels
*/}}
{{- define "kubernetes-game.frontend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "kubernetes-game.name" . }}-frontend
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
