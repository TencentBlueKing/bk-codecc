// Package config loads and exposes the PreCI server configuration.
package config

import (
	"codecc/preci_server/internal/infra/config/model"
	"codecc/preci_server/internal/infra/logger"
	"fmt"
	"github.com/knadh/koanf/parsers/json"
	"github.com/knadh/koanf/providers/file"
	"github.com/knadh/koanf/v2"
	"path/filepath"
)

// InitConfig 初始化配置
func InitConfig(installDir string) (*model.Config, error) {
	log := logger.GetLogger()
	var k = koanf.New(".")

	configPath := filepath.Join(installDir, "config", "initConfig.json")
	log.Info(fmt.Sprintf("load config from %s", configPath))

	if err := k.Load(file.Provider(configPath), json.Parser()); err != nil {
		return nil, fmt.Errorf("failed to load config file %s: %w", configPath, err)
	}

	// 加载认证配置
	authConfigPath := filepath.Join(installDir, "config", "authConfig.json")
	log.Info(fmt.Sprintf("load auth config from %s", authConfigPath))
	if err := k.Load(file.Provider(authConfigPath), json.Parser()); err != nil {
		return nil, fmt.Errorf("failed to load auth config file %s: %w", authConfigPath, err)
	}

	var cfg model.Config
	if err := k.Unmarshal("", &cfg); err != nil {
		return nil, fmt.Errorf("failed to unmarshal config: %w", err)
	}

	// 初始化全局配置
	model.GlobalConf = model.GlobalConfig{
		InstallDir:             installDir,
		MaxIncrementalFileSize: cfg.Task.MaxIncrementalFileSize,
	}

	return &cfg, nil
}
