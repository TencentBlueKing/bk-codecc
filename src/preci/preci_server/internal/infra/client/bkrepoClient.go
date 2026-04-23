package client

import (
	"codecc/preci_server/internal/infra/logger"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
)

// BkRepoClient BkRepo 接口客户端
type BkRepoClient struct {
	httpClient *HTTPClient
	logger     *logger.Logger
}

// NewBkRepoClient 创建 BkRepo 客户端
func NewBkRepoClient() *BkRepoClient {
	config := &ClientConfig{
		BaseURL: BkRepoBaseUrl,
	}

	return &BkRepoClient{
		httpClient: NewHTTPClient(config),
		logger:     logger.GetLogger(),
	}
}

func (c *BkRepoClient) UploadLog(userId, token, uploadedFile string) error {
	// 打开要上传的文件
	file, err := os.Open(uploadedFile)
	if err != nil {
		c.logger.Error(fmt.Sprintf("failed to open file: %v", err))
		return fmt.Errorf("failed to open file: %w", err)
	}
	defer file.Close()

	path := fmt.Sprintf(
		"/generic/temporary/upload/%s/%s/%s/%s.log?token=",
		BkRepoProject, BkRepoRepo, BkRepoUploadSubPath, userId,
	) + token

	headers := map[string]string{
		"Content-Type":       "application/octet-stream",
		"X-BKREPO-OVERWRITE": "true",
		"X-BKREPO-EXPIRES":   "1",
	}

	err = c.httpClient.PutFile(path, headers, file, nil, UploadTimeout)
	if err != nil {
		c.logger.Error(fmt.Sprintf("failed to upload file: %v", err))
		return fmt.Errorf("failed to upload file: %w", err)
	}

	return nil
}

// GetLatestVersion 从 bkrepo 的 latest_version.txt 文件中获取当前的最新版本
func GetLatestVersion() (string, error) {
	url := BkRepoDownloadFolder + "/latest_version.txt"
	resp, err := http.Get(url)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	return strings.TrimSpace(string(body)), nil
}

// DownloadFile 从 bkrepo 下载文件
func DownloadFile(url string, filePath string) error {
	resp, err := http.Get(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	file, err := os.Create(filePath)
	if err != nil {
		return err
	}
	defer file.Close()

	_, err = io.Copy(file, resp.Body)
	if err != nil {
		return err
	}

	return nil
}
