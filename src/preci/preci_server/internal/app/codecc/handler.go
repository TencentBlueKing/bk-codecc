package codecc

import (
	"codecc/preci_server/internal/app/codecc/model"
	"codecc/preci_server/internal/domain/task"
	"codecc/preci_server/internal/infra/client/dto"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/web"
	"codecc/preci_server/internal/util/perror"
	"fmt"
	"net/http"
	"path/filepath"
)

func ListRemoteTaskHandler(_ *http.Request) web.Encoder {
	log := logger.GetLogger()
	log.Info("start list remote task")
	tasks, err := task.ListRemoteTask()
	if err != nil {
		log.Error(fmt.Sprintf("list remote task failed: %v", err))
		return web.SimpleEncoder{
			Err: err,
		}
	}

	var taskInfos []model.TaskInfo
	for _, it := range tasks {
		taskInfos = append(taskInfos, model.TaskInfo{
			TaskId: it.TaskId,
			NameEn: it.NameEn,
			NameCn: it.NameCn,
		})
	}

	log.Info(fmt.Sprintf("get %d tasks", len(taskInfos)))

	return web.SimpleEncoder{
		Body: &model.ListRemoteTaskResp{
			TaskInfos: taskInfos,
		},
	}
}

func pathConvert(relPath, projectRoot string) string {
	return filepath.Join(projectRoot, filepath.FromSlash(relPath))
}

func ListRemoteDefectHandler(r *http.Request) web.Encoder {
	log := logger.GetLogger()
	var req model.ListRemoteDefectReq
	err := web.Decode(r, &req)
	if err != nil {
		log.Error(fmt.Sprintf("decode list remote defect request failed: %v", err))
		return web.SimpleEncoder{Err: err}
	}

	log.Info(fmt.Sprintf("start list remote defect, req: %v", req))
	if req.ProjectRoot == "" {
		return web.SimpleEncoder{Err: perror.ErrInvalidRootDir}
	}

	if req.PageNum == 0 {
		req.PageNum = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 100
	}
	if req.SortField == "" {
		req.SortField = "fileName"
	}
	if req.SortType == "" {
		req.SortType = "ASC"
	}
	if req.Status == nil {
		req.Status = []string{"1"}
	}
	if req.Severity == nil {
		req.Severity = []string{"1", "2", "4", "8"}
	}

	data, err := task.ListRemoteDefects(
		dto.DefectQueryReq{
			TaskIdList:    req.TaskIdList,
			ToolNameList:  req.ToolNameList,
			DimensionList: req.DimensionList,
			Checker:       req.Checker,
			Author:        req.Author,
			Severity:      req.Severity,
			Status:        req.Status,
			FileList:      req.FileList,
			DefectType:    req.DefectType,
			ClusterType:   "defect",
			BuildId:       req.BuildId,
			CheckerSets:   make([]string, 0),
		},
		req.PageNum, req.PageSize, req.SortField, req.SortType,
	)
	if err != nil {
		log.Error(fmt.Sprintf("list remote defect failed: %v", err))
		return web.SimpleEncoder{Err: err}
	}

	seriousCount, normalCount, promptCount := 0, 0, 0
	var defects []model.RemoteDefect
	for _, d := range data.DefectList.Records {
		filePath := pathConvert(d.RelPath, req.ProjectRoot)
		defects = append(defects, model.RemoteDefect{
			FileName: d.FileName,
			FilePath: filePath,
			LineNum:  d.LineNum,
			Author:   d.Author,
			Checker:  d.Checker,
			Severity: d.Severity,
			Message:  d.Message,
			Status:   d.Status,
			ToolName: d.ToolName,
		})

		if d.Severity == 1 {
			seriousCount++
		} else if d.Severity == 2 {
			normalCount++
		} else {
			promptCount++
		}
	}

	totalCount := len(defects)

	log.Info(fmt.Sprintf("get %d defects", totalCount))
	return web.SimpleEncoder{
		Body: &model.ListRemoteDefectResp{
			SeriousCount: seriousCount,
			NormalCount:  normalCount,
			PromptCount:  promptCount,
			TotalCount:   totalCount,
			ExistCount:   data.ExistCount,
			FixCount:     data.FixCount,
			IgnoreCount:  data.IgnoreCount,
			Defects:      defects,
		},
	}
}
