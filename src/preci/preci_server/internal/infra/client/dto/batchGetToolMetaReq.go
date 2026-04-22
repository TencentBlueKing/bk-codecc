package dto

// BatchGetToolMetaReq GetToolInfos 请求体
type BatchGetToolMetaReq struct {
	ToolNames []string `json:"toolNames"`
}
