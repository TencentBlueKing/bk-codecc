package perror

var (
	ErrInvalidRootDir    = PreCIError(CodeInvalidRootDir, "项目根目录不合法")
	ErrNoScanTask        = PreCIError(CodeNoScanTask, "当前没有扫描任务")
	ErrInvalidPaths      = PreCIError(CodeInvalidPaths, "不合法的路径")
	ErrInvalidProjectId  = PreCIError(CodeInvalidProjectId, "蓝盾项目 id 无效或缺失, 请用 preci project set 指定")
	ErrNoDefects         = PreCIError(CodeNoDefects, "当前没有缺陷")
	ErrInvalidCheckerSet = PreCIError(CodeInvalidCheckerSet, `扫描规则集无效或缺失。
	1. 用 preci checkerset list 查看可用规则集；
	2. 用 preci checkerset select <规则集id> 指定规则集；
	3. 用 preci init 重新初始化。
然后再尝试重新扫描。`)
	ErrSCCScan     = PreCIError(CodeSCCScanErr, "SCC 扫描失败")
	ErrScanRunning = PreCIError(CodeScanRunning, "扫描正在运行中")
)
