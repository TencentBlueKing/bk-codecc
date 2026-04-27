package tool

import (
	"codecc/preci_server/cmd/client"
	"codecc/preci_server/cmd/mcp/config"
	"codecc/preci_server/internal/domain/task"
	"codecc/preci_server/internal/util/perror"
	"context"
	"errors"
	"fmt"
	"github.com/modelcontextprotocol/go-sdk/mcp"
	"log"
	"time"
)

// ScanRequest MCP 工具 Scan / ScanAndResult 的入参，指定扫描类型、路径和项目根目录
type ScanRequest struct {
	ScanType    int      `json:"scanType" jsonschema:"扫描类型。0，全量扫描；100，指定目录/文件扫描；102，pre-commit 扫描；103，pre-push 扫描"`
	Paths       []string `json:"paths,omitempty" jsonschema:"待扫描的路径列表"`
	ProjectRoot string   `json:"projectRoot" jsonschema:"项目根目录"`
}

// ScanResponse MCP 工具 Scan 的出参，返回启动扫描后的提示信息
type ScanResponse struct {
	Message string `json:"message" jsonschema:"启动扫描成功后的返回信息，包括扫描工具列表、扫描文件数和其他信息"`
}

// Scan MCP 工具：按入参启动一次扫描并返回提示信息，扫描异步执行
func Scan(ctx context.Context, req *mcp.CallToolRequest, input ScanRequest) (*mcp.CallToolResult, ScanResponse, error) {
	log.Printf("start Scan: %s\n", input.ProjectRoot)
	cli, err := client.NewPreCIServerClient(config.Port)
	if err != nil {
		log.Printf("创建客户端失败: %v\n", err)
		return nil, ScanResponse{}, err
	}

	// 校验 input.RootDir 是否存在
	if input.ProjectRoot == "" {
		log.Println("必须提供项目根目录")
		return nil, ScanResponse{}, fmt.Errorf("必须提供项目根目录")
	}

	message, err := cli.StartScan(input.ScanType, input.Paths, input.ProjectRoot)
	if err != nil {
		log.Printf("启动扫描失败: %v\n", err)
		return nil, ScanResponse{}, err
	}

	return nil, ScanResponse{
		Message: message,
	}, nil
}

// ProgressResponse MCP 工具 Progress 的出参，包含当前扫描任务的整体状态以及各工具的运行状态
type ProgressResponse struct {
	ProjectRoot  string            `json:"projectRoot" jsonschema:"项目根目录"`
	ToolStatuses map[string]string `json:"toolStatuses" jsonschema:"各工具的运行状态"`
	Status       string            `json:"status" jsonschema:"本次扫描任务的整体状态"`
}

// Progress MCP 工具：查询当前扫描任务的进度，返回整体状态及各工具的运行状态
func Progress(ctx context.Context, req *mcp.CallToolRequest, _ interface{}) (*mcp.CallToolResult,
	ProgressResponse, error) {
	log.Println("开始获取当前扫描进度")
	cli, err := client.NewPreCIServerClient(config.Port)
	if err != nil {
		log.Printf("创建客户端失败: %v\n", err)
		return nil, ProgressResponse{}, err
	}

	resp, err := cli.GetScanProgress()
	if err != nil || resp == nil {
		log.Printf("获取扫描进度失败: %v\n", err)
		return nil, ProgressResponse{}, err
	}

	return nil, ProgressResponse{
		ProjectRoot:  resp.ProjectRoot,
		ToolStatuses: resp.ToolStatuses,
		Status:       resp.Status,
	}, nil
}

// ResultRequest MCP 工具 Result / ProgressAndResult 的入参，指定查询扫描结果的目录
type ResultRequest struct {
	Path string `json:"path" jsonschema:"查询该目录下的扫描结果"`
}

// Defect 表示一条扫描告警，包含工具名、规则名、告警位置以及严重程度等信息
type Defect struct {
	ToolName    string `json:"toolName" jsonschema:"工具名"`
	CheckerName string `json:"checkerName" jsonschema:"规则名"`
	Description string `json:"description" jsonschema:"告警描述"`
	FilePath    string `json:"filePath" jsonschema:"告警所在文件的路径"`
	Line        int    `json:"line" jsonschema:"告警所在行"`
	Severity    int64  `json:"severity" jsonschema:"告警的严重程度。1，严重；2，一般；other，提示"`
}

// ResultResponse MCP 工具 Result / ScanAndResult 的出参，返回本次扫描结果中的全部告警
type ResultResponse struct {
	Defects []Defect `json:"defects" jsonschema:"扫描结果中的所有告警"`
}

func getResult(cli *client.PreCIServerClient, path string) (ResultResponse, error) {
	log.Printf("start getResult(path: %s)", path)
	results, err := cli.GetScanResult(path)
	if err != nil {
		log.Printf("GetScanResult error: %v", err)
		if errors.Is(err, perror.ErrNoDefects) {
			return ResultResponse{
				Defects: []Defect{},
			}, nil
		}

		return ResultResponse{}, err
	}

	defects := make([]Defect, 0)
	for _, d := range results.Defects {
		defects = append(defects, Defect{
			ToolName:    d.ToolName,
			CheckerName: d.CheckerName,
			Description: d.Description,
			FilePath:    d.FilePath,
			Line:        d.Line,
			Severity:    d.Severity,
		})
	}

	return ResultResponse{
		Defects: defects,
	}, nil
}

// Result MCP 工具：查询已完成扫描在指定路径下的告警列表
func Result(ctx context.Context, req *mcp.CallToolRequest, input ResultRequest) (*mcp.CallToolResult,
	ResultResponse, error) {
	cli, err := client.NewPreCIServerClient(config.Port)
	if err != nil {
		log.Printf("创建客户端失败: %v\n", err)
		return nil, ResultResponse{}, err
	}

	resp, err := getResult(cli, input.Path)
	return nil, resp, err
}

// ProgressAndResultResponse MCP 工具 ProgressAndResult 的出参，同时返回扫描进度和扫描结果以及可能的错误
type ProgressAndResultResponse struct {
	Progress ProgressResponse `json:"progress" jsonschema:"扫描进度"`
	Result   ResultResponse   `json:"result" jsonschema:"扫描结果"`
	Err      error            `json:"err" jsonschema:"错误信息"`
}

// ProgressAndResult MCP 工具：一次性返回当前扫描进度，若扫描已结束则附带对应路径下的告警结果
func ProgressAndResult(ctx context.Context, req *mcp.CallToolRequest, input ResultRequest) (*mcp.CallToolResult,
	ProgressAndResultResponse, error) {
	log.Printf("start ProgressAndResult(path: %s)", input.Path)
	cli, err := client.NewPreCIServerClient(config.Port)
	if err != nil {
		log.Printf("创建客户端失败: %v\n", err)
		return nil, ProgressAndResultResponse{
			Err: err,
		}, err
	}

	progress, err := cli.GetScanProgress()
	if err != nil {
		log.Printf("GetScanProgress error: %v", err)
		return nil, ProgressAndResultResponse{
			Err: err,
		}, err
	}

	resp := ProgressAndResultResponse{
		Progress: ProgressResponse{
			ProjectRoot:  progress.ProjectRoot,
			ToolStatuses: progress.ToolStatuses,
			Status:       progress.Status,
		},
	}
	if progress.Status != task.Done {
		resp.Err = perror.ErrScanRunning
		log.Println("scan is running")
		return nil, resp, nil
	}

	result, err := getResult(cli, input.Path)
	if err != nil {
		resp.Err = err
		log.Printf("GetScanResult error: %v", err)
		return nil, resp, err
	}

	resp.Result = result
	return nil, resp, nil
}

// ScanAndResult MCP 工具：启动扫描并等待完成，然后返回告警结果；轮询超过上限会返回错误
func ScanAndResult(ctx context.Context, req *mcp.CallToolRequest, input ScanRequest) (*mcp.CallToolResult,
	ResultResponse, error) {
	cli, err := client.NewPreCIServerClient(config.Port)
	if err != nil {
		return nil, ResultResponse{}, err
	}

	// 校验 input.RootDir 是否存在
	if input.ProjectRoot == "" {
		log.Println("ProjectRoot is empty")
		return nil, ResultResponse{}, fmt.Errorf("必须提供项目根目录")
	}

	_, err = cli.StartScan(input.ScanType, input.Paths, input.ProjectRoot)
	if err != nil {
		log.Printf("StartScan error: %v", err)
		return nil, ResultResponse{}, err
	}

	ticker := time.NewTicker(2 * time.Second) // 每2秒查询一次进度
	defer ticker.Stop()

	errorTime := 0
	cnt := 0

Loop:
	for {
		if cnt > 300 {
			log.Println("查询进度超时")
			return nil, ResultResponse{}, fmt.Errorf("查询进度超时")
		}

		select {
		case <-ticker.C:
			progress, err := cli.GetScanProgress()
			if err != nil {
				if errors.Is(err, perror.ErrNoScanTask) {
					break Loop
				}

				errorTime++
				if errorTime > 3 {
					log.Printf("获取扫描进度失败: %v", err)
					return nil, ResultResponse{}, fmt.Errorf("获取扫描进度失败: %w", err)
				}

				continue
			}

			// 检查是否完成
			if progress.Status == "done" {
				break Loop
			}
		}

		cnt++
	}

	resp, err := getResult(cli, input.ProjectRoot)
	return nil, resp, err
}
