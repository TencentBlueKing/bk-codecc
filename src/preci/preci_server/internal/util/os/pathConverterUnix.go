//go:build !windows

package os

// NeedPathConvert 判断给定路径是否需要进行平台适配转换（Unix 下始终为 false）
func NeedPathConvert(path string) bool {
	return false
}

// PathConvert 将路径转换为当前平台格式（Unix 下直接返回原路径）
func PathConvert(path string) string {
	return path
}
