package bbolt

import (
	"encoding/binary"
)

// int64ToBytes 转换 int64 为 8 字节 []byte (大端序/网络字节序)
func int64ToBytes(n int64) []byte {
	b := make([]byte, 8)                     // 64位 = 8字节
	binary.BigEndian.PutUint64(b, uint64(n)) // 大端序
	return b
}
