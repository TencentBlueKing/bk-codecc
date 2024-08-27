rootProject.name = "codecc"

include("core")
include("core:common")
include("core:common:common-api")
include("core:common:common-client")
include("core:common:common-client:common-client-base")
include("core:common:common-client:common-client-k8s")
include("core:common:common-client:common-client-consul")
include("core:common:common-db")
include("core:common:common-mq")
include("core:common:common-otel")
include("core:common:common-service")
include("core:common:common-web")
include("core:common:common-util")
include("core:common:common-auth")
include("core:common:common-auth:common-auth-api")
include("core:common:common-auth:common-auth-mock")
include("core:common:common-auth:common-auth-v3")
include("core:common:common-auth:common-auth-github")
include("core:common:common-auth:common-auth-op")
include("core:common:common-auth:common-auth-rbac")
include("core:common:common-redis")
include("core:common:common-code-generator")
include("core:common:common-storage")
include("core:common:common-storage:common-storage-core")
include("core:common:common-storage:common-storage-local")
include("core:common:common-storage:common-storage-nfs")
include("core:common:common-storage:common-storage-bkrepo")
include("core:common:common-storage:common-storage-cos")

include("core:defect")
include("core:defect:api-defect")
include("core:defect:biz-defect")
include("core:defect:biz-defect-migration")
include("core:defect:model-defect")
include("core:defect:boot-defect")
include("core:defect:biz-defect-base")
include("core:defect:biz-defect-bkcheck")

include("core:task")
include("core:task:api-task")
include("core:task:biz-task")
include("core:task:biz-task-migration")
include("core:task:model-task")
include("core:task:boot-task")

include("core:codeccjob")
include("core:codeccjob:api-codeccjob")
include("core:codeccjob:biz-codeccjob")
include("core:codeccjob:model-codeccjob")
include("core:codeccjob:boot-codeccjob")

include("core:openapi")
include("core:openapi:api-openapi")
include("core:openapi:biz-openapi")
include("core:openapi:boot-openapi")
include("core:openapi:model-openapi")

include("core:quartz")
include("core:quartz:api-quartz")
include("core:quartz:model-quartz")
include("core:quartz:biz-quartz")
include("core:quartz:sdk-quartz")
include("core:quartz:boot-quartz")

include("core:schedule")
include("core:schedule:api-schedule")
include("core:schedule:model-schedule")
include("core:schedule:biz-schedule")
include("core:schedule:boot-schedule")
















