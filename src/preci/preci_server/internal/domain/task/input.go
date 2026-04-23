package task

import (
	checkerrepo "codecc/preci_server/internal/domain/checker/model/repository"
	"codecc/preci_server/internal/domain/task/model"
	model2 "codecc/preci_server/internal/domain/tool/model"
	"codecc/preci_server/internal/domain/tool/model/repository"
	"codecc/preci_server/internal/infra/cache"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util/constant"
	"codecc/preci_server/internal/util/perror"
	"fmt"
)

// Checkers 表示检查器的集合
type Checkers map[string][]model2.CheckerParam

var sccInput = &model2.ToolScanInput{
	ToolName: "SCC",
	Language: 1073741826,
	OpenCheckers: []model2.Checker{
		{
			CheckerName:   "CodeLineCount",
			NativeChecker: true,
			Severity:      3,
		},
	},
}

// processCheckerSets 处理规则集信息，提取工具和规则到toolCheckers中
func processCheckerSets(checkerSets []*checkerrepo.CheckerSetEntity, toolCheckers map[string]Checkers) {
	for _, checkerSetInfo := range checkerSets {
		toolName := checkerSetInfo.ToolName
		if _, exists := toolCheckers[toolName]; !exists {
			toolCheckers[toolName] = make(Checkers)
		}

		// 将规则添加到对应工具的集合中
		for _, checker := range checkerSetInfo.Checkers {
			toolCheckers[toolName][checker] = []model2.CheckerParam{}
		}

		for _, option := range checkerSetInfo.CheckerOptions {
			params := option.CheckerOption
			for k, v := range params {
				toolCheckers[toolName][option.CheckerId] =
					append(toolCheckers[toolName][option.CheckerId], model2.CheckerParam{Key: k, Value: v})
			}
		}
	}
}

func getProjectId() (string, error) {
	return cache.GetProjectId()
}

func GenerateSCCScanInput(scanType int, incrementalFiles []string, projectId string,
	taskInfo *model.TaskInfo) (*model2.ToolScanInput, error) {
	log := logger.GetLogger()

	if scanType != constant.FullScan && len(incrementalFiles) <= 0 {
		log.Warn(fmt.Sprintf("ScanType=%d, IncrementalFiles is empty, skip generating scan input", scanType))
		return nil, perror.ErrInvalidParam
	}

	if scanType == constant.FullScan {
		incrementalFiles = []string{}
	}

	sccInput.ProjectName = projectId
	sccInput.ProjectId = projectId
	sccInput.ScanPath = taskInfo.RootDir
	sccInput.WhitePaths = taskInfo.WhitePaths
	sccInput.BlackPaths = taskInfo.BlackPaths
	sccInput.ScanType = getScanTypeStr(scanType)
	sccInput.IncrementalFiles = incrementalFiles

	return sccInput, nil
}

func GenerateToolInputCore(scanType int, projectId, rootDir string, incrementalFiles []string,
	toolCheckers map[string]Checkers) (local []*model2.ToolScanInput) {
	log := logger.GetLogger()

	scanTypeStr := getScanTypeStr(scanType)

	for toolName, checkers := range toolCheckers {
		openCheckers := make([]model2.Checker, 0, len(checkers))
		for checkerName, checkerParams := range checkers {
			openCheckers = append(openCheckers, model2.Checker{
				CheckerName:   checkerName,
				NativeChecker: true, // todo: ??
				Severity:      1,
				CheckerParams: checkerParams,
			})
		}

		lang, _ := repository.GetToolLang(storage.DB, toolName)

		toolScanInput := &model2.ToolScanInput{
			ProjectName:  projectId,
			ProjectId:    projectId,
			ToolName:     toolName,
			ScanPath:     rootDir,
			Language:     lang,
			WhitePaths:   []string{},
			BlackPaths:   []string{},
			OpenCheckers: openCheckers,
		}

		toolScanInput.ScanType = scanTypeStr
		toolScanInput.IncrementalFiles = incrementalFiles
		local = append(local, toolScanInput)

		log.Info(fmt.Sprintf(
			"Generated scan input for tool: %s, checkers count: %d",
			toolName, len(openCheckers)))
	}

	return
}

func GenerateScanInput(scanType int, incrementalFiles []string, projectId string,
	taskInfo *model.TaskInfo) (local []*model2.ToolScanInput, err error) {
	log := logger.GetLogger()

	if scanType != constant.FullScan && len(incrementalFiles) <= 0 {
		log.Warn(fmt.Sprintf("ScanType=%d, IncrementalFiles is empty, skip generating scan input", scanType))
		return nil, perror.ErrInvalidParam
	}

	if scanType == constant.FullScan {
		incrementalFiles = []string{}
	}

	toolCheckers := make(map[string]Checkers)

	checkerSetInfos := checkerrepo.GetByCheckerSetIdIn(storage.DB, taskInfo.CheckerSet, taskInfo.TaskId)
	processCheckerSets(checkerSetInfos, toolCheckers)

	local = GenerateToolInputCore(scanType, projectId, taskInfo.RootDir, incrementalFiles, toolCheckers)
	return local, nil
}

func getScanTypeStr(scanType int) string {
	switch scanType {
	case constant.FullScan:
		return FullScanName
	case constant.TargetScan:
		return IncrementScanName
		// todo: 其他扫描类型
	default:
		return "full"
	}
}
