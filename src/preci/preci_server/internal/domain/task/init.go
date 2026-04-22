package task

import (
	"codecc/preci_server/internal/domain/checker"
	checkermodel "codecc/preci_server/internal/domain/checker/model"
	"codecc/preci_server/internal/domain/common"
	"codecc/preci_server/internal/domain/task/model"
	taskrepo "codecc/preci_server/internal/domain/task/model/repository"
	"codecc/preci_server/internal/domain/tool"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/scm"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util"
	utilos "codecc/preci_server/internal/util/os"
	"codecc/preci_server/internal/util/perror"
	"fmt"
	"os"
	"path"
	"sort"
)

// initProjInfo 初始化项目信息
func initProjInfo(currentPath, rootPath string) (*scm.ScmInfo, error) {
	log := logger.GetLogger()
	log.Info(fmt.Sprintf("init scm info, currentPath=%s, rootPath=%s", currentPath, rootPath))

	var scmInfo = scm.GetScmInfo(currentPath)
	if scmInfo != nil {
		log.Info(fmt.Sprintf("当前正在项目根目录, 直接获取 scmInfo=%+v", scmInfo))
		return scmInfo, nil
	}

	if rootPath == "" {
		// 拉缓存中的项目根目录信息
		var scmInfoCache = model.GetScmInfoCache()
		if utilos.IsParentPath(scmInfoCache.ProjectRoot, currentPath) {
			rootPath = scmInfoCache.ProjectRoot
			log.Info(fmt.Sprintf("get project root path from cache: %s", rootPath))
		}
	}

	if rootPath != "" {
		scmInfo = scm.GetScmInfo(rootPath)
		if scmInfo == nil {
			scmInfo = &scm.ScmInfo{
				ProjectRoot: rootPath,
			}
		}

		return scmInfo, nil
	}

	scmInfo = scm.SearchProjRoot(currentPath)
	if scmInfo == nil {
		log.Info(fmt.Sprintf("无法推断出项目根目录, 无法找到 scm 信息, 以 currentPath=%s 作为项目根目录", currentPath))
		scmInfo = &scm.ScmInfo{
			ProjectRoot: currentPath,
		}
	}

	return scmInfo, nil
}

func initTaskInfo(rootPath, codeCCDir, scmType string) (*model.TaskInfo, int, error) {
	log := logger.GetLogger()
	log.Info(fmt.Sprintf("init task info, rootPath=%s", rootPath))

	mustInsert := false
	oldTaskInfo, err := taskrepo.GetTaskInfoByRootDir(storage.DB, rootPath)
	if err != nil || oldTaskInfo == nil {
		taskId, err := storage.DB.IncrementAndGet(common.BaseDataBulk, common.LatestTaskId)
		if err != nil {
			return nil, taskrepo.Noop, fmt.Errorf("生成任务 id 失败: %w", err)
		}

		oldTaskInfo = taskrepo.NewTaskInfoEntity(taskId, rootPath, scmType)
		mustInsert = true
	}

	var newTaskInfo *taskrepo.TaskInfoEntity = nil

	// 检查 taskInfo.json 文件是否存在
	taskInfoPath := path.Join(codeCCDir, taskInfoJson)
	if _, err := os.Stat(taskInfoPath); err == nil {
		/****************************************************************
		 * 注意: taskInfo.json 文件主要关注一些用户可参与修改的信息，比如规则集名, 黑白名单.
		 * 同样, 这些信息修改后需要同步修改数据库和文件.
		 ****************************************************************/

		// 读取文件内容
		data, err := os.ReadFile(taskInfoPath)
		if err != nil {
			log.Warn(fmt.Sprintf("读取 taskInfo.json 文件失败: %s", err.Error()))
		} else {
			newTaskInfo = new(taskrepo.TaskInfoEntity)
			// 反序列化为 TaskInfoEntity
			if err = newTaskInfo.Decode(data); err != nil {
				log.Warn(fmt.Sprintf("反序列化原 taskInfo.json 失败: %s", err.Error()))
				newTaskInfo = nil
			}
		}
	}

	saveType := 0
	// 在 oldTaskInfo 上更新 newTaskInfo 的数据
	if oldTaskInfo.MergeUpdate(newTaskInfo) {
		// 如果有更新，则保存
		saveType = taskrepo.UpsertDB + taskrepo.WriteFile
	} else if mustInsert {
		// mustInsert 代表 oldTaskInfo 是前面刚 new 出来的, 需要更新数据库
		saveType = taskrepo.InsertDB
	} else if newTaskInfo == nil {
		// newTaskInfo 为空, 代表 .codecc 目录下的 taskInfo.json 不存在或有问题, 只写文件即可
		saveType = taskrepo.WriteFile
	} else {
		// 没有更新
		saveType = taskrepo.Noop
	}

	log.Info(fmt.Sprintf("init task info end, saveType=%d", saveType))
	return model.NewTaskInfo(oldTaskInfo), saveType, nil
}

// reloadCheckerSets 重新加载项目规则集
func reloadCheckerSets(taskInfo *model.TaskInfo, checkerSetDir string, toolRec map[string]bool) int {
	log := logger.GetLogger()

	log.Info(fmt.Sprintf("reload checker sets, checkerSetDir=%s", checkerSetDir))

	err, checkerSets := checker.ReloadCheckerSet(checkerSetDir)
	if err != nil {
		log.Error(fmt.Sprintf("加载用户自定义规则集失败: %v", err))
		return taskrepo.Noop
	}

	log.Info(fmt.Sprintf("加载用户自定义规则集成功, len=%d", len(checkerSets)))

	if len(checkerSets) == 0 {
		log.Warn(fmt.Sprintf("没有找到规则集"))
		return taskrepo.Noop
	}

	checkerSetIds := make([]string, 0)
	for _, checkerSet := range checkerSets {
		log.Debug(fmt.Sprintf("save checker set %v", checkerSet))
		if err := checkermodel.SaveCheckerSet(storage.DB, checkerSet, taskInfo.TaskId); err != nil {
			log.Error(fmt.Sprintf("保存用户规则集 %s 失败: %v", checkerSet.CheckerSetId, err))
			continue
		}

		checkerSetIds = append(checkerSetIds, checkerSet.CheckerSetId)
		toolRec[checkerSet.ToolName] = true
	}

	sort.Strings(checkerSetIds)
	if len(checkerSetIds) != len(taskInfo.CheckerSet) || !util.IsSameStringSlice(taskInfo.CheckerSet, checkerSetIds) {
		taskInfo.CheckerSet = checkerSetIds
		return taskrepo.UpsertDB
	}

	return taskrepo.Noop
}

// 1. 刷新用户自定义规则集的信息.
// 2. 检查任务所用到的所有工具, 对于二进制扫描类型的工具, 如果本地没有二进制或者本地的二进制不是最新版本, 则下载工具.
func reloadCheckerSetsAndTools(taskInfo *model.TaskInfo, saveType *int,
	needReloadTools bool, checkerSetDir string) error {
	if taskInfo == nil {
		return perror.ErrUnknownError
	}

	log := logger.GetLogger()
	tools := make(map[string]bool)

	// 每次初始化都是从 .codecc/checkerset 目录读取
	cSaveType := reloadCheckerSets(taskInfo, checkerSetDir, tools)
	if cSaveType == taskrepo.UpsertDB && (*saveType == taskrepo.Noop || *saveType == taskrepo.WriteFile) {
		*saveType = taskrepo.UpsertDB + taskrepo.WriteFile
	}

	toolNames := make([]string, 0, len(tools))
	for toolName := range tools {
		toolNames = append(toolNames, toolName)
	}

	if len(toolNames) == 0 {
		log.Info("没有配置有效规则集")
		return perror.ErrInvalidCheckerSet
	}

	taskInfo.Tools = toolNames

	err := checker.ReloadToolCheckers(toolNames)
	if err != nil {
		log.Error(fmt.Sprintf("重载工具规则失败: %v", err))
	}

	log.Info(fmt.Sprintf("工具集: %v", toolNames))

	if needReloadTools {
		return tool.ReloadTools(toolNames)
	}

	return nil
}
