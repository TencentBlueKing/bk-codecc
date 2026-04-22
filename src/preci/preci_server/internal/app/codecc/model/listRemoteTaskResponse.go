package model

import "encoding/json"

type ListRemoteTaskResp struct {
	TaskInfos []TaskInfo `json:"taskInfos"`
}

type TaskInfo struct {
	// 任务id
	TaskId int64 `json:"taskId"`
	// 任务英文名
	NameEn string `json:"nameEn"`
	// 任务中文名
	NameCn string `json:"nameCn"`
}

func (resp *ListRemoteTaskResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
