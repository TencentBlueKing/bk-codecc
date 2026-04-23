// Package web contains common HTTP helpers, such as request/response
// utilities used across PreCI web handlers.
package web

import (
	"codecc/preci_server/internal/infra/cache"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/util/constant"
	"encoding/json"
	"net/http"
)

// Encoder 请求返回 response 的编码层
type Encoder interface {
	Encode() (data []byte, contentType string, err error)
}

type ResponseBody interface {
	Encode() ([]byte, error)
}

type SimpleEncoder struct {
	ContentType string `default:"application/json"`
	Err         error
	Body        ResponseBody
}

func (se SimpleEncoder) Encode() (data []byte, contentType string, err error) {
	// 设置默认 ContentType
	if se.ContentType == "" {
		se.ContentType = "application/json"
	}

	if se.Body != nil {
		data, encodeErr := se.Body.Encode()
		if encodeErr != nil {
			// 如果编码失败，返回错误
			return nil, se.ContentType, encodeErr
		}
		return data, se.ContentType, se.Err
	}
	return nil, se.ContentType, se.Err
}

type HandlerFunc func(r *http.Request) Encoder

type App struct {
	Logger *logger.Logger
}

// OauthTokenMiddleware OauthToken验证中间件
func (app *App) OauthTokenMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// 获取全局配置中的access token
		_, err := cache.GetAccessToken()
		if err != nil {
			w.WriteHeader(http.StatusUnauthorized)
			app.writeErrorResponse(w, err.Error())
			return
		}
		next.ServeHTTP(w, r)
	})
}

func (app *App) HttpHandler(handlerFunc HandlerFunc) func(w http.ResponseWriter, r *http.Request) {
	h := func(w http.ResponseWriter, r *http.Request) {
		encoder := handlerFunc(r)
		if encoder == nil {
			app.Logger.Error("HttpHandler 找不到 encoder")
			w.Header().Set(constant.ContentType, constant.ContentTypeJson)
			w.WriteHeader(http.StatusInternalServerError)
			app.writeErrorResponse(w, "没有返回编码器")
			return
		}

		data, contentType, err := encoder.Encode()

		// 确保 contentType 不为空
		if contentType == "" {
			contentType = constant.ContentTypeJson
		}
		w.Header().Set(constant.ContentType, contentType)

		if err != nil {
			app.Logger.Error("Http Handler error: " + err.Error())
			w.WriteHeader(http.StatusBadRequest)
			app.writeErrorResponse(w, err.Error())
			return
		}

		w.WriteHeader(http.StatusOK)
		// 如果 data 为空，返回空 JSON 对象
		if data == nil || len(data) == 0 {
			app.writeResponseBody(w, []byte(`{}`))
		} else {
			app.writeResponseBody(w, data)
		}
	}

	return h
}

func (app *App) writeResponseBody(w http.ResponseWriter, data []byte) {
	_, err := w.Write(data)
	if err != nil {
		app.Logger.Error("write response body error: " + err.Error())
	}
}

// writeErrorResponse 安全地写入错误响应，使用 json.Marshal 转义特殊字符，防止 XSS 攻击
func (app *App) writeErrorResponse(w http.ResponseWriter, errMsg string) {
	errResp := struct {
		Error string `json:"error"`
	}{
		Error: errMsg,
	}
	data, marshalErr := json.Marshal(errResp)
	if marshalErr != nil {
		app.Logger.Error("marshal error response failed: " + marshalErr.Error())
		app.writeResponseBody(w, []byte(`{"error": "internal error"}`))
		return
	}
	app.writeResponseBody(w, data)
}
