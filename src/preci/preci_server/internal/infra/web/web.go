// Package web contains common HTTP helpers, such as request/response
// utilities used across PreCI web handlers.
package web

import (
	"codecc/preci_server/internal/infra/cache"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/util/constant"
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5/middleware"
)

// Encoder 请求返回 response 的编码层
// 保留对外接口，向后兼容 internal/app/** 下的 handler
type Encoder interface {
	Encode() (data []byte, contentType string, err error)
}

// ResponseBody 业务响应体接口
type ResponseBody interface {
	Encode() ([]byte, error)
}

// SimpleEncoder 业务 handler 的默认返回载体
type SimpleEncoder struct {
	ContentType string `default:"application/json"`
	Err         error
	Body        ResponseBody
}

// Encode 将 Body 序列化为字节流
// 注意：Body.Encode() 一般实现为 json.Marshal，结果天然是 JSON 合法字节
func (se SimpleEncoder) Encode() (data []byte, contentType string, err error) {
	if se.ContentType == "" {
		se.ContentType = constant.ContentTypeJson
	}

	if se.Body != nil {
		body, encodeErr := se.Body.Encode()
		if encodeErr != nil {
			return nil, se.ContentType, encodeErr
		}
		return body, se.ContentType, se.Err
	}
	return nil, se.ContentType, se.Err
}

// HandlerFunc 业务层 handler 类型（chi 兼容：接受 *http.Request，返回 Encoder）
type HandlerFunc func(r *http.Request) Encoder

// App web 应用上下文，承载全局依赖（如 logger）
type App struct {
	Logger *logger.Logger
}

// ------------------------------------------------------------------
// 统一响应汇聚点（Render / RenderError / RenderJSON）
// 所有返回 body 的写入都在这里完成。
// ------------------------------------------------------------------

// RenderJSON 以 application/json 形式输出任意对象
// 使用 encoding/json.Marshal 作为净化器，天然转义 <、>、&
func (app *App) RenderJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set(constant.ContentType, constant.ContentTypeJson)
	// 关闭 siniffing
	w.Header().Set("X-Content-Type-Options", "nosniff")

	data, err := json.Marshal(v)
	if err != nil {
		app.Logger.Error("marshal response failed: " + err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte(`{"error":"internal error"}`))
		return
	}
	w.WriteHeader(status)
	if _, err := w.Write(data); err != nil {
		app.Logger.Error("write response body error: " + err.Error())
	}
}

// RenderBytes 以指定 Content-Type 输出已完成净化的字节流
// 仅当 contentType 为 application/json 时作 json.Valid 二次校验
func (app *App) RenderBytes(w http.ResponseWriter, status int, contentType string, data []byte) {
	if contentType == "" {
		contentType = constant.ContentTypeJson
	}

	// 对 JSON 响应执行合法性校验；若 body 为空则输出空对象
	if contentType == constant.ContentTypeJson {
		if len(data) == 0 {
			data = []byte(`{}`)
		} else if !json.Valid(data) {
			app.Logger.Error("invalid JSON body, fallback to empty object")
			data = []byte(`{}`)
		}
	}

	w.Header().Set(constant.ContentType, contentType)
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.WriteHeader(status)
	if _, err := w.Write(data); err != nil {
		app.Logger.Error("write response body error: " + err.Error())
	}
}

// RenderError 统一错误响应
func (app *App) RenderError(w http.ResponseWriter, status int, errMsg string) {
	app.RenderJSON(w, status, map[string]string{"error": errMsg})
}

// ------------------------------------------------------------------
// chi 中间件
// ------------------------------------------------------------------

// OauthTokenMiddleware OauthToken 验证中间件（chi 风格）
func (app *App) OauthTokenMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if _, err := cache.GetAccessToken(); err != nil {
			app.RenderError(w, http.StatusUnauthorized, err.Error())
			return
		}
		next.ServeHTTP(w, r)
	})
}

// Recoverer 导出 chi 的 panic 恢复中间件，供 route 层使用
func Recoverer() func(http.Handler) http.Handler {
	return middleware.Recoverer
}

// RequestID 导出 chi 的 RequestID 中间件
func RequestID() func(http.Handler) http.Handler {
	return middleware.RequestID
}

// ------------------------------------------------------------------
// 业务 handler 适配器
// ------------------------------------------------------------------

// HttpHandler 将业务层的 HandlerFunc 适配为标准 http.HandlerFunc，可直接注册到 chi.Router
func (app *App) HttpHandler(handlerFunc HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		encoder := handlerFunc(r)
		if encoder == nil {
			app.Logger.Error("HttpHandler 找不到 encoder")
			app.RenderError(w, http.StatusInternalServerError, "没有返回编码器")
			return
		}

		data, contentType, err := encoder.Encode()
		if err != nil {
			app.Logger.Error("Http Handler error: " + err.Error())
			app.RenderError(w, http.StatusBadRequest, err.Error())
			return
		}

		app.RenderBytes(w, http.StatusOK, contentType, data)
	}
}
