package model

var GlobalConf GlobalConfig

// GlobalConfig 运行期可被其他模块访问的全局配置，从 Config 中派生出来
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
