package model

import "time"

// DBConfig 本地数据库相关配置，控制超时和 mmap 初始大小等参数
type DBConfig struct {
	Timeout         time.Duration `koanf:"Timeout"`
	InitialMmapSize int           `koanf:"InitialMmapSize"`
}
