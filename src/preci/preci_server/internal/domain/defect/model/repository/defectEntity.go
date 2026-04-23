package repository

import (
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"encoding/json"
	"fmt"
	"path/filepath"
	"strconv"
	"strings"
)

const bucketName = "b_defect_batch"

type DefectEntity struct {
	ToolName    string `json:"toolName"`
	CheckerName string `json:"checkerName"`
	Description string `json:"description"`
	FilePath    string `json:"filePath"`
	Line        int    `json:"line"`
	Severity    int    `json:"severity"`
}

func Save(sto storage.Storage, key string, defects []string) error {
	data, err := json.Marshal(defects)
	if err != nil {
		return err
	}

	return sto.UpdateByStrKey(bucketName, key, data)
}

// DeleteProjectAllDefects 删除项目下的所有 defect
func DeleteProjectAllDefects(sto storage.Storage, projectRoot string) error {
	if !strings.HasSuffix(projectRoot, string(filepath.Separator)) {
		projectRoot = projectRoot + string(filepath.Separator)
	}

	// 找到所有 key 以 projectRoot 开头的, 并且从 db 中删除
	return sto.DeleteByPrefix(bucketName, projectRoot)
}

func GetByPathPre(sto storage.Storage, pathPre string) ([]*DefectEntity, error) {
	log := logger.GetLogger()

	// 使用前缀查询获取所有匹配的键值对
	results, err := sto.GetByPrefix(bucketName, pathPre)
	if err != nil {
		log.Error(fmt.Sprintf("failed to get by prefix: %s, error: %v", pathPre, err))
		return nil, fmt.Errorf("failed to get by prefix: %w", err)
	}

	// 如果没有找到任何匹配的记录
	if len(results) == 0 {
		return []*DefectEntity{}, nil
	}

	// 合并所有匹配的 defects
	var allDefects []*DefectEntity
	for key, data := range results {
		filePath, toolName, checkerName := getInfoFromKey(key)
		if filePath == "" || toolName == "" || checkerName == "" {
			log.Error(fmt.Sprintf("invalid key: %s", key))
			continue
		}

		var lineInfoData []string
		if err := json.Unmarshal(data, &lineInfoData); err != nil {
			log.Error(fmt.Sprintf("failed to unmarshal defects for key %s: %v", key, err))
			continue
		}

		for _, lineInfo := range lineInfoData {
			line, description := getInfoFromValue(lineInfo)
			if line == 0 || description == "" {
				log.Error(fmt.Sprintf("invalid line info: %s", lineInfo))
				continue
			}

			defect := DefectEntity{
				ToolName:    toolName,
				CheckerName: checkerName,
				Description: description,
				FilePath:    filePath,
				Line:        line,
			}

			allDefects = append(allDefects, &defect)
		}
	}

	return allDefects, nil
}

func getInfoFromValue(value string) (int, string) {
	idx := strings.Index(value, "#")
	if idx == -1 {
		return 0, ""
	}

	linNum, err := strconv.Atoi(value[:idx])
	if err != nil {
		return 0, ""
	}

	return linNum, value[idx+1:]
}

func getInfoFromKey(key string) (string, string, string) {
	idx1 := strings.LastIndex(key, "#")
	if idx1 == -1 {
		return "", "", ""
	}
	checkerName := key[idx1+1:]

	remain := key[:idx1]
	idx2 := strings.LastIndex(remain, "#")
	if idx2 == -1 {
		return "", "", ""
	}
	toolName := remain[idx2+1:]

	filePath := remain[:idx2]

	return filePath, toolName, checkerName
}
