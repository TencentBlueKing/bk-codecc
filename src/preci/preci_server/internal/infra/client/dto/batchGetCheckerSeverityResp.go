package dto

// BatchGetCheckerSeverityResp 批量获取规则 Severity 响应体
type BatchGetCheckerSeverityResp struct {
	CodeCCBaseResponse
	Data BatchGetCheckerSeverityData `json:"data"`
}

// BatchGetCheckerSeverityData 批量获取规则 Severity 数据
type BatchGetCheckerSeverityData struct {
	Tools []ToolCheckerSeverities `json:"tools"`
}

// ToolCheckerSeverities 工具规则 Severity 列表
type ToolCheckerSeverities struct {
	ToolName string            `json:"toolName"`
	Checkers []CheckerSeverity `json:"checkers"`
}

// CheckerSeverity 规则 Severity
type CheckerSeverity struct {
	CheckerKey string `json:"checkerKey"`
	Severity   int    `json:"severity"`
}
