// Package task implements the task domain, including scan orchestration,
// progress tracking and task metadata management.
package task

import (
	"codecc/preci_server/internal/domain/task/model"
	"codecc/preci_server/internal/domain/version"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/scm/git"
	"codecc/preci_server/internal/infra/storage"
	codeccos "codecc/preci_server/internal/thirdparty/codecctoolsdk/os"
	"codecc/preci_server/internal/util/constant"
	utilos "codecc/preci_server/internal/util/os"
	"codecc/preci_server/internal/util/perror"
	"fmt"
	"os"
	"path"
	"path/filepath"
	"strings"
)

const taskInfoJson = "taskInfo.json"

// UpdateCheckInfo 版本检查结果，用于在 Init 响应中通知调用方是否有可用更新
type UpdateCheckInfo struct {
	HasUpdate      bool
	CurrentVersion string
	LatestVersion  string
}

// Init 初始化. 负责:
//  1. 创建 .codecc 文件夹 (如果没有)
//  2. 刷新项目信息, 保存到 TaskInfoCache
//  3. 刷新工具信息
//  4. 检查版本更新（仅检查，不下载）
func Init(currentPath, rootPath string, needReloadTools bool) (*model.TaskInfo, *UpdateCheckInfo, error) {
	log := logger.GetLogger()

	projInfo, err := initProjInfo(currentPath, rootPath)
	if err != nil {
		return nil, nil, err
	}

	codeCCDir, err := mkCodeCCDir(projInfo.ProjectRoot)
	if err != nil {
		return nil, nil, err
	}

	taskInfo, saveType, err := initTaskInfo(projInfo.ProjectRoot, codeCCDir, projInfo.ScmType)
	defer func() {
		if taskInfo != nil {
			if err = taskInfo.Save(storage.DB, saveType, codeCCDir); err != nil {
				log.Error(fmt.Sprintf("Save TaskInfo fail: %v", err))
			}
		}
	}()

	if err != nil || taskInfo == nil {
		return nil, nil, err
	}

	checkerSetDir := filepath.Join(taskInfo.RootDir, constant.CodeCCDir, constant.CheckerSetDir)

	selectCheckerSet := false
	// 判断 checkerSetDir 目录下是否有文件
	entries, readDirErr := os.ReadDir(checkerSetDir)
	if readDirErr != nil || len(entries) == 0 {
		log.Info(fmt.Sprintf("目录 %s 为空或不存在", checkerSetDir))
		selectCheckerSet = true
	}

	if err = ScanSCC(projInfo.ProjectRoot, constant.FullScan, []string{}, "", taskInfo, selectCheckerSet); err != nil {
		return nil, nil, err
	}

	if err = reloadCheckerSetsAndTools(taskInfo, &saveType, needReloadTools, checkerSetDir); err != nil {
		return nil, nil, err
	}

	updateInfo := checkForUpdate()

	log.Info("初始化成功")

	return taskInfo, updateInfo, nil
}

// checkForUpdate 检查是否有可用的版本更新
func checkForUpdate() *UpdateCheckInfo {
	log := logger.GetLogger()

	currentVersion := version.GetVersion()

	latestVersion, err := version.GetLatestVersion()
	if err != nil {
		log.Error(fmt.Sprintf("GetLatestVersion fail: %v", err))
		return &UpdateCheckInfo{HasUpdate: false, CurrentVersion: currentVersion}
	}

	latestParts := strings.Split(latestVersion, ".")
	currentParts := strings.Split(currentVersion, ".")
	if len(latestParts) < 3 || len(currentParts) < 3 {
		log.Error(fmt.Sprintf("版本号格式不正确, latestVersion: %s, currentVersion: %s", latestVersion, currentVersion))
		return &UpdateCheckInfo{HasUpdate: false, CurrentVersion: currentVersion}
	}

	hasUpdate := latestParts[0] != currentParts[0] || latestParts[1] != currentParts[1]
	if hasUpdate {
		log.Info(fmt.Sprintf("检测到可用更新, 当前版本: %s, 最新版本: %s", currentVersion, latestVersion))
	}

	return &UpdateCheckInfo{
		HasUpdate:      hasUpdate,
		CurrentVersion: currentVersion,
		LatestVersion:  latestVersion,
	}
}

// GetScanProgress 获取扫描进度
func GetScanProgress(rootDir string) (*model.ScanProgress, error) {
	// 检查是否有正在执行的扫描任务
	if NowScan == nil {
		return nil, perror.ErrNoScanTask
	}

	if rootDir != "" && NowScan.ProjectRoot != rootDir {
		return nil, perror.ErrNoScanTask
	}

	toolStatuses := make(map[string]string)
	for _, tool := range NowScan.ToolStatuses {
		toolStatuses[tool.ToolName] = tool.Status
	}

	scanStatus := Running
	if NowScan.IsComplete() {
		scanStatus = Done
	}

	return &model.ScanProgress{
		ProjectRoot:  NowScan.ProjectRoot,
		ToolStatuses: toolStatuses,
		Status:       scanStatus,
	}, nil
}

// Scan 开始扫描. rootDir 是项目根目录, 必填.
// 根据 scanType 有不同的处理逻辑:
//
//	0(FullScan), 全项目扫描;
//	100(TargetScan), 目标扫描, paths 必填, 代表扫描文件的路径列表;
//	102(PreCommitScan), pre-commit 扫描;
//	103(PrePushScan), pre-push 扫描.
func Scan(scanType int, paths []string, rootDir string) (message string, tools []string, fileNum int, err error) {
	log := logger.GetLogger()
	log.Info(fmt.Sprintf("start scan. scanType: %d, paths: %v, rootDir: %s", scanType, paths, rootDir))

	// rootDir 必填
	if project, err := os.Stat(rootDir); err != nil || !project.IsDir() {
		log.Error(fmt.Sprintf("#FullScan error: 项目根目录无效. message: %v", err))
		return "", nil, 0, perror.ErrInvalidRootDir
	}

	switch scanType {
	case constant.FullScan:
		// 全项目扫描
		message, tools, err := fullScan(rootDir)
		return message, tools, 0, err

	case constant.TargetScan:
		// 目标扫描, paths 必填, 代表扫描文件的路径列表
		if paths == nil || len(paths) == 0 {
			return "", nil, 0, perror.ErrInvalidPaths
		}
		return targetScan(rootDir, paths)

	case constant.PreCommitScan, constant.PrePushScan:
		return gitHookScan(scanType, rootDir)
	}

	return "", nil, 0, fmt.Errorf("不支持的扫描类型: %d", scanType)
}

func gitHookScan(scanType int, rootDir string) (message string, tools []string, fileNum int, err error) {
	var paths []string
	if scanType == constant.PreCommitScan {
		paths, err = git.GetUncommittedFiles(rootDir)
		if err != nil {
			return "", nil, 0, err
		}
	} else {
		paths, err = git.GetUnpushedFiles(rootDir)
		if err != nil {
			return "", nil, 0, err
		}
	}

	if paths == nil || len(paths) == 0 {
		return "", []string{}, 0, nil
	}

	log := logger.GetLogger()

	// 过滤掉隐藏文件
	filteredPaths := make([]string, 0, len(paths))
	for _, p := range paths {
		isHidden, err := codeccos.IsHidden(p)
		if err != nil {
			log.Warn(fmt.Sprintf("check if file is hidden failed: %s, error: %s", p, err.Error()))
			continue
		}
		if !isHidden {
			filteredPaths = append(filteredPaths, p)
		}
	}

	// 如果过滤后没有文件，直接返回
	if len(filteredPaths) == 0 {
		return "", []string{}, 0, nil
	}

	return targetScan(rootDir, filteredPaths)
}

func mkCodeCCDir(rootPath string) (string, error) {
	log := logger.GetLogger()
	codeCCDir := path.Join(rootPath, constant.CodeCCDir)
	if err := utilos.MkDir(codeCCDir); err != nil {
		log.Error(fmt.Sprintf("create .codecc dir failed: %s", err.Error()))
		return "", err
	}

	return codeCCDir, nil
}
