package core

import (
	"flag"
	"fmt"
	"log"

	"codecc/preci_server/internal/thirdparty/codecctoolsdk/io"
)

const (
	EnvStandard    = "standard"     // 标准环境
	EnvNonStandard = "non-standard" // 非标准环境
)

var (
	ErrEnvRequirementNotMet = fmt.Errorf("不满足工具运行环境要求")
)

const (
	ErrReadJsonMessage  = "读取 json 文件失败"
	ErrWriteJsonMessage = "写入 json 文件失败"
)

// Scanner 扫描器
type Scanner interface {
	// InitEnvironment 初始化程序运行环境, 如不需要可直接返回 true, nil
	InitEnvironment(envType string) (bool, error)

	// Scan 核心扫描逻辑（必须实现）
	Scan(input *io.ScanInput) (*io.ScanOutput, error)
}

// DefaultScanner 默认扫描器（提供基础实现）
type DefaultScanner struct{}

// InitEnvironment 默认环境初始化（总是通过）
func (d *DefaultScanner) InitEnvironment(envType string) (bool, error) {
	log.Printf("环境初始化: %s (默认通过)", envType)
	return true, nil
}

// Scan 默认扫描逻辑（必须实现）
func (d *DefaultScanner) Scan(input *io.ScanInput) (*io.ScanOutput, error) {
	return nil, fmt.Errorf("请实现自定义的Scan方法")
}

// RunScan 执行完整的扫描流程
func RunScan(scanner Scanner) error {
	log.Println("开始执行扫描流程...")

	// 1. 解析参数
	var inputPath, outputPath, envType string
	flag.StringVar(&inputPath, "input", "input.json", "输入文件路径")
	flag.StringVar(&outputPath, "output", "output.json", "输出文件路径")
	flag.StringVar(&envType, "env", EnvStandard, "环境类型")
	flag.Parse()

	log.Printf("解析参数: input=%s, output=%s, env=%s", inputPath, outputPath, envType)

	// 2. 环境初始化
	log.Println("执行环境初始化...")
	passed, err := scanner.InitEnvironment(envType)
	if err != nil {
		return err
	}
	if !passed {
		log.Println("环境初始化失败")
		return ErrEnvRequirementNotMet
	}
	log.Println("环境初始化成功")

	// 3. 解析input.json
	log.Println("加载输入参数...")
	input, err := io.LoadScanInput(inputPath)
	if err != nil {
		return fmt.Errorf("%s(%s): %v", ErrReadJsonMessage, inputPath, err)
	}

	// 4. 执行扫描逻辑
	log.Println("开始执行扫描逻辑...")
	output, err := scanner.Scan(input)
	if err != nil {
		return err
	}
	log.Printf("扫描完成，发现 %d 个缺陷", len(output.Defects))

	// 5. 输出output.json
	log.Println("保存扫描结果...")
	err = output.Save(outputPath, nil)
	if err != nil {
		return fmt.Errorf("%s(%s): %v", ErrWriteJsonMessage, outputPath, err)
	}
	log.Printf("扫描结果已保存到: %s", outputPath)

	log.Println("扫描流程执行完成")
	return nil
}
