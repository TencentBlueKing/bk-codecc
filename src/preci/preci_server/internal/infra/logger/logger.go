// Package logger wraps zap to provide the PreCI server's structured logger
// with log-file rotation support.
package logger

import (
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"gopkg.in/natefinch/lumberjack.v2"
	"os"
	"path"
)

type Logger struct {
	*zap.Logger
	atomicLevel zap.AtomicLevel
}

var globalLogger *Logger

func InitLogger(installDir, fileName string) *Logger {
	// 创建log目录
	logDir := path.Join(installDir, "log")
	if err := os.MkdirAll(logDir, 0755); err != nil {
		panic("Failed to create log directory: " + err.Error())
	}

	// lumberjack 配置
	ljack := &lumberjack.Logger{
		Filename:   path.Join(logDir, fileName), // 日志文件路径
		MaxSize:    5,                           // 单个文件最大尺寸(MB)
		MaxBackups: 3,                           // 最多保留的备份文件数
		MaxAge:     7,                           // 最多保留的天数
		Compress:   true,                        // 是否压缩备份文件
	}

	// 配置编码器
	encoderConfig := zapcore.EncoderConfig{
		TimeKey:        "time",
		LevelKey:       "level",
		NameKey:        zapcore.OmitKey,
		CallerKey:      "caller",
		FunctionKey:    zapcore.OmitKey,
		MessageKey:     "msg",
		StacktraceKey:  "stacktrace",
		LineEnding:     zapcore.DefaultLineEnding,
		EncodeLevel:    zapcore.LowercaseLevelEncoder,
		EncodeTime:     zapcore.ISO8601TimeEncoder,
		EncodeDuration: zapcore.StringDurationEncoder,
		EncodeCaller:   zapcore.ShortCallerEncoder,
	}

	// 创建可动态修改的日志级别
	atomicLevel := zap.NewAtomicLevelAt(zap.InfoLevel)

	// 创建核心
	fileCore := zapcore.NewCore(
		zapcore.NewJSONEncoder(encoderConfig),
		zapcore.AddSync(ljack),
		atomicLevel, // 使用 AtomicLevel，支持动态修改
	)

	// TODO: 上线前删除 console 日志
	consoleCore := zapcore.NewCore(
		zapcore.NewConsoleEncoder(encoderConfig),
		zapcore.AddSync(os.Stdout),
		atomicLevel, // 使用同一个 AtomicLevel，保持一致
	)

	// 合并核心
	core := zapcore.NewTee(fileCore, consoleCore)

	// 创建logger
	log := zap.New(core, zap.AddCaller(), zap.AddStacktrace(zap.ErrorLevel))

	log.Info("Logger initialized successfully")

	globalLogger = &Logger{
		Logger:      log,
		atomicLevel: atomicLevel,
	}

	return globalLogger
}

func GetLogger() *Logger {
	return globalLogger
}

// SetLevel 动态修改日志级别，无需重启服务
// 支持的级别: "debug", "info", "warn", "error", "dpanic", "panic", "fatal"
func (l *Logger) SetLevel(level string) error {
	var zapLevel zapcore.Level
	if err := zapLevel.UnmarshalText([]byte(level)); err != nil {
		return err
	}
	l.atomicLevel.SetLevel(zapLevel)
	l.Info("日志级别已修改", zap.String("new_level", level))
	return nil
}

// GetLevel 获取当前日志级别
func (l *Logger) GetLevel() string {
	return l.atomicLevel.Level().String()
}

// SetDebugLevel 快捷方法：启用 Debug 模式
func (l *Logger) SetDebugLevel() {
	l.atomicLevel.SetLevel(zap.DebugLevel)
	l.Info("日志模块已启用 Debug 模式")
}

// SetInfoLevel 快捷方法：恢复 Info 模式
func (l *Logger) SetInfoLevel() {
	l.atomicLevel.SetLevel(zap.InfoLevel)
	l.Info("日志模块已恢复 Info 模式")
}
