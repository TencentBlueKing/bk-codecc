package model

import (
	defectmodel "codecc/preci_server/internal/domain/defect/model"
	"encoding/json"
)

// ScanResultRequest 查询扫描结果接口的请求，指定待查询的路径
type ScanResultRequest struct {
	Path string `json:"path" validate:"required"`
}

func (req *ScanResultRequest) Decode(data []byte) error {
	return json.Unmarshal(data, req)
}

// ScanResultResponse 查询扫描结果接口的响应，返回指定路径下的所有告警
type ScanResultResponse struct {
	Defects []defectmodel.Defect `json:"defects"`
}

func (resp *ScanResultResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
