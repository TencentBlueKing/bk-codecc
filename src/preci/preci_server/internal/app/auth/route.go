package auth

import (
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/web"
	"github.com/go-chi/chi/v5"
)

func Routes(mux *chi.Mux, log *logger.Logger) {
	app := &web.App{
		Logger: log,
	}

	mux.Route("/auth", func(r chi.Router) {
		r.Post("/oauth/device/login", app.HttpHandler(OAuthDeviceLoginHandler))
		r.Get("/list/projects", app.HttpHandler(ListProjectsHandler))
		r.Get("/project/{projectId}", app.HttpHandler(SetProjectHandler))
		r.Get("/project", app.HttpHandler(GetProjectHandler))
	})
}
