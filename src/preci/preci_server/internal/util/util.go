// Package util provides small utility helpers shared across the PreCI
// server codebase.
package util

import (
	"encoding/binary"
	"fmt"
)

// BytesToInt64 转换 8 字节 []byte 为 int64 (大端序/网络字节序)
func BytesToInt64(b []byte) (int64, error) {
	if len(b) < 8 {
		return 0, fmt.Errorf("input must be at least 8 bytes, got %d", len(b))
	}
	// 读取前8字节 (避免panic)
	n := binary.BigEndian.Uint64(b[:8]) // 无符号解码
	return int64(n), nil                // 有符号转换
}

// IsSameStringSlice 比较两个字符串切片是否相同
func IsSameStringSlice(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}

	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}
