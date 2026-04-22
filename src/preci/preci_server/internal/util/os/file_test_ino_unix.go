//go:build !windows

package os

import (
	"os"
	"syscall"
	"testing"
)

func fileIno(t *testing.T, path string) uint64 {
	t.Helper()
	info, err := os.Stat(path)
	if err != nil {
		t.Fatalf("stat %s: %v", path, err)
	}
	stat, ok := info.Sys().(*syscall.Stat_t)
	if !ok {
		t.Fatalf("无法获取 Stat_t: %s", path)
	}
	return stat.Ino
}
