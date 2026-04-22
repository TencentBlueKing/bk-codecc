package model

import (
	"codecc/preci_server/internal/domain/task/model/repository"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"encoding/json"
	"fmt"
	"os"
	"path"
)

const taskInfoJson = "taskInfo.json"

type TaskInfo struct {
	TaskId         int64    // 任务 ID
	RootDir        string   // 项目根目录
	ScmType        string   // scm 类型
	EnableSCC      bool     // 是否启用 SCC
	Tools          []string // 工具集
	CheckerSet     []string // 规则集
	UserCheckerSet []string // 用户自定义规则集
	WhitePaths     []string
	BlackPaths     []string
}

func NewTaskInfo(entity *repository.TaskInfoEntity) *TaskInfo {
	return &TaskInfo{
		TaskId:     entity.TaskId,
		RootDir:    entity.RootDir,
		ScmType:    entity.ScmType,
		EnableSCC:  entity.EnableSCC,
		CheckerSet: entity.CheckerSet,
		WhitePaths: entity.WhitePaths,
		BlackPaths: entity.BlackPaths,
	}
}

func GetTaskInfo(sto storage.Storage, cachedInfo TaskInfo, rootPath string) (*TaskInfo, error) {
	if rootPath == "" || cachedInfo.RootDir == rootPath {
		// 直接将 cachedInfo 中的数据返回
		return &cachedInfo, nil
	}

	entity, err := repository.GetTaskInfoByRootDir(sto, rootPath)
	if err != nil {
		return nil, err
	}

	return NewTaskInfo(entity), nil
}

// Encode 将结构体编码为 []byte 格式的 json 数据. 对于 nil 的字段, 会初始化为空数组.
func (t *TaskInfo) Encode() ([]byte, error) {
	if t.CheckerSet == nil {
		t.CheckerSet = []string{}
	}
	if t.WhitePaths == nil {
		t.WhitePaths = []string{}
	}
	if t.BlackPaths == nil {
		t.BlackPaths = []string{}
	}

	return json.Marshal(t)
}

func (t *TaskInfo) Save(sto storage.Storage, saveType int, codeCCDir string) error {
	if saveType == repository.Noop {
		return nil
	}

	SetTaskInfoCache(t)

	log := logger.GetLogger()
	if (saveType & (repository.InsertDB + repository.UpsertDB)) > 0 {
		entity := &repository.TaskInfoEntity{
			TaskId:     t.TaskId,
			RootDir:    t.RootDir,
			ScmType:    t.ScmType,
			EnableSCC:  t.EnableSCC,
			CheckerSet: t.CheckerSet,
			WhitePaths: t.WhitePaths,
			BlackPaths: t.BlackPaths,
		}

		err := entity.Save(sto, saveType)
		if err != nil {
			log.Error(fmt.Sprintf("保存失败! entity: %v\nerror: %v", t, err))
			return err
		} else {
			log.Info("保存 DB 成功")
		}
	}

	data, err := json.MarshalIndent(struct {
		TaskId     int64    `json:"TaskId"`
		RootDir    string   `json:"RootDir"`
		ScmType    string   `json:"ScmType"`
		EnableSCC  bool     `json:"EnableSCC"`
		Tools      []string `json:"Tools"`
		CheckerSet []string `json:"CheckerSet"`
		// 写入 taskInfo.json 时忽略 UserCheckerSet 字段
		WhitePaths []string `json:"WhitePaths"`
		BlackPaths []string `json:"BlackPaths"`
	}{
		TaskId:     t.TaskId,
		RootDir:    t.RootDir,
		ScmType:    t.ScmType,
		EnableSCC:  t.EnableSCC,
		CheckerSet: t.CheckerSet,
		WhitePaths: t.WhitePaths,
		BlackPaths: t.BlackPaths,
		Tools:      t.Tools,
	}, "", "  ")
	if err != nil {
		log.Error(fmt.Sprintf("序列化失败! entity: %v\nerror: %v", t, err))
	} else {
		taskInfoPath := path.Join(codeCCDir, taskInfoJson)
		if err = os.WriteFile(taskInfoPath, data, 0644); err != nil {
			log.Error(fmt.Sprintf("写入 taskInfo.json 文件失败: %v", err))
		} else {
			log.Info("写入 taskInfo.json 文件成功")
		}
	}

	return err
}
