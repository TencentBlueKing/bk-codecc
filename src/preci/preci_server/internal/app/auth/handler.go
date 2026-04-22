package auth

import (
	domain "codecc/preci_server/internal/domain/auth"
	"codecc/preci_server/internal/infra/web"
	"github.com/go-chi/chi/v5"
	"net/http"
)

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
