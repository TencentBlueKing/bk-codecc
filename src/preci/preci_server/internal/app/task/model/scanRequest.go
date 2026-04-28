package model

import "encoding/json"

// ScanRequest 启动扫描接口的请求，指定扫描类型、待扫描路径列表和项目根目录
type ScanRequest struct {
	ScanType int      `json:"scanType" validate:"required"`
	Paths    []string `json:"paths"`
	RootDir  string   `json:"rootDir"`
}

// Decode 从 JSON 字节流反序列化到当前请求
func (sr *ScanRequest) Decode(data []byte) error {
	return json.Unmarshal(data, sr)
}

// ScanResponse 启动扫描接口的响应，返回提示信息、被触发的工具列表及本次扫描的文件数
type ScanResponse struct {
	Message     string   `json:"message"`
	Tools       []string `json:"tools"`
	ScanFileNum int      `json:"scanFileNum"`
}

// Encode 将当前响应序列化为 JSON 字节流
func (sr *ScanResponse) Encode() ([]byte, error) {
	return json.Marshal(sr)
}
