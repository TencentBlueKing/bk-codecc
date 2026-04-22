package model

import "time"

type DBConfig struct {
	Timeout         time.Duration `koanf:"Timeout"`
	InitialMmapSize int           `koanf:"InitialMmapSize"`
}
