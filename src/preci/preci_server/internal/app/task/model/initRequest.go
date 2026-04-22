package model

import (
	"encoding/json"
)

type InitRequest struct {
	CurrentPath          string `json:"currentPath" validate:"required"` // 当前路径
	RootPath             string `json:"rootPath"`                        // 项目根路径
	NoReloadToolCheckers bool   `json:"noReloadToolCheckers"`            // 是否需要重新加载工具检查器
}

func (req *InitRequest) Decode(data []byte) error {
	return json.Unmarshal(data, req)
}

type InitResponse struct {
	RootPath       string   `json:"rootPath"`                 // 项目根路径
	Tools          []string `json:"tools"`                    // 工具列表
	HasUpdate      bool     `json:"hasUpdate"`                // 是否有可用的版本更新
	CurrentVersion string   `json:"currentVersion,omitempty"` // 当前版本号
	LatestVersion  string   `json:"latestVersion,omitempty"`  // 最新版本号
}

func (resp *InitResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
