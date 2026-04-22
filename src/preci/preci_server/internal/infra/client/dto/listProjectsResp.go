package dto

// ListProjectsResp 获取用户有权限的蓝盾项目响应
type ListProjectsResp struct {
	CodeCCBaseResponse
	Data ListProjectsData `json:"data"`
}

// ListProjectsData 项目列表数据
type ListProjectsData struct {
	Projects []Project `json:"projects"`
}

// Project 项目信息
type Project struct {
	ProjectId   string `json:"projectId"`
	ProjectName string `json:"projectName"`
}
