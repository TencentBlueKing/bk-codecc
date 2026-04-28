package checker

import (
	"codecc/preci_server/internal/domain/checker/model"
	"encoding/json"
)

// CheckerSetListResp 查询规则集列表接口的响应
type CheckerSetListResp struct {
	CheckerSets []model.CheckerSet `json:"checkerSets"`
}

// Encode 将当前响应序列化为 JSON 字节流
func (resp *CheckerSetListResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}

// SelectReq 选择规则集接口的请求，指定项目根目录下要启用的规则集列表
type SelectReq struct {
	ProjectRootDir string   `json:"projectRootDir"`
	CheckerSets    []string `json:"checkerSets"`
}

// Decode 从 JSON 字节流反序列化到当前请求
func (sreq *SelectReq) Decode(data []byte) error {
	return json.Unmarshal(data, sreq)
}

// SelectResp 选择规则集接口的响应，返回本次生效的规则集列表
type SelectResp struct {
	ProjectRoot string   `json:"projectRoot"`
	CheckerSets []string `json:"checkerSets"`
}

// Encode 将当前响应序列化为 JSON 字节流
func (sresp *SelectResp) Encode() ([]byte, error) {
	return json.Marshal(sresp)
}
