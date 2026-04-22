package log

import (
	"fmt"
	"log"
)

// Success 成功日志
func Success(format string, v ...any) {
	log.Println("[+]SUCCESS " + fmt.Sprintf(format, v...))
}

// Fail 失败日志
func Fail(format string, v ...any) {
	log.Println("[X]FAIL " + fmt.Sprintf(format, v...))
}

// Start 开始日志
func Start(format string, v ...any) {
	log.Println("[>]START " + fmt.Sprintf(format, v...))
}

func Info(format string, v ...any) {
	log.Println(fmt.Sprintf(format, v...))
}
