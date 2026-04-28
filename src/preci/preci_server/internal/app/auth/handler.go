package auth

import (
	domain "codecc/preci_server/internal/domain/auth"
	"codecc/preci_server/internal/infra/web"
	"github.com/go-chi/chi/v5"
	"net/http"
)

// SetProjectHandler HTTP 处理器：设置当前选中的蓝盾项目
func SetProjectHandler(r *http.Request) web.Encoder {
	projectId := chi.URLParam(r, "projectId")
	err := domain.SetProjectId(projectId)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{}
}

// GetProjectHandler HTTP 处理器：查询当前选中的蓝盾项目 ID
func GetProjectHandler(_ *http.Request) web.Encoder {
	projectId, err := domain.GetProjectId()
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{
		Body: &GetProjectResp{
			ProjectId: projectId,
		},
	}
}

// ListProjectsHandler HTTP 处理器：查询当前登录用户可见的项目列表
func ListProjectsHandler(_ *http.Request) web.Encoder {
	projects, err := domain.ListProjects()
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{
		Body: &ListProjectsResp{
			Projects: projects,
		},
	}
}

// OAuthDeviceLoginHandler HTTP 处理器：使用 OAuth 设备码流程完成登录并返回用户及项目信息
func OAuthDeviceLoginHandler(r *http.Request) web.Encoder {
	var req OAuthDeviceLoginReq
	if err := web.Decode(r, &req); err != nil {
		return web.SimpleEncoder{Err: err}
	}

	userId, projectId, err := domain.DeviceLogin(
		req.AccessToken, req.RefreshToken, req.ProjectId, req.ExpiresIn,
	)
	if err != nil {
		return web.SimpleEncoder{Err: err}
	}

	return web.SimpleEncoder{
		Body: &LoginResp{
			ProjectId: projectId,
			UserId:    userId,
		},
	}
}
