package repository

import (
	"codecc/preci_server/internal/infra/storage"
	"encoding/json"
	"fmt"
)

type CheckerSetEntity struct {
	CheckerSetId   string          `json:"checkerSetId"`   // 规则集 ID
	CheckerSetName string          `json:"checkerSetName"` // 规则集名称
	ToolName       string          `json:"toolName"`
	Checkers       []string        `json:"checkers"`
	CheckerOptions []CheckerOption `json:"checkerOptions"`
}

type CheckerOption struct {
	CheckerId     string            `json:"checkerId"`
	CheckerOption map[string]string `json:"checkerOption"`
}

type ToolCheckerSetEntity struct {
	ToolName       string          `json:"toolName"`
	Checkers       []string        `json:"checkers"`
	CheckerOptions []CheckerOption `json:"checkerOptions"`
}

const bucketName = "b_checker_set"

func (t *CheckerSetEntity) Encode() ([]byte, error) {
	jsonData, err := json.Marshal(t)
	if err != nil {
		return nil, fmt.Errorf("JSON序列化失败: %w", err)
	}
	return jsonData, nil
}

// genStoreKey 在存储 key 前增加 [taskId]# 前缀
func genStoreKey(checkerSetId string, taskId int64) string {
	return fmt.Sprintf("%d#%s", taskId, checkerSetId)
}

// Save 将token信息保存到 db
func (t *CheckerSetEntity) Save(sto storage.Storage, taskId int64) error {
	jsonData, err := t.Encode()
	if err != nil {
		return err
	}

	key := genStoreKey(t.CheckerSetId, taskId)
	err = sto.UpdateByStrKey(bucketName, key, jsonData)

	return nil
}

func GetByCheckerSetIdIn(sto storage.Storage, checkerSetIds []string, taskId int64) []*CheckerSetEntity {
	var result []*CheckerSetEntity
	for _, checkerSetId := range checkerSetIds {
		if checkerSetId == "" {
			continue
		}
		key := genStoreKey(checkerSetId, taskId)
		data, err := sto.GetByStrKey(bucketName, key)
		if err != nil || len(data) == 0 {
			continue
		}

		checkerSetEntity := new(CheckerSetEntity)
		err = json.Unmarshal(data, checkerSetEntity)
		if err != nil {
			continue
		}

		result = append(result, checkerSetEntity)
	}

	return result
}
