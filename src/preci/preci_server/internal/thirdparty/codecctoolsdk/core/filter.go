package core

import (
	"regexp"
	"strings"
)

func isEmpty(s []string) bool {
	return s == nil || len(s) == 0
}

// PathFilter 路径过滤器。判断 path 是否通过 whitePaths 和 blackPaths 的过滤
func PathFilter(path string, whitePaths, blackPaths []string) bool {
	if isEmpty(whitePaths) && isEmpty(blackPaths) {
		return true
	}

	pathItems := splitPath(path)

	pass := true
	if !isEmpty(whitePaths) {
		pass = false
		for _, wp := range whitePaths {
			if matchPath(path, wp, pathItems) {
				pass = true
				break
			}
		}
	}

	if !pass {
		return false
	}

	if !isEmpty(blackPaths) {
		for _, bp := range blackPaths {
			if matchPath(path, bp, pathItems) {
				pass = false
				break
			}
		}
	}

	return pass
}

// splitPath 将路径分割成路径段
func splitPath(path string) []string {
	return strings.FieldsFunc(path, func(c rune) bool {
		return c == '/' || c == '\\'
	})
}

// matchPath 判断 path 是否匹配 pattern。pattern 可以是正则表达式、目录、具体文件路径
func matchPath(path, pattern string, pathItems []string) bool {
	// 1. 尝试将 pattern 作为正则表达式匹配
	if maybeIsRegex(pattern) {
		re, err := regexp.Compile(pattern)
		if err == nil && re.MatchString(path) {
			return true
		}
	}

	// 2. 尝试作为路径匹配
	patternItems := splitPath(pattern)
	if len(patternItems) > len(pathItems) {
		return false
	}

	for i, patternItem := range patternItems {
		if patternItem != pathItems[i] {
			return false
		}
	}

	return true
}

// maybeIsRegex 判断 pattern 是否包含正则表达式特殊字符
func maybeIsRegex(pattern string) bool {
	// 检查是否包含常见的正则表达式特殊字符
	specialChars := []string{"*", "+", "?", "^", "$"}
	for _, char := range specialChars {
		if strings.Contains(pattern, char) {
			return true
		}
	}

	return false
}
