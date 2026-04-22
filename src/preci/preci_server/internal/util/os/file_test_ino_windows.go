//go:build windows

package os

import "testing"

// Windows 上不直接比较 inode，返回 0 表示跳过比较
func fileIno(t *testing.T, path string) uint64 {
	t.Helper()
	return 0
}
