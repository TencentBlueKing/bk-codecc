package task

import (
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/web"
	"github.com/go-chi/chi/v5"
)

func Routes(mux *chi.Mux, log *logger.Logger) {
	app := &web.App{
		Logger: log,
	}

	mux.Route("/task", func(r chi.Router) {
		r.Use(app.OauthTokenMiddleware)
		r.Post("/init", app.HttpHandler(InitHandler))
		r.Get("/reload/tool/{toolName}", app.HttpHandler(ReloadToolHandler))
		r.Post("/scan", app.HttpHandler(ScanHandler))
		r.Get("/scan/progress", app.HttpHandler(ScanProgressHandler))
		r.Post("/scan/result", app.HttpHandler(ScanResultHandler))
		r.Get("/scan/cancel", app.HttpHandler(ScanCancelHandler))
	})
}
