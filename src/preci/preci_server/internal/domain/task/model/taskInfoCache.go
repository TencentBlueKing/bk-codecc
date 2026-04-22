package model

import (
	"codecc/preci_server/internal/infra/scm"
)

// 任务信息缓存
// TODO: 将关于任务信息的缓存利用起来
var taskInfoCache = new(TaskInfo)

func GetScmInfoCache() scm.ScmInfo {
	return scm.ScmInfo{
		ScmType:     taskInfoCache.ScmType,
		ProjectRoot: taskInfoCache.RootDir,
	}
}

func SetTaskInfoCache(taskInfo *TaskInfo) {
	taskInfoCache = taskInfo
}

func GetTaskInfoCache() TaskInfo {
	return *taskInfoCache
}
