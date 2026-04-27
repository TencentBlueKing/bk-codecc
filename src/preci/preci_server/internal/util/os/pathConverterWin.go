//go:build windows

package os

import "strings"

// NeedPathConvert 判断给定路径是否需要进行平台适配转换（Windows 下检测是否含有 '/'）
func NeedPathConvert(path string) bool {
	// 有的工具会将 win 环境下的分隔符统一替换成 '/', 在 PreCI 中需要将这种替换还原
	return strings.Contains(path, "/")
}

// PathConvert 将路径中的正斜杠替换为 Windows 反斜杠
func PathConvert(path string) string {
	return strings.ReplaceAll(path, "/", "\\")
}
