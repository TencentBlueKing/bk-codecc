// Package tool implements the tool domain, providing metadata and
// installation/invocation logic for PreCI scanning tools.
package tool

import (
	"archive/zip"
	"bufio"
	domainerror "codecc/preci_server/internal/domain/perror"
	"codecc/preci_server/internal/domain/tool/model"
	"codecc/preci_server/internal/domain/tool/model/repository"
	"codecc/preci_server/internal/infra/client"
	conf "codecc/preci_server/internal/infra/config/model"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util/constant"
	"codecc/preci_server/internal/util/perror"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
)

// downloadFile 下载文件到本地
func downloadFile(url, savedDir, fileName string) error {
	log := logger.GetLogger()
	log.Info(fmt.Sprintf("download file from %s to %s, %s", url, savedDir, fileName))

	// 创建HTTP请求
	resp, err := http.Get(url)

	if err != nil {
		log.Error(fmt.Sprintf("get file.zip from %s error: %v", url, err))

		if resp != nil && resp.Body != nil {
			_ = resp.Body.Close()
		}
		return perror.ErrExternalServiceError
	}

	defer resp.Body.Close()

	// 检查响应状态
	if resp.StatusCode != http.StatusOK {
		log.Error(fmt.Sprintf("get file.zip from %s fail. code = %d", url, resp.StatusCode))
		return perror.ErrExternalServiceError
	}

	// 创建目录（如果不存在）
	if err = os.MkdirAll(savedDir, 0755); err != nil {
		log.Error(fmt.Sprintf("failed to create directory: %v", err))
		return perror.ErrDirectoryCreateFailed
	}

	filePath := filepath.Join(savedDir, fileName)

	// 如果文件已存在，先删除
	if _, err = os.Stat(filePath); err == nil {
		if err = os.Remove(filePath); err != nil {
			log.Error(fmt.Sprintf("failed to remove file: %v", err))
			return perror.ErrFileRemoveFailed
		}
	}

	// 创建文件
	out, err := os.Create(filePath)
	if err != nil {
		log.Error(fmt.Sprintf("failed to create file: %v", err))
		return perror.ErrFileCreateFailed
	}
	defer out.Close()

	// 复制响应体到文件
	_, err = io.Copy(out, resp.Body)
	if err != nil {
		log.Error(fmt.Sprintf("failed to copy file: %v", err))
		return perror.ErrFileWriteFailed
	}

	return nil
}

// getFileNameFromURL 从URL中提取文件名
func getFileNameFromURL(url string) (string, error) {
	parts := strings.Split(url, "/")
	if len(parts) > 0 {
		return parts[len(parts)-1], nil
	}

	return "", perror.ErrInvalidParam
}

// unzipFile 解压zip文件到指定目录
func unzipFile(zipPath, destDir string) error {
	log := logger.GetLogger()

	// 打开zip文件
	reader, err := zip.OpenReader(zipPath)
	if err != nil {
		return err
	}
	defer reader.Close()

	// 遍历zip文件中的所有文件
	for _, file := range reader.File {
		// 构建目标文件路径
		destPath := filepath.Join(destDir, file.Name)

		// 检查是否为目录
		if file.FileInfo().IsDir() {
			// 创建目录
			if err := os.MkdirAll(destPath, file.Mode()); err != nil {
				log.Error(fmt.Sprintf("failed to create directory %s: %v", destPath, err))
				return err
			}
			continue
		}

		// 确保父目录存在
		if err := os.MkdirAll(filepath.Dir(destPath), 0755); err != nil {
			log.Error(fmt.Sprintf("failed to create parent directory for %s: %v", destPath, err))
			return err
		}

		// 打开zip中的文件
		srcFile, err := file.Open()
		if err != nil {
			log.Error(fmt.Sprintf("failed to open file in zip %s: %v", file.Name, err))
			return err
		}

		// 创建目标文件
		destFile, err := os.OpenFile(destPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, file.Mode())
		if err != nil {
			srcFile.Close()
			log.Error(fmt.Sprintf("failed to create destination file %s: %v", destPath, err))
			return err
		}

		// 复制文件内容
		_, err = io.Copy(destFile, srcFile)
		srcFile.Close()
		destFile.Close()

		if err != nil {
			log.Error(fmt.Sprintf("failed to copy file content to %s: %v", destPath, err))
			return err
		}
	}

	return nil
}

func readToolVersionTable() map[string]string {
	baseDir := conf.GlobalConf.GetInstallDir()
	path := filepath.Join(baseDir, "tool", "toolVersion.json")

	log := logger.GetLogger()

	data, err := os.ReadFile(path)
	if err != nil {
		log.Error(fmt.Sprintf("failed to read tool version table: %v", err))
		return nil
	}

	// 读取 json 并将其转成 map
	result := make(map[string]string)
	err = json.Unmarshal(data, &result)
	if err != nil {
		log.Error(fmt.Sprintf("failed to read tool version table: %v", err))
		return nil
	}

	return result
}

func ReloadTools(toolNames []string) error {
	log := logger.GetLogger()
	codeCCClient := client.NewCodeCCClient()
	toolMetaDTOs, err := codeCCClient.GetToolInfos(toolNames)
	if err != nil {
		return err
	}

	localVersionTable := readToolVersionTable()
	versionTable := make(map[string]string)
	if len(localVersionTable) > 0 {
		for toolName, status := range localVersionTable {
			versionTable[toolName] = status
		}
	}

	for _, toolMetaDTO := range toolMetaDTOs {
		toolMeta := model.NewToolMeta(toolMetaDTO, versionTable)
		if toolMeta == nil {
			log.Warn(fmt.Sprintf("cannot find wanted version in tool: %v", toolMetaDTO))
			continue
		}

		log.Info(fmt.Sprintf("save tool %s, download url: %s", toolMeta.Name, toolMeta.GetBinDownloadUrl()))

		updated, err := repository.SaveToolMeta(storage.DB, toolMeta)
		if err != nil {
			log.Warn(fmt.Sprintf("failed to save tool: %v, err: %v", toolMeta, err))
		}

		baseDir := conf.GlobalConf.GetInstallDir()
		savedDir := filepath.Join(baseDir, "tool", toolMeta.Name)
		if updated {
			log.Info(fmt.Sprintf("tool %s need updated", toolMeta.Name))
			if downloadUrl := toolMeta.GetBinDownloadUrl(); downloadUrl != "" {

				// 删除旧目录
				_ = os.RemoveAll(savedDir)

				fileName, err := getFileNameFromURL(downloadUrl)
				if err != nil {
					log.Warn(fmt.Sprintf("failed to get file name from URL: %s", downloadUrl))
					continue
				}
				var filePath = filepath.Join(savedDir, fileName)

				err = downloadFile(downloadUrl, savedDir, fileName)
				if err != nil {
					log.Warn(fmt.Sprintf("failed to download tool %s: %v", toolMeta.Name, err))
					continue
				}

				if !strings.HasSuffix(strings.ToLower(fileName), ".zip") {
					log.Warn(fmt.Sprintf("file %s is not a zip file, skip it", fileName))
					continue
				}

				// 如果是 zip 文件，解压并删除压缩包
				log.Info(fmt.Sprintf("unzipping file %s", filePath))
				err = unzipFile(filePath, savedDir)
				if err != nil {
					log.Warn(fmt.Sprintf("failed to unzip file %s: %v", filePath, err))
					continue
				}
				log.Info(fmt.Sprintf("successfully unzipped file %s", filePath))

				// 删除压缩包
				err = os.Remove(filePath)
				if err != nil {
					log.Warn(fmt.Sprintf("failed to remove zip file %s: %v", filePath, err))
				}
			}
		}

		// TODO: 处理 docker 扫描的场景
	}

	return nil
}

func RunTool(ctx context.Context, rootDir string, toolInput *model.ToolScanInput) (string, error) {
	log := logger.GetLogger()
	toolName := toolInput.ToolName
	binaryInfo, err := repository.GetToolBinaryInfo(storage.DB, toolName)
	if err != nil {
		return "", err
	}

	inputJsonPath := filepath.Join(rootDir, constant.CodeCCDir, toolName, "input.json")
	outputJsonPath := filepath.Join(rootDir, constant.CodeCCDir, toolName, "output.json")

	// 将 toolInput 序列化为 JSON
	jsonData, err := json.MarshalIndent(toolInput, "", "  ")
	if err != nil {
		log.Error(fmt.Sprintf("failed to marshal toolInput to JSON: %v", err))
		return "", perror.ErrFileWriteFailed
	}

	// 确保目录存在
	inputDir := filepath.Dir(inputJsonPath)
	if err = os.MkdirAll(inputDir, 0755); err != nil {
		log.Error(fmt.Sprintf("failed to create directory %s: %v", inputDir, err))
		return "", perror.ErrDirectoryCreateFailed
	}

	// 写入 JSON 文件
	if err = os.WriteFile(inputJsonPath, jsonData, 0644); err != nil {
		log.Error(fmt.Sprintf("failed to write JSON to %s: %v", inputJsonPath, err))
		return "", perror.ErrFileWriteFailed
	}

	log.Info(fmt.Sprintf("successfully saved toolInput to %s", inputJsonPath))

	cmd := binaryInfo.GetCommand(inputJsonPath, outputJsonPath)

	baseDir := conf.GlobalConf.GetInstallDir()
	workspace := filepath.Join(baseDir, "tool", toolInput.ToolName)

	// 检查工作目录是否存在
	if _, err = os.Stat(workspace); os.IsNotExist(err) {
		log.Error(fmt.Sprintf("workspace directory does not exist: %s", workspace))
		return "", domainerror.ErrToolNotFound
	}

	// 在 workspace 目录中执行命令
	log.Info(fmt.Sprintf("executing command in workspace %s: %s", workspace, cmd))

	// 创建命令执行对象
	var cmdExec *exec.Cmd
	switch runtime.GOOS {
	case constant.Windows:
		cmdExec = exec.CommandContext(ctx, "cmd", "/C", cmd)
	case constant.Linux:
		cmdExec = exec.CommandContext(ctx, "bash", "-c", cmd)
	case constant.Darwin:
		cmdExec = exec.CommandContext(ctx, "zsh", "-c", cmd)
	default:
		log.Error(fmt.Sprintf("unsupported operating system: %s", runtime.GOOS))
		return "", domainerror.ErrRuntimeEnvironment
	}

	// 设置工作目录
	cmdExec.Dir = workspace

	// 创建标准输出和标准错误的管道
	stdoutPipe, err := cmdExec.StdoutPipe()
	if err != nil {
		log.Error(fmt.Sprintf("failed to create stdout pipe: %v", err))
		return "", perror.ErrExternalServiceError
	}

	stderrPipe, err := cmdExec.StderrPipe()
	if err != nil {
		log.Error(fmt.Sprintf("failed to create stderr pipe: %v", err))
		return "", perror.ErrExternalServiceError
	}

	// 使用 WaitGroup 确保所有输出都被读取完
	var wg sync.WaitGroup
	wg.Add(2)

	// 启动 goroutine 实时读取标准输出并写入日志
	go func() {
		defer wg.Done()
		scanner := bufio.NewScanner(stdoutPipe)
		for scanner.Scan() {
			line := scanner.Text()
			log.Info(fmt.Sprintf("[%s stdout] %s", toolName, line))
		}
		if err := scanner.Err(); err != nil {
			log.Error(fmt.Sprintf("[%s stdout] scanner error: %v", toolName, err))
		}
	}()

	// 启动 goroutine 实时读取标准错误并写入日志
	go func() {
		defer wg.Done()
		scanner := bufio.NewScanner(stderrPipe)
		for scanner.Scan() {
			line := scanner.Text()
			log.Warn(fmt.Sprintf("[%s stderr] %s", toolName, line))
		}
		if err := scanner.Err(); err != nil {
			log.Error(fmt.Sprintf("[%s stderr] scanner error: %v", toolName, err))
		}
	}()

	// 启动命令执行
	if err = cmdExec.Start(); err != nil {
		log.Error(fmt.Sprintf("failed to start command: %v", err))
		return "", perror.ErrExternalServiceError
	}

	// 等待命令执行完成
	if err = cmdExec.Wait(); err != nil {
		log.Error(fmt.Sprintf("command execution failed: %v", err))
		if ctx.Err() == context.Canceled {
			log.Info(fmt.Sprintf("[%s] scan canceled", toolName))
			return "", perror.ErrUserCancel
		}

		// 即使命令失败也要等待日志输出完成
		wg.Wait()
		return "", perror.ErrExternalServiceError
	}

	// 等待所有日志输出完成
	wg.Wait()

	log.Info(fmt.Sprintf("command executed successfully"))

	return outputJsonPath, nil
}
