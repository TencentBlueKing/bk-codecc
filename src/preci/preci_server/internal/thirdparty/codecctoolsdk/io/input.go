package io

import (
	"encoding/json"
	"os"
)

const (
	Full      = "full"      // 全量扫描
	Increment = "increment" // 增量扫描
)

// CheckerOption 规则配置参数
type CheckerOption struct {
	Name  string `json:"checkerOptionName"`
	Value string `json:"checkerOptionValue"`
}

// Checker 规则信息
type Checker struct {
	CheckerName    string          `json:"checkerName"`    // 规则名称
	Severity       int             `json:"severity"`       // 严重级别
	CheckerOptions []CheckerOption `json:"checkerOptions"` // 规则配置参数列表
}

// ToolOption 工具配置参数
type ToolOption struct {
	Name  string `json:"optionName"`
	Value string `json:"optionValue"`
}

// ScanInput 代码扫描的输入参数
type ScanInput struct {
	ProjectId        string       `json:"projectId"`        // 项目ID
	ScanPath         string       `json:"scanPath"`         // 扫描路径
	ScanType         string       `json:"scanType"`         // 扫描类型（full, increment）
	Language         int          `json:"language"`         // 扫描的目标代码语言
	WhitePaths       []string     `json:"whitePathList"`    // 白名单路径列表
	BlackPaths       []string     `json:"skipPaths"`        // 黑名单路径列表
	IncrementalFiles []string     `json:"incrementalFiles"` // 增量扫描文件列表
	OpenCheckers     []Checker    `json:"openCheckers"`     // 启用的规则列表
	ToolOptions      []ToolOption `json:"toolOptions"`      // 工具配置参数列表
}

// Decode 将JSON数据解码到ScanInput结构体
func (si *ScanInput) Decode(data []byte) error {
	return json.Unmarshal(data, si)
}

// LoadScanInput 从input.json加载扫描输入参数
// inputJsonPath: input.json文件路径
// 返回值: ScanInput和可能的错误
func LoadScanInput(inputJsonPath string) (*ScanInput, error) {
	data, err := os.ReadFile(inputJsonPath)
	if err != nil {
		return nil, err
	}

	si := new(ScanInput)
	err = si.Decode(data)
	if err != nil {
		return nil, err
	}

	return si, nil
}
