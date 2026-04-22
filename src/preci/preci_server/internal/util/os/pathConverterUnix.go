//go:build !windows

package os

func NeedPathConvert(path string) bool {
	return false
}

func PathConvert(path string) string {
	return path
}
