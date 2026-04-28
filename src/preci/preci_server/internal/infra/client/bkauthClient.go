package client

import (
	"fmt"
	"net/http"
	"net/url"
	"strings"

	"codecc/preci_server/internal/infra/logger"
)

const BKAuthOAuthTokenPath = "/oauth2/token"

// BKAuthTokenResponse 蓝鲸 BKAuth OAuth2 接口返回的 token 结构
type BKAuthTokenResponse struct {
	AccessToken  string `json:"access_token"`
	TokenType    string `json:"token_type"`
	ExpiresIn    int64  `json:"expires_in"`
	RefreshToken string `json:"refresh_token"`
	Scope        string `json:"scope"`
}

// BKAuthClient 封装与蓝鲸 BKAuth 服务交互的 HTTP 客户端
type BKAuthClient struct {
	httpClient *HTTPClient
	logger     *logger.Logger
	clientID   string
}

// NewBKAuthClient 创建 BKAuth OAuth2 客户端
func NewBKAuthClient() *BKAuthClient {
	config := &ClientConfig{
		BaseURL: BKAuthBaseURL,
	}

	return &BKAuthClient{
		httpClient: NewHTTPClient(config),
		logger:     logger.GetLogger(),
		clientID:   BKAuthClientID,
	}
}

// RefreshToken 使用 refresh token 向 BKAuth 请求刷新 access token
func (c *BKAuthClient) RefreshToken(refreshToken string) (*BKAuthTokenResponse, error) {
	formData := url.Values{
		"grant_type":    {"refresh_token"},
		"refresh_token": {refreshToken},
		"client_id":     {c.clientID},
	}

	reqBody := strings.NewReader(formData.Encode())

	var response BKAuthTokenResponse
	err := c.httpClient.DoRequest(&RequestOptions{
		Method: http.MethodPost,
		Path:   BKAuthOAuthTokenPath,
		Headers: map[string]string{
			"Content-Type": "application/x-www-form-urlencoded",
		},
		RawBody: reqBody,
	}, &response)

	if err != nil {
		c.logger.Error(fmt.Sprintf("refresh token failed: %v", err))
		return nil, fmt.Errorf("refresh token failed: %w", err)
	}

	if response.AccessToken == "" {
		return nil, fmt.Errorf("refresh token response invalid: empty access token")
	}

	return &response, nil
}
