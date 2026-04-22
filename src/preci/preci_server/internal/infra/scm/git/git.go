// Package git implements Git-specific helpers used by the SCM layer to
// inspect repositories and collect changed-file information.
package git

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/perror"
	codeccos "codecc/preci_server/internal/thirdparty/codecctoolsdk/os"
	"github.com/go-git/go-git/v6"
	"github.com/go-git/go-git/v6/plumbing"
	"github.com/go-git/go-git/v6/plumbing/object"
	"github.com/go-git/go-git/v6/utils/merkletrie"
)

func isValidFile(path string) bool {
	// 检查文件是否存在
	if _, err := os.Stat(path); err != nil {
		return false
	}

	// 忽略隐藏文件
	if isHidden, err := codeccos.IsHidden(path); err != nil || isHidden {
		return false
	}

	return true
}

// GetUncommittedFiles 获取未提交的文件列表
// rootDir: 项目根目录
// 返回文件绝对路径列表
//
// 注意：使用原生 git diff 命令而非 go-git 的 worktree.Status()
// 原因：Windows 环境下如果用户开启了 core.autocrlf，go-git 会因为换行符差异
// （CRLF vs LF）将文件误判为 Modified 状态，导致收集到很多实际没有变更的文件。
// 原生 git diff 命令会正确处理 core.autocrlf 配置，只返回真正有内容变更的文件。
func GetUncommittedFiles(rootDir string) ([]string, error) {
	log := logger.GetLogger()
	log.Info(fmt.Sprintf("开始获取未提交文件，项目目录: %s", rootDir))

	filesMap := make(map[string]bool)

	// 1. 获取已暂存的变更文件（排除删除的）
	// --cached: 只比较暂存区和 HEAD
	// --name-only: 只输出文件名
	// --diff-filter=ACMR: A=Added, C=Copied, M=Modified, R=Renamed（排除 D=Deleted）
	log.Info("执行命令: git diff --cached --name-only --diff-filter=ACMR")
	stagedCmd := exec.Command("git", "diff", "--cached", "--name-only", "--diff-filter=ACMR")
	stagedCmd.Dir = rootDir

	stagedOutput, err := stagedCmd.Output()
	if err != nil {
		log.Error(fmt.Sprintf("获取暂存区变更文件失败: %v", err))
	} else {
		stagedFiles := parseGitDiffOutput(string(stagedOutput))
		for _, file := range stagedFiles {
			filesMap[file] = true
		}
		log.Info(fmt.Sprintf("暂存区变更文件数: %d", len(stagedFiles)))
	}

	// 2. 获取未暂存的变更文件（排除删除的和未跟踪的）
	// 不带 --cached: 比较工作区和暂存区
	// --diff-filter=ACMR: 同上，只获取新增、复制、修改、重命名的文件
	log.Info("执行命令: git diff --name-only --diff-filter=ACMR")
	unstagedCmd := exec.Command("git", "diff", "--name-only", "--diff-filter=ACMR")
	unstagedCmd.Dir = rootDir

	unstagedOutput, err := unstagedCmd.Output()
	if err != nil {
		log.Error(fmt.Sprintf("获取工作区变更文件失败: %v", err))
	} else {
		unstagedFiles := parseGitDiffOutput(string(unstagedOutput))
		for _, file := range unstagedFiles {
			filesMap[file] = true
		}
		log.Info(fmt.Sprintf("工作区变更文件数: %d", len(unstagedFiles)))
	}

	// 转换为绝对路径列表
	var files []string
	for file := range filesMap {
		absPath := filepath.Join(rootDir, file)
		if isValidFile(absPath) {
			files = append(files, absPath)
		} else {
			log.Info(fmt.Sprintf("跳过无效文件: %s", absPath))
		}
	}

	log.Info(fmt.Sprintf("获取到 %d 个未提交的文件", len(files)))
	return files, nil
}

// parseGitDiffOutput 解析 git diff --name-only 的输出
// 返回文件路径列表（相对于仓库根目录）
func parseGitDiffOutput(output string) []string {
	var files []string
	lines := strings.Split(strings.TrimSpace(output), "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line != "" {
			files = append(files, line)
		}
	}

	return files
}

// GetUnpushedFiles 使用 go-git 库获取未推送的文件列表
// rootDir: 项目根目录
// 返回: 文件绝对路径列表
func GetUnpushedFiles(rootDir string) ([]string, error) {
	log := logger.GetLogger()

	// 打开 Git 仓库
	repo, err := git.PlainOpen(rootDir)
	if err != nil {
		log.Error(fmt.Sprintf("打开 Git 仓库失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	// 获取当前分支的 HEAD 引用
	head, err := repo.Head()
	if err != nil {
		log.Error(fmt.Sprintf("获取 HEAD 引用失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	// 获取当前分支名
	currentBranch := head.Name().Short()

	remoteName := "origin"
	cfg, err := repo.Config()
	if err != nil {
		log.Error(fmt.Sprintf("获取仓库配置失败: %v", err))
	} else {
		branchConfig := cfg.Branches[currentBranch]
		if branchConfig != nil && branchConfig.Remote != "" {
			remoteName = branchConfig.Remote
		}
	}

	// 构造远程分支引用名
	remoteBranchName := fmt.Sprintf("refs/remotes/%s/%s", remoteName, currentBranch)
	remoteBranchRef := plumbing.ReferenceName(remoteBranchName)

	// 获取本地和远程的提交对象
	localCommit, err := repo.CommitObject(head.Hash())
	if err != nil {
		log.Error(fmt.Sprintf("获取本地提交对象失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	// 尝试获取远程分支引用
	remoteRef, err := repo.Reference(remoteBranchRef, true)
	if err != nil {
		log.Warn(fmt.Sprintf("远程分支 %s 不存在，可能是新分支，将获取所有本地提交的文件", remoteBranchName))
		// 如果远程分支不存在，获取所有已提交的文件
		return getAllCommittedFiles(localCommit, rootDir)
	}

	remoteCommit, err := repo.CommitObject(remoteRef.Hash())
	if err != nil {
		log.Error(fmt.Sprintf("获取远程提交对象失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	// 如果本地和远程指向同一个提交，说明没有未推送的内容
	if localCommit.Hash == remoteCommit.Hash {
		log.Info("本地分支与远程分支同步，没有未推送的文件")
		return []string{}, nil
	}

	// 获取本地和远程的树对象
	localTree, err := localCommit.Tree()
	if err != nil {
		log.Error(fmt.Sprintf("获取本地树对象失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	remoteTree, err := remoteCommit.Tree()
	if err != nil {
		log.Error(fmt.Sprintf("获取远程树对象失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	// 比较两个树，找出差异文件
	changes, err := localTree.Diff(remoteTree)
	if err != nil {
		log.Error(fmt.Sprintf("比较树对象失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	// 收集修改的文件（排除已删除的文件）
	// 已删除的文件不存在于文件系统中，无法进行代码扫描
	filesMap := make(map[string]bool)
	for _, change := range changes {
		// 获取变更类型
		action, err := change.Action()
		if err != nil {
			log.Warn(fmt.Sprintf("获取变更类型失败: %v", err))
			continue
		}

		// 排除删除操作：已删除的文件不存在，无法扫描
		if action == merkletrie.Delete {
			continue
		}

		// 获取文件路径（Insert 和 Modify 操作使用 To.Name）
		if change.To.Name != "" {
			filesMap[change.To.Name] = true
		}
	}

	// 转换为绝对路径列表
	var files []string
	for file := range filesMap {
		absPath := filepath.Join(rootDir, file)
		if isValidFile(absPath) {
			files = append(files, absPath)
		}
	}

	log.Info(fmt.Sprintf("获取到 %d 个未推送的文件", len(files)))
	return files, nil
}

// getAllCommittedFiles 获取所有已提交的文件
func getAllCommittedFiles(commit *object.Commit, rootDir string) ([]string, error) {
	log := logger.GetLogger()

	// 获取树对象
	tree, err := commit.Tree()
	if err != nil {
		log.Error(fmt.Sprintf("获取树对象失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	// 遍历树中的所有文件
	var files []string
	err = tree.Files().ForEach(func(file *object.File) error {
		absPath := filepath.Join(rootDir, file.Name)
		if isValidFile(absPath) {
			files = append(files, absPath)
		}

		return nil
	})

	if err != nil {
		log.Error(fmt.Sprintf("遍历树文件失败: %v", err))
		return nil, perror.ErrGitOperation
	}

	log.Info(fmt.Sprintf("获取到 %d 个已提交的文件", len(files)))
	return files, nil
}
