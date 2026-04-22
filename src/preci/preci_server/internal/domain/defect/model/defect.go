package model

import (
	checkerRepo "codecc/preci_server/internal/domain/checker/model/repository"
	"codecc/preci_server/internal/domain/defect/model/repository"
	"codecc/preci_server/internal/domain/perror"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/thirdparty/codecctoolsdk/core"
	"codecc/preci_server/internal/util/os"
	"encoding/json"
	"fmt"
	"runtime"
	"sort"
	"strconv"
)

type Defect struct {
	ToolName    string `json:"toolName"`
	CheckerName string `json:"checkerName"`
	Description string `json:"description"`
	FilePath    string `json:"filePath"`
	Line        int    `json:"line"`
	Severity    int64  `json:"severity"`
}

type ToolScanOutput struct {
	Defects []Defect `json:"defects"`
}

func (tso *ToolScanOutput) filter(whitePaths, blackPaths []string) {
	if len(whitePaths) == 0 && len(blackPaths) == 0 {
		return
	}

	filtered := tso.Defects[:0]

	// 注意: 此处默认 Defects 已经按路径排序, 所以缓存了上次未通过 PathFilter 的路径。所以在 filter 之前先调用 sortDefects 可以提升效率
	lastUnfilteredPath := ""
	for i, defect := range tso.Defects {
		if defect.FilePath == lastUnfilteredPath {
			continue
		} else {
			lastUnfilteredPath = ""
		}

		if core.PathFilter(defect.FilePath, whitePaths, blackPaths) {
			filtered = append(filtered, tso.Defects[i])
		} else {
			lastUnfilteredPath = defect.FilePath
		}
	}

	for i := len(filtered); i < len(tso.Defects); i++ {
		tso.Defects[i] = Defect{}
	}
	tso.Defects = filtered
}

func (tso *ToolScanOutput) Decode(data []byte) error {
	return json.Unmarshal(data, tso)
}

func genKey(filePath, toolName, checkerName string) string {
	return filePath + "#" + toolName + "#" + checkerName
}

// pathConvert 为环境适配性所做的路径转换
func (tso *ToolScanOutput) pathConvert() {
	// 如果不是 windows 环境，直接退出
	if runtime.GOOS != "windows" || len(tso.Defects) == 0 {
		return
	}

	// 采样第一个路径, 判断是否需要做转换
	if !os.NeedPathConvert(tso.Defects[0].FilePath) {
		return
	}

	log := logger.GetLogger()
	log.Info("windows path convert start")

	for i := range tso.Defects {
		tso.Defects[i].FilePath = os.PathConvert(tso.Defects[i].FilePath)
	}
}

func (tso *ToolScanOutput) Save(sto storage.Storage, toolName string, openCheckers map[string]bool,
	whitePaths, blackPaths []string) error {
	log := logger.GetLogger()
	key := ""
	var value []string
	var retError error = nil
	total := 0

	tso.sortDefects()
	tso.pathConvert()
	tso.filter(whitePaths, blackPaths)

	for _, defect := range tso.Defects {
		if !openCheckers[defect.CheckerName] {
			continue
		}

		tempKey := genKey(defect.FilePath, toolName, defect.CheckerName)
		if tempKey != key {
			if key != "" && len(value) > 0 {
				err := repository.Save(sto, key, value)
				if err != nil {
					log.Error(fmt.Sprintf("save %s failed: %v", key, err))
					retError = perror.ErrStorageError
				} else {
					total += len(value)
				}
			}

			key = tempKey
			value = []string{}
		}

		tempValue := strconv.Itoa(defect.Line) + "#" + defect.Description
		value = append(value, tempValue)
	}

	if key != "" && len(value) > 0 {
		err := repository.Save(sto, key, value)
		if err != nil {
			log.Error(fmt.Sprintf("save %s failed: %v", key, err))
			return perror.ErrStorageError
		} else {
			total += len(value)
		}
	}

	log.Info(fmt.Sprintf("save %d success", total))

	return retError
}

// sortDefects 根据 FilePath 和 Line 对 Defects 进行排序
func (tso *ToolScanOutput) sortDefects() {
	sort.Slice(tso.Defects, func(i, j int) bool {
		// 首先按 FilePath 排序
		if tso.Defects[i].FilePath != tso.Defects[j].FilePath {
			return tso.Defects[i].FilePath < tso.Defects[j].FilePath
		}

		// 其次按 CheckerName 排序
		if tso.Defects[i].CheckerName != tso.Defects[j].CheckerName {
			return tso.Defects[i].CheckerName < tso.Defects[j].CheckerName
		}

		// 最后按 Line 排序
		return tso.Defects[i].Line < tso.Defects[j].Line
	})
}

func LoadToolScanOutputJson(data []byte) (*ToolScanOutput, error) {
	tso := new(ToolScanOutput)
	err := json.Unmarshal(data, &tso)
	if err != nil {
		return nil, err
	}

	return tso, nil
}

func DeleteProjectAllDefects(sto storage.Storage, projectRoot string) error {
	return repository.DeleteProjectAllDefects(sto, projectRoot)
}

func getCheckerSeverity(sto storage.Storage, toolName, checkerName string, cache map[string]int64) (int64, error) {
	key := toolName + "#" + checkerName
	log := logger.GetLogger()
	if severity, ok := cache[key]; ok {
		return severity, nil
	}
	severity, err := checkerRepo.GetCheckerSeverity(sto, toolName, checkerName)
	log.Info(fmt.Sprintf("%s#%s : %d", toolName, checkerName, severity))

	if err != nil {
		return 0, err
	}
	cache[key] = severity
	return severity, nil
}

func GetAllDefectsByPathPre(sto storage.Storage, pathPre string) ([]*Defect, error) {
	log := logger.GetLogger()
	defectEntities, err := repository.GetByPathPre(sto, pathPre)
	if err != nil {
		log.Error(fmt.Sprintf("get defects failed: %s", err.Error()))
		return []*Defect{}, nil
	}

	log.Info(fmt.Sprintf("get %d defects", len(defectEntities)))

	var result []*Defect
	cache := make(map[string]int64) // 初始化 cache

	for i, defectEntity := range defectEntities {
		severity, err := getCheckerSeverity(sto, defectEntity.ToolName, defectEntity.CheckerName, cache)
		if err != nil {
			log.Error(fmt.Sprintf("get checker %s#%s severity failed: %s",
				defectEntity.ToolName, defectEntity.CheckerName, err.Error()))
		}

		if i%1000 == 0 {
			log.Info(fmt.Sprintf("processing defects: %d/%d", i, len(defectEntities)))
		}

		result = append(result, &Defect{
			ToolName:    defectEntity.ToolName,
			FilePath:    defectEntity.FilePath,
			CheckerName: defectEntity.CheckerName,
			Line:        defectEntity.Line,
			Description: defectEntity.Description,
			Severity:    severity,
		})
	}

	log.Info(fmt.Sprintf("processed all %d defects successfully", len(result)))
	return result, nil
}
