package storage

// AuditEntity 公共的审计字段，供其他存储实体内嵌以记录创建和更新时间
type AuditEntity struct {
	CreatedAt int64 `json:"createdAt"`
	UpdatedAt int64 `json:"updatedAt"`
}
