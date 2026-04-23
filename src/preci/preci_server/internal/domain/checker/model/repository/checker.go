package repository

import (
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util"
)

const checkerBucketName = "b_checker"
const severityPre = "s#"

func UpdateCheckerSeverity(sto storage.Storage, toolName, checker string, severity int64) error {
	key := severityPre + toolName + "#" + checker
	return sto.UpdateByStrKey2Int(checkerBucketName, key, severity)
}

// BatchUpdateCheckerSeverity 批量更新 checker severity，合并为一次事务写入
func BatchUpdateCheckerSeverity(sto storage.Storage, kvs map[string]int64) error {
	return sto.BatchUpdateByStrKey2Int(checkerBucketName, kvs)
}

// BuildCheckerSeverityKey 构建 checker severity 的存储 key
func BuildCheckerSeverityKey(toolName, checker string) string {
	return severityPre + toolName + "#" + checker
}

func GetCheckerSeverity(sto storage.Storage, toolName, checker string) (int64, error) {
	key := severityPre + toolName + "#" + checker
	data, err := sto.GetByStrKey(checkerBucketName, key)
	if err != nil {
		return 0, err
	}

	return util.BytesToInt64(data)
}
