package repository

import (
	"codecc/preci_server/internal/domain/tool/model"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"encoding/json"
	"fmt"
	"strings"
)

const (
	bucketName = "b_tool_meta"
	versionPre = "v#" // 版本号信息存储格式: v#[工具 Name] <-> [二进制版本],[docker镜像版本]
	metaPre    = "m#" // ToolMeta存储格式: m#[工具 Name] <-> ToolMetaEntity
	binPre     = "b#" // 二进制信息存储格式: b#[工具 Name] <-> BinaryEntity
	dockerPre  = "d#" // Docker镜像信息存储格式: d#[工具 Name] <-> DockerImageEntity
)

type ToolMetaEntity struct {
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Lang        int64  `json:"lang"`
	// 工具版本类型，T-测试版本，G-灰度版本，P-正式发布版本，O-开源扫描
	VersionType string `json:"versionType"`
}

// Encode 将ToolMetaEntity序列化为[]byte
func (e *ToolMetaEntity) Encode() ([]byte, error) {
	return json.Marshal(e)
}

// Decode 将[]byte反序列化为ToolMetaEntity
func (e *ToolMetaEntity) Decode(data []byte) error {
	return json.Unmarshal(data, e)
}

type DockerImageEntity struct {
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
}

// Encode 将ToolVersionEntity序列化为[]byte
func (e *DockerImageEntity) Encode() ([]byte, error) {
	return json.Marshal(e)
}

type BinaryEntity struct {
	WinUrl        string `json:"winUrl"`        // win二进制的下载路径
	LinuxUrl      string `json:"linuxUrl"`      // linux二进制的下载路径
	MacUrl        string `json:"macUrl"`        // mac二进制的下载路径
	WinCommand    string `json:"winCommand"`    // win环境下命令行
	LinuxCommand  string `json:"linuxCommand"`  // linux环境下命令行
	MacCommand    string `json:"macCommand"`    // mac环境下命令行
	BinaryVersion string `json:"binaryVersion"` // 二进制工具版本
}

// Encode 将BinaryEntity序列化为[]byte
func (b *BinaryEntity) Encode() ([]byte, error) {
	return json.Marshal(b)
}

// Decode 将[]byte反序列化为BinaryEntity
func (b *BinaryEntity) Decode(data []byte) error {
	return json.Unmarshal(data, b)
}

func GetToolBinaryInfo(sto storage.Storage, toolName string) (*model.Binary, error) {
	data, err := sto.GetByStrKey(bucketName, binPre+toolName)
	if err != nil {
		return nil, err
	}

	bin := new(BinaryEntity)
	if err = bin.Decode(data); err != nil {
		return nil, err
	}

	return &model.Binary{
		WinUrl:        bin.WinUrl,
		LinuxUrl:      bin.LinuxUrl,
		MacUrl:        bin.MacUrl,
		WinCommand:    bin.WinCommand,
		LinuxCommand:  bin.LinuxCommand,
		MacCommand:    bin.MacCommand,
		BinaryVersion: bin.BinaryVersion,
	}, nil
}

func SaveToolMeta(sto storage.Storage, tool *model.ToolMeta) (bool, error) {
	log := logger.GetLogger()
	toolName := tool.Name

	versionKey := versionPre + toolName
	oldVersion, err := sto.GetByStrKey(bucketName, versionKey)
	binVersion, imageVersion := "", ""
	if err != nil {
		log.Info(fmt.Sprintf("GetByStrKey error: %s", err.Error()))
	} else if oldVersion != nil {
		str := string(oldVersion)
		log.Info(fmt.Sprintf("old version is %s", str))
		versions := strings.Split(str, ",")
		binVersion, imageVersion = versions[0], versions[1]
	}

	updateFlag := false
	if binVersion == "" || tool.ToolVersions.Binary.BinaryVersion != binVersion {
		binVersion = tool.ToolVersions.Binary.BinaryVersion
		err := updateBinary(sto, tool.ToolVersions.Binary, toolName)
		if err != nil {
			log.Error(fmt.Sprintf("updateBinary error: %v", err))
			return true, err
		}
		updateFlag = true
	}

	if imageVersion == "" || tool.ToolVersions.DockerImageVersion != imageVersion {
		imageVersion = tool.ToolVersions.DockerImageVersion
		err := updateDockerImage(sto, tool.ToolVersions, toolName)
		if err != nil {
			log.Error(fmt.Sprintf("updateDockerImage error: %v", err))
			return true, err
		}
		updateFlag = true
	}

	if updateFlag {
		value := fmt.Sprintf("%s,%s", binVersion, imageVersion)
		err := sto.UpdateByStrKey(bucketName, versionKey, []byte(value))
		if err != nil {
			// 如果更新 b_tool_meta 失败，就不再继续更新 b_tool_meta 了
			log.Error(fmt.Sprintf("update b_tool_meta [%s : %s] error: %v", versionKey, value, err))
			return true, err
		}

		if err = updateToolMeta(sto, tool); err != nil {
			return true, err
		}
	}

	return updateFlag, nil
}

func GetToolLang(sto storage.Storage, toolName string) (int64, error) {
	log := logger.GetLogger()

	metaKey := metaPre + toolName
	data, err := sto.GetByStrKey(bucketName, metaKey)
	if err != nil {
		log.Error(fmt.Sprintf("GetByStrKey error: %v", err))
		return 0, err
	}

	meta := new(ToolMetaEntity)
	if err = meta.Decode(data); err != nil {
		log.Error(fmt.Sprintf("Decode ToolMetaEntity error: %v", err))
		return 0, err
	}

	return meta.Lang, nil
}

func updateToolMeta(sto storage.Storage, tool *model.ToolMeta) error {
	log := logger.GetLogger()

	toolName := tool.Name
	metaKey := metaPre + toolName
	meta := ToolMetaEntity{
		Name:        toolName,
		DisplayName: tool.DisplayName,
		Lang:        tool.Lang,
		VersionType: tool.ToolVersions.VersionType,
	}
	metaBytes, err := meta.Encode()
	if err != nil {
		log.Error(fmt.Sprintf("Encode ToolMetaEntity error: %v", err))
		return err
	}

	err = sto.UpdateByStrKey(bucketName, metaKey, metaBytes)
	if err != nil {
		log.Error(fmt.Sprintf("save ToolMetaEntity error: %v", err))
		return err
	}

	return nil
}

func updateDockerImage(sto storage.Storage, toolVersion model.ToolVersion, toolName string) error {
	log := logger.GetLogger()

	dockerEntity := DockerImageEntity{
		DockerTriggerShell:        toolVersion.DockerTriggerShell,
		DockerImageURL:            toolVersion.DockerImageURL,
		DockerImageVersion:        toolVersion.DockerImageVersion,
		ForeignDockerImageVersion: toolVersion.ForeignDockerImageVersion,
		DockerImageHash:           toolVersion.DockerImageHash,
	}
	dockerBytes, err := dockerEntity.Encode()
	if err != nil {
		log.Error(fmt.Sprintf("Encode DockerImageEntity error: %v", err))
		return err
	}

	dockerKey := dockerPre + toolName
	err = sto.UpdateByStrKey(bucketName, dockerKey, dockerBytes)
	if err != nil {
		log.Error(fmt.Sprintf("save DockerImageEntity error: %v", err))
		return err
	}

	return nil
}

func updateBinary(sto storage.Storage, bin model.Binary, toolName string) error {
	log := logger.GetLogger()

	binEntity := BinaryEntity{
		WinUrl:        bin.WinUrl,
		LinuxUrl:      bin.LinuxUrl,
		MacUrl:        bin.MacUrl,
		WinCommand:    bin.WinCommand,
		LinuxCommand:  bin.LinuxCommand,
		MacCommand:    bin.MacCommand,
		BinaryVersion: bin.BinaryVersion,
	}
	binBytes, err := binEntity.Encode()
	if err != nil {
		log.Error(fmt.Sprintf("Encode BinaryEntity error: %v", err))
		return err
	}

	binKey := binPre + toolName
	err = sto.UpdateByStrKey(bucketName, binKey, binBytes)
	if err != nil {
		log.Error(fmt.Sprintf("save BinaryEntity error: %v", err))
		return err
	}

	return nil
}
