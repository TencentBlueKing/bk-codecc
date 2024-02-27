# CodeCC

 CodeCC 是腾讯的代码分析平台。本文档内容为如何在 Kubernetes 集群上部署 CodeCC 服务。

## 环境要求
- Kubernetes 1.12+
- Helm 3+
- PV provisioner

## 安装Chart
使用以下命令安装名称为`codecc`的release, 其中`<codecc helm repo url>`代表helm仓库地址:

```shell
$ helm repo add bkee <codecc helm repo url>
$ helm install codecc bkee/codecc
```

上述命令将使用默认配置在Kubernetes集群中部署codecc, 并输出访问指引。

## 卸载Chart
使用以下命令卸载`codecc`:

```shell
$ helm uninstall codecc
```

上述命令将移除所有和codecc相关的Kubernetes组件，并删除release。

## Chart依赖
- [bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/nginx-ingress-controller)
- [bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)
- [bitnami/redis](https://github.com/bitnami/charts/blob/master/bitnami/redis)
- [bitnami/common](https://github.com/bitnami/charts/blob/master/bitnami/common)
- [bitnami/rabbitmq](https://github.com/bitnami/charts/blob/master/bitnami/rabbitmq)

## 配置说明
下面展示了可配置的参数列表以及默认值

### Charts 全局设置

|参数|描述|默认值 |
|---|---|---|
| `global.imageRegistry`    | Global Docker image registry                    | `nil`                                                   |
| `global.imagePullSecrets` | Global Docker registry secret names as an array | `[]` (does not add image pull secrets to deployed pods) |
| `global.storageClass`     | Global storage class for dynamic provisioning   | `nil`                                                   |

### Kubernetes组件公共配置

下列参数用于配置Kubernetes组件的公共属性，一份配置作用到每个组件

|参数|描述|默认值 |
|---|---|---|
| `commonAnnotations` | Annotations to add to all deployed objects | `{}` |
| `commonLabels` | Labels to add to all deployed objects | `{}` |

### Kubernetes组件通用配置

下列参数表示Kubernetes组件的通用配置，每个微服务进行单独配置。能够配置的微服务有:

- gateway
- task
- asyncreport
- defect
- report
- quartz
- codeccjob
- schedule


|参数|描述|默认值 |
|---|---|---|
| `image.registry`    | 镜像仓库              | `mirrors.tencent.com` |
| `image.repository`  | 镜像名称              | `bkce/codecc/xxx`          |
| `image.tag`         | 镜像tag               | `{TAG_NAME}`         |
| `image.pullPolicy`  | 镜像拉取策略           | `IfNotPresent`        |
| `image.pullSecrets` | 镜像拉取Secret名称数组  | `[]`                  |
| `imagePullSecrets`  | 镜像拉取secret名称列表  | `[]`                  |
| `securityContext`   | 容器 Security Context | `{}`                  |
| `replicaCount`                       | Number of pod replicas                                                                      | `2`                                                     |
| `hostAliases`                        | Add deployment host aliases                                                                       | `[]`                                                    |
| `resources.limits`                   | The resources limits for containers                                                          | `{}`                                                    |
| `resources.requests`                 | The requested resources for containers                                                       | `{}`                                                    |
| `affinity`                           | Affinity for pod assignment (evaluated as a template)                                                                   | `{}`                           |
| `containerSecurityContext.enabled`      | Enable containers' Security Context                                                                             | `false`                                      |
| `containerSecurityContext.runAsUser`    | Containers' Security Context                                                                                    | `1001`                                      |
| `containerSecurityContext.runAsNonRoot` | Containers' Security Context Non Root                                                                           | `true`                                      |
| `nodeAffinityPreset.key`             | Node label key to match Ignored if `affinity` is set.                                                                   | `""`                           |
| `nodeAffinityPreset.type`            | Node affinity preset type. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                               | `""`                           |
| `nodeAffinityPreset.values`          | Node label values to match. Ignored if `affinity` is set.                                                               | `[]`                           |
| `nodeSelector`                       | Node labels for pod assignment                                                                                          | `{}` (evaluated as a template) |
| `podLabels`                             | Add additional labels to the pod (evaluated as a template)                                                            | `nil`                                       |
| `podAnnotations`                     | Pod annotations                                                                                                         | `{}` (evaluated as a template) |
| `podAffinityPreset`                  | Pod affinity preset. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                                     | `""`                           |
| `podAntiAffinityPreset`              | Pod anti-affinity preset. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                                | `soft`                         |
| `podSecurityContext.enabled`         | Enable pod security context                                                                                             | `true`                         |
| `podSecurityContext.fsGroup`         | fsGroup ID for the pod                                                                                                  | `1001`                         |
| `priorityClassName`                     | Define the priority class name for the pod.                                                        | `""`                                        |
| `tolerations`                        | Tolerations for pod assignment                                                                                          | `[]` (evaluated as a template) |

### RBAC配置

|参数|描述|默认值 |
|---|---|---|
| `rbac.create`                        | If true, create & use RBAC resources                                                                                    | `true`                        |
| `serviceAccount.annotations`         | Annotations for service account                                                                                         | `{}`                           |
| `serviceAccount.create`              | If true, create a service account                                                                                       | `false`                        |
| `serviceAccount.name`                | The name of the service account to use. If not set and create is true, a name is generated using the fullname template. | ``                             |

### ingress 配置

|参数|描述|默认值 |
|---|---|---|
| `ingress.enabled` | 是否创建ingress | `true` |
| `annotations` | ingress标注 | Check `values.yaml` |

### nginx-ingress-controller 配置

默认将部署`nginx-ingress-controller`，如果不需要可以关闭。
相关配置请参考[bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/)

|参数|描述|默认值 |
|---|---|---|
| `nginx-ingress-controller.enabled` | 是否部署nginx ingress controller | `true` |
| `nginx-ingress-controller.defaultBackend.enabled` | nginx ingress controller默认backend | `false` |

### mongodb 配置
默认将部署mongodb，如果不需要可以关闭。
相关配置请参考[bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)

|参数|描述|默认值 |
|---|---|---|
| `mongodb.enabled` | 是否部署mognodb。如果需要使用外部数据库，设置为`false`并配置`externalMongodb` | `true` |
| `externalMongodb.defectUrl` | 外部mongodb服务的连接地址。当`mongodb.enabled`配置为`false`时，codecc将使用此参数连接外部mongodb | `mongodb://codecc:codecc@localhost:27017/db_defect` |
| `externalMongodb.taskUrl` | 外部mongodb服务的连接地址。当`mongodb.enabled`配置为`false`时，codecc将使用此参数连接外部mongodb | `mongodb://codecc:codecc@localhost:27017/db_task` |
| `externalMongodb.quartzUrl` | 外部mongodb服务的连接地址。当`mongodb.enabled`配置为`false`时，codecc将使用此参数连接外部mongodb | `mongodb://codecc:codecc@localhost:27017/db_quartz` |

> 如果需要持久化mongodb数据，请参考[bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)配置存储卷

### redis 配置

默认将部署`redis`，如果不需要可以关闭。
相关配置请参考[bitnami/redis](https://github.com/bitnami/charts/tree/master/bitnami/reids)

|参数|描述|默认值 |
|---|---|---|
| `redis.enabled` | 是否部署mognodb。如果需要使用外部数据库，设置为`false`并配置`externalRedis` | `true` |
| `externalRedis.host` | 外部redis服务的连接地址。当`redis.enabled`配置为`false`时，codecc将使用此参数连接外部redis | `localhost` |
| `externalRedis.port` | 外部redis服务的连接端口。当`redis.enabled`配置为`false`时，codecc将使用此参数连接外部redis | `6379` |
| `externalRedis.password` | 外部redis服务的密码。当`redis.enabled`配置为`false`时，codecc将使用此参数连接外部redis | `codec` |

> 如果需要持久化mongodb数据，请参考[bitnami/redis](https://github.com/bitnami/charts/blob/master/bitnami/redis)配置存储卷

### rabbitmq 配置

默认将部署`rabbitmq`，如果不需要可以关闭。
相关配置请参考[bitnami/rabbitmq](https://github.com/bitnami/charts/tree/master/bitnami/rabbitmq)

|参数|描述|默认值 |
|---|---|---|
| `rabbitmq.enabled` | 是否部署mognodb。如果需要使用外部数据库，设置为`false`并配置`externalRabbitmq` | `true` |
| `externalRabbitmq.host` | 外部rabbitmq服务的连接地址。当`rabbitmq.enabled`配置为`false`时，codecc将使用此参数连接外部rabbitmq | `localhost` |
| `externalRabbitmq.virtualhost` | 外部rabbitmq服务的连接地址。当`rabbitmq.enabled`配置为`false`时，codecc将使用此参数连接外部rabbitmq | `localhost` |
| `externalRabbitmq.username` | 外部rabbitmq服务的用户名。当`rabbitmq.enabled`配置为`false`时，codecc将使用此参数连接外部rabbitmq | `codec` |
| `externalRabbitmq.password` | 外部rabbitmq服务的密码。当`rabbitmq.enabled`配置为`false`时，codecc将使用此参数连接外部rabbitmq | `codec` |

> 如果需要持久化mongodb数据，请参考[bitnami/rabbitmq](https://github.com/bitnami/charts/blob/master/bitnami/rabbitmq)配置存储卷


### 数据持久化配置

数据持久化配置, 当使用filesystem方式存储时需要配置。

|参数|描述|默认值 |
|---|---|---|
| `persistence.enabled` | 是否开启数据持久化，false则使用emptyDir类型volume, pod结束后数据将被清空，无法持久化 | `true` |
| `persistence.accessMode` | PVC Access Mode for codecc data volume | `ReadWriteOnce` |
| `persistence.size` | PVC Storage Request for codecc data volume | `100Gi` |
| `persistence.storageClass` | 指定storageClass。如果设置为"-", 则禁用动态卷供应; 如果不设置, 将使用默认的storageClass(minikube上是standard) | `nil` |
| `persistence.existingClaim` | 如果开启持久化并且定义了该项，则绑定k8s集群中已存在的pvc | `nil` |

> 如果开启数据持久化，并且没有配置`existingClaim`，将使用[动态卷供应](https://kubernetes.io/docs/concepts/storage/dynamic-provisioning/)提供存储，使用`storageClass`定义的存储类。**在删除该声明后，这个卷也会被销毁(用于单节点环境，生产环境不推荐)。**。

### codecc公共配置

|参数|描述|默认值 |
|---|---|---|
| `common.springProfile` | SpringBoot active profile | `dev` |
| `common.mountPath` | pod volume挂载路径 | `/data/storage` |

### 数据初始化job配置

|参数|描述|默认值 |
|---|---|---|
| `init.mongodb.enabled` | 是否初始化mongodb数据，支持幂等执行 | `true` |
| `init.mongodb.image` | mongodb job镜像拉取相关配置 | Check `values.yaml` |
| `init.entrance.enabled` | 是否初始化蓝盾入口数据，支持幂等执行 | `true` |
| `init.entrance.image` | entrance job镜像拉取相关配置 | Check `values.yaml` |
| `init.storage.enabled` | 是否初始化文件存储 | `true` |
| `init.storage.image` | storage job镜像拉取相关配置 | Check `values.yaml` |


### 网关配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `gateway.service.type` | 服务类型 | `ClusterIP` |
| `gateway.service.port` | 服务类型为`ClusterIP`时端口设置 | `80` |
| `gateway.service.nodePort` | 服务类型为`NodePort`时端口设置 | `80` |
| `gateway.dnsServer` | dns服务器地址，用于配置nginx resolver | `local=on`(openrestry语法，取本机`/etc/resolv.conf`配置) |
| `gateway.deployMode` | 部署模式，standalone: 独立模式，ci: 与ci搭配模式 | `standalone` |


### 公共配置项
***配置是需要加入前缀config.  如："config.bkCodeccConsulDiscoveryTag"

|参数|描述|默认值 |
|---|---|---|
|`bkCodeccConsulDiscoveryTag`|服务发现时的标签|codecc|
|`bkCiPublicUrl`|CI的公开地址|example.com|
|`bkCiPublicSchema`|CI的公开地址Schemes |http|
|`bkCiPrivateUrl`|CI的集群内地址|example.com|
|`bkCodeccPublicUrl`|codecc为集群外访问提供的URL|example.com|
|`bkCodeccPrivateUrl`|codecc为集群内访问提供的URL|example.com|
|`bkCiProjectInnerUrl`|CI内部项目服务访问地址，用于注册CodeCC入口|example.com/project|
|`bkCiAuthProvider`|服务权限校验方式|sample|
|`bkIamPrivateUrl`|bkIam对内提供的访问URL|""|
|`bkCodeccAppCode`|CodeCC在蓝鲸体系中的唯一ID|bk_codecc|
|`bkCodeccAppToken`|唯一ID密钥|""|
|`bkPaasPrivateUrl`|pass对内提供的访问URL|pass.example.com|
|`bkCiIamCallbackUser`|bkIam回调地址|""|
|`bkCodeccPipelineImageName`|流水线使用的镜像名称，用于CodeCC独立入口创建任务|bkci/ci|
|`bkCodeccPipelineBuildType`|构建机类型，用于CodeCC独立入口创建任务|KUBERNETES|
|`bkCodeccPipelineImageTag`|流水线使用的镜像版本，用于CodeCC独立入口创建任务|latest|
|`bkCodeccTaskEncryptorKey`|插件密钥|abcde|
|`bkCodeccPipelineAtomCode`|CodeCC插件Code，用于CodeCC独立入口创建任务|CodeCCCheckAtom|
|`bkCodeccPipelineAtomVersion`|CodeCC插件版本，用于CodeCC独立入口创建任务|1.*|
|`bkGitPipelineAtomCode`|Git插件Code，用于CodeCC独立入口创建任务|git|
|`bkGitPipelineAtomVersion`|Git插件版本，用于CodeCC独立入口创建任务|1.*|
|`bkGithubPipelineAtomCode`|Github插件Code，用于CodeCC独立入口创建任务|PullFromGithub|
|`bkGithubPipelineAtomVersion`|Github插件版本，用于CodeCC独立入口创建任务|1.*|
|`bkSvnPipelineAtomCode`|Svn插件Code，用于CodeCC独立入口创建任务|svnCodeRepo|
|`bkSvnPipelineAtomVersion`|Svn插件版本，用于CodeCC独立入口创建任务|1.*|
|`bkCodeccPipelineImageType`|镜像类型，用于CodeCC独立入口创建任务|THIRD|
|`bkCodeccPipelineScmIsOldSvn`||true|
|`bkCodeccPipelineScmIsOldGithub`||true|
|`bkCodeccTaskAnalysisMaxHour`|最大的分析时长|7|
|`bkCiEnv`|网关相关参数|dev|
|`bkHttpSchema`|网关相关参数|http|
|`bkPaasFqdn`|网关相关参数|""|
|`bkPaasHttpsPort`|网关相关参数|80|
|`bkCodeccPaasLoginUrl`|网关相关参数|""|
|`bkCodeccGatewayCorsAllowList`|网关相关参数|""|
|`bkCiIamEnv`|网关相关参数|staging|
|`bkSsmHost`|网关相关参数|""|
|`bkSsmPort`|网关相关参数|80|
|`bkCiGatewaySsmTokenUrl`|网关相关参数|/oauth/token|
|`bkCiAppCode`|网关相关参数|workbench|
|`bkCiAppToken`|网关相关参数|""|
|`bkCodeccGatewayRegionName`|网关相关参数|""|
|`bkCodeccFileDataPath`|CodeCC告警文件存放地址|/data/workspace/nfs|
|`bkCodeccNfsServer`|NFS服务地址，仅当bkCodeccStorageType=nfs是才需要|""|
|`bkCodeccNfsServerPath`|NFS挂在地址，仅当bkCodeccStorageType=nfs是才需要，建议与bkCodeccFileDataPath一致|/data/workspace/nfs|
|`bkCodeccStorageType`|文件存储类型|nfs|
|`bkCodeccStorageExpired`|文件存储的过期时间，单位天，0表示不限制|0|
|`bkCodeccStorageBkrepoAdminUsername`|Bkrepo的管理员帐号，用于storage job创建仓库、用户，仅当bkCodeccStorageType=bkrepo是才需要|bkrepo_admin|
|`bkCodeccStorageBkrepoAdminPassword`|Bkrepo的管理员密码，用于storage job创建仓库、用户，仅当bkCodeccStorageType=bkrepo是才需要|password|
|`bkCodeccStorageBkrepoUsername`|Bkrepo仓库的帐号，仅当bkCodeccStorageType=bkrepo是才需要|codecc|
|`bkCodeccStorageBkrepoPassword`|Bkrepo仓库的密码，仅当bkCodeccStorageType=bkrepo是才需要|codecc|
|`bkCodeccStorageBkrepoProject`|Bkrepo仓库的项目，仅当bkCodeccStorageType=bkrepo是才需要|codecc|
|`bkCodeccStorageBkrepoRepo`|Bkrepo仓库，仅当bkCodeccStorageType=bkrepo是才需要|repo|
|`bkCodeccStorageBkrepoHost`|Bkrepo地址，仅当bkCodeccStorageType=bkrepo是才需要|""|
|`bkCodeccLogCollectEnable`|是否开启日志收集|false|
|`bkCodeccServiceLogDataId`|服务日志收集的dataId，仅当bkCodeccLogCollectEnable=true才需要|""|
|`bkCodeccGatewayLogDataId`|网关日志收集的dataId，仅当bkCodeccLogCollectEnable=true才需要|""|
|`bkCodeccMonitorEnable`|是否开启监控|false|




### task服务配置

|参数|描述|默认值 |
|---|---|---|
| `task.enabled`       | 是否部署task     | `true`


### defect服务配置

|参数|描述|默认值 |
|---|---|---|
| `defect.enabled`       | 是否部署defect     | `true`         


### asyncreport服务配置

|参数|描述|默认值 |
|---|---|---|
| `asyncreport.enabled`       | 是否部署asyncreport     | `true`         

### report服务配置

|参数|描述|默认值 |
|---|---|---|
| `report.enabled`       | 是否部署report     | `true`                          |

### quartz服务配置

|参数|描述|默认值 |
|---|---|---|
| `quartz.enabled`       | 是否部署quartz     | `false`                          |

### codeccjob服务配置

|参数|描述|默认值 |
|---|---|---|
| `codeccjob.enabled`       | 是否部署codeccjob    | `true`                          |

### schedule服务配置

|参数|描述|默认值 |
|---|---|---|
| `schedule.enabled`       | 是否部署schedule    | `true`                          |


###  使用默认的value.yaml文件部署即可部署