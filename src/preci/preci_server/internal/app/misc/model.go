package misc

import (
	"encoding/json"
)

// LatestVersionResp 查询最新可用 PreCI 版本号接口的响应
type LatestVersionResp struct {
	LatestVersion string `json:"latestVersion"`
}

// Encode 将当前响应序列化为 JSON 字节流
func (resp *LatestVersionResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}

// HealthResp 健康检查接口的响应，返回服务状态、token 有效性及当前登录用户信息
type HealthResp struct {
	Healthy        bool   `json:"healthy"`
	TokenValid     bool   `json:"tokenValid"`
	TokenExpiresIn int64  `json:"tokenExpiresIn"`
	UserId         string `json:"userId"`
	ProjectId      string `json:"projectId"`
}

// Encode 将当前响应序列化为 JSON 字节流
func (h *HealthResp) Encode() ([]byte, error) {
	return json.Marshal(h)
}
