{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "codecc.imagePullSecrets" -}}
{{- if .Values.imagePullSecrets -}}
imagePullSecrets:
  - name: {{- .Values.imagePullSecrets -}}
{{- end -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "codecc.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default (printf "%s-foo" (include "common.names.fullname" .)) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Create a default fully qualified mongodb subchart.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "codecc.mongodb.fullname" -}}
{{- if .Values.mongodb.fullnameOverride -}}
{{- .Values.mongodb.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "mongodb" .Values.mongodb.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "codecc.redis.fullname" -}}
{{- if .Values.redis.fullnameOverride -}}
{{ $name := .Values.redis.fullnameOverride | trunc 63 | trimSuffix "-"}}
{{- list $name "master" | join "-" -}}
{{- else -}}
{{- $name := default "redis" .Values.redis.nameOverride -}}
{{- printf "%s-%s-master" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "codecc.rabbitmq.fullname" -}}
{{- if .Values.rabbitmq.fullnameOverride -}}
{{- .Values.rabbitmq.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "rabbitmq" .Values.rabbitmq.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the mongodb connection uri
*/}}
{{- define "codecc.mongo.addr" -}}
{{- if eq .Values.mongodb.enabled true -}}
{{- (include "codecc.mongodb.fullname" .) -}}
{{- else -}}
{{- .Values.config.bkCodeccMongodbAddr -}}
{{- end -}}
{{- end -}}


{{- define "codecc.mongo.username" -}}
{{- if eq .Values.mongodb.enabled true -}}
{{- .Values.mongodb.auth.username -}}
{{- else -}}
{{- .Values.config.bkCodeccMongodbUser -}}
{{- end -}}
{{- end -}}

{{- define "codecc.mongo.password" -}}
{{- if eq .Values.mongodb.enabled true -}}
{{- .Values.mongodb.auth.password -}}
{{- else -}}
{{- .Values.config.bkCodeccMongodbPassword -}}
{{- end -}}
{{- end -}}

{{- define "codecc.redis.host" -}}
{{- if eq .Values.redis.enabled true -}}
{{- (include "codecc.redis.fullname" .) -}}
{{- else -}}
{{- .Values.config.bkCodeccRedisHost -}}
{{- end -}}
{{- end -}}


{{- define "codecc.redis.port" -}}
{{- if eq .Values.redis.enabled true -}}
6379
{{- else -}}
{{- .Values.config.bkCodeccRedisPort -}}
{{- end -}}
{{- end -}}

{{- define "codecc.redis.password" -}}
{{- if eq .Values.redis.enabled true -}}
{{- .Values.redis.auth.password -}}
{{- else -}}
{{- .Values.config.bkCodeccRedisPassword -}}
{{- end -}}
{{- end -}}

{{- define "codecc.redis.db" -}}
{{- if eq .Values.redis.enabled true -}}
1
{{- else -}}
{{- .Values.config.bkCodeccRedisDb -}}
{{- end -}}
{{- end -}}

{{- define "codecc.rabbitmq.host" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- include "codecc.rabbitmq.fullname" . -}}
{{- else -}}
{{- .Values.config.bkCodeccRabbitmqAddr -}}
{{- end -}}
{{- end -}}

{{- define "codecc.rabbitmq.username" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- .Values.rabbitmq.auth.username -}}
{{- else -}}
{{- .Values.config.bkCodeccRabbitmqUser -}}
{{- end -}}
{{- end -}}

{{- define "codecc.rabbitmq.password" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- .Values.rabbitmq.auth.password -}}
{{- else -}}
{{- .Values.config.bkCodeccRabbitmqPassword -}}
{{- end -}}
{{- end -}}

{{- define "codecc.rabbitmq.virtualhost" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
default-vhost
{{- else -}}
{{- .Values.config.bkCodeccRabbitmqVhost -}}
{{- end -}}
{{- end -}}

{{/*
codecc standard labels
*/}}
{{- define "codecc.labels.standard" -}}
helm.sh/chart: {{ include "common.names.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Labels to use on deploy.spec.selector.matchLabels and svc.spec.selector
*/}}
{{- define "codecc.labels.matchLabels" -}}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}