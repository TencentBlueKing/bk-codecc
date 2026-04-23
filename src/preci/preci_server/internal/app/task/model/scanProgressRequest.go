package model

import "encoding/json"

type ScanProgressRequest struct {
	RootDir string `json:"rootDir" validate:"required"`
}

func (req *ScanProgressRequest) Decode(data []byte) error {
	return json.Unmarshal(data, req)
}

type ScanProgressResponse struct {
	ProjectRoot  string            `json:"projectRoot"`
	ToolStatuses map[string]string `json:"toolStatuses"`
	Status       string            `json:"status"`
}

func (resp *ScanProgressResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
