// Package scm provides source-code-management abstractions shared by the
// individual SCM implementations (e.g. Git) used by PreCI.
package scm

import (
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/util/constant"
	utilos "codecc/preci_server/internal/util/os"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
)

type ScmInfo struct {
	ScmType     string
	ProjectRoot string
}

// maybeProjRootPath 是否可能是项目根目录
func maybeProjRootPath(path string) bool {
	codeccDir := filepath.Join(path, constant.CodeCCDir)
	if utilos.IsExist(codeccDir) {
		return true
	}

	gitDir := filepath.Join(path, constant.GitDir)
	if utilos.IsExist(gitDir) {
		return true
	}

	return false
}

func findProjRoot(startPath string) string {
	currentPath := startPath

	for {
		// 获取上级目录
		parentPath := filepath.Dir(currentPath)

		// 如果已经到达文件系统根目录，停止查找
		if parentPath == currentPath {
			break
		}

		if maybeProjRootPath(currentPath) {
			return currentPath
		}

		currentPath = parentPath
	}

	return ""
}

// GetScmInfo 从当前目录直接获取项目 scm 信息
func GetScmInfo(currentPath string) *ScmInfo {
	gitDir := filepath.Join(currentPath, constant.GitDir)
	info, err := os.Stat(gitDir)
	if err == nil && info.IsDir() {
		return &ScmInfo{
			ScmType:     constant.GitScmType,
			ProjectRoot: currentPath,
		}
	}

	return nil
}

// SearchProjRoot 从当前目录向上搜索项目根目录, 并获取 scm 信息
func SearchProjRoot(currentPath string) *ScmInfo {
	scmType := ""

	// TODO: 支持其他 scm
	cmd := exec.Command("git", "status")
	cmd.Dir = currentPath
	if _, err := cmd.Output(); err == nil {
		scmType = constant.GitScmType
	}

	log := logger.GetLogger()
	log.Info(fmt.Sprintf("scm type: %s", scmType))
	projRoot := findProjRoot(currentPath)
	if projRoot == "" {
		return nil
	}

	return &ScmInfo{
		ScmType:     scmType,
		ProjectRoot: projRoot,
	}
}
