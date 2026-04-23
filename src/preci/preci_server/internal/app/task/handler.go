package task

import (
	"codecc/preci_server/internal/app/task/model"
	"codecc/preci_server/internal/domain/defect"
	defectmodel "codecc/preci_server/internal/domain/defect/model"
	"codecc/preci_server/internal/domain/task"
	"codecc/preci_server/internal/domain/tool"
	"codecc/preci_server/internal/infra/web"
	"codecc/preci_server/internal/util/perror"
	"github.com/go-chi/chi/v5"
	"net/http"
)

func ScanCancelHandler(_ *http.Request) web.Encoder {
	projectRoot := task.Cancel()
	return web.SimpleEncoder{
		Body: &model.ScanCancelResponse{
			ProjectRoot: projectRoot,
		},
	}
}

func ScanHandler(r *http.Request) web.Encoder {
	var req model.ScanRequest
	err := web.Decode(r, &req)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	message, tools, fileNum, err := task.Scan(req.ScanType, req.Paths, req.RootDir)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	resp := &model.ScanResponse{
		Message:     message,
		Tools:       tools,
		ScanFileNum: fileNum,
	}

	return web.SimpleEncoder{
		Body: resp,
	}
}

func ScanResultHandler(r *http.Request) web.Encoder {
	var req model.ScanResultRequest
	err := web.Decode(r, &req)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	defects, err := defect.GetAllDefectsByPathPre(req.Path)
	if err != nil {
		return web.SimpleEncoder{
			Err: perror.ErrNoDefects,
		}
	}

	var defectResp []defectmodel.Defect
	for _, it := range defects {
		defectResp = append(defectResp, *it)
	}

	return web.SimpleEncoder{
		Body: &model.ScanResultResponse{
			Defects: defectResp,
		},
	}
}

func ReloadToolHandler(r *http.Request) web.Encoder {
	toolName := chi.URLParam(r, "toolName")

	err := tool.ReloadTools([]string{toolName})
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{}
}

func ScanProgressHandler(_ *http.Request) web.Encoder {
	progress, err := task.GetScanProgress("")
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{
		Body: &model.ScanProgressResponse{
			ProjectRoot:  progress.ProjectRoot,
			ToolStatuses: progress.ToolStatuses,
			Status:       progress.Status,
		},
	}
}

func InitHandler(r *http.Request) web.Encoder {
	var req model.InitRequest
	err := web.Decode(r, &req)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	taskInfo, updateInfo, err := task.Init(req.CurrentPath, req.RootPath, false)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	resp := &model.InitResponse{
		RootPath: taskInfo.RootDir,
		Tools:    taskInfo.Tools,
	}

	if updateInfo != nil {
		resp.HasUpdate = updateInfo.HasUpdate
		resp.CurrentVersion = updateInfo.CurrentVersion
		resp.LatestVersion = updateInfo.LatestVersion
	}

	return web.SimpleEncoder{
		Body: resp,
	}
}
