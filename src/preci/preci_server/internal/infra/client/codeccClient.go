package client

import (
	"fmt"
	"net/http"
	"strings"

	"codecc/preci_server/internal/infra/cache"
	"codecc/preci_server/internal/infra/client/dto"
	"codecc/preci_server/internal/infra/logger"
)

const (
	GetToolInfosPath    = "/ms/task/api/user/toolmeta/preci/getByToolNameIn"
	BkRepoTempTokenPath = "/ms/task/api/user/preci/create/tempToken"
	ListProjectsPath    = "/ms/task/api/user/preci/list/projects"
	ListTaskBasePath    = "/ms/task/api/user/task/base"

	BatchGetCheckerSeverityPath = "/ms/defect/api/user/preci/checker/batchGet/severity"
	ListDefectsPath             = "/ms/defect/api/user/warn/issue/list"

	UserInfoPath = "/ms/task/api/user/userInfo"
)

const oauthTokenHeader = "bkci_"

// CodeCCClient CodeCC 接口客户端
type CodeCCClient struct {
	httpClient *HTTPClient
	logger     *logger.Logger
}

// NewCodeCCClient 创建 CodeCC 客户端
func NewCodeCCClient() *CodeCCClient {
	config := &ClientConfig{
		BaseURL: CodeCCBaseUrl,
	}

	return &CodeCCClient{
		httpClient: NewHTTPClient(config),
		logger:     logger.GetLogger(),
	}
}

func (c *CodeCCClient) ListTaskBase() ([]dto.TaskBase, error) {
	var response dto.TaskInfoResp
	if err := c.get(ListTaskBasePath, &response); err != nil {
		return nil, err
	}
	return response.Data.EnableTasks, nil
}

// BkRepoTempToken 为了上传日志, 生成 bkrepo 临时 token
func (c *CodeCCClient) BkRepoTempToken() (string, error) {
	var resp dto.BkrepoTempTokenResp
	if err := c.get(BkRepoTempTokenPath, &resp); err != nil {
		return "", err
	}
	return resp.Data.Token, nil
}

// ListProjects 获取用户有权限的蓝盾项目列表
func (c *CodeCCClient) ListProjects() ([]dto.Project, error) {
	var response dto.ListProjectsResp
	if err := c.get(ListProjectsPath, &response, false); err != nil {
		return nil, err
	}
	return response.Data.Projects, nil
}

func (c *CodeCCClient) GetToolInfos(toolNames []string) ([]dto.ToolMetaDetail, error) {
	var response dto.BatchGetToolMetaResp
	if err := c.post(GetToolInfosPath, dto.BatchGetToolMetaReq{ToolNames: toolNames}, &response); err != nil {
		c.logger.Error(fmt.Sprintf("get tool infos failed: %v", err))
		return nil, err
	}
	return response.Data.ToolMetas, nil
}

// BatchGetCheckerSeverities 批量获取规则的 Severity
func (c *CodeCCClient) BatchGetCheckerSeverities(toolNames []string) (*dto.BatchGetCheckerSeverityData, error) {
	var response dto.BatchGetCheckerSeverityResp
	if err := c.post(
		BatchGetCheckerSeverityPath,
		dto.BatchGetCheckerSeverityReq{ToolNames: toolNames},
		&response,
	); err != nil {
		c.logger.Error(fmt.Sprintf("batch get checker severities failed: %v", err))
		return nil, err
	}
	return &response.Data, nil
}

func (c *CodeCCClient) ListDefects(req dto.DefectQueryReq, pageNum, pageSize int,
	sortField, sortType string) (*dto.LintDefectQueryData, error) {
	path := fmt.Sprintf("%s?pageNum=%d&pageSize=%d&sortField=%s&sortType=%s",
		ListDefectsPath, pageNum, pageSize, sortField, sortType)
	var response dto.DefectQueryResp
	if err := c.post(path, req, &response); err != nil {
		c.logger.Error(fmt.Sprintf("list defects failed: %v", err))
		return nil, err
	}
	return &response.Data, nil
}

func (c *CodeCCClient) GetUserInfoByToken(accessToken string) (string, error) {
	headers := map[string]string{
		"Authorization": "Bearer " + accessToken,
	}

	var response dto.UserInfoResponse
	err := c.httpClient.DoRequest(&RequestOptions{
		Method:  http.MethodGet,
		Path:    UserInfoPath,
		Headers: headers,
	}, &response)
	if err != nil {
		c.logger.Error(fmt.Sprintf("get user info by token failed: %v", err))
		return "", fmt.Errorf("get user info failed: %w", err)
	}

	if response.Status != 0 || !response.Data.Authenticated {
		return "", fmt.Errorf("user not authenticated, status: %d", response.Status)
	}

	return response.Data.Username, nil
}

func (c *CodeCCClient) get(path string, result interface{}, needProjectId ...bool) error {
	headers, err := c.commonHeaders(needProjectId...)
	if err != nil {
		return err
	}
	return c.httpClient.DoRequestWithRefresh(&RequestOptions{
		Method: http.MethodGet, Path: path, Headers: headers,
	}, result, func() (map[string]string, error) { return c.commonHeaders(needProjectId...) })
}

func (c *CodeCCClient) post(path string, body interface{}, result interface{}) error {
	headers, err := c.commonHeaders()
	if err != nil {
		return err
	}
	return c.httpClient.DoRequestWithRefresh(&RequestOptions{
		Method: http.MethodPost, Path: path, Headers: headers, Body: body,
	}, result, func() (map[string]string, error) { return c.commonHeaders() })
}

func (c *CodeCCClient) commonHeaders(opts ...bool) (map[string]string, error) {
	log := logger.GetLogger()
	needProjectId := true
	if len(opts) > 0 && !opts[0] {
		needProjectId = false
	}

	userInfo, err := cache.GetUserInfo(needProjectId)
	if err != nil {
		log.Error(fmt.Sprintf("get user info error: %v", err))
		return nil, err
	}

	headers := make(map[string]string)
	headers["Content-Type"] = "application/json"
	headers["X-DEVOPS-UID"] = userInfo.UserId
	headers["X-DEVOPS-PROJECT-ID"] = userInfo.ProjectId

	if strings.HasPrefix(userInfo.AccessToken, oauthTokenHeader) {
		headers["Authorization"] = "Bearer " + userInfo.AccessToken
	} else {
		headers["X-DEVOPS-ACCESS-TOKEN"] = userInfo.AccessToken
	}

	return headers, nil
}
