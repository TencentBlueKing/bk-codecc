package task

import (
	"codecc/preci_server/internal/domain/checker"
	defectmodel "codecc/preci_server/internal/domain/defect/model"
	"codecc/preci_server/internal/domain/task/model"
	"codecc/preci_server/internal/domain/tool"
	toolmodel "codecc/preci_server/internal/domain/tool/model"
	model2 "codecc/preci_server/internal/infra/config/model"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/thirdparty/codecctoolsdk/core"
	codeccos "codecc/preci_server/internal/thirdparty/codecctoolsdk/os"
	"codecc/preci_server/internal/util/constant"
	"codecc/preci_server/internal/util/perror"
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

func getTaskInfo(rootDir string) (*model.TaskInfo, error) {
	log := logger.GetLogger()

	taskInfo, err := model.GetTaskInfo(storage.DB, model.GetTaskInfoCache(), rootDir)
	if err != nil {
		log.Warn(fmt.Sprintf("rootDir=%s. failed to get task info directly: %v, %v.", rootDir, taskInfo, err))
		if rootDir == "" {
			return nil, err
		}

		taskInfo, _, err = Init(rootDir, rootDir, true)
		if err != nil {
			log.Error(fmt.Sprintf("rootDir=%s. failed to init task info: %v.", rootDir, err))
			return nil, err
		}
	}

	return taskInfo, nil
}

func ScanSCC(rootDir string, scanType int, incrementalFiles []string, projectId string,
	taskInfo *model.TaskInfo, selectCheckerSet bool) error {
	log := logger.GetLogger()
	if !taskInfo.EnableSCC {
		log.Info("没有开启 SCC 扫描")
		return nil
	}

	sccScanInput, err := GenerateSCCScanInput(scanType, incrementalFiles, projectId, taskInfo)
	if err != nil {
		return err
	}

	progress, err := NewScanProgress(rootDir, []*toolmodel.ToolScanInput{sccScanInput})
	if err != nil {
		log.Error(fmt.Sprintf("rootDir=%s. failed to new scan progress: %v.", rootDir, err))
		return err
	}

	err = tool.ReloadTools([]string{SCC})
	if err != nil {
		log.Error(fmt.Sprintf("failed to reload SCC: %v.", err))
		return err
	}

	// SCC 的输出不做过滤
	runSingleTool(rootDir, sccScanInput, progress, nil, nil)
	if !progress.IsDone(SCC) {
		markSCCScanError([]*toolmodel.ToolScanInput{sccScanInput}, progress)
		log.Error("SCC 扫描失败，扫描流程结束。")
		return perror.ErrSCCScan
	}

	if !selectCheckerSet {
		return nil
	}

	// 用户未配置规则集，根据 SCC 产出的 languages.txt 自动使用默认规则集
	langs := readLanguagesFromSCC(rootDir)
	if len(langs) > 0 {
		seenCheckerSet := make(map[string]bool) // 去重：同一 checkerSetId 只处理一次
		checkerSetIds := make([]string, 0)
		for lang := range langs {
			checkerSetId, ok := defaultCheckerSetByLang[lang]
			if !ok || seenCheckerSet[checkerSetId] {
				continue
			}
			seenCheckerSet[checkerSetId] = true
			checkerSetIds = append(checkerSetIds, checkerSetId)
		}

		checkerSetIds, err := checker.SelectCheckerSet(rootDir, checkerSetIds)
		if err != nil {
			log.Error(fmt.Sprintf("rootDir=%s. failed to select checker set: %v.", rootDir, err))
			return err
		}

		log.Info(fmt.Sprintf("SCC自动选取如下规则集：%v", checkerSetIds))
	}

	return nil
}

func scanCore(rootDir string, scanType int, incrementalFiles []string) (message string, tools []string, err error) {
	log := logger.GetLogger()

	taskInfo, err := getTaskInfo(rootDir)
	if err != nil {
		return "", nil, err
	}

	projectId, err := getProjectId()
	if err != nil {
		log.Error(fmt.Sprintf("Failed to get project id, err: %v", err))
		return "", nil, perror.ErrInvalidProjectId
	}

	localInputs, err := GenerateScanInput(scanType, incrementalFiles, projectId, taskInfo)
	if err != nil {
		return "", nil, err
	}

	if len(localInputs) == 0 {
		return "没有扫描工具配置", nil, nil
	}

	tools = make([]string, 0, len(localInputs))
	for _, input := range localInputs {
		tools = append(tools, input.ToolName)
	}

	progress, err := NewScanProgress(rootDir, localInputs)
	if err != nil {
		log.Error(fmt.Sprintf("failed to create scan progress: %v.", err))
		return "", nil, err
	}

	if len(localInputs) > 0 {
		go runScanInBackground(rootDir, localInputs, progress, taskInfo.WhitePaths, taskInfo.BlackPaths)
	}

	return "", tools, nil
}

func fullScan(rootDir string) (message string, tools []string, err error) {
	return scanCore(rootDir, constant.FullScan, []string{})
}

func searchAllFiles(now string, incrementalFiles *[]string, whitePaths, blackPaths []string) (reachedMaxLimit bool) {
	log := logger.GetLogger()
	maxIncFile := model2.GlobalConf.GetMaxIncrementalFileSize()
	if len(*incrementalFiles) >= maxIncFile {
		return true
	}

	fileInfo, err := os.Stat(now)
	if err != nil {
		if os.IsNotExist(err) {
			log.Warn(fmt.Sprintf("路径 %s 不存在，跳过", now))
			return false
		}
		log.Warn(fmt.Sprintf("无法访问路径 %s: %s，跳过", now, err.Error()))
		return false
	}

	if isHidden, err := codeccos.IsHidden(now); err != nil || isHidden {
		log.Warn(fmt.Sprintf("路径 %s 是隐藏的，跳过", now))
		if err != nil {
			log.Error(fmt.Sprintf("error: %s", err.Error()))
		}
		return false
	}

	if !fileInfo.IsDir() {
		// 文件直接添加
		*incrementalFiles = append(*incrementalFiles, now)
		return false
	}

	// 递归遍历目录下的所有非隐藏文件
	err = filepath.Walk(now, func(filePath string, info os.FileInfo, err error) error {
		if len(*incrementalFiles) >= maxIncFile {
			return filepath.SkipAll // 达到上限，停止遍历
		}

		if err != nil {
			log.Warn(fmt.Sprintf("访问路径 %s 失败: %v，跳过", filePath, err))
			return nil // 继续遍历其他文件
		}

		// 隐藏的文件/目录都跳过
		if isHidden, err := codeccos.IsHidden(filePath); err != nil || isHidden {
			if info.IsDir() {
				return filepath.SkipDir // 跳过整个隐藏目录
			}
			return nil
		}

		// 黑白名单过滤
		if !core.PathFilter(filePath, whitePaths, blackPaths) {
			if info.IsDir() {
				return filepath.SkipDir
			}
			return nil
		}

		if info.IsDir() {
			return nil // 目录不需要添加，继续遍历其子内容
		}

		// 添加文件到列表
		*incrementalFiles = append(*incrementalFiles, filePath)
		return nil
	})

	if err != nil {
		log.Warn(fmt.Sprintf("遍历目录 %s 失败: %s", now, err.Error()))
		return false
	}

	return len(*incrementalFiles) >= maxIncFile
}

func targetScan(rootDir string, targetPaths []string) (message string, tools []string, fileNum int, err error) {
	var incrementalFiles []string

	taskInfo, err := getTaskInfo(rootDir)
	if err != nil {
		return "", nil, 0, err
	}

	reachedMaxLimit := false
	for _, path := range targetPaths {
		if !core.PathFilter(path, taskInfo.WhitePaths, taskInfo.BlackPaths) {
			continue
		}
		if reachedMaxLimit = searchAllFiles(path, &incrementalFiles,
			taskInfo.WhitePaths, taskInfo.BlackPaths); reachedMaxLimit {
			break
		}
	}

	message, tools, err = scanCore(rootDir, constant.TargetScan, incrementalFiles)
	if reachedMaxLimit {
		message += fmt.Sprintf("超过 MaxIncrementalFileSize(%d), 被熔断\n",
			model2.GlobalConf.GetMaxIncrementalFileSize())
	}

	return message, tools, len(incrementalFiles), err
}

func runSingleTool(rootDir string, input *toolmodel.ToolScanInput,
	progress *ScanProgress, whitePaths, blackPaths []string) {
	log := logger.GetLogger()

	// 添加 panic 恢复机制，防止单个工具的 panic 导致整个服务崩溃
	defer func() {
		if r := recover(); r != nil {
			log.Error(fmt.Sprintf("[%s] panic recovered: %v", input.ToolName, r))
			progress.MarkFailed(input.ToolName, fmt.Errorf("panic: %v", r))
		}
	}()

	toolName := input.ToolName

	// 为每个工具创建独立的 context
	ctx, cancel := context.WithCancel(context.Background())
	progress.SetCancelFunc(toolName, cancel)

	progress.MarkRunning(toolName)

	log.Info(fmt.Sprintf("[%s] 开始扫描!", toolName))

	outputJsonPath, err := tool.RunTool(ctx, rootDir, input)
	if err != nil {
		log.Error(fmt.Sprintf("[%s] 扫描失败: %v.", toolName, err))
		progress.MarkFailed(toolName, err)
	}
	log.Info(fmt.Sprintf("[%s] 扫描完成, outputJsonPath=%s.", toolName, outputJsonPath))

	openCheckers := make(map[string]bool)
	for _, openChecker := range input.OpenCheckers {
		openCheckers[openChecker.CheckerName] = true
	}

	err = loadOutputDefect(toolName, outputJsonPath, openCheckers, whitePaths, blackPaths)
	if err != nil {
		log.Error(fmt.Sprintf("[%s] 保存代码问题失败: %v.", toolName, err))
		progress.MarkFailed(toolName, err)
	}

	log.Info(fmt.Sprintf("[%s] 保存代码问题成功, 扫描成功!", toolName))
	progress.MarkDone(toolName)
}

func markSCCScanError(toolScanInputs []*toolmodel.ToolScanInput, progress *ScanProgress) {
	for _, input := range toolScanInputs {
		progress.MarkFailed(input.ToolName, perror.ErrSCCScan)
	}
}

func runScanInBackground(rootDir string, toolScanInputs []*toolmodel.ToolScanInput,
	progress *ScanProgress, whitePaths, blackPaths []string) {
	log := logger.GetLogger()

	log.Info(fmt.Sprintf("开始扫描。rootDir=%s，工具数=%d，%d 个白名单，%d 个黑名单。",
		rootDir, len(toolScanInputs), len(whitePaths), len(blackPaths)))

	if err := defectmodel.DeleteProjectAllDefects(storage.DB, rootDir); err != nil {
		log.Error(fmt.Sprintf("failed to delete project all defects: %v.", err))
	}

	// 使用 WaitGroup 等待所有 goroutine 完成
	var wg sync.WaitGroup

	for _, toolScanInput := range toolScanInputs {
		wg.Add(1)
		// 显式传递参数，避免闭包捕获问题
		go func(input *toolmodel.ToolScanInput) {
			defer wg.Done()

			runSingleTool(rootDir, input, progress, whitePaths, blackPaths)
		}(toolScanInput)
	}

	// 等待所有工具扫描完成
	wg.Wait()

	// 输出最终汇总
	log.Info(fmt.Sprintf("扫描完成! 总耗时: %v", time.Since(progress.StartTime)))
	// 如果有任何错误，返回错误
	if progress.HasErrors() {
		errors := progress.GetErrors()
		for toolName, err := range errors {
			log.Error(fmt.Sprintf("工具 %s 扫描失败: %v", toolName, err))
		}
	}
}

func Cancel() string {
	resp := ""
	if NowScan != nil && !NowScan.IsComplete() {
		resp = NowScan.ProjectRoot
		NowScan.Cancel()
	}

	NowScan = nil

	return resp
}

func loadOutputDefect(toolName, jsonPath string, openCheckers map[string]bool, whitePaths, blackPaths []string) error {
	log := logger.GetLogger()

	data, err := os.ReadFile(jsonPath)
	if err != nil {
		log.Error(fmt.Sprintf("failed to read file: %v.", err))
		return perror.ErrFileReadFailed
	}

	toolScanOutput, err := defectmodel.LoadToolScanOutputJson(data)
	if err != nil {
		log.Error(fmt.Sprintf("failed to load tool scan output json: %v.", err))
		return perror.ErrJsonDecodeError
	}

	return toolScanOutput.Save(storage.DB, toolName, openCheckers, whitePaths, blackPaths)
}

// defaultCheckerSetByLang 根据 SCC 检测到的语言名，映射到对应的默认规则集 ID
var defaultCheckerSetByLang = map[string]string{
	"Go":         "standard_go",
	"Kotlin":     "standard_kotlin",
	"Python":     "standard_python_pylint",
	"JavaScript": "standard_javascript",
	"TypeScript": "standard_javascript",
	"C++":        "standard_cpp",
	"C Header":   "standard_cpp",
	"C#":         "standard_csharp",
}

// readLanguagesFromSCC 读取 SCC 产出的 languages.txt，返回语言名集合
func readLanguagesFromSCC(rootDir string) map[string]bool {
	log := logger.GetLogger()
	langFile := filepath.Join(rootDir, constant.CodeCCDir, SCC, "languages.txt")
	data, err := os.ReadFile(langFile)
	if err != nil {
		log.Warn(fmt.Sprintf("读取 languages.txt 失败: %v", err))
		return nil
	}
	parts := strings.Split(strings.TrimSpace(string(data)), ";")
	result := make(map[string]bool, len(parts))
	for _, lang := range parts {
		lang = strings.TrimSpace(lang)
		if lang != "" {
			result[lang] = true
		}
	}
	return result
}
