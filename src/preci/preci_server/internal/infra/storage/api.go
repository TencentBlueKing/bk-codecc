package storage

import "codecc/preci_server/internal/infra/config/model"

var DB Storage

type Storage interface {
	Init(config model.DBConfig) error

	UpdateByStrKey2Int(bucket, key string, value int64) error
	BatchUpdateByStrKey2Int(bucket string, kvs map[string]int64) error
	UpdateByStrKey(bucket, key string, value []byte) error
	UpdateByIntKey(bucket string, key int64, value []byte) error

	GetByStrKey(bucket, key string) ([]byte, error)
	GetByIntKey(bucket string, key int64) ([]byte, error)
	GetByPrefix(bucket, prefix string) (map[string][]byte, error)

	DeleteByPrefix(bucket, prefix string) error

	IncrementAndGet(bucket, key string) (int64, error)

	Close()
}
