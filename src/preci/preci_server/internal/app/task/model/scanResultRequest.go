package model

import (
	defectmodel "codecc/preci_server/internal/domain/defect/model"
	"encoding/json"
)

// ScanResultRequest 查询扫描结果接口的请求，指定待查询的路径
type ScanResultRequest struct {
	Path string `json:"path" validate:"required"`
}

// Decode 从 JSON 字节流反序列化到当前请求
func (req *ScanResultRequest) Decode(data []byte) error {
	return json.Unmarshal(data, req)
}

// ScanResultResponse 查询扫描结果接口的响应，返回指定路径下的所有告警
type ScanResultResponse struct {
	Defects []defectmodel.Defect `json:"defects"`
}

// Encode 将当前响应序列化为 JSON 字节流
func (resp *ScanResultResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
