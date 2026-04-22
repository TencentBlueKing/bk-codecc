package cmd

import (
	"os"
	"path/filepath"
)

// GetCurrentPath 获取当前路径
func GetCurrentPath() (string, error) {
	cwd, err := os.Getwd()
	if err == nil {
		return cwd, nil
	}

	return "", err
}

// GetProjectRoot 从当前路径往上找, 直到有 .codecc 文件夹的目录, 即为项目根目录
func GetProjectRoot() string {
	currentPath, err := GetCurrentPath()
	if err != nil || currentPath == "" {
		return ""
	}

	for {
		// 检查当前目录下是否有 .codecc 文件夹
		codeccPath := filepath.Join(currentPath, ".codecc")
		if info, err := os.Stat(codeccPath); err == nil && info.IsDir() {
			return currentPath
		}

		// 获取父目录
		parent := filepath.Dir(currentPath)
		if parent == currentPath {
			// 已经到达根目录，未找到 .codecc 文件夹
			return ""
		}

		currentPath = parent
	}
}
