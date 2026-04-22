package model

var GlobalConf GlobalConfig

type GlobalConfig struct {
	InstallDir             string // server-bin 安装目录
	MaxIncrementalFileSize int
}

func (gc *GlobalConfig) GetInstallDir() string {
	return gc.InstallDir
}

func (gc *GlobalConfig) GetMaxIncrementalFileSize() int {
	if gc.MaxIncrementalFileSize == 0 {
		return 1000
	}
	return gc.MaxIncrementalFileSize
}
