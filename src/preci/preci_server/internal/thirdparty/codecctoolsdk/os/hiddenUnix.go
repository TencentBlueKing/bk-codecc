//go:build !windows

package os

import (
	"path/filepath"
	"strings"
)

// IsHidden 检查路径在 Unix 系统下是否隐藏（以 . 开头）
func IsHidden(path string) (bool, error) {
	filename := filepath.Base(path)
	// Unix 下约定：以 . 开头的文件/目录为隐藏
	return strings.HasPrefix(filename, "."), nil
}
