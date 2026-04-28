package model

import (
	"codecc/preci_server/internal/infra/scm"
)

// 任务信息缓存
// TODO: 将关于任务信息的缓存利用起来
var taskInfoCache = new(TaskInfo)

// GetScmInfoCache 从任务信息缓存中提取 SCM 相关信息返回
func GetScmInfoCache() scm.ScmInfo {
	return scm.ScmInfo{
		ScmType:     taskInfoCache.ScmType,
		ProjectRoot: taskInfoCache.RootDir,
	}
}

// SetTaskInfoCache 更新任务信息的进程级缓存
func SetTaskInfoCache(taskInfo *TaskInfo) {
	taskInfoCache = taskInfo
}

// GetTaskInfoCache 返回当前任务信息缓存的副本
func GetTaskInfoCache() TaskInfo {
	return *taskInfoCache
}
