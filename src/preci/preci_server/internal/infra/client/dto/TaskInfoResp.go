package dto

// TaskInfoResp 获取任务信息响应
type TaskInfoResp struct {
	CodeCCBaseResponse
	Data TaskInfoData `json:"data"`
}

// TaskInfoData 获取任务信息 data 字段
type TaskInfoData struct {
	EnableTasks []TaskBase `json:"enableTasks"`
}

// TaskBase 任务基础信息
type TaskBase struct {
	// 任务主键id
	TaskId int64 `json:"taskId"`
	// 任务英文名
	NameEn string `json:"nameEn"`
	// 任务中文名
	NameCn string `json:"nameCn"`
	// 项目ID
	ProjectId string `json:"projectId"`
	// 项目名称
	ProjectName string `json:"projectName"`
	// 流水线ID
	PipelineId string `json:"pipelineId"`
	// 流水线一对多标识
	MultiPipelineMark string `json:"multiPipelineMark"`
	// 流水线名称
	PipelineName string `json:"pipelineName"`
	// 代码语言
	CodeLang int64 `json:"codeLang"`
	// 任务负责人
	TaskOwner []string `json:"taskOwner"`
	// 任务状态
	Status int `json:"status"`
}
