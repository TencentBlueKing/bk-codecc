package io

import (
	"encoding/json"
	"os"
)

// Trace 告警的一个追踪点信息
type Trace struct {
	TraceNum int    `json:"traceNum"` // 追踪点序号
	FilePath string `json:"filePath"` // 追踪点所在的文件路径
	LineNum  int    `json:"lineNum"`  // 追踪点所在的行号
	Main     bool   `json:"main"`     // 是否为告警的主事件
	Message  string `json:"message"`  // 描述信息
}

// DefectInstance 告警实例，包含一个告警的完整追踪链
type DefectInstance struct {
	Traces []Trace `json:"traces"` // 追踪点列表
}

// Defect 告警信息
type Defect struct {
	Line            int              `json:"line"`            // 行号
	FilePath        string           `json:"filePath"`        // 文件路径
	Description     string           `json:"description"`     // 详细描述
	CheckerName     string           `json:"checkerName"`     // 规则名
	DefectInstances []DefectInstance `json:"defectInstances"` // 告警实例列表
}

// ScanOutput 代码扫描的输出结果
type ScanOutput struct {
	Defects []Defect `json:"defects"` // 告警列表
}

// Encode 将ScanOutput结构体编码为JSON数据
// 返回值: JSON字节数组和可能的错误
func (so *ScanOutput) Encode() ([]byte, error) {
	jsonData, err := json.Marshal(so)
	if err != nil {
		return nil, err
	}

	return jsonData, nil
}

// Save 将扫描结果保存到指定文件
// filePath: 输出文件路径，必须是有效的文件路径
// openCheckers: 启用的checker列表，用于过滤结果（可选，如果为空则保存所有缺陷）
// 返回值: error
func (so *ScanOutput) Save(filePath string, openCheckers []string) error {
	// 如果指定了启用的检查器列表，则进行结果过滤
	if openCheckers != nil && len(openCheckers) > 0 {
		// 创建checker的映射表，用于快速查找
		ocm := make(map[string]bool)
		for _, checker := range openCheckers {
			ocm[checker] = true
		}

		// 过滤告警
		newDefects := make([]Defect, 0)
		for _, defect := range so.Defects {
			if ocm[defect.CheckerName] {
				newDefects = append(newDefects, defect)
			}
		}
		so.Defects = newDefects
	}

	// 将结果编码为JSON格式
	data, err := so.Encode()
	if err != nil {
		return err
	}

	// 将JSON数据写入指定文件
	return os.WriteFile(filePath, data, 0644)
}
