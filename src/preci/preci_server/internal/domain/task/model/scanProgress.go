package model

// ScanProgress 扫描进度
type ScanProgress struct {
	ProjectRoot  string
	ToolStatuses map[string]string
	Status       string
}
