package model

import (
	"encoding/json"
	defectmodel "git.woa.com/codecc/preci_server/internal/domain/defect/model"
)

type ScanResultRequest struct {
	Path string `json:"path" validate:"required"`
}

func (req *ScanResultRequest) Decode(data []byte) error {
	return json.Unmarshal(data, req)
}

type ScanResultResponse struct {
	Defects []defectmodel.Defect `json:"defects"`
}

func (resp *ScanResultResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
