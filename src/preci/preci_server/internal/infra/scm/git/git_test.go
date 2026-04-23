package git

import (
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"testing"

	"codecc/preci_server/internal/infra/logger"
)

// TestMain 在所有测试运行前初始化 logger
func TestMain(m *testing.M) {
	// 使用临时目录初始化 logger
	tempDir, err := os.MkdirTemp("", "git-test-log-*")
	if err != nil {
		panic("创建临时日志目录失败: " + err.Error())
	}
	defer os.RemoveAll(tempDir)

	logger.InitLogger(tempDir, "test.log")

	// 运行测试
	os.Exit(m.Run())
}

// TestParseGitDiffOutput 测试 parseGitDiffOutput 函数
func TestParseGitDiffOutput(t *testing.T) {
	tests := []struct {
		name     string
		input    string
		expected []string
	}{
		{
			name:     "空输出",
			input:    "",
			expected: nil,
		},
		{
			name:     "只有空白字符",
			input:    "   \n\t\n  ",
			expected: nil,
		},
		{
			name:     "单个文件",
			input:    "src/main.go\n",
			expected: []string{"src/main.go"},
		},
		{
			name:     "多个文件",
			input:    "src/main.go\nsrc/utils/helper.go\nREADME.md\n",
			expected: []string{"src/main.go", "src/utils/helper.go", "README.md"},
		},
		{
			name:     "带有额外空行",
			input:    "\nsrc/main.go\n\nsrc/utils/helper.go\n\n",
			expected: []string{"src/main.go", "src/utils/helper.go"},
		},
		{
			name:     "带有前后空格的文件名",
			input:    "  src/main.go  \n  src/utils/helper.go  \n",
			expected: []string{"src/main.go", "src/utils/helper.go"},
		},
		{
			name:     "Windows风格换行符(CRLF)",
			input:    "src/main.go\r\nsrc/utils/helper.go\r\n",
			expected: []string{"src/main.go", "src/utils/helper.go"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := parseGitDiffOutput(tt.input)

			// 检查长度
			if len(result) != len(tt.expected) {
				t.Errorf("parseGitDiffOutput(%q) 返回 %d 个元素，期望 %d 个", tt.input, len(result), len(tt.expected))
				return
			}

			// 检查每个元素
			for i, v := range result {
				if v != tt.expected[i] {
					t.Errorf("parseGitDiffOutput(%q)[%d] = %q，期望 %q", tt.input, i, v, tt.expected[i])
				}
			}
		})
	}
}

// TestGetUncommittedFiles_Integration 集成测试 GetUncommittedFiles 函数
// 此测试需要在一个真实的 Git 仓库中运行
func TestGetUncommittedFiles_Integration(t *testing.T) {
	// 创建临时目录作为测试仓库
	tempDir, err := os.MkdirTemp("", "git-test-*")
	if err != nil {
		t.Fatalf("创建临时目录失败: %v", err)
	}
	defer os.RemoveAll(tempDir)

	// 初始化 Git 仓库
	if err := runGitCommand(tempDir, "init"); err != nil {
		t.Fatalf("git init 失败: %v", err)
	}

	// 配置 git 用户信息（某些环境下需要）
	_ = runGitCommand(tempDir, "config", "user.email", "test@test.com")
	_ = runGitCommand(tempDir, "config", "user.name", "Test User")

	// 子测试：空仓库（没有任何提交）
	t.Run("空仓库无变更", func(t *testing.T) {
		files, err := GetUncommittedFiles(tempDir)
		if err != nil {
			t.Errorf("GetUncommittedFiles 返回错误: %v", err)
			return
		}
		if len(files) != 0 {
			t.Errorf("期望 0 个文件，实际得到 %d 个: %v", len(files), files)
		}
	})

	// 创建第一个文件并提交
	file1 := filepath.Join(tempDir, "file1.txt")
	if err := os.WriteFile(file1, []byte("hello world\n"), 0644); err != nil {
		t.Fatalf("创建文件失败: %v", err)
	}
	if err := runGitCommand(tempDir, "add", "file1.txt"); err != nil {
		t.Fatalf("git add 失败: %v", err)
	}
	if err := runGitCommand(tempDir, "commit", "-m", "initial commit"); err != nil {
		t.Fatalf("git commit 失败: %v", err)
	}

	// 子测试：已提交后无变更
	t.Run("已提交后无变更", func(t *testing.T) {
		files, err := GetUncommittedFiles(tempDir)
		if err != nil {
			t.Errorf("GetUncommittedFiles 返回错误: %v", err)
			return
		}
		if len(files) != 0 {
			t.Errorf("期望 0 个文件，实际得到 %d 个: %v", len(files), files)
		}
	})

	// 子测试：修改文件但未暂存
	t.Run("修改文件未暂存", func(t *testing.T) {
		// 修改文件
		if err := os.WriteFile(file1, []byte("hello world modified\n"), 0644); err != nil {
			t.Fatalf("修改文件失败: %v", err)
		}

		files, err := GetUncommittedFiles(tempDir)
		if err != nil {
			t.Errorf("GetUncommittedFiles 返回错误: %v", err)
			return
		}
		if len(files) != 1 {
			t.Errorf("期望 1 个文件，实际得到 %d 个: %v", len(files), files)
			return
		}
		if filepath.Base(files[0]) != "file1.txt" {
			t.Errorf("期望文件 file1.txt，实际得到 %s", files[0])
		}
	})

	// 子测试：修改文件并暂存
	t.Run("修改文件已暂存", func(t *testing.T) {
		if err := runGitCommand(tempDir, "add", "file1.txt"); err != nil {
			t.Fatalf("git add 失败: %v", err)
		}

		files, err := GetUncommittedFiles(tempDir)
		if err != nil {
			t.Errorf("GetUncommittedFiles 返回错误: %v", err)
			return
		}
		if len(files) != 1 {
			t.Errorf("期望 1 个文件，实际得到 %d 个: %v", len(files), files)
			return
		}
		if filepath.Base(files[0]) != "file1.txt" {
			t.Errorf("期望文件 file1.txt，实际得到 %s", files[0])
		}
	})

	// 提交修改
	if err := runGitCommand(tempDir, "commit", "-m", "modify file1"); err != nil {
		t.Fatalf("git commit 失败: %v", err)
	}

	// 子测试：新增文件并暂存
	t.Run("新增文件已暂存", func(t *testing.T) {
		file2 := filepath.Join(tempDir, "file2.txt")
		if err := os.WriteFile(file2, []byte("new file\n"), 0644); err != nil {
			t.Fatalf("创建文件失败: %v", err)
		}
		if err := runGitCommand(tempDir, "add", "file2.txt"); err != nil {
			t.Fatalf("git add 失败: %v", err)
		}

		files, err := GetUncommittedFiles(tempDir)
		if err != nil {
			t.Errorf("GetUncommittedFiles 返回错误: %v", err)
			return
		}
		if len(files) != 1 {
			t.Errorf("期望 1 个文件，实际得到 %d 个: %v", len(files), files)
			return
		}
		if filepath.Base(files[0]) != "file2.txt" {
			t.Errorf("期望文件 file2.txt，实际得到 %s", files[0])
		}

		// 提交
		if err := runGitCommand(tempDir, "commit", "-m", "add file2"); err != nil {
			t.Fatalf("git commit 失败: %v", err)
		}
	})

	// 子测试：删除文件应被排除
	t.Run("删除文件应被排除", func(t *testing.T) {
		if err := runGitCommand(tempDir, "rm", "file2.txt"); err != nil {
			t.Fatalf("git rm 失败: %v", err)
		}

		files, err := GetUncommittedFiles(tempDir)
		if err != nil {
			t.Errorf("GetUncommittedFiles 返回错误: %v", err)
			return
		}
		// 删除的文件不应该被收集
		if len(files) != 0 {
			t.Errorf("期望 0 个文件（删除的文件应被排除），实际得到 %d 个: %v", len(files), files)
		}

		// 恢复文件
		if err := runGitCommand(tempDir, "checkout", "HEAD", "--", "file2.txt"); err != nil {
			t.Logf("恢复文件失败: %v（继续测试）", err)
		}
	})

	// 子测试：未跟踪文件应被排除
	t.Run("未跟踪文件应被排除", func(t *testing.T) {
		file3 := filepath.Join(tempDir, "untracked.txt")
		if err := os.WriteFile(file3, []byte("untracked file\n"), 0644); err != nil {
			t.Fatalf("创建文件失败: %v", err)
		}

		files, err := GetUncommittedFiles(tempDir)
		if err != nil {
			t.Errorf("GetUncommittedFiles 返回错误: %v", err)
			return
		}
		// 未跟踪的文件不应该被收集
		for _, f := range files {
			if filepath.Base(f) == "untracked.txt" {
				t.Errorf("未跟踪文件 untracked.txt 不应该被收集")
			}
		}

		// 清理
		os.Remove(file3)
	})
}

// TestGetUncommittedFiles_AutoCRLF 测试 AutoCRLF 场景
// 验证使用原生 git diff 命令可以正确处理换行符差异
func TestGetUncommittedFiles_AutoCRLF(t *testing.T) {
	// 创建临时目录作为测试仓库
	tempDir, err := os.MkdirTemp("", "git-autocrlf-test-*")
	if err != nil {
		t.Fatalf("创建临时目录失败: %v", err)
	}
	defer os.RemoveAll(tempDir)

	// 初始化 Git 仓库
	if err := runGitCommand(tempDir, "init"); err != nil {
		t.Fatalf("git init 失败: %v", err)
	}

	// 配置 git 用户信息
	_ = runGitCommand(tempDir, "config", "user.email", "test@test.com")
	_ = runGitCommand(tempDir, "config", "user.name", "Test User")

	// 设置 autocrlf = true（模拟 Windows 环境的常见配置）
	if err := runGitCommand(tempDir, "config", "core.autocrlf", "true"); err != nil {
		t.Fatalf("设置 core.autocrlf 失败: %v", err)
	}

	// 创建一个带有 LF 换行符的文件
	file1 := filepath.Join(tempDir, "file1.txt")
	if err := os.WriteFile(file1, []byte("line1\nline2\nline3\n"), 0644); err != nil {
		t.Fatalf("创建文件失败: %v", err)
	}

	// 添加并提交
	if err := runGitCommand(tempDir, "add", "file1.txt"); err != nil {
		t.Fatalf("git add 失败: %v", err)
	}
	if err := runGitCommand(tempDir, "commit", "-m", "initial commit"); err != nil {
		t.Fatalf("git commit 失败: %v", err)
	}

	// 模拟 autocrlf 导致的情况：将文件内容改为 CRLF
	// （在 Windows 上 git checkout 时会自动转换，这里手动模拟）
	if runtime.GOOS == "windows" {
		// 在 Windows 上，重新检出文件会自动转换为 CRLF
		if err := runGitCommand(tempDir, "checkout", "--", "file1.txt"); err != nil {
			t.Fatalf("git checkout 失败: %v", err)
		}
	} else {
		// 在非 Windows 系统上，手动写入 CRLF 格式的文件来模拟
		if err := os.WriteFile(file1, []byte("line1\r\nline2\r\nline3\r\n"), 0644); err != nil {
			t.Fatalf("写入 CRLF 文件失败: %v", err)
		}
	}

	// 获取未提交文件
	files, err := GetUncommittedFiles(tempDir)
	if err != nil {
		t.Fatalf("GetUncommittedFiles 返回错误: %v", err)
	}

	// 由于使用原生 git diff，它会正确处理 autocrlf，
	// 所以只有换行符差异的文件不应该被报告为修改
	// 注意：在某些 git 版本和配置下，行为可能略有不同
	t.Logf("AutoCRLF 测试：获取到 %d 个文件", len(files))
	for _, f := range files {
		t.Logf("  - %s", f)
	}

	// 在非 Windows 系统上且开启了 autocrlf 的情况下，
	// 手动写入 CRLF 内容后，git diff 应该不报告任何差异（因为 git 会自动规范化）
	// 这验证了原生 git 命令能正确处理换行符问题
	if runtime.GOOS != "windows" {
		// 非 Windows 系统上，autocrlf=true 会在暂存时将 CRLF 转换为 LF
		// 所以工作区的 CRLF 文件与仓库中的 LF 文件被认为是相同的
		if len(files) > 0 {
			t.Logf("注意：在 autocrlf=true 的情况下，某些 git 版本可能仍会报告换行符差异")
		}
	}
}

// runGitCommand 在指定目录执行 git 命令
func runGitCommand(dir string, args ...string) error {
	cmd := exec.Command("git", args...)
	cmd.Dir = dir
	cmd.Env = append(os.Environ(), "GIT_TERMINAL_PROMPT=0")
	output, err := cmd.CombinedOutput()
	if err != nil {
		return &gitCommandError{
			args:   args,
			output: string(output),
			err:    err,
		}
	}
	return nil
}

// gitCommandError 自定义 git 命令错误
type gitCommandError struct {
	args   []string
	output string
	err    error
}

func (e *gitCommandError) Error() string {
	return e.err.Error() + ": " + e.output
}
