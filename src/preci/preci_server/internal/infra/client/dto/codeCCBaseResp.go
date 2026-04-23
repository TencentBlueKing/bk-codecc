package dto

// CodeCCBaseResponse 基础响应结构体
type CodeCCBaseResponse struct {
	Status int    `json:"status"`
	Code   string `json:"code"`
}
