package model

import "encoding/json"

// ScanProgressRequest 查询扫描进度接口的请求，指定待查询的项目根目录
type ScanProgressRequest struct {
	RootDir string `json:"rootDir" validate:"required"`
}

func (req *ScanProgressRequest) Decode(data []byte) error {
	return json.Unmarshal(data, req)
}

// ScanProgressResponse 查询扫描进度接口的响应，返回各工具的运行状态和扫描任务整体状态
type ScanProgressResponse struct {
	ProjectRoot  string            `json:"projectRoot"`
	ToolStatuses map[string]string `json:"toolStatuses"`
	Status       string            `json:"status"`
}

func (resp *ScanProgressResponse) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
