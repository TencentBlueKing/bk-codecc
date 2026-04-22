package repository

import (
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util"
	"encoding/json"
	"time"
)

const bucketName = "b_task_info"

const (
	Noop      = 0
	InsertDB  = 1
	UpsertDB  = 2
	WriteFile = 4
)

type TaskInfoEntity struct {
	TaskId              int64    `json:"taskId"`     // 任务 ID
	RootDir             string   `json:"rootDir"`    // 项目根目录
	ScmType             string   `json:"scmType"`    // scm 类型
	EnableSCC           bool     `json:"enableSCC"`  // 是否启用 SCC
	CheckerSet          []string `json:"checkerSet"` // 规则集
	WhitePaths          []string `json:"whitePaths"`
	BlackPaths          []string `json:"blackPaths"`
	storage.AuditEntity `json:"auditEntity"`
}

func NewTaskInfoEntity(taskId int64, rootDir, scmType string) *TaskInfoEntity {
	return &TaskInfoEntity{
		TaskId:     taskId,
		RootDir:    rootDir,
		ScmType:    scmType,
		EnableSCC:  false,
		CheckerSet: []string{},
		WhitePaths: []string{},
		BlackPaths: []string{},
	}
}

func (t *TaskInfoEntity) Decode(data []byte) error {
	return json.Unmarshal(data, t)
}

// Encode 将结构体编码为 []byte 格式的 json 数据, 后续用于存储. 对于 nil 的字段, 会初始化为空数组.
func (t *TaskInfoEntity) Encode() ([]byte, error) {
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

func (t *TaskInfoEntity) Save(sto storage.Storage, saveType int) error {
	now := time.Now().Unix()
	// 如果是新数据, 设置 CreatedAt
	if (saveType & InsertDB) > 0 {
		t.CreatedAt = now
		err := sto.UpdateByStrKey2Int(bucketName, t.RootDir, t.TaskId)
		if err != nil {
			return err
		}
	}
	t.UpdatedAt = now

	jsonData, err := t.Encode()
	if err != nil {
		return err
	}

	return sto.UpdateByIntKey(bucketName, t.TaskId, jsonData)
}

// MergeUpdate 用新的 TaskInfoEntity 合并更新到当前实体。新实体有的字段以新的为准，没有的字段保持原来的值. 返回是否有字段更新
func (t *TaskInfoEntity) MergeUpdate(newEntity *TaskInfoEntity) bool {
	isUpdated := false
	if newEntity == nil {
		return false
	}

	// 只在 condition 为 true 时更新
	updateField := func(condition bool, updateFunc func()) {
		if condition {
			updateFunc()
			isUpdated = true
		}
	}

	updateField(newEntity.ScmType != "" && newEntity.ScmType != t.ScmType, func() {
		t.ScmType = newEntity.ScmType
	})
	updateField(newEntity.EnableSCC != t.EnableSCC, func() {
		t.EnableSCC = newEntity.EnableSCC
	})

	// 更新切片字段. 注意: checkerSet 字段需要单独更新
	updateField(sliceIsUpdated(newEntity.WhitePaths, t.WhitePaths), func() {
		t.WhitePaths = newEntity.WhitePaths
	})
	updateField(sliceIsUpdated(newEntity.BlackPaths, t.BlackPaths), func() {
		t.BlackPaths = newEntity.BlackPaths
	})

	return isUpdated
}

// GetTaskInfoByRootDir 根据 rootDir 从 db 获取任务信息
func GetTaskInfoByRootDir(sto storage.Storage, rootDir string) (*TaskInfoEntity, error) {
	data, err := sto.GetByStrKey(bucketName, rootDir)
	if err != nil {
		return nil, err
	}

	taskId, err := util.BytesToInt64(data)
	if err != nil {
		return nil, err
	}

	data, err = sto.GetByIntKey(bucketName, taskId)
	if err != nil {
		return nil, err
	}

	taskInfo := &TaskInfoEntity{}
	err = taskInfo.Decode(data)
	if err != nil {
		return nil, err
	}

	return taskInfo, nil
}

// sliceIsUpdated 判断 new 是否相对于 old 有更新.
func sliceIsUpdated(new, old []string) bool {
	// 如果新切片为 nil，说明没有更新. (注意跟 [] 的区别.)
	if new == nil {
		return false
	}

	if len(new) != len(old) {
		return true
	}

	for i := range new {
		if new[i] != old[i] {
			return true
		}
	}

	return false
}
