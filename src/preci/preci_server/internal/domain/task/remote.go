package task

import (
	"codecc/preci_server/internal/infra/client"
	"codecc/preci_server/internal/infra/client/dto"
)

// ListRemoteTask 从远端 CodeCC 服务获取用户任务列表
func ListRemoteTask() ([]dto.TaskBase, error) {
	c := client.NewCodeCCClient()
	return c.ListTaskBase()
}

// ListRemoteDefects 从远端 CodeCC 服务分页查询告警列表
func ListRemoteDefects(req dto.DefectQueryReq,
	pageNum, pageSize int, sortField, sortType string) (*dto.LintDefectQueryData, error) {
	c := client.NewCodeCCClient()
	return c.ListDefects(req, pageNum, pageSize, sortField, sortType)
}
