package model

import "encoding/json"

// UploadArchiveRequest 上传代码压缩包请求
type UploadArchiveRequest struct {
	ProjectRoot string   `json:"projectRoot" validate:"required"` // 项目根目录（绝对路径）
	Files       []string `json:"files" validate:"required"`       // 文件列表（绝对路径）
}

func (req *UploadArchiveRequest) Decode(data []byte) error {
	return json.Unmarshal(data, req)
}

// UploadArchiveResponse 上传代码压缩包响应
type UploadArchiveResponse struct {
	Message string `json:"message"`
}

func (resp *UploadArchiveResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
