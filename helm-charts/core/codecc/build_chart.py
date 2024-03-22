#! /usr/bin/python3
import os
import re
import humps
import json
import sys
import shutil

files = os.listdir('.')
replace_pattern = re.compile(r'__BK_[A-Z0-9_]*__')
replace_dict = {}
config_parent = '../../../support-files/codecc/templates'
config_merge_py = './config_merge.py'
template_parent = './templates/configmap/tpl/'
env_properties_file = '../../../scripts/deploy-codecc/codecc.properties'
output_value_yaml = './values.yaml'
default_value_json = './build/values.json'
default_value_yaml = './build/values.yaml'
default_chart_yaml = './Charts.yaml'

# 额外配置
env_ext_properties_file = '../../../scripts/deploy-codecc/ext/codecc.properties'
default_ext_value_json = './build/ext_values.json'
default_ext_value_yaml = './build/ext_values.yaml'

# 创建目录
os.system("mkdir -p " + template_parent)
os.system("ls -l")
# 设置一些默认值
default_value_dict = {
    'bkCiAuthProvider': 'sample',
    'bkCiIamCallbackUser': 'bk_iam',
    'bkCiPrivateUrl': 'devops.example.com',
    'bkCiPublicUrl': 'devops.example.com',
    'bkHttpSchema': 'http',
    'bkIamPrivateUrl': 'iam.example.com',
    'bkPassPrivateUrl': 'pass.example.com',
    "bkCodeccAuthUrl": 'iam.example.com',
    "bkCodeccAppCode": "bk_codecc",
    "bkCodeccAppToken": "",
    "bkCodeccDataDir": "/data/workspace/public/codecc",
    "bkCodeccFqdn": "http://codecc.example.com",
    "bkCodeccHome": "/data/workspace/codecc",
    "bkCodeccHost": "bk-codecc.service.consul",
    "bkCodeccHttpsPort": "443",
    "bkCodeccHttpPort": "80",
    "bkCodeccLogsDir": "/data/workspace/logs/codecc",
    "bkCodeccPrivateUrl": "http://bk-codecc.service.consul",
    "bkCodeccPublicUrl": "http://codecc.example.com",
    "bkCodeccServicePerfix": "",
    "bkCodeccServiceSuffix": "",
    "bkCodeccProjectGray": "false",
    "bkCodeccConsulDevnetIp": "127.0.0.1",
    "bkCodeccConsulDiscoveryTag": "codecc",
    "bkCodeccConsulDnsPort": "53",
    "bkCodeccConsulDomain": "consul",
    "bkCodeccConsulHttpPort": "8500",
    "bkCodeccConsulPort": "8500",
    "bkCodeccFileDataPath": "/data/workspace/public/codecc/files",
    "bkCodeccGatewayRegionName": "DEVNET",
    "bkCodeccMongodbAddr": "127.0.0.1:27017",
    "bkCodeccMongodbPassword": "",
    "bkCodeccMongodbUser": "",
    "bkCodeccRabbitmqAddr": "127.0.0.1:5672",
    "bkCodeccRabbitmqPassword": "",
    "bkCodeccRabbitmqUser": "",
    "bkCodeccRabbitmqVhost": "bkcodecc",
    "bkCodeccRedisDb": "1",
    "bkCodeccRedisHost": "127.0.0.1",
    "bkCodeccRedisPassword": "",
    "bkCodeccRedisPort": "6379",
    "bkCodeccStorageType": "nfs",
    "bkCodeccNfsServerPath": "/data/workspace/nfs",
    "bkCodeccLocalPath": "",
    "bkCodeccStorageBkrepoUsername": "",
    "bkCodeccStorageBkrepoPassword": "",
    "bkCodeccStorageBkrepoProject": "",
    "bkCodeccStorageBkrepoRepo": "",
    "bkCodeccStorageBkrepoHost": "",
    "bkCodeccServiceSuffix": ""
}

if os.path.isfile(default_value_json):
    default_value_dict.update(json.load(open(default_value_json)))

if os.path.isfile(default_ext_value_json):
    default_value_dict.update(json.load(open(default_ext_value_json)))

# include 模板
include_dict = {
    '__BK_CODECC_MONGODB_ADDR__': '{{ include "codecc.mongo.addr" . }}',
    '__BK_CODECC_MONGODB_PASSWORD__': '{{ include "codecc.mongo.password" . }}',
    '__BK_CODECC_MONGODB_USER__': '{{ include "codecc.mongo.username" . }}',
    '__BK_CODECC_MONGO_DEFECT_CORE_URL__': '{{ include "codecc.defect.core.mongodbUri" . }}',
    '__BK_CODECC_MONGO_OP_URL__': '{{ include "codecc.op.mongodbUri" . }}',
    '__BK_CODECC_MONGO_DEFECT_URL__': '{{ include "codecc.defect.mongodbUri" . }}',
    '__BK_CODECC_MONGO_QUARTZ_URL__': '{{ include "codecc.quartz.mongodbUri" . }}',
    '__BK_CODECC_MONGO_SCHEDULE_URL__': '{{ include "codecc.schedule.mongodbUri" . }}',
    '__BK_CODECC_MONGO_TASK_URL__': '{{ include "codecc.task.mongodbUri" . }}',
    '__BK_CODECC_RABBITMQ_ADDR__': '{{ include "codecc.rabbitmq.host" . }}',
    '__BK_CODECC_RABBITMQ_PASSWORD__': '{{ include "codecc.rabbitmq.password" . }}',
    '__BK_CODECC_RABBITMQ_USER__': '{{ include "codecc.rabbitmq.username" . }}',
    '__BK_CODECC_RABBITMQ_VHOST__': '{{ include "codecc.rabbitmq.virtualhost" . }}',
    '__BK_CODECC_REDIS_DB__': '{{ include "codecc.redis.db" . }}',
    '__BK_CODECC_REDIS_HOST__': '{{ include "codecc.redis.host" . }}',
    '__BK_CODECC_REDIS_PASSWORD__': '{{ include "codecc.redis.password" . }}',
    '__BK_CODECC_REDIS_PORT__': '{{ include "codecc.redis.port" . }}',
    '__BK_CODECC_SERVICE_PERFIX__': '{{ include "common.names.fullname" . }}-'
}

# 读取变量映射
env_file = open(env_properties_file, 'r', encoding='UTF-8')
for line in env_file:
    if line.startswith('BK_'):
        datas = line.split("=")
        key = datas[0]
        replace_dict[key] = humps.camelize(key.lower())
env_file.close()

# 读取额外变量映射
if os.path.isfile(env_ext_properties_file):
    env_file = open(env_ext_properties_file, 'r', encoding='UTF-8')
    for line in env_file:
        if line.startswith('BK_'):
            datas = line.split("=")
            key = datas[0]
            replace_dict[key] = humps.camelize(key.lower())
    env_file.close()


# 读取传入变量
image_host = sys.argv[1]
image_path = sys.argv[2]
image_gateway_tag = sys.argv[3]
image_backend_tag = sys.argv[4]
chart_backend_tag = sys.argv[5]

# 替换Chart.yaml的版本变量
chart_line = []
for line in open(default_chart_yaml, 'r', encoding='UTF-8'):
    line = line.replace("__chart_backend_tag__", chart_backend_tag)
    line = line.replace("__image_backend_tag__", image_backend_tag)
    chart_line.append(line)
with open(default_chart_yaml, 'w', encoding='utf-8') as file:
    file.writelines(chart_line)

# 生成value.yaml
if os.path.exists(output_value_yaml):
    os.remove(output_value_yaml)
value_file = open(output_value_yaml, 'w')
for line in open(default_value_yaml, 'r', encoding='UTF-8'):
    line = line.replace("__image_gateway_tag__", image_gateway_tag)
    line = line.replace("__image_backend_tag__", image_backend_tag)
    line = line.replace("__image_host__", image_host)
    line = line.replace("__image_path__", image_path)
    value_file.write(line)
value_file.write('\n')
if os.path.isfile(default_ext_value_yaml):
    for line in open(default_ext_value_yaml, 'r', encoding='UTF-8'):
        line = line.replace("__image_gateway_tag__", image_gateway_tag)
        line = line.replace("__image_backend_tag__", image_backend_tag)
        line = line.replace("__image_host__", image_host)
        line = line.replace("__image_path__", image_path)
        value_file.write(line)

value_file.write('\nconfig:\n')
for key in sorted(replace_dict):
    default_value = '""'
    if key.endswith("PORT"):
        default_value = '80'
    value_file.write('  ' + replace_dict[key] + ': ' + default_value_dict.get(replace_dict[key], default_value) + '\n')
value_file.flush()
value_file.close()

# 判断需不需合并配置文件
core_config = config_parent + '/core'
ext_config = config_parent + '/ext'
merge_config = config_parent + '/merge'
if os.path.isfile(config_merge_py):
    os.system('python3 ' + config_merge_py + ' ' + core_config + ' ' + ext_config + ' ' + merge_config)
else:
    if os.path.exists(merge_config): \
            shutil.rmtree(merge_config)
    shutil.copytree(core_config, merge_config)
# 生成服务tpl
config_re = re.compile(r'-[a-z\-]*|common')
for config_name in os.listdir(merge_config):
    if config_name.endswith('yaml') or config_name.endswith('yml'):
        config_file = open(merge_config + '/' + config_name, 'r', encoding='UTF-8')
        the_name = config_re.findall(config_name)[0].replace('-', '', 1)
        new_file = open(template_parent + '_' + the_name + '.tpl', 'w')

        new_file.write('{{- define "application-' + the_name + '.yaml" -}}\n')
        for line in config_file:
            for key in replace_pattern.findall(line):
                if include_dict.__contains__(key):
                    line = line.replace(key, include_dict[key])
                else:
                    line = line.replace(key, '{{ .Values.config.' + replace_dict.get(key.replace('__', ''), '') + ' }}')
            new_file.write(line)
        new_file.write('\n{{- end -}}')

        new_file.flush()
        new_file.close()
        config_file.close()
