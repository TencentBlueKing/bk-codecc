// Package client provides HTTP clients for external services consumed by
// PreCI (CodeCC, bkrepo, bkauth, etc.).
package client

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"time"

	"codecc/preci_server/internal/util/constant"
	"codecc/preci_server/internal/util/perror"
)

var httpTokenRefresher func() error

func RegisterHTTPTokenRefresher(fn func() error) {
	httpTokenRefresher = fn
}

// HTTPClient 封装HTTP客户端
type HTTPClient struct {
	client  *http.Client
	baseURL string
	timeout time.Duration
}

// ClientConfig HTTP客户端配置
type ClientConfig struct {
	BaseURL    string
	Timeout    time.Duration
	MaxRetries int
}

// NewHTTPClient 创建新的HTTP客户端
func NewHTTPClient(config *ClientConfig) *HTTPClient {
	if config.Timeout == 0 {
		config.Timeout = DefaultTimeout

	}
	return &HTTPClient{
		client:  &http.Client{},
		baseURL: config.BaseURL,
		timeout: config.Timeout,
	}
}

// RequestOptions 请求选项
type RequestOptions struct {
	Method  string
	Path    string
	Headers map[string]string
	Body    interface{}   // JSON 序列化的请求体
	RawBody io.Reader     // 原始请求体（如文件流），优先级高于 Body
	Timeout time.Duration // 单个请求的超时时间，为0时使用客户端默认超时
}

// DoRequest 执行HTTP请求
func (c *HTTPClient) DoRequest(options *RequestOptions, result interface{}) error {
	url := c.baseURL + options.Path

	// 准备请求体
	var bodyReader io.Reader
	if options.RawBody != nil {
		// 优先使用原始请求体（如文件流）
		bodyReader = options.RawBody
	} else if options.Body != nil {
		// 否则使用 JSON 序列化的请求体
		bodyBytes, err := json.Marshal(options.Body)
		if err != nil {
			return fmt.Errorf("failed to marshal request body: %w", err)
		}
		bodyReader = bytes.NewReader(bodyBytes)
	}

	// 确定超时时间
	timeout := c.timeout
	if options.Timeout > 0 {
		timeout = options.Timeout
	}

	// 创建带超时的 context
	ctx, cancel := context.WithTimeout(context.Background(), timeout)
	defer cancel()

	// 创建请求
	req, err := http.NewRequestWithContext(ctx, options.Method, url, bodyReader)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	// 设置请求头
	if req.Header.Get(constant.ContentType) == "" {
		req.Header.Set(constant.ContentType, constant.ContentTypeJson)
	}
	for key, value := range options.Headers {
		req.Header.Set(key, value)
	}

	// 执行请求
	resp, err := c.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to execute request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusUnauthorized {
		return perror.ErrInvalidAccessToken
	}

	// 检查响应状态
	if resp.StatusCode < http.StatusOK || resp.StatusCode >= http.StatusMultipleChoices {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("request failed with status %d: %s", resp.StatusCode, string(body))
	}

	// 解析响应体
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("failed to read response body: %w", err)
	}

	if result != nil {
		if err := json.Unmarshal(body, result); err != nil {
			return fmt.Errorf("failed to unmarshal response: %w", err)
		}
	}

	return nil
}

// DoRequestWithRefresh 在 DoRequest 上层封装 401 自动重试：
// 遇到 ErrInvalidAccessToken 时刷新 token，用 refreshHeaders 获取新请求头后重试一次。
func (c *HTTPClient) DoRequestWithRefresh(opts *RequestOptions, result interface{},
	refreshHeaders func() (map[string]string, error)) error {

	err := c.DoRequest(opts, result)
	if !errors.Is(err, perror.ErrInvalidAccessToken) || httpTokenRefresher == nil {
		return err
	}
	if refreshErr := httpTokenRefresher(); refreshErr != nil {
		return err
	}
	if refreshHeaders != nil {
		if h, e := refreshHeaders(); e == nil {
			opts.Headers = h
		}
	}
	return c.DoRequest(opts, result)
}

// Get 执行GET请求
func (c *HTTPClient) Get(path string, headers map[string]string, result interface{}) error {
	return c.DoRequest(&RequestOptions{
		Method:  http.MethodGet,
		Path:    path,
		Headers: headers,
	}, result)
}

// Put 执行PUT请求
func (c *HTTPClient) Put(path string, headers map[string]string, body interface{}, result interface{}) error {
	return c.DoRequest(&RequestOptions{
		Method:  http.MethodPut,
		Path:    path,
		Headers: headers,
		Body:    body,
	}, result)
}

// Post 执行POST请求
func (c *HTTPClient) Post(path string, headers map[string]string, body interface{}, result interface{}) error {
	return c.DoRequest(&RequestOptions{
		Method:  http.MethodPost,
		Path:    path,
		Headers: headers,
		Body:    body,
	}, result)
}

// PostWithTimeout 执行POST请求，支持自定义超时时间
func (c *HTTPClient) PostWithTimeout(path string, headers map[string]string,
	body interface{}, result interface{}, timeout time.Duration) error {
	return c.DoRequest(&RequestOptions{
		Method:  http.MethodPost,
		Path:    path,
		Headers: headers,
		Body:    body,
		Timeout: timeout,
	}, result)
}

// PutFile 执行PUT请求上传文件，直接传输io.Reader内容而不进行JSON序列化，支持自定义超时时间
func (c *HTTPClient) PutFile(path string, headers map[string]string, body io.Reader, result interface{}, timeout time.Duration) error {
	return c.DoRequest(&RequestOptions{
		Method:  http.MethodPut,
		Path:    path,
		Headers: headers,
		RawBody: body,
		Timeout: timeout,
	}, result)
}
