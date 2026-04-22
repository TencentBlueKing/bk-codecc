package model

import "encoding/json"

type ScanCancelResponse struct {
	ProjectRoot string `json:"projectRoot"`
}

func (resp *ScanCancelResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
