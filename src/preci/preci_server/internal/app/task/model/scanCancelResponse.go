package model

import "encoding/json"

// ScanCancelResponse 取消扫描接口的响应，返回已取消扫描任务的项目根目录
type ScanCancelResponse struct {
	ProjectRoot string `json:"projectRoot"`
}

// Encode 将当前响应序列化为 JSON 字节流
func (resp *ScanCancelResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
