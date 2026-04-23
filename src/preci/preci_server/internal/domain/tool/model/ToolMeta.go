package model

import (
	"codecc/preci_server/internal/infra/client/dto"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/util/constant"
	"fmt"
	"runtime"
	"strings"
)

const (
	ToolVersionTest = "T"
	ToolVersionGray = "G"
	ToolVersionProd = "P"
)

type ToolMeta struct {
	Name         string      `json:"name"`
	DisplayName  string      `json:"displayName"`
	Lang         int64       `json:"lang"`
	ToolVersions ToolVersion `json:"toolVersions"`
}

func validVersion(version string) bool {
	return version == ToolVersionTest || version == ToolVersionGray || version == ToolVersionProd
}

// NewToolMeta 将 ToolMetaDetail dto 的信息赋值到一个 ToolMeta
func NewToolMeta(toolMetaDto dto.ToolMetaDetail, toolVersionTable map[string]string) *ToolMeta {
	log := logger.GetLogger()

	log.Info(fmt.Sprintf("#TMP toolMeta: %v", toolMetaDto))
	selectedVersion := ToolVersionProd

	if toolVersionTable != nil {
		if v, ok := toolVersionTable[toolMetaDto.Name]; ok && validVersion(v) {
			selectedVersion = v
		}
	}

	log.Info(fmt.Sprintf("tool: %s, selected version: %s", toolMetaDto.Name, selectedVersion))
	for _, version := range toolMetaDto.ToolVersions {
		if version.VersionType == selectedVersion {
			selectedToolVersion := ToolVersion{
				VersionType:               version.VersionType,
				DockerTriggerShell:        version.DockerTriggerShell,
				DockerImageURL:            version.DockerImageURL,
				DockerImageVersion:        version.DockerImageVersion,
				ForeignDockerImageVersion: version.ForeignDockerImageVersion,
				DockerImageHash:           version.DockerImageHash,
				Binary: Binary{
					WinUrl:        version.Binary.WinUrl,
					LinuxUrl:      version.Binary.LinuxUrl,
					MacUrl:        version.Binary.MacUrl,
					WinCommand:    version.Binary.WinCommand,
					LinuxCommand:  version.Binary.LinuxCommand,
					MacCommand:    version.Binary.MacCommand,
					BinaryVersion: version.Binary.BinaryVersion,
				},
			}

			return &ToolMeta{
				Name:         toolMetaDto.Name,
				DisplayName:  toolMetaDto.DisplayName,
				Lang:         toolMetaDto.Lang,
				ToolVersions: selectedToolVersion,
			}
		}
	}

	return nil
}

// GetBinDownloadUrl 自动识别当前操作系统, 获取工具二进制下载链接
func (t *ToolMeta) GetBinDownloadUrl() string {
	switch runtime.GOOS {
	case constant.Windows:
		return t.ToolVersions.Binary.WinUrl
	case constant.Linux:
		return t.ToolVersions.Binary.LinuxUrl
	case constant.Darwin: // macOS的系统标识是darwin
		return t.ToolVersions.Binary.MacUrl
	default:
		return ""
	}
}

type ToolVersion struct {
	// 工具版本类型，T-测试版本，G-灰度版本，P-正式发布版本，O-开源扫描. 目前 PreCI 只使用 P 版本
	VersionType string `json:"versionType"`

	// docker启动运行的命令
	DockerTriggerShell string `json:"dockerTriggerShell"`

	// docker镜像存放URL
	DockerImageURL string `json:"dockerImageURL"`

	// 工具docker镜像版本号
	DockerImageVersion string `json:"dockerImageVersion"`

	// 工具外部docker镜像版本号，用于关联第三方直接提供的docker镜像版本
	ForeignDockerImageVersion string `json:"foreignDockerImageVersion"`

	// docker镜像hash值
	DockerImageHash string `json:"dockerImageHash"`

	// 工具二进制相关信息
	Binary Binary `json:"binary"`
}

type Binary struct {
	WinUrl        string `json:"winUrl"`        // win二进制的下载路径
	LinuxUrl      string `json:"linuxUrl"`      // linux二进制的下载路径
	MacUrl        string `json:"macUrl"`        // mac二进制的下载路径
	WinCommand    string `json:"winCommand"`    // win环境下命令行
	LinuxCommand  string `json:"linuxCommand"`  // linux环境下命令行
	MacCommand    string `json:"macCommand"`    // mac环境下命令行
	BinaryVersion string `json:"binaryVersion"` // 二进制工具版本
}

func (b *Binary) GetCommand(inputJsonPath, outputJsonPath string) string {
	cmd := ""
	switch runtime.GOOS {
	case constant.Windows:
		cmd = b.WinCommand
	case constant.Linux:
		cmd = b.LinuxCommand
	case constant.Darwin:
		cmd = b.MacCommand
	default:
		return ""
	}

	if cmd == "" {
		return cmd
	}

	// 替换命令中的占位符
	cmd = strings.Replace(cmd, "{input.json}", inputJsonPath, 1)
	cmd = strings.Replace(cmd, "{output.json}", outputJsonPath, 1)

	return cmd
}
