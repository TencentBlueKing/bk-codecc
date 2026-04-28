package misc

import (
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/web"
	"github.com/go-chi/chi/v5"
)

// Routes 注册 misc 模块的 HTTP 路由（健康检查、版本、日志上报等）
func Routes(mux *chi.Mux, log *logger.Logger) {
	app := &web.App{
		Logger: log,
	}

	mux.Route("/misc", func(r chi.Router) {
		r.Get("/health", app.HttpHandler(HealthHandler))

		r.Group(func(r chi.Router) {
			r.Use(app.OauthTokenMiddleware)
			r.Get("/downloadLatest", app.HttpHandler(DownloadLatestPreCIHandler))
			r.Get("/latestVersion", app.HttpHandler(LatestVersionHandler))
			r.Get("/reportLog", app.HttpHandler(ReportLogHandler))
			r.Get("/debug/{mode}", app.HttpHandler(DebugHandler))
		})
	})
}
