package codecc

import (
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/web"
	"github.com/go-chi/chi/v5"
)

// Routes 注册 codecc 模块的 HTTP 路由（远端任务和告警查询）
func Routes(mux *chi.Mux, log *logger.Logger) {
	app := &web.App{
		Logger: log,
	}

	mux.Route("/codecc", func(r chi.Router) {
		r.Use(app.OauthTokenMiddleware)
		r.Get("/task/list", app.HttpHandler(ListRemoteTaskHandler))
		r.Post("/defect/list", app.HttpHandler(ListRemoteDefectHandler))
	})
}
