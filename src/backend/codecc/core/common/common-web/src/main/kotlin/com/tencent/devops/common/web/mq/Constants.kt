/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web.mq

const val CORE_LISTENER_CONTAINER_NAME = "rabbitListenerContainerFactory"
const val CORE_CONNECTION_FACTORY_NAME = "connectionFactory"
const val CORE_RABBIT_ADMIN_NAME = "rabbitAdmin"
const val CORE_RABBIT_TEMPLATE_NAME = "rabbitTemplate"

const val ROUTE_NOTIFY_MESSAGE = "route.notify.message"
const val QUEUE_NOTIFY_MESSAGE = "queue.notify.message"
const val EXCHANGE_NOTIFY_MESSAGE = "exchange.notify.message"

const val EXCHANGE_TASK_FILTER_PATH = "exchange.task.filter.path"
const val ROUTE_ADD_TASK_FILTER_PATH = "route.add.task.filter.path"
const val QUEUE_ADD_TASK_FILTER_PATH = "queue.add.task.filter.path"

const val ROUTE_DEL_TASK_FILTER_PATH = "route.del.task.filter.path"
const val QUEUE_DEL_TASK_FILTER_PATH = "queue.del.task.filter.path"

const val EXCHANGE_AUTHOR_TRANS = "exchange.author.trans"
const val ROUTE_AUTHOR_TRANS = "route.author.trans"
const val QUEUE_AUTHOR_TRANS = "queue.author.trans"

const val EXCHANGE_OPERATION_HISTORY = "exchange.operation.history"
const val ROUTE_OPERATION_HISTORY = "route.operation.history"
const val QUEUE_OPERATION_HISTORY = "queue.operation.history"

const val EXCHANGE_TASK_CHECKER_CONFIG = "exchange.task.checker.config"
const val ROUTE_IGNORE_CHECKER = "route.ignore.checker.config"
const val QUEUE_IGNORE_CHECKER = "queue.ignore.checker.config"

const val EXCHANGE_ANALYSIS_VERSION = "exchange.analysis.version"
const val ROUTE_ANALYSIS_VERSION = "route.analysis.version"
const val QUEUE_ANALYSIS_VERSION = "queue.analysis.version"

const val EXCHANGE_EXTERNAL_JOB = "exchange.external.job.cluster"
const val QUEUE_EXTERNAL_JOB = "queue.external.job.cluster."

const val EXCHANGE_INTERNAL_JOB = "exchange.internal.job.cluster"
const val ROUTE_INTERNAL_JOB = "route.internal.job.cluster"
const val QUEUE_INTERNAL_JOB = "queue.internal.job.cluster."

const val EXCHANGE_GONGFENG_DELETE_ALL_JOB = "exchange.gongfeng.delete.all.job"
const val QUEUE_GONGFENG_DELETE_ALL_JOB = "queue.gongfeng.delete.all.job."

const val EXCHANGE_GONGFENG_INIT_ALL_JOB = "exchange.gongfeng.init.all.job"
const val QUEUE_GONGFENG_INIT_ALL_JOB = "queue.gongfeng.init.all.job."

const val PREFIX_EXCHANGE_DEFECT_COMMIT = "exchange.defect.commit."
const val PREFIX_ROUTE_DEFECT_COMMIT = "route.defect.commit."
const val PREFIX_QUEUE_DEFECT_COMMIT = "queue.defect.commit."

const val PREFIX_EXCHANGE_OPENSOURCE_DEFECT_COMMIT = "exchange.opensource.defect.commit."
const val PREFIX_ROUTE_OPENSOURCE_DEFECT_COMMIT = "route.opensource.defect.commit."
const val PREFIX_QUEUE_OPENSOURCE_DEFECT_COMMIT = "queue.opensource.defect.commit."

const val EXCHANGE_DEFECT_COMMIT_KLOCWORK = "exchange.defect.commit.klocwork"
const val ROUTE_DEFECT_COMMIT_KLOCWORK = "route.defect.commit.klocwork"
const val QUEUE_DEFECT_COMMIT_KLOCWORK = "queue.defect.commit.klocwork"

const val EXCHANGE_DEFECT_COMMIT_LINT_NEW = "exchange.defect.commit.lint.new"
const val ROUTE_DEFECT_COMMIT_LINT_NEW = "route.defect.commit.lint.new"
const val QUEUE_DEFECT_COMMIT_LINT_NEW = "queue.defect.commit.lint.new"

const val EXCHANGE_DEFECT_COMMIT_CCN_NEW = "exchange.defect.commit.ccn.new"
const val ROUTE_DEFECT_COMMIT_CCN_NEW = "route.defect.commit.ccn.new"
const val QUEUE_DEFECT_COMMIT_CCN_NEW = "queue.defect.commit.ccn.new"

const val EXCHANGE_DEFECT_COMMIT_DUPC_NEW = "exchange.defect.commit.dupc.new"
const val ROUTE_DEFECT_COMMIT_DUPC_NEW = "route.defect.commit.dupc.new"
const val QUEUE_DEFECT_COMMIT_DUPC_NEW = "queue.defect.commit.dupc.new"

const val EXCHANGE_DEFECT_COMMIT_CLOC_NEW = "exchange.defect.commit.cloc.new"
const val ROUTE_DEFECT_COMMIT_CLOC_NEW = "route.defect.commit.cloc.new"
const val QUEUE_DEFECT_COMMIT_CLOC_NEW = "queue.defect.commit.cloc.new"

const val EXCHANGE_DEFECT_COMMIT_PINPOINT_NEW = "exchange.defect.commit.pinpoint.new"
const val ROUTE_DEFECT_COMMIT_PINPOINT_NEW = "route.defect.commit.pinpoint.new"
const val QUEUE_DEFECT_COMMIT_PINPOINT_NEW = "queue.defect.commit.pinpoint.new"

const val EXCHANGE_DEFECT_COMMIT_STAT_NEW = "exchange.defect.commit.stat.new"
const val ROUTE_DEFECT_COMMIT_STAT_NEW = "route.defect.commit.stat.new"
const val QUEUE_DEFECT_COMMIT_STAT_NEW = "queue.defect.commit.stat.new"

const val EXCHANGE_DEFECT_COMMIT_METRICS = "exchange.defect.commit.metrics"
const val ROUTE_DEFECT_COMMIT_METRICS = "route.defect.commit.metrics"
const val QUEUE_DEFECT_COMMIT_METRICS = "queue.defect.commit.metrics"

const val EXCHANGE_DEFECT_COMMIT_CLUSTER = "exchange.defect.commit.cluster"
const val ROUTE_DEFECT_COMMIT_CLUSTER = "route.defect.commit.cluster"
const val QUEUE_DEFECT_COMMIT_CLUSTER = "queue.defect.commit.cluster"

const val EXCHANGE_DEFECT_COMMIT_LINT_LARGE = "exchange.defect.commit.lint.large"
const val ROUTE_DEFECT_COMMIT_LINT_LARGE = "route.defect.commit.lint.large"
const val QUEUE_DEFECT_COMMIT_LINT_LARGE = "queue.defect.commit.lint.large"

const val EXCHANGE_DEFECT_COMMIT_CCN_LARGE = "exchange.defect.commit.ccn.large"
const val ROUTE_DEFECT_COMMIT_CCN_LARGE = "route.defect.commit.ccn.large"
const val QUEUE_DEFECT_COMMIT_CCN_LARGE = "queue.defect.commit.ccn.large"

const val EXCHANGE_DEFECT_COMMIT_DUPC_LARGE = "exchange.defect.commit.dupc.large"
const val ROUTE_DEFECT_COMMIT_DUPC_LARGE = "route.defect.commit.dupc.large"
const val QUEUE_DEFECT_COMMIT_DUPC_LARGE = "queue.defect.commit.dupc.large"

const val EXCHANGE_DEFECT_COMMIT_CLOC_LARGE = "exchange.defect.commit.cloc.large"
const val ROUTE_DEFECT_COMMIT_CLOC_LARGE = "route.defect.commit.cloc.large"
const val QUEUE_DEFECT_COMMIT_CLOC_LARGE = "queue.defect.commit.cloc.large"

const val EXCHANGE_DEFECT_COMMIT_PINPOINT_LARGE = "exchange.defect.commit.pinpoint.large"
const val ROUTE_DEFECT_COMMIT_PINPOINT_LARGE = "route.defect.commit.pinpoint.large"
const val QUEUE_DEFECT_COMMIT_PINPOINT_LARGE = "queue.defect.commit.pinpoint.large"

const val EXCHANGE_DEFECT_COMMIT_STAT_LARGE = "exchange.defect.commit.stat.large"
const val ROUTE_DEFECT_COMMIT_STAT_LARGE = "route.defect.commit.stat.large"
const val QUEUE_DEFECT_COMMIT_STAT_LARGE = "queue.defect.commit.stat.large"

const val EXCHANGE_DEFECT_COMMIT_SUPER_LARGE = "exchange.defect.commit.super.large"
const val ROUTE_DEFECT_COMMIT_SUPER_LARGE = "route.defect.commit.super.large"
const val QUEUE_DEFECT_COMMIT_SUPER_LARGE = "queue.defect.commit.super.large"

const val EXCHANGE_ANALYZE_DISPATCH = "exchange.analyze.schedule"
const val ROUTE_ANALYZE_DISPATCH = "route.analyze.schedule"
const val QUEUE_ANALYZE_DISPATCH = "queue.analyze.schedule"

const val EXCHANGE_ANALYZE_DISPATCH_OPENSOURCE = "exchange.analyze.schedule.opensource"
const val ROUTE_ANALYZE_DISPATCH_OPENSOURCE = "route.analyze.schedule.opensource"
const val QUEUE_ANALYZE_DISPATCH_OPENSOURCE = "queue.analyze.schedule.opensource"

const val EXCHANGE_CHECK_THREAD_ALIVE = "exchange.check.thread.alive"
const val ROUTE_CHECK_THREAD_ALIVE = "route.check.thread.alive"
const val QUEUE_CHECK_THREAD_ALIVE = "queue.check.thread.alive"


const val EXCHANGE_CODECC_GENERAL_NOTIFY = "exchange.codecc.general.notify"
const val ROUTE_CODECC_EMAIL_NOTIFY = "route.codecc.email.notify"
const val QUEUE_CODECC_EMAIL_NOTIFY = "queue.codecc.email.notify"

const val EXCHANGE_REGISTER_KW_PROJECT = "exchange.register.kw.project"
const val ROUTE_REGISTER_KW_PROJECT = "route.register.kw.project"
const val QUEUE_REGISTER_KW_PROJECT = "queue.register.kw.project"

const val ROUTE_CODECC_RTX_NOTIFY = "route.codecc.rtx.notify"
const val QUEUE_CODECC_RTX_NOTIFY = "queue.codecc.rtx.notify"

const val ROUTE_CODECC_RTX_NOTIFY_SEND = "route.codecc.rtx.notify.send"
const val QUEUE_CODECC_RTX_NOTIFY_SEND = "queue.codecc.rtx.notify.send"

const val ROUTE_CODECC_BKPLUGINEMAIL_NOTIFY = "route.codecc.bkpluginemail.notify"
const val QUEUE_CODECC_BKPLUGINEMAIL_NOTIFY = "queue.codecc.bkpluginemail.notify"

const val ROUTE_CODECC_BKPLUGINWECHAT_NOTIFY = "route.codecc.bkpluginwechat.notify"
const val QUEUE_CODECC_BKPLUGINWECHAT_NOTIFY = "queue.codecc.bkpluginwechat.notify"

const val EXCHANGE_IGNORE_EMAIL_SEND = "exchange.ignore.email.send"
const val ROUTE_IGNORE_EMAIL_SEND = "route.ignore.email.send"
const val QUEUE_IGNORE_EMAIL_SEND = "queue.ignore.email.send"

const val EXCHANGE_KAFKA_DATA_PLATFORM = "exchange.kafka.data.platform"

const val ROUTE_KAFKA_DATA_TRIGGER_TASK = "route.kafka.data.trigger.task"
const val QUEUE_KAFKA_DATA_TRIGGER_TASK = "queue.kafka.data.trigger.task"

const val EXCHANGE_EXPIRED_TASK_STATUS = "exchange.expired.task.status"
const val ROUTE_EXPIRED_TASK_STATUS = "route.expired.task.status"
const val QUEUE_EXPIRED_TASK_STATUS = "queue.expired.task.status"

const val EXCHANGE_REFRESH_CHECKERSET_USAGE = "exchange.refresh.checkerset.usage"
const val ROUTE_REFRESH_CHECKERSET_USAGE = "route.refresh.checkerset.usage"
const val QUEUE_REFRESH_CHECKERSET_USAGE = "queue.refresh.checkerset.usage"

const val EXCHANGE_REFRESH_TOOLMETA_CACHE = "exchange.refresh.toolmeta.cache"
const val QUEUE_REFRESH_TOOLMETA_CACHE = "queue.refresh.toolmeta.cache"

const val EXCHANGE_CLUSTER_ALLOCATION = "exchange.cluster.allocation"
const val ROUTE_CLUSTER_ALLOCATION = "route.cluster.allocation"
const val QUEUE_CLUSTER_ALLOCATION = "queue.cluster.allocation"
const val QUEUE_REPLY_CLUSTER_ALLOCATION = "queue.reply.cluster.allocation"

const val EXCHANGE_CLUSTER_ALLOCATION_OPENSOURCE = "exchange.cluster.allocation.opensource"
const val ROUTE_CLUSTER_ALLOCATION_OPENSOURCE = "route.cluster.allocation.opensource"
const val QUEUE_CLUSTER_ALLOCATION_OPENSOURCE = "queue.cluster.allocation.opensource"
const val QUEUE_REPLY_CLUSTER_ALLOCATION_OPENSOURCE = "queue.reply.cluster.allocation.opensource"

const val EXCHANGE_CUSTOM_PIPELINE_TRIGGER = "exchange.custom.pipeline.trigger"
const val ROUTE_CUSTOM_PIPELINE_TRIGGER = "route.custom.pipeline.trigger"
const val QUEUE_CUSTOM_PIPELINE_TRIGGER = "queue.custom.pipeline.trigger"

const val EXCHANGE_TASK_REFRESH_ORG = "exchange.task.refresh.org"
const val ROUTE_TASK_REFRESH_ORG = "route.task.refresh.org"
const val QUEUE_TASK_REFRESH_ORG = "queue.task.refresh.org"

const val EXCHANGE_LINT_DEFECT_MIGRATION = "exchange.lint.defect.migration"
const val ROUTE_LINT_DEFECT_MIGRATION = "route.lint.defect.migration"
const val QUEUE_LINT_DEFECT_MIGRATION = "queue.lint.defect.migration"

const val PREFIX_EXCHANGE_FAST_INCREMENT = "exchange.fast.increment."
const val PREFIX_ROUTE_FAST_INCREMENT = "route.fast.increment."

const val EXCHANGE_FAST_INCREMENT_LINT = "exchange.fast.increment.lint"
const val ROUTE_FAST_INCREMENT_LINT = "route.fast.increment.lint"
const val QUEUE_FAST_INCREMENT_LINT = "queue.fast.increment.lint"

const val EXCHANGE_FAST_INCREMENT_CCN = "exchange.fast.increment.ccn"
const val ROUTE_FAST_INCREMENT_CCN = "route.fast.increment.ccn"
const val QUEUE_FAST_INCREMENT_CCN = "queue.fast.increment.ccn"

const val EXCHANGE_FAST_INCREMENT_DUPC = "exchange.fast.increment.dupc"
const val ROUTE_FAST_INCREMENT_DUPC = "route.fast.increment.dupc"
const val QUEUE_FAST_INCREMENT_DUPC = "queue.fast.increment.dupc"

const val EXCHANGE_FAST_INCREMENT_CLOC = "exchange.fast.increment.cloc"
const val ROUTE_FAST_INCREMENT_CLOC = "route.fast.increment.cloc"
const val QUEUE_FAST_INCREMENT_CLOC = "queue.fast.increment.cloc"

const val EXCHANGE_FAST_INCREMENT_STAT = "exchange.fast.increment.stat"
const val ROUTE_FAST_INCREMENT_STAT = "route.fast.increment.stat"
const val QUEUE_FAST_INCREMENT_STAT = "queue.fast.increment.stat"

const val EXCHANGE_FAST_INCREMENT_PINPOINT = "exchange.fast.increment.pinpoint"
const val ROUTE_FAST_INCREMENT_PINPOINT = "route.fast.increment.pinpoint"
const val QUEUE_FAST_INCREMENT_PINPOINT = "queue.fast.increment.pinpoint"

const val EXCHANGE_FAST_INCREMENT_COVERITY = "exchange.fast.increment.coverity"
const val ROUTE_FAST_INCREMENT_COVERITY = "route.fast.increment.coverity"
const val QUEUE_FAST_INCREMENT_COVERITY = "queue.fast.increment.coverity"

const val EXCHANGE_FAST_INCREMENT_KLOCWORK = "exchange.fast.increment.klocwork"
const val ROUTE_FAST_INCREMENT_KLOCWORK = "route.fast.increment.klocwork"
const val QUEUE_FAST_INCREMENT_KLOCWORK = "queue.fast.increment.klocwork"

const val EXCHANGE_TOOL_REFRESH_FOLLOWSTATUS = "exchange.tool.refresh.followstatus"
const val ROUTE_TOOL_REFRESH_FOLLOWSTATUS = "route.tool.refresh.followstatus"
const val QUEUE_TOOL_REFRESH_FOLLOWSTATUS = "queue.tool.refresh.followstatus"

const val EXCHANGE_SCORING_OPENSOURCE = "exchange.scoring.opensource"
const val ROUTE_SCORING_OPENSOURCE = "route.scoring.opensource"
const val QUEUE_SCORING_OPENSOURCE = "queue.scoring.opensource"

const val EXCHANGE_ATOM_MONITOR_DATA_REPORT_FANOUT = "e.engine.atom.monitor.data.report.fanout"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC = "exchange.close.defect.statistic"
const val ROUTE_CLOSE_DEFECT_STATISTIC = "route.close.defect.statistic"
const val QUEUE_CLOSE_DEFECT_STATISTIC = "queue.close.defect.statistic"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT = "exchange.close.defect.statistic.lint"
const val ROUTE_CLOSE_DEFECT_STATISTIC_LINT = "route.close.defect.statistic.lint"
const val QUEUE_CLOSE_DEFECT_STATISTIC_LINT = "queue.close.defect.statistic.lint"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN = "exchange.close.defect.statistic.ccn"
const val ROUTE_CLOSE_DEFECT_STATISTIC_CCN = "route.close.defect.statistic.ccn"
const val QUEUE_CLOSE_DEFECT_STATISTIC_CCN = "queue.close.defect.statistic.ccn"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_OPENSOURCE = "exchange.close.defect.statistic.opensource"
const val ROUTE_CLOSE_DEFECT_STATISTIC_OPENSOURCE = "route.close.defect.statistic.opensource"
const val QUEUE_CLOSE_DEFECT_STATISTIC_OPENSOURCE = "queue.close.defect.statistic.opensource"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE = "exchange.close.defect.statistic.lint.opensource"
const val ROUTE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE = "route.close.defect.statistic.lint.opensource"
const val QUEUE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE = "queue.close.defect.statistic.lint.opensource"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE = "exchange.close.defect.statistic.ccn.opensource"
const val ROUTE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE = "route.close.defect.statistic.ccn.opensource"
const val QUEUE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE = "queue.close.defect.statistic.ccn.opensource"

const val EXCHANGE_USER_LOG_INFO_STAT = "exchange.user.log.info.stat"
const val ROUTE_USER_LOG_INFO_STAT = "route.user.log.info.stat"
const val QUEUE_USER_LOG_INFO_STAT = "queue.user.log.info.stat"

const val EXCHANGE_CHECKER_DEFECT_STAT = "exchange.checker.defect.stat"
const val ROUTE_CHECKER_DEFECT_STAT = "route.checker.defect.stat"
const val QUEUE_CHECKER_DEFECT_STAT = "queue.checker.defect.stat"

const val EXCHANGE_ACTIVE_STAT = "exchange.active.stat"
const val ROUTE_ACTIVE_STAT = "route.active.stat"
const val QUEUE_ACTIVE_STAT = "queue.active.stat"
const val QUEUE_ACTIVE_STAT_EXT = "queue.active.stat.ext"

const val EXCHANGE_TASK_PERSONAL = "exchange.task.personal"
const val ROUTE_TASK_PERSONAL = "route.task.personal"
const val QUEUE_TASK_PERSONAL = "queue.task.personal"

const val EXCHANGE_CODE_REPO_STAT = "exchange.code.repo.stat"
const val ROUTE_CODE_REPO_STAT = "route.code.repo.stat"
const val QUEUE_CODE_REPO_STAT = "queue.code.repo.stat"

const val EXCHANGE_TRIGGER_COMMIT_HANDLER = "exchange.trigger.commit.handler"
const val ROUTE_TRIGGER_COMMIT_HANDLER = "route.trigger.commit.handler"
const val QUEUE_TRIGGER_COMMIT_HANDLER = "queue.trigger.commit.handler"

const val EXCHANGE_LINE_CHART_STAT = "exchange.line.chart.stat"
const val ROUTE_LINE_CHART_STAT = "route.line.chart.stat"
const val QUEUE_LINE_CHART_STAT = "queue.line.chart.stat"

const val EXCHANGE_PRECI_USER_STAT = "exchange.preci.user.stat"
const val ROUTE_PRECI_USER_STAT = "route.preci.user.stat"
const val QUEUE_PRECI_USER_STAT = "queue.preci.user.stat"

const val EXCHANG_PRECI_CLOUD_DAY_STAT = "exchange.preci.cloud.day.stat"
const val ROUTE_PRECI_CLOUD_DAY_STAT = "route.preci.cloud.day.stat"
const val QUEUE_PRECI_CLOUD_DAY_STAT = "queue.preci.cloud.day.stat"

const val EXCHANG_PRECI_CLOUD_WEEK_STAT = "exchange.preci.cloud.week.stat"
const val ROUTE_PRECI_CLOUD_WEEK_STAT = "route.preci.cloud.week.stat"
const val QUEUE_PRECI_CLOUD_WEEK_STAT = "queue.preci.cloud.week.stat"

const val EXCHANGE_SMOKE_CHECK = "exchange.smoke.check"
const val ROUTE_SMOKE_CHECK = "route.smoke.check"
const val QUEUE_SMOKE_CHECK = "queue.smoke.check"

const val EXCHANGE_SMOKE_TRIGGER_ANALYZE = "exchange.smoke.trigger.analyze"
const val ROUTE_SMOKE_TRIGGER_ANALYZE = "route.smoke.trigger.analyze"
const val QUEUE_SMOKE_TRIGGER_ANALYZE = "queue.smoke.trigger.analyze"

const val EXCHANGE_COVERITY_INST_UPDATE = "exchange.coverity.inst.update"
const val QUEUE_COVERITY_INST_UPDATE = "queue.coverity.inst.update"

const val EXCHANGE_KLOCWORK_INST_UPDATE = "exchange.klocwork.inst.update"
const val QUEUE_KLOCWORK_INST_UPDATE = "queue.klocwork.inst.update"

const val EXCHANGE_SCAN_FINISH = "exchange.scan.finish"
const val ROUTE_SCAN_FINISH = "route.scan.finish"
const val QUEUE_SCAN_FINISH_FOR_SYNC_DATA = "queue.scan.finish.sync.data"
const val QUEUE_SCAN_FINISH_FOR_SCAN_SLA = "queue.scan.finish.scan_sla"

const val EXCHANGE_CODECCJOB_TASKLOG_WEBSOCKET = "exchange.codeccjob.tasklog.websocket"
const val QUEUE_CODECCJOB_TASKLO_WEBSOCKET = "queue.codeccjob.tasklog.websocket."

const val EXCHANGE_CLEAN_MONGO_DATA = "exchange.clean.mongo.data"
const val QUEUE_CLEAN_MONGO_DATA = "queue.clean.mongo.data"

const val EXCHANGE_IGNORE_TYPE_NOTIFY = "exchange.ignore.type.notify"
const val ROUTE_IGNORE_TYPE_NOTIFY = "route.ignore.type.notify"
const val QUEUE_IGNORE_TYPE_NOTIFY = "queue.ignore.type.notify"

const val EXCHANGE_PIPELINE_BUILD_END_CALLBACK = "exchange.pipeline.build.end.callback"
const val ROUTE_PIPELINE_BUILD_END_CALLBACK = "route.pipeline.build.end.callback"
const val QUEUE_PIPELINE_BUILD_END_CALLBACK = "queue.pipeline.build.end.callback"

const val EXCHANGE_PLUGIN_ERROR_CALLBACK = "exchange.plugin.error.callback"
const val ROUTE_PLUGIN_ERROR_CALLBACK = "route.plugin.error.callback"
const val QUEUE_PLUGIN_ERROR_CALLBACK = "queue.plugin.error.callback"

const val EXCHANGE_BK_METRICS_DAILY_FANOUT = "e.metrics.statistic.codecc.daily"
const val EXCHANGE_BK_METRICS_DAULY_TRIGGER = "exchange.metrics.statistic.trigger"
const val QUEUE_BK_METRICS_DAILY_TRIGGER = "queue.metrics.statistic.trigger"
const val ROUTE_BK_METRICS_DAILY_TRIGGER = "route.metrics.statistic.trigger"

const val EXCHANGE_DEFECT_MIGRATION_COMMON = "exchange.defect.migration.common"
const val ROUTE_DEFECT_MIGRATION_COMMON = "route.defect.migration.common"
const val QUEUE_DEFECT_MIGRATION_COMMON = "queue.defect.migration.common"

const val EXCHANGE_DEFECT_MIGRATION_COMMON_OPENSOURCE = "exchange.defect.migration.common.opensource"
const val ROUTE_DEFECT_MIGRATION_COMMON_OPENSOURCE = "route.defect.migration.common.opensource"
const val QUEUE_DEFECT_MIGRATION_COMMON_OPENSOURCE = "queue.defect.migration.common.opensource"

const val EXCHANGE_DEFECT_MIGRATION_TRIGGER_BATCH = "exchange.defect.migration.trigger.batch"
const val ROUTE_DEFECT_MIGRATION_TRIGGER_BATCH = "route.defect.migration.trigger.batch"
const val QUEUE_DEFECT_MIGRATION_TRIGGER_BATCH = "queue.defect.migration.trigger.batch"

const val EXCHANGE_SYNC_USER_TASK_ORG_INFO = "exchange.sync.user.task.org.info"
const val ROUTE_SYNC_USER_TASK_ORG_INFO = "route.sync.user.task.org.info"
const val QUEUE_SYNC_USER_TASK_ORG_INFO = "queue.sync.user.task.org.info"

const val EXCHANGE_DEFECT_CHANGE_LOG = "exchange.defect.change.log"
const val ROUTE_DEFECT_CHANGE_LOG = "route.defect.change.log"
const val QUEUE_DEFECT_CHANGE_LOG = "queue.defect.change.log"

const val EXCHANGE_SCANSCHEDULE_TOOL_SCAN = "exchange.scanschedule.tool.scan"
const val ROUTE_SCANSCHEDULE_TOOL_SCAN = "route.scanschedule.tool.scan"
const val QUEUE_SCANSCHEDULE_TOOL_SCAN = "queue.scanschedule.tool.scan"

const val EXCHANGE_DATA_SEPARATION = "exchange.data.separation"
const val ROUTE_DATA_SEPARATION_COOL_DOWN = "route.data.separation.cool.down"
const val QUEUE_DATA_SEPARATION_COOL_DOWN = "queue.data.separation.cool.down"
const val ROUTE_DATA_SEPARATION_WARM_UP = "route.data.separation.warm.up"
const val QUEUE_DATA_SEPARATION_WARM_UP = "queue.data.separation.warm.up"
const val ROUTE_DATA_SEPARATION_COOL_DOWN_TRIGGER = "route.data.separation.cool.down.trigger"
const val QUEUE_DATA_SEPARATION_COOL_DOWN_TRIGGER = "queue.data.separation.cool.down.trigger"
const val ROUTE_DATA_SEPARATION_FILE_CACHE_PURGING = "route.data.separation.file.cache.purging"
const val QUEUE_DATA_SEPARATION_FILE_CACHE_PURGING = "queue.data.separation.file.cache.purging"

const val EXCHANGE_TASK_INVALID_TOOL_DEFECT = "exchange.task.invalid.tool.defect"
const val ROUTE_TASK_INVALID_TOOL_DEFECT = "route.task.invalid.tool.defect"
const val QUEUE_TASK_INVALID_TOOL_DEFECT = "queue.task.invalid.tool.defect"
const val ROUTE_TASK_INVALID_TOOL_DEFECT_OPENSOURCE = "route.task.invalid.tool.defect.opensource"
const val QUEUE_TASK_INVALID_TOOL_DEFECT_OPENSOURCE = "queue.task.invalid.tool.defect.opensource"
