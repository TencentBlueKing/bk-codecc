//go:build windows

package os

import "strings"

func NeedPathConvert(path string) bool {
	// 有的工具会将 win 环境下的分隔符统一替换成 '/', 在 PreCI 中需要将这种替换还原
	return strings.Contains(path, "/")
}

func PathConvert(path string) string {
	return strings.ReplaceAll(path, "/", "\\")
}
