package checker

import (
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/web"
	"github.com/go-chi/chi/v5"
)

// Routes 注册 checker 模块的 HTTP 路由（规则集列表、选择、取消选择）
func Routes(mux *chi.Mux, log *logger.Logger) {
	app := &web.App{
		Logger: log,
	}

	mux.Route("/checker", func(r chi.Router) {
		r.Get("/set/list", app.HttpHandler(ListHandler))
		r.Post("/set/select", app.HttpHandler(SelectHandler))
		r.Post("/set/unselect", app.HttpHandler(UnselectHandler))
	})
}
