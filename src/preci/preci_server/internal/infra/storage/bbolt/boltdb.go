package bbolt

import (
	"codecc/preci_server/internal/infra/config/model"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/util"
	"codecc/preci_server/internal/util/constant"
	pos "codecc/preci_server/internal/util/os"
	"fmt"
	"go.etcd.io/bbolt"
	"path/filepath"
	"runtime"
	"strings"
	"time"
)

type BoltDB struct {
	dataDir    string
	dbInstance *bbolt.DB
}

func NewBoltDB(dataDir string) *BoltDB {
	return &BoltDB{
		dataDir: dataDir,
	}
}

const (
	dbFile   = "bbolt.db"
	waitTime = 500 * time.Millisecond
)

func (db *BoltDB) Init(config model.DBConfig) error {
	log := logger.GetLogger()

	var err error
	// 创建数据目录
	if err = pos.MkDir(db.dataDir); err != nil {
		log.Error(fmt.Sprintf("failed to create data directory: %v", err))
		return err
	}

	// 配置
	timeout := config.Timeout
	if timeout == 0 {
		timeout = 5 * time.Second // 默认 5 秒超时，避免永久阻塞
	}
	opt := &bbolt.Options{
		Timeout:         timeout,
		InitialMmapSize: config.InitialMmapSize,
	}

	dbPath := filepath.Join(db.dataDir, dbFile)

	log.Info(fmt.Sprintf("open bbolt db: %s", dbPath))
	// 初始化
	db.dbInstance, err = bbolt.Open(dbPath, 0600, opt)
	if err == nil {
		log.Info("bbolt db initialized successfully")
		return nil
	}

	if runtime.GOOS != constant.Windows || !strings.Contains(err.Error(), constant.ErrorLockViolation) {
		return err
	}

	// windwos 环境需要处理 ERROR_LOCK_VIOLATION 误报, 重试 3 次
	for i := 0; i < 3; i++ {
		time.Sleep(waitTime)

		db.dbInstance, err = bbolt.Open(dbPath, 0600, opt)
		if err == nil || !strings.Contains(err.Error(), constant.ErrorLockViolation) {
			return err
		}
	}

	return err
}

func (db *BoltDB) Close() {
	log := logger.GetLogger()

	err := db.instanceCheck()
	if err != nil {
		log.Error(fmt.Sprintf("failed to close db instance: %v", err))
		return
	}

	err = db.dbInstance.Close()
	if err == nil {
		return
	}

	// 重试 3 次
	for i := 0; i < 3; i++ {
		time.Sleep(waitTime)
		err = db.dbInstance.Close()
		if err == nil {
			return
		}
	}

	log.Error(fmt.Sprintf("failed to close db instance: %v", err))
}

func (db *BoltDB) UpdateByStrKey2Int(bucket, key string, value int64) error {
	return db.update([]byte(bucket), []byte(key), int64ToBytes(value))
}

// BatchUpdateByStrKey2Int 批量更新，将所有写入合并到同一个事务中，避免多次 fsync
func (db *BoltDB) BatchUpdateByStrKey2Int(bucket string, kvs map[string]int64) error {
	err := db.instanceCheck()
	if err != nil {
		return err
	}

	bucketBytes := []byte(bucket)
	return db.dbInstance.Update(func(tx *bbolt.Tx) error {
		buk, err := tx.CreateBucketIfNotExists(bucketBytes)
		if err != nil {
			return err
		}

		for key, value := range kvs {
			if err := buk.Put([]byte(key), int64ToBytes(value)); err != nil {
				return err
			}
		}
		return nil
	})
}

func (db *BoltDB) UpdateByStrKey(bucket, key string, value []byte) error {
	return db.update([]byte(bucket), []byte(key), value)
}

func (db *BoltDB) UpdateByIntKey(bucket string, key int64, value []byte) error {
	return db.update([]byte(bucket), int64ToBytes(key), value)
}

func (db *BoltDB) GetByStrKey(bucket, key string) ([]byte, error) {
	return db.get([]byte(bucket), []byte(key))
}

func (db *BoltDB) GetByIntKey(bucket string, key int64) ([]byte, error) {
	return db.get([]byte(bucket), int64ToBytes(key))
}

func (db *BoltDB) IncrementAndGet(bucket, key string) (int64, error) {
	err := db.instanceCheck()
	if err != nil {
		return 0, err
	}

	var result int64
	err = db.dbInstance.Update(func(tx *bbolt.Tx) error {
		buk, err := tx.CreateBucketIfNotExists([]byte(bucket))
		if err != nil {
			return err
		}

		// 读取当前值
		value := buk.Get([]byte(key))
		if value == nil {
			result = 1 // 不存在, 认为是 0, 直接递增到 1
			return buk.Put([]byte(key), int64ToBytes(result))
		}

		result, err = util.BytesToInt64(value)
		if err != nil {
			return err
		}

		// 递增
		result = result + 1

		// 写回新值
		return buk.Put([]byte(key), int64ToBytes(result))
	})

	return result, err
}

func (db *BoltDB) instanceCheck() error {
	if db.dbInstance == nil {
		return fmt.Errorf("db instance is not initialized")
	}

	return nil
}

func (db *BoltDB) update(bucket, key, value []byte) error {
	err := db.dbInstance.Update(func(tx *bbolt.Tx) error {
		buk, err := tx.CreateBucketIfNotExists(bucket)
		if err != nil {
			return err
		}

		return buk.Put(key, value)
	})

	return err
}

func (db *BoltDB) get(bucket, key []byte) ([]byte, error) {
	err := db.instanceCheck()
	if err != nil {
		return nil, err
	}

	var result []byte
	err = db.dbInstance.View(func(tx *bbolt.Tx) error {
		buk := tx.Bucket(bucket)
		if buk == nil {
			return fmt.Errorf("bucket %s not found", bucket)
		}

		value := buk.Get(key)
		if value == nil {
			return fmt.Errorf("key %s not found in bucket %s", key, bucket)
		}

		// 复制数据，因为返回的字节切片在事务结束后可能无效
		result = make([]byte, len(value))
		copy(result, value)
		return nil
	})

	return result, err
}

// GetByPrefix 根据 key 前缀获取所有匹配的键值对
// 返回 map[string][]byte，key 为字符串形式的键，value 为对应的值
func (db *BoltDB) GetByPrefix(bucket, prefix string) (map[string][]byte, error) {
	err := db.instanceCheck()
	if err != nil {
		return nil, err
	}

	result := make(map[string][]byte)
	prefixBytes := []byte(prefix)

	err = db.dbInstance.View(func(tx *bbolt.Tx) error {
		buk := tx.Bucket([]byte(bucket))
		if buk == nil {
			return fmt.Errorf("bucket %s not found", bucket)
		}

		// 使用 Cursor 进行前缀匹配
		cursor := buk.Cursor()

		// Seek 会定位到大于等于 prefix 的第一个 key
		for k, v := cursor.Seek(prefixBytes); k != nil && hasPrefix(k, prefixBytes); k, v = cursor.Next() {
			// 复制 key 和 value，因为它们在事务结束后可能无效
			keyCopy := make([]byte, len(k))
			copy(keyCopy, k)

			valueCopy := make([]byte, len(v))
			copy(valueCopy, v)

			result[string(keyCopy)] = valueCopy
		}

		return nil
	})

	return result, err
}

// hasPrefix 检查 key 是否以 prefix 开头
func hasPrefix(key, prefix []byte) bool {
	if len(key) < len(prefix) {
		return false
	}
	for i := len(prefix) - 1; i >= 0; i-- {
		if key[i] != prefix[i] {
			return false
		}
	}
	return true
}

// DeleteByPrefix 根据 key 前缀删除所有匹配的键值对
func (db *BoltDB) DeleteByPrefix(bucket, prefix string) error {
	err := db.instanceCheck()
	if err != nil {
		return err
	}

	log := logger.GetLogger()

	prefixBytes := []byte(prefix)

	return db.dbInstance.Update(func(tx *bbolt.Tx) error {
		buk := tx.Bucket([]byte(bucket))
		if buk == nil {
			// bucket 不存在，认为删除成功
			return nil
		}

		// 使用 Cursor 进行前缀匹配并删除
		cursor := buk.Cursor()

		// 直接在遍历时删除，cursor.Delete() 会自动移动到下一个位置
		for k, _ := cursor.Seek(prefixBytes); k != nil && hasPrefix(k, prefixBytes); k, _ = cursor.Next() {
			if err := cursor.Delete(); err != nil {
				log.Error(fmt.Sprintf("delete key %s error %s", string(k), err.Error()))
			}
		}

		return nil
	})
}
