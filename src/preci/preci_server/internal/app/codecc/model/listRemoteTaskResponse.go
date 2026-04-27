package model

import "encoding/json"

// ListRemoteTaskResp 查询用户可见的远端 CodeCC 任务列表接口的响应
type ListRemoteTaskResp struct {
	TaskInfos []TaskInfo `json:"taskInfos"`
}

// TaskInfo 远端 CodeCC 任务的基础信息
type TaskInfo struct {
	// 任务id
	TaskId int64 `json:"taskId"`
	// 任务英文名
	NameEn string `json:"nameEn"`
	// 任务中文名
	NameCn string `json:"nameCn"`
}

// Encode 将当前响应序列化为 JSON 字节流
func (resp *ListRemoteTaskResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
