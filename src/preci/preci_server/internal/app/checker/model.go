package checker

import (
	"encoding/json"
	"git.woa.com/codecc/preci_server/internal/domain/checker/model"
)

type CheckerSetListResp struct {
	CheckerSets []model.CheckerSet `json:"checkerSets"`
}

func (resp *CheckerSetListResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}

type SelectReq struct {
	ProjectRootDir string   `json:"projectRootDir"`
	CheckerSets    []string `json:"checkerSets"`
}

func (sreq *SelectReq) Decode(data []byte) error {
	return json.Unmarshal(data, sreq)
}

type SelectResp struct {
	ProjectRoot string   `json:"projectRoot"`
	CheckerSets []string `json:"checkerSets"`
}

func (sresp *SelectResp) Encode() ([]byte, error) {
	return json.Marshal(sresp)
}
