package main

import (
	"codecc/preci_server/cmd/mcp/cmd"
	"log"
	"os"
	"path/filepath"
)

func main() {
	// 获取可执行文件所在路径
	exePath, err := os.Executable()
	if err != nil {
		log.Fatalf("获取可执行文件路径失败: %v\n", err)
	}

	// 设置安装目录
	cmd.InstallDir = filepath.Dir(exePath)

	log.Printf("启动服务: %s\n", cmd.InstallDir)

	// 执行命令
	if err := cmd.Execute(); err != nil {
		os.Exit(1)
	}
}
