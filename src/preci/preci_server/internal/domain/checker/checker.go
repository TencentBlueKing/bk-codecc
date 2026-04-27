// Package checker implements the checker/checker-set domain, covering rule
// management and synchronization with the remote CodeCC service.
package checker

import (
	"codecc/preci_server/internal/domain/checker/model"
	"codecc/preci_server/internal/domain/checker/model/repository"
	"codecc/preci_server/internal/infra/client"
	config "codecc/preci_server/internal/infra/config/model"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util/constant"
	"codecc/preci_server/internal/util/perror"
	"fmt"
	"os"
	"path/filepath"
	"time"
)

func ReloadToolCheckers(toolNames []string) error {
	c := client.NewCodeCCClient()
	log := logger.GetLogger()
	log.Info(fmt.Sprintf("get tool checker severities for tools: %v", toolNames))

	toolCheckerSeverities, err := c.BatchGetCheckerSeverities(toolNames)
	if err != nil {
		return err
	}

	// 先将所有待更新的 checker severity 收集到 map 中
	kvs := make(map[string]int64)
	for _, tool := range toolCheckerSeverities.Tools {
		toolName := tool.ToolName
		for _, checker := range tool.Checkers {
			key := repository.BuildCheckerSeverityKey(toolName, checker.CheckerKey)
			kvs[key] = int64(checker.Severity)
		}
	}

	// 批量写入，合并为一次事务，避免多次 fsync
	startTime := time.Now()
	err = repository.BatchUpdateCheckerSeverity(storage.DB, kvs)
	elapsed := time.Since(startTime)

	if err != nil {
		log.Error(fmt.Sprintf("batch update checker severities failed: %s", err.Error()))
		return err
	}

	log.Info(fmt.Sprintf("batch update %d checker severities, cost %v", len(kvs), elapsed))

	return nil
}

func SelectCheckerSet(projectRootDir string, checkerSetIds []string) ([]string, error) {
	installDir := config.GlobalConf.GetInstallDir()
	checkerSetDir := filepath.Join(installDir, constant.CheckerSetDir)
	targetDir := filepath.Join(projectRootDir, constant.CodeCCDir, constant.CheckerSetDir)

	log := logger.GetLogger()

	// 如果目标目录不存在，则创建该目录
	if _, err := os.Stat(targetDir); os.IsNotExist(err) {
		if err := os.MkdirAll(targetDir, 0755); err != nil {
			log.Error(fmt.Sprintf("创建目录 %s 失败: %v", targetDir, err))
			return nil, perror.ErrDirectoryCreateFailed
		}
	}

	log.Info(fmt.Sprintf("get checker set from %s, to %s, checkerSetIds=%v",
		checkerSetDir, targetDir, checkerSetIds))

	var succCheckerSetIds []string
	// 在 checkerSetDir 找到 checkerSetIds 对应的 json 文件, 逐个复制到 targetDir 下
	// 如果 targetDir 中本来已经有对应的文件, 则覆盖
	for _, checkerSetId := range checkerSetIds {
		srcFile := filepath.Join(checkerSetDir, checkerSetId+JsonExt)
		dstFile := filepath.Join(targetDir, checkerSetId+JsonExt)

		// 检查源文件是否存在
		if _, err := os.Stat(srcFile); os.IsNotExist(err) {
			log.Error(fmt.Sprintf("源文件 %s 不存在", srcFile))
			continue
		}

		// 读取源文件
		data, err := os.ReadFile(srcFile)
		if err != nil {
			log.Error(fmt.Sprintf("读取源文件 %s 失败: %v", srcFile, err))
			continue
		}

		// 写入目标文件（覆盖已存在的文件）
		if err := os.WriteFile(dstFile, data, 0644); err != nil {
			log.Error(fmt.Sprintf("写入目标文件 %s 失败: %v", dstFile, err))
			continue
		}

		succCheckerSetIds = append(succCheckerSetIds, checkerSetId)
		log.Info(fmt.Sprintf("成功复制文件 %s 到 %s", srcFile, dstFile))
	}

	return succCheckerSetIds, nil
}

func UnselectCheckerSet(projectRootDir string, checkerSetIds []string) ([]string, error) {
	targetDir := filepath.Join(projectRootDir, constant.CodeCCDir, constant.CheckerSetDir)
	log := logger.GetLogger()

	log.Info(fmt.Sprintf("unselect checker set from %s, checkerSetIds=%v", targetDir, checkerSetIds))

	var succCheckerSetIds []string
	for _, checkerSetId := range checkerSetIds {
		targetFile := filepath.Join(targetDir, checkerSetId+JsonExt)

		if _, err := os.Stat(targetFile); err != nil {
			log.Warn(fmt.Sprintf("文件 %s 不存在或无法访问，跳过: %v", targetFile, err))
			continue
		}

		if err := os.Remove(targetFile); err != nil {
			log.Error(fmt.Sprintf("删除文件 %s 失败: %v", targetFile, err))
			continue
		}

		succCheckerSetIds = append(succCheckerSetIds, checkerSetId)
		log.Info(fmt.Sprintf("成功删除文件 %s", targetFile))
	}

	return succCheckerSetIds, nil
}

func ListOfficialCheckerSet() (error, []*model.CheckerSet) {
	installDir := config.GlobalConf.GetInstallDir()
	officialCheckerSetDir := filepath.Join(installDir, constant.CheckerSetDir)
	log := logger.GetLogger()
	log.Info(fmt.Sprintf("list official checker set from %s", officialCheckerSetDir))

	return ReloadCheckerSet(officialCheckerSetDir)
}

func ReloadCheckerSet(checkerSetDir string) (error, []*model.CheckerSet) {
	log := logger.GetLogger()

	dirEntries, err := os.ReadDir(checkerSetDir)
	if err != nil {
		log.Error(fmt.Sprintf("读取目录 %s 失败: %v", checkerSetDir, err))
		return perror.ErrDirectoryReadFailed, nil
	}

	var result []*model.CheckerSet
	for _, dirEntry := range dirEntries {
		if dirEntry.IsDir() {
			continue
		}

		// 判断文件名是否以 .json 结尾, 如果是, 读取文件内容
		if filepath.Ext(dirEntry.Name()) == JsonExt {
			file := filepath.Join(checkerSetDir, dirEntry.Name())
			data, err := os.ReadFile(file)
			if err != nil {
				log.Error(fmt.Sprintf("读取文件 %s 失败: %v", dirEntry.Name(), err))
				continue
			}

			checkerSet := new(model.CheckerSet)
			err = checkerSet.Decode(data)
			if err != nil {
				log.Error(fmt.Sprintf("反序列化文件 %s 失败: %v", dirEntry.Name(), err))
				continue
			}

			result = append(result, checkerSet)
		}
	}

	return nil, result
}
