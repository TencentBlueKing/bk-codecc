package model

var GlobalConf GlobalConfig

// GlobalConfig 运行期可被其他模块访问的全局配置，从 Config 中派生出来
type GlobalConfig struct {
	InstallDir             string // server-bin 安装目录
	MaxIncrementalFileSize int
}

// GetInstallDir 返回 server-bin 安装目录路径
func (gc *GlobalConfig) GetInstallDir() string {
	return gc.InstallDir
}

// GetMaxIncrementalFileSize 返回增量扫描最大文件数限制，默认 1000
func (gc *GlobalConfig) GetMaxIncrementalFileSize() int {
	if gc.MaxIncrementalFileSize == 0 {
		return 1000
	}
	return gc.MaxIncrementalFileSize
}
