package web

import (
	"codecc/preci_server/internal/infra/logger"
	"fmt"
	"io"
	"net/http"
)

// Decoder request 解码器接口
type Decoder interface {
	Decode(data []byte) error
}

func Decode(req *http.Request, decoder Decoder) error {
	log := logger.GetLogger()

	data, err := io.ReadAll(req.Body)
	if err != nil {
		log.Error(fmt.Sprintf("read data error. error: %s, payload: %s", err.Error(), string(data)))
		return fmt.Errorf("request: unable to read payload: %w", err)
	}

	if err := decoder.Decode(data); err != nil {
		log.Error(fmt.Sprintf("decode data error. error: %s, payload: %s", err.Error(), string(data)))
		return fmt.Errorf("request: decode error: %w", err)
	}

	return nil
}
