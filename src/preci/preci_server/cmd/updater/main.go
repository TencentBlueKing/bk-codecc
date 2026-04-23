package main

import (
	pcos "codecc/preci_server/internal/util/os"
	"flag"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"time"
)

var (
	srcDir  string
	destDir string
)

func init() {
	flag.StringVar(&srcDir, "src", "", "源目录（新版本文件所在目录）")
	flag.StringVar(&destDir, "dest", "", "目标目录（安装目录）")
}

func main() {
	flag.Parse()

	// 验证参数
	if srcDir == "" || destDir == "" {
		log.Fatal("缺少必要参数: -src, -dest")
	}

	fmt.Println("正在替换文件...")

	// 替换文件
	if err := replaceFiles(srcDir, destDir); err != nil {
		log.Fatalf("替换文件失败: %v", err)
	}

	fmt.Println("正在启动新服务...")

	// 启动新服务
	if err := startServer(); err != nil {
		fmt.Printf("启动新服务失败: %v\n", err)
	}

	fmt.Println("正在清理临时文件...")

	// 清理临时文件
	cleanupTempFiles()

	fmt.Println("更新完成！")
}

// replaceFiles 将临时目录中的文件替换到目标目录
func replaceFiles(srcDir, destDir string) error {
	// 需要替换的文件列表
	filesToReplace := needUpdatedFiles()

	// 替换文件
	for _, file := range filesToReplace {
		srcPath := filepath.Join(srcDir, file)
		destPath := filepath.Join(destDir, file)

		if err := pcos.ReplaceFile(srcPath, destPath); err != nil {
			return fmt.Errorf("替换文件 %s 失败: %w", file, err)
		}
	}

	// 需要替换的目录列表
	dirsToReplace := needUpdatedFolders()

	// 替换目录
	for _, dir := range dirsToReplace {
		srcPath := filepath.Join(srcDir, dir)
		destPath := filepath.Join(destDir, dir)

		// 先删除旧目录
		if err := os.RemoveAll(destPath); err != nil {
			return fmt.Errorf("删除旧目录 %s 失败: %w", dir, err)
		}

		// 复制新目录
		if err := copyDir(srcPath, destPath); err != nil {
			return fmt.Errorf("复制目录 %s 失败: %w", dir, err)
		}
	}

	return nil
}

// copyDir 递归复制目录
func copyDir(src, dest string) error {
	// 获取源目录信息
	srcInfo, err := os.Stat(src)
	if err != nil {
		return fmt.Errorf("获取源目录信息失败: %w", err)
	}

	// 创建目标目录
	if err := os.MkdirAll(dest, srcInfo.Mode()); err != nil {
		return fmt.Errorf("创建目标目录失败: %w", err)
	}

	// 读取源目录内容
	entries, err := os.ReadDir(src)
	if err != nil {
		return fmt.Errorf("读取源目录失败: %w", err)
	}

	// 遍历目录内容
	for _, entry := range entries {
		srcPath := filepath.Join(src, entry.Name())
		destPath := filepath.Join(dest, entry.Name())

		if entry.IsDir() {
			// 递归复制子目录
			if err := copyDir(srcPath, destPath); err != nil {
				return err
			}
		} else {
			// 复制文件
			if err := pcos.ReplaceFile(srcPath, destPath); err != nil {
				return err
			}
		}
	}

	return nil
}

// needUpdatedFiles 返回需要更新的文件列表
func needUpdatedFiles() []string {
	if runtime.GOOS == "windows" {
		return []string{"preci.exe", "preci-server.exe", "preci-mcp.exe", "install.ps1",
			"uninstall.ps1", "uninstall_old_preci.ps1"}
	}
	return []string{"preci", "preci-server", "preci-mcp", "install.sh", "uninstall.sh", "uninstall_old_preci.sh"}
}

// needUpdatedFolders 返回需要更新的目录列表
func needUpdatedFolders() []string {
	return []string{"config", "checkerset"}
}

// startServer 启动新服务
func startServer() error {
	// 构建启动命令
	cmd := exec.Command("preci", "server", "start")

	// 在后台启动
	if err := cmd.Start(); err != nil {
		return fmt.Errorf("启动服务失败: %w", err)
	}

	// 不等待进程结束
	go func() {
		cmd.Wait()
	}()

	// 等待一下确保服务启动
	time.Sleep(500 * time.Millisecond)

	return nil
}

// cleanupTempFiles 清理临时文件
func cleanupTempFiles() {
	tmpDir := filepath.Join(destDir, "preci_tmp")
	if err := os.RemoveAll(tmpDir); err != nil {
		fmt.Printf("清理临时目录失败 %s: %v\n", tmpDir, err)
	}

	zipPath := filepath.Join(destDir, "preci_tmp.zip")
	if err := os.Remove(zipPath); err != nil && !os.IsNotExist(err) {
		fmt.Printf("清理 zip 文件失败 %s: %v\n", zipPath, err)
	}

	// 清理 ReplaceFile 产生的备份临时目录
	pcos.CleanupBackups()
}
