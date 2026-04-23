package auth

import (
	"codecc/preci_server/internal/infra/client/dto"
	"encoding/json"
)

type LoginResp struct {
	ProjectId string `json:"projectId"`
	UserId    string `json:"userId"`
}

func (resp *LoginResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}

type ListProjectsResp struct {
	Projects []dto.Project `json:"projects"`
}

func (resp2 *ListProjectsResp) Encode() ([]byte, error) {
	return json.Marshal(resp2)
}

type GetProjectResp struct {
	ProjectId string `json:"projectId"`
}

func (resp3 *GetProjectResp) Encode() ([]byte, error) {
	return json.Marshal(resp3)
}

type OAuthDeviceLoginReq struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
	ProjectId    string `json:"projectId"`
	ExpiresIn    int64  `json:"expiresIn"`
}

func (r *OAuthDeviceLoginReq) Decode(data []byte) error {
	return json.Unmarshal(data, r)
}
