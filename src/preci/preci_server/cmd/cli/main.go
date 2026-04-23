package main

import (
	"codecc/preci_server/cmd/cli/cmd"
	"log"
	"os"
	"path/filepath"
	"strconv"
)

func main() {
	// 获取 cli 二进制文件所在目录
	exePath, err := os.Executable()
	if err != nil {
		log.Fatalf("get executable path fail: %v\n", err)
	}

	// 获取可执行文件所在目录
	cmd.InstallDir = filepath.Dir(exePath)

	portFilePath := filepath.Join(cmd.InstallDir, "config", "server.port")
	portData, err := os.ReadFile(portFilePath)
	if err != nil {
		log.Printf("read port file fail: %s\n", err.Error())
	} else {
		tempPort, err := strconv.Atoi(string(portData))
		if err == nil {
			cmd.Port = tempPort
		}
	}

	if len(os.Args) < 2 || os.Args[1] != "server" {
		// 非 server 命令才需要打印端口号
		log.Printf("port: %d, install dir: %s\n", cmd.Port, cmd.InstallDir)
	} else {
		log.Printf("install dir: %s\n", cmd.InstallDir)
	}

	if err := cmd.Execute(); err != nil {
		log.Fatalf("execute cmd failed: %v\n", err)
	}
}
