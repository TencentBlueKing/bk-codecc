package misc

import (
	"encoding/json"
)

type LatestVersionResp struct {
	LatestVersion string `json:"latestVersion"`
}

func (resp *LatestVersionResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}

type HealthResp struct {
	Healthy        bool   `json:"healthy"`
	TokenValid     bool   `json:"tokenValid"`
	TokenExpiresIn int64  `json:"tokenExpiresIn"`
	UserId         string `json:"userId"`
	ProjectId      string `json:"projectId"`
}

func (h *HealthResp) Encode() ([]byte, error) {
	return json.Marshal(h)
}
