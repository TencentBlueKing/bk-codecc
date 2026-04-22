//go:build windows

package os

import (
	"path/filepath"
	"strings"
	"syscall"
)

// IsHidden 检查路径在 Windows 系统下是否具有隐藏属性
func IsHidden(path string) (bool, error) {
	// . 开头的文件仍视为隐藏
	if strings.HasPrefix(filepath.Base(path), ".") {
		return true, nil
	}

	// 将 Go 字符串转换为 Windows UTF-16 指针
	pointer, err := syscall.UTF16PtrFromString(path)
	if err != nil {
		return false, err
	}

	// 获取文件属性
	attributes, err := syscall.GetFileAttributes(pointer)
	if err != nil {
		return false, err
	}

	// 检查是否包含 FILE_ATTRIBUTE_HIDDEN 位
	return attributes&syscall.FILE_ATTRIBUTE_HIDDEN != 0, nil
}
