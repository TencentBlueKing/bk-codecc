package client

import (
	"codecc/preci_server/cmd/cli/perror"
	"codecc/preci_server/internal/app/auth"
	"codecc/preci_server/internal/app/checker"
	"codecc/preci_server/internal/app/misc"
	"codecc/preci_server/internal/app/task/model"
	"codecc/preci_server/internal/infra/client"
	"codecc/preci_server/internal/infra/client/dto"
	"codecc/preci_server/internal/util/constant"
	"fmt"
	"strings"
	"time"
)

const (
	host = "http://localhost"

	shutDownPath = "/shutdown"

	oauthDeviceLoginPath = "/auth/oauth/device/login"
	setProjectPath       = "/auth/project/[projectId]"
	getProjectPath       = "/auth/project"
	listProjectsPath     = "/auth/list/projects"

	initPath         = "/task/init"
	reloadToolPath   = "/task/reload/tool/"
	scanPath         = "/task/scan"
	scanProgressPath = "/task/scan/progress"
	scanResultPath   = "/task/scan/result"
	cancelPath       = "/task/scan/cancel"

	checkerSetListPath     = "/checker/set/list"
	checkerSetSelectPath   = "/checker/set/select"
	checkerSetUnselectPath = "/checker/set/unselect"

	healthPath           = "/misc/health"
	getLatestVersionPath = "/misc/latestVersion"
	downloadLatestPath   = "/misc/downloadLatest"
	reportLogPath        = "/misc/reportLog"
	debugModePath        = "/misc/debug/[mode]"
)

// PreCIServerClient PreCI-server 客户端
type PreCIServerClient struct {
	httpClient *client.HTTPClient
}

// NewPreCIServerClient 创建
func NewPreCIServerClient(port int) (*PreCIServerClient, error) {
	if port == 0 {
		return nil, perror.ErrServerInvalid
	}

	config := &client.ClientConfig{
		BaseURL: fmt.Sprintf("%s:%d", host, port),
	}

	return &PreCIServerClient{
		httpClient: client.NewHTTPClient(config),
	}, nil
}

func (c *PreCIServerClient) Health() (*misc.HealthResp, error) {
	var resp misc.HealthResp
	err := c.httpClient.Get(healthPath, nil, &resp)
	if err != nil {
		return nil, err
	}
	return &resp, nil
}

func (c *PreCIServerClient) DebugMode(mode string) error {
	return c.httpClient.Get(strings.Replace(debugModePath, "[mode]", mode, 1), nil, nil)
}

func (c *PreCIServerClient) GetLatestVersion() (*misc.LatestVersionResp, error) {
	var resp misc.LatestVersionResp

	err := c.httpClient.Get(getLatestVersionPath, nil, &resp)
	if err != nil {
		return nil, err
	}

	return &resp, nil
}

func (c *PreCIServerClient) DownloadLatestPreCI() error {
	err := c.httpClient.Get(downloadLatestPath, nil, nil)
	if err != nil {
		return err
	}
	return nil
}

func (c *PreCIServerClient) GetScanProgress() (*model.ScanProgressResponse, error) {
	var resp model.ScanProgressResponse
	err := c.httpClient.Get(scanProgressPath, nil, &resp)
	if err != nil {
		return nil, err
	}

	return &resp, nil
}

func (c *PreCIServerClient) GetScanResult(path string) (*model.ScanResultResponse, error) {
	var resp model.ScanResultResponse

	request := &model.ScanResultRequest{
		Path: path,
	}
	err := c.httpClient.Post(scanResultPath, nil, request, &resp)
	if err != nil {
		return nil, err
	}

	return &resp, nil
}

func (c *PreCIServerClient) GetCheckerSetList() (*CheckerSetListResp, error) {
	var resp CheckerSetListResp
	err := c.httpClient.Get(checkerSetListPath, nil, &resp)
	if err != nil {
		return nil, err
	}

	return &resp, nil
}

func (c *PreCIServerClient) SelectCheckerSets(currentPath string, checkerSets []string) (*checker.SelectResp, error) {
	req := &checker.SelectReq{
		ProjectRootDir: currentPath,
		CheckerSets:    checkerSets,
	}

	var resp checker.SelectResp
	err := c.httpClient.Post(checkerSetSelectPath, nil, req, &resp)
	if err != nil {
		return nil, err
	}

	return &resp, nil
}

func (c *PreCIServerClient) UnselectCheckerSets(currentPath string, checkerSets []string) (*checker.SelectResp, error) {
	req := &checker.SelectReq{
		ProjectRootDir: currentPath,
		CheckerSets:    checkerSets,
	}

	var resp checker.SelectResp
	err := c.httpClient.Post(checkerSetUnselectPath, nil, req, &resp)
	if err != nil {
		return nil, err
	}

	return &resp, nil
}

func (c *PreCIServerClient) ReportLog() error {
	err := c.httpClient.Get(reportLogPath, nil, nil)
	if err != nil {
		return err
	}
	return nil
}

func (c *PreCIServerClient) ShutdownServer() error {
	err := c.httpClient.Get(shutDownPath, nil, nil)
	if err != nil {
		return err
	}

	return nil
}

func (c *PreCIServerClient) ListProjects() ([]dto.Project, error) {
	var resp auth.ListProjectsResp
	err := c.httpClient.Get(listProjectsPath, nil, &resp)
	if err != nil {
		return nil, err
	}
	return resp.Projects, nil
}

func (c *PreCIServerClient) OAuthDeviceLogin(accessToken, refreshToken, projectId string,
	expiresIn int64) (string, string, error) {
	request := &auth.OAuthDeviceLoginReq{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
		ProjectId:    projectId,
		ExpiresIn:    expiresIn,
	}

	var resp auth.LoginResp
	err := c.httpClient.Post(oauthDeviceLoginPath, nil, request, &resp)
	if err != nil {
		return "", "", err
	}

	return resp.UserId, resp.ProjectId, nil
}

func (c *PreCIServerClient) StartScan(scanType int, paths []string, rootDir string) (string, error) {
	request := &model.ScanRequest{
		ScanType: scanType,
		Paths:    paths,
		RootDir:  rootDir,
	}

	var resp model.ScanResponse

	err := c.httpClient.Post(scanPath, nil, request, &resp)
	if err != nil {
		return "", err
	}

	successMessage := fmt.Sprintf("启动扫描成功! 扫描工具列表: %v", resp.Tools)
	if scanType != constant.FullScan {
		successMessage += fmt.Sprintf(", 扫描文件数: %d", resp.ScanFileNum)
	}
	if resp.Message != "" {
		successMessage += fmt.Sprintf(", Message: %s", resp.Message)
	}

	return successMessage, nil
}

func (c *PreCIServerClient) Init(currentPath, projRootPath string) (*model.InitResponse, error) {
	request := &model.InitRequest{
		CurrentPath: currentPath,
		RootPath:    projRootPath,
	}

	var resp model.InitResponse
	// Init 请求可能耗时较长，设置 10 分钟超时
	err := c.httpClient.PostWithTimeout(initPath, nil, request, &resp, 10*60*time.Second)
	if err != nil {
		return nil, err
	}

	return &resp, nil
}

// ReloadTool 调用 /task/reload/tool/{toolName} 下载指定工具
func (c *PreCIServerClient) ReloadTool(toolName string) error {
	err := c.httpClient.Get(reloadToolPath+toolName, nil, nil)
	if err != nil {
		return fmt.Errorf("下载工具 %s 失败: %w", toolName, err)
	}
	return nil
}

func (c *PreCIServerClient) CancelScan() (string, error) {
	var resp model.ScanCancelResponse
	err := c.httpClient.Get(cancelPath, nil, &resp)
	if err != nil {
		return "", err
	}

	return resp.ProjectRoot, nil
}

func (c *PreCIServerClient) SetProject(projectId string) error {
	return c.httpClient.Get(strings.Replace(setProjectPath, "[projectId]", projectId, 1), nil, nil)
}

func (c *PreCIServerClient) GetProject() (string, error) {
	var resp auth.GetProjectResp
	err := c.httpClient.Get(getProjectPath, nil, &resp)
	if err != nil {
		return "", err
	}

	return resp.ProjectId, nil
}

// CheckerSetListResp 规则集列表响应
type CheckerSetListResp struct {
	CheckerSets []CheckerSet `json:"checkerSets"`
}

// CheckerSet 规则集
type CheckerSet struct {
	CheckerSetId   string `json:"checkerSetId"`   // 规则集 ID
	CheckerSetName string `json:"checkerSetName"` // 规则集名称
	ToolName       string `json:"toolName"`
}
