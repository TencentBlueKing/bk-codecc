package dto

// BatchGetCheckerSeverityReq 批量获取规则 Severity 请求体
type BatchGetCheckerSeverityReq struct {
	ToolNames []string `json:"toolNames"`
}
