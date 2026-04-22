package model

import "time"

type Config struct {
	Web struct {
		ReadTimeout     time.Duration `koanf:"ReadTimeout"`
		WriteTimeout    time.Duration `koanf:"WriteTimeout"`
		IdleTimeout     time.Duration `koanf:"IdleTimeout"`
		ShutdownTimeout time.Duration `koanf:"ShutdownTimeout"`
		CodeCCBaseUrl   string        `koanf:"CodeCCBaseUrl"`
	}
	BkRepo struct {
		BaseUrl        string `koanf:"BaseUrl"`
		Project        string `koanf:"Project"`
		Repo           string `koanf:"Repo"`
		DownloadFolder string `koanf:"DownloadFolder"`
		UploadSubPath  string `koanf:"UploadSubPath"`
	}
	BKAuth struct {
		BaseURL  string `koanf:"BaseURL"`
		ClientID string `koanf:"ClientID"`
		Resource string `koanf:"Resource"`
	}
	Log struct {
		Level string `koanf:"Level"`
	}
	Db   DBConfig `koanf:"Db"`
	Task struct {
		MaxIncrementalFileSize int `koanf:"MaxIncrementalFileSize"`
	}
}
