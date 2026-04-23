package constant

const (
	FullScan      = 0   // 全项目扫描
	TargetScan    = 100 // 目标扫描
	PreCommitScan = 102 // pre-commit 扫描
	PrePushScan   = 103 // pre-push 扫描
)

const (
	GitScmType = "git"
)

const (
	GitDir    = ".git"
	CodeCCDir = ".codecc"
)

const ErrorLockViolation = "ERROR_LOCK_VIOLATION"
