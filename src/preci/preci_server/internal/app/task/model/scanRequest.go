package model

import "encoding/json"

type ScanRequest struct {
	ScanType int      `json:"scanType" validate:"required"`
	Paths    []string `json:"paths"`
	RootDir  string   `json:"rootDir"`
}

func (sr *ScanRequest) Decode(data []byte) error {
	return json.Unmarshal(data, sr)
}

type ScanResponse struct {
	Message     string   `json:"message"`
	Tools       []string `json:"tools"`
	ScanFileNum int      `json:"scanFileNum"`
}

func (sr *ScanResponse) Encode() ([]byte, error) {
	return json.Marshal(sr)
}
