package main

import (
	appauth "codecc/preci_server/internal/app/auth"
	"codecc/preci_server/internal/app/checker"
	"codecc/preci_server/internal/app/codecc"
	"codecc/preci_server/internal/app/misc"
	"codecc/preci_server/internal/app/task"
	auth "codecc/preci_server/internal/domain/auth"
	"codecc/preci_server/internal/infra/cache"
	"codecc/preci_server/internal/infra/client"
	"codecc/preci_server/internal/infra/config"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/infra/storage/bbolt"
	utilos "codecc/preci_server/internal/util/os"
	"context"
	"fmt"
	"github.com/go-chi/chi/v5"
	"net"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strconv"
	"syscall"
	"time"
)

// shutdownOldServer 关停旧服务
func shutdownOldServer(log *logger.Logger, portPath, pidPath string) {
	log.Info(fmt.Sprintf("starting shutdown old server: %s, %s", portPath, pidPath))
	if !utilos.IsExist(pidPath) {
		log.Info("pid file is not exist")
		return
	}

	// 如果 server.pid 存在
	pidData, err := utilos.ReadFile(pidPath)
	if err != nil {
		log.Error(fmt.Sprintf("read pid file fail: %v", err))
		return
	}

	pid, err := strconv.Atoi(string(pidData))
	if err != nil {
		log.Error(fmt.Sprintf("parse pid file fail: %v", err))
		return
	}

	proc := utilos.GetProcess(pid)
	if proc == nil {
		log.Info(fmt.Sprintf("process %d not found", pid))
		return
	}

	log.Info(fmt.Sprintf("process %d is running", pid))

	killProc := true
	defer func() {
		// kill 进程是最终兜底措施
		if killProc {
			log.Info(fmt.Sprintf("kill process: %d", pid))
			proc.Kill()
		}
	}()

	if !utilos.IsExist(portPath) {
		log.Info("server.port is not exist")
		return
	}

	// 如果 server.port 存在
	portData, err := utilos.ReadFile(portPath)
	if err != nil {
		log.Error(fmt.Sprintf("read port file fail: %v", err))
		return
	}

	port, err := strconv.Atoi(string(portData))
	if err != nil {
		log.Error(fmt.Sprintf("parse port file fail: %v", err))
		return
	}
	log.Info(fmt.Sprintf("port is: %d", port))

	// Phase 1: Send a single shutdown request
	shutdownURL := fmt.Sprintf("http://localhost:%d/shutdown", port)
	shutdownClient := &http.Client{Timeout: 3 * time.Second}
	resp, err := shutdownClient.Get(shutdownURL)
	if err != nil {
		log.Info(fmt.Sprintf("shutdown request failed (server may already be stopping): %v", err))
	} else {
		resp.Body.Close()
		log.Info(fmt.Sprintf("shutdown request sent, status: %s", resp.Status))
	}

	// Phase 2: Wait for the process to exit gracefully (up to 10s, polling every 500ms)
	for i := 0; i < 20; i++ {
		if !utilos.IsProcessAlive(proc) {
			log.Info(fmt.Sprintf("shutdown success: port(%d), pid(%d)", port, pid))
			killProc = false
			return
		}
		time.Sleep(500 * time.Millisecond)
	}

	log.Error(fmt.Sprintf("process %d did not exit after graceful shutdown, will be force-killed", pid))
}

func run(ctx context.Context) error {
	/**********************************************************************
	 * 监听所有终止的系统信号
	 **********************************************************************/
	shutdownChan := make(chan os.Signal, 1)
	signal.Notify(shutdownChan,
		syscall.SIGINT,  // Ctrl+C
		syscall.SIGTERM, // kill 命令默认信号（优雅终止）
		syscall.SIGQUIT, // Ctrl+\
		syscall.SIGHUP,  // 终端挂断
	)

	/**********************************************************************
	 *	配置和日志
	 **********************************************************************/
	// 获取二进制文件所在目录
	exePath, err := os.Executable()
	if err != nil {
		return fmt.Errorf("get install dir fail: %w", err)
	}

	// 获取可执行文件所在目录的上级目录
	installDir := filepath.Dir(exePath)

	// 初始化日志
	log := logger.InitLogger(installDir, "preci-server.log")
	defer log.Sync()
	log.Info(fmt.Sprintf("install dir: %s", installDir))

	cfg, err := config.InitConfig(installDir)
	if err != nil {
		return fmt.Errorf("load config fail: %w", err)
	}
	log.Info(fmt.Sprintf("cfg: %+v", cfg))
	if cfg.Web.CodeCCBaseUrl != "" {
		client.CodeCCBaseUrl = cfg.Web.CodeCCBaseUrl
	}
	if cfg.BkRepo.BaseUrl != "" {
		client.BkRepoBaseUrl = cfg.BkRepo.BaseUrl
	}
	if cfg.BkRepo.Project != "" {
		client.BkRepoProject = cfg.BkRepo.Project
	}
	if cfg.BkRepo.Repo != "" {
		client.BkRepoRepo = cfg.BkRepo.Repo
	}
	if cfg.BkRepo.DownloadFolder != "" {
		client.BkRepoDownloadFolder = cfg.BkRepo.DownloadFolder
	}
	if cfg.BkRepo.UploadSubPath != "" {
		client.BkRepoUploadSubPath = cfg.BkRepo.UploadSubPath
	}
	if cfg.BKAuth.BaseURL != "" {
		client.BKAuthBaseURL = cfg.BKAuth.BaseURL
	}
	if cfg.BKAuth.ClientID != "" {
		client.BKAuthClientID = cfg.BKAuth.ClientID
	}

	/**********************************************************************
	 *	环境清理
	 **********************************************************************/
	// 定义文件路径
	portPath := filepath.Join(installDir, "config", "server.port")
	pidPath := filepath.Join(installDir, "config", "server.pid")

	shutdownOldServer(log, portPath, pidPath)

	// 将进程 PID 提前写入，确保即使后续 DB 初始化卡住，下次启动也能通过 pid 文件找到并 kill 掉卡死的进程
	if err := os.WriteFile(pidPath, []byte(strconv.Itoa(os.Getpid())), 0644); err != nil {
		return fmt.Errorf("write pid file fail: %w", err)
	}

	/**********************************************************************
	 *	DB
	 **********************************************************************/
	dbDataDir := filepath.Join(installDir, "db")
	storage.DB = bbolt.NewBoltDB(dbDataDir)
	err = storage.DB.Init(cfg.Db)
	if err != nil {
		return fmt.Errorf("init db fail: %w", err)
	}
	defer storage.DB.Close()

	// 注册 token 刷新器
	cache.RegisterTokenRefresher(func(refreshToken string) (*cache.RefreshedToken, error) {
		token, err := auth.RefreshAccessToken(refreshToken)
		if err != nil {
			return nil, err
		}
		return &cache.RefreshedToken{
			UserId:       token.UserId,
			AccessToken:  token.AccessToken,
			RefreshToken: token.RefreshToken,
			ExpiredTime:  token.ExpiredTime,
		}, nil
	})

	client.RegisterHTTPTokenRefresher(func() error {
		return cache.ForceRefresh()
	})

	/**********************************************************************
	 *	路由
	 **********************************************************************/
	route := chi.NewRouter()
	route.Get("/shutdown", func(w http.ResponseWriter, r *http.Request) {
		// 先返回响应，避免阻塞
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("shutdown signal received"))

		// 异步发送关闭信号，避免阻塞 HTTP 响应
		go func() {
			shutdownChan <- syscall.SIGTERM
		}()
	})

	appauth.Routes(route, log)
	task.Routes(route, log)
	checker.Routes(route, log)
	codecc.Routes(route, log)
	misc.Routes(route, log)

	/**********************************************************************
	 *	启动服务
	 **********************************************************************/
	// 让操作系统自行分配端口
	listener, err := net.Listen("tcp", "0.0.0.0:0")
	if err != nil {
		return fmt.Errorf("listen fail: %w", err)
	}

	actualAddr := listener.Addr().(*net.TCPAddr)
	actualPort := actualAddr.Port

	server := http.Server{
		Handler:      route,
		ReadTimeout:  cfg.Web.ReadTimeout,
		WriteTimeout: cfg.Web.WriteTimeout,
		IdleTimeout:  cfg.Web.IdleTimeout,
	}

	log.Info(fmt.Sprintf("Starting server on port %d", actualPort))

	// 将端口号写到 config/port 中
	if err := os.WriteFile(portPath, []byte(strconv.Itoa(actualPort)), 0644); err != nil {
		return fmt.Errorf("write port file fail: %w", err)
	}
	defer os.Remove(portPath)

	serverErrorChan := make(chan error, 1)

	// 在 goroutine 中启动服务并监听 error 信息
	go func() {
		serverErrorChan <- server.Serve(listener)
	}()

	/**********************************************************************
	 *	接收 shutdown 信号，优雅关停服务
	 **********************************************************************/
	select {
	case err := <-serverErrorChan:
		log.Error(fmt.Sprintf("Server runtime error: %v", err))
		return err
	case <-shutdownChan:
		log.Info("Shutting down server gracefully...")

		ctx, cancel := context.WithTimeout(ctx, cfg.Web.ShutdownTimeout)
		defer cancel()

		if err := server.Shutdown(ctx); err != nil {
			log.Error(fmt.Sprintf("Server shutdown error: %v", err))
			_ = server.Close()
			return err
		}
		log.Info("Server stopped successfully")
	}

	return nil
}

func main() {
	ctx := context.Background()

	if err := run(ctx); err != nil {
		fmt.Printf("start server fail: %v\n", err)
		os.Exit(1)
	}
}
