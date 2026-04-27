package auth

import (
	"codecc/preci_server/internal/infra/client/dto"
	"encoding/json"
)

// LoginResp 登录接口的响应，包含当前登录用户及其项目信息
type LoginResp struct {
	ProjectId string `json:"projectId"`
	UserId    string `json:"userId"`
}

func (resp *LoginResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}

// ListProjectsResp 查询用户可见项目列表接口的响应
type ListProjectsResp struct {
	Projects []dto.Project `json:"projects"`
}

func (resp2 *ListProjectsResp) Encode() ([]byte, error) {
	return json.Marshal(resp2)
}

// GetProjectResp 查询当前所选项目信息接口的响应
type GetProjectResp struct {
	ProjectId string `json:"projectId"`
}

func (resp3 *GetProjectResp) Encode() ([]byte, error) {
	return json.Marshal(resp3)
}

// OAuthDeviceLoginReq OAuth 设备登录接口的请求，携带从 OAuth 服务获取的 token 信息
type OAuthDeviceLoginReq struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
	ProjectId    string `json:"projectId"`
	ExpiresIn    int64  `json:"expiresIn"`
}

func (r *OAuthDeviceLoginReq) Decode(data []byte) error {
	return json.Unmarshal(data, r)
}
