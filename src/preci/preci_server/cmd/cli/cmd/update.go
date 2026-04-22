package cmd

import (
	"archive/zip"
	"codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/client"
	"fmt"
	"github.com/spf13/cobra"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
)

var updateCmd = &cobra.Command{
	Use:   "update",
	Short: "更新版本",
	Long: `更新版本。

Usage:
  preci update		# 更新到最新版本`,
	RunE: runUpdate,
}

func needUpdatedFiles() []string {
	if runtime.GOOS == "windows" {
		return []string{"preci.exe", "preci-server.exe", "preci-mcp.exe", "install.ps1",
			"uninstall.ps1", "uninstall_old_preci.ps1"}
	}

	return []string{"preci", "preci-server", "preci-mcp", "install.sh", "uninstall.sh", "uninstall_old_preci.sh"}
}

func needUpdatedFolders() []string {
	return []string{"config", "checkerset"}
}

func init() {
	rootCmd.AddCommand(updateCmd)
}

// installUpdate 下载完成后的安装流程（解压、校验、停服、运行更新器）
// 该函数是 runUpdate 和 init 自动更新的公共逻辑
func installUpdate() error {
	// 判断 zip 文件是否存在
	zipPath := filepath.Join(InstallDir, "preci_tmp.zip")
	if _, err := os.Stat(zipPath); os.IsNotExist(err) {
		return fmt.Errorf("zip 文件不存在: %s, 下载失败。 请稍后重试或者咨询 O2000", zipPath)
	}

	// 解压 zip 文件到临时目录
	tmpDir := filepath.Join(InstallDir, "preci_tmp")
	if err := unzipFile(zipPath, tmpDir); err != nil {
		return fmt.Errorf("解压文件失败: %w", err)
	}

	dir := filepath.Join(tmpDir, "PreCI")
	log.Info("dir: %s", dir)

	// 检查必要的文件是否齐全
	if err := validatePreCIFiles(dir); err != nil {
		return fmt.Errorf("文件校验失败: %w", err)
	}

	if err := stopServer(); err != nil {
		return fmt.Errorf("停止旧服务失败: %w", err)
	}

	// 编译并启动独立的更新器进程
	// 这样可以避免替换正在运行的可执行文件导致进程被kill
	if err := runUpdater(dir, InstallDir); err != nil {
		return fmt.Errorf("更新器执行失败: %w", err)
	}

	return nil
}

// runUpdate 处理 update 子命令
func runUpdate(cmd *cobra.Command, args []string) error {
	// 创建客户端
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}

	err = cli.DownloadLatestPreCI()
	if err != nil {
		log.Fail("更新失败: %s", err.Error())
		return nil
	}

	if err = installUpdate(); err != nil {
		log.Fail("安装更新失败: %s", err.Error())
		return nil
	}

	log.Success("更新成功")

	return nil
}

// unzipFile 解压zip文件到指定目录
func unzipFile(zipPath, destDir string) error {
	// 打开zip文件
	reader, err := zip.OpenReader(zipPath)
	if err != nil {
		return fmt.Errorf("打开zip文件失败: %w", err)
	}
	defer reader.Close()

	// 遍历zip文件中的所有文件
	for _, file := range reader.File {
		// 构建目标文件路径
		destPath := filepath.Join(destDir, file.Name)

		// 确保父目录存在
		if err := os.MkdirAll(filepath.Dir(destPath), 0755); err != nil {
			return fmt.Errorf("创建父目录失败 %s: %w", destPath, err)
		}

		// 检查是否为目录
		if file.FileInfo().IsDir() {
			// 创建目录
			if err := os.MkdirAll(destPath, file.Mode()); err != nil {
				return fmt.Errorf("创建目录失败 %s: %w", destPath, err)
			}
			continue
		}

		// 打开zip中的文件
		srcFile, err := file.Open()
		if err != nil {
			return fmt.Errorf("打开zip中的文件失败 %s: %w", file.Name, err)
		}

		// 创建目标文件
		destFile, err := os.OpenFile(destPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, file.Mode())
		if err != nil {
			srcFile.Close()
			return fmt.Errorf("创建目标文件失败 %s: %w", destPath, err)
		}

		// 复制文件内容
		_, err = io.Copy(destFile, srcFile)
		srcFile.Close()
		destFile.Close()

		if err != nil {
			return fmt.Errorf("复制文件内容失败 %s: %w", destPath, err)
		}
	}

	return nil
}

// validatePreCIFiles 校验必要的文件是否齐全
func validatePreCIFiles(dir string) error {
	// 必要的文件
	requiredFiles := needUpdatedFiles()

	// 检查可执行文件
	for _, file := range requiredFiles {
		filePath := filepath.Join(dir, file)
		if _, err := os.Stat(filePath); os.IsNotExist(err) {
			return fmt.Errorf("缺少必要的文件: %s", file)
		}
	}

	// 检查必要的目录
	requiredDirs := needUpdatedFolders()
	for _, d := range requiredDirs {
		dirPath := filepath.Join(dir, d)
		if info, err := os.Stat(dirPath); os.IsNotExist(err) || !info.IsDir() {
			return fmt.Errorf("缺少必要的目录: %s", d)
		}
	}

	// 检查配置文件是否存在
	configPath := filepath.Join(dir, "config", "initConfig.json")
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		return fmt.Errorf("缺少必要的文件: %s", configPath)
	}

	return nil
}

// runUpdater 运行独立的更新器进程
func runUpdater(srcDir, destDir string) error {
	// 更新器可执行文件名
	updaterName := "preci-updater"
	if runtime.GOOS == "windows" {
		updaterName = "preci-updater.exe"
	}

	// 优先使用新版本中的更新器（从下载的包中）
	updaterBin := filepath.Join(srcDir, updaterName)

	// 如果新版本中没有更新器，则使用当前安装目录中的更新器
	if _, err := os.Stat(updaterBin); err != nil {
		updaterBin = filepath.Join(destDir, updaterName)
		if _, err = os.Stat(updaterBin); err != nil {
			return fmt.Errorf("找不到更新器程序: %s", updaterName)
		}
	}

	log.Start("正在启动更新器: %s", updaterBin)
	updaterCmd := exec.Command(
		updaterBin,
		"-src", srcDir,
		"-dest", destDir,
	)
	updaterCmd.Stdout = os.Stdout
	updaterCmd.Stderr = os.Stderr

	if err := updaterCmd.Run(); err != nil {
		return fmt.Errorf("更新器执行失败: %w", err)
	}

	return nil
}
