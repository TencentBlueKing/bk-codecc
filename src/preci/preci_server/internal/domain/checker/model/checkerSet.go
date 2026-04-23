package model

import (
	"codecc/preci_server/internal/domain/checker/model/repository"
	"codecc/preci_server/internal/infra/storage"
	"encoding/json"
)

type CheckerSet struct {
	CheckerSetId   string                     `json:"checkerSetId"`   // 规则集 ID
	CheckerSetName string                     `json:"checkerSetName"` // 规则集名称
	ToolName       string                     `json:"toolName"`
	Checkers       []string                   `json:"checkers"`
	CheckerOptions []repository.CheckerOption `json:"checkerOptions"`
}

func (cs *CheckerSet) Decode(data []byte) error {
	return json.Unmarshal(data, cs)
}

// SaveCheckerSet 保存规则集信息
func SaveCheckerSet(sto storage.Storage, checkerSet *CheckerSet, taskId int64) error {
	entity := repository.CheckerSetEntity{
		CheckerSetName: checkerSet.CheckerSetName,
		CheckerSetId:   checkerSet.CheckerSetId,
		ToolName:       checkerSet.ToolName,
		Checkers:       checkerSet.Checkers,
		CheckerOptions: checkerSet.CheckerOptions,
	}

	return entity.Save(sto, taskId)
}
