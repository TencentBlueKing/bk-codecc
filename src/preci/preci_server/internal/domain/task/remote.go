package task

import (
	"codecc/preci_server/internal/infra/client"
	"codecc/preci_server/internal/infra/client/dto"
)

func ListRemoteTask() ([]dto.TaskBase, error) {
	c := client.NewCodeCCClient()
	return c.ListTaskBase()
}

func ListRemoteDefects(req dto.DefectQueryReq,
	pageNum, pageSize int, sortField, sortType string) (*dto.LintDefectQueryData, error) {
	c := client.NewCodeCCClient()
	return c.ListDefects(req, pageNum, pageSize, sortField, sortType)
}
