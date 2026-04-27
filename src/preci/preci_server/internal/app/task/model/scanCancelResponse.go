package model

import "encoding/json"

// ScanCancelResponse 取消扫描接口的响应，返回已取消扫描任务的项目根目录
type ScanCancelResponse struct {
	ProjectRoot string `json:"projectRoot"`
}

func (resp *ScanCancelResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
