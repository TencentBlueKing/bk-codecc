package task

import (
	toolmodel "codecc/preci_server/internal/domain/tool/model"
	"codecc/preci_server/internal/util/perror"
	"context"
	"fmt"
	"sync"
	"time"
)

// NowScan 当前正在执行的扫描
var NowScan *ScanProgress = nil

// ToolScanStatus 单个工具的扫描状态
type ToolScanStatus struct {
	ToolName  string    // 工具名称
	Status    string    // 执行状态
	StartTime time.Time // 开始时间
	EndTime   time.Time // 结束时间
	Error     error     // 错误信息
	Cancel    context.CancelFunc
	ctx       context.Context
}

// ScanProgress 扫描进度跟踪器
type ScanProgress struct {
	mu           sync.RWMutex
	ProjectRoot  string                     // 扫描项目根目录
	TotalTools   int                        // 总工具数
	Completed    int                        // 已完成数（包括成功和失败）
	Succeeded    int                        // 成功数
	Failed       int                        // 失败数
	Canceled     int                        // 取消数
	ToolStatuses map[string]*ToolScanStatus // 各工具状态
	StartTime    time.Time                  // 扫描开始时间
}

// NewScanProgress 创建扫描进度跟踪器, 单例模式, 保证只有一个扫描进度跟踪器
func NewScanProgress(projectRoot string, toolInputs []*toolmodel.ToolScanInput) (*ScanProgress, error) {
	if NowScan != nil && !NowScan.IsComplete() {
		return nil, perror.PreCIError(perror.CodeOtherScanRunning,
			fmt.Sprintf("other scan(%s) is running", NowScan.ProjectRoot))
	}

	NowScan = &ScanProgress{
		ProjectRoot:  projectRoot,
		TotalTools:   len(toolInputs),
		ToolStatuses: make(map[string]*ToolScanStatus),
		StartTime:    time.Now(),
	}

	for _, input := range toolInputs {
		NowScan.ToolStatuses[input.ToolName] = &ToolScanStatus{
			ToolName: input.ToolName,
			Status:   Pending,
		}
	}

	return NowScan, nil
}

// 判断一个 status 是否死状态 (即已经无法变成别的状态)
func isDead(status string) bool {
	return status == Done || status == Failed || status == Canceled
}

// SetCancelFunc 设置取消函数
func (p *ScanProgress) SetCancelFunc(toolName string, cancel context.CancelFunc) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if status, ok := p.ToolStatuses[toolName]; ok {
		status.Cancel = cancel
	}
}

// Cancel 取消扫描任务
func (p *ScanProgress) Cancel() {
	for tool := range p.ToolStatuses {
		p.MarkCanceled(tool)
	}
}

// MarkCanceled 标记工具扫描取消
func (p *ScanProgress) MarkCanceled(toolName string) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if status, ok := p.ToolStatuses[toolName]; ok && !isDead(status.Status) && status.Cancel != nil {
		status.Cancel()

		status.Status = Canceled
		status.EndTime = time.Now()

		p.Completed++
		p.Canceled++
	}
}

func (p *ScanProgress) IsDone(toolName string) bool {
	p.mu.Lock()
	defer p.mu.Unlock()

	if status, ok := p.ToolStatuses[toolName]; ok && status.Status == Done {
		return true
	}

	return false
}

// MarkRunning 标记工具开始运行
func (p *ScanProgress) MarkRunning(toolName string) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if status, ok := p.ToolStatuses[toolName]; ok && !isDead(status.Status) {
		status.Status = Running
		status.StartTime = time.Now()
	}
}

// MarkDone 标记工具扫描成功
func (p *ScanProgress) MarkDone(toolName string) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if status, ok := p.ToolStatuses[toolName]; ok && !isDead(status.Status) {
		status.Status = Done
		status.EndTime = time.Now()

		p.Completed++
		p.Succeeded++
	}
}

// MarkFailed 标记工具扫描失败
func (p *ScanProgress) MarkFailed(toolName string, err error) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if status, ok := p.ToolStatuses[toolName]; ok && !isDead(status.Status) {
		status.Status = Failed
		status.EndTime = time.Now()
		status.Error = err

		p.Completed++
		p.Failed++
	}
}

// IsComplete 检查是否全部完成
func (p *ScanProgress) IsComplete() bool {
	p.mu.RLock()
	defer p.mu.RUnlock()
	return p.Completed >= p.TotalTools
}

// HasErrors 检查是否有错误
func (p *ScanProgress) HasErrors() bool {
	p.mu.RLock()
	defer p.mu.RUnlock()
	return p.Failed > 0
}

// GetErrors 获取所有错误
func (p *ScanProgress) GetErrors() map[string]error {
	p.mu.RLock()
	defer p.mu.RUnlock()

	errors := make(map[string]error)
	for name, status := range p.ToolStatuses {
		if status.Error != nil {
			errors[name] = status.Error
		}
	}

	return errors
}
