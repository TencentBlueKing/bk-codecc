package perror

import (
	"errors"
	"fmt"
	"testing"
)

func TestPreCIErr_Error(t *testing.T) {
	err := PreCIError(100005, "access token无效")
	expected := "[100005] access token无效"
	if err.Error() != expected {
		t.Errorf("Error() = %v, want %v", err.Error(), expected)
	}
}

func TestPreCIErr_Is(t *testing.T) {
	tests := []struct {
		name   string
		err    error
		target error
		want   bool
	}{
		{
			name:   "相同错误码应该匹配",
			err:    PreCIError(CodeInvalidAccessToken, "access token无效"),
			target: ErrInvalidAccessToken,
			want:   true,
		},
		{
			name:   "不同错误码不应该匹配",
			err:    PreCIError(CodeInvalidRootDir, "无效的根目录"),
			target: ErrInvalidAccessToken,
			want:   false,
		},
		{
			name:   "包装后的错误应该匹配",
			err:    fmt.Errorf("wrapped: %w", ErrInvalidAccessToken),
			target: ErrInvalidAccessToken,
			want:   true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := errors.Is(tt.err, tt.target); got != tt.want {
				t.Errorf("errors.Is() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetErrorCode(t *testing.T) {
	tests := []struct {
		name string
		err  error
		want int
	}{
		{
			name: "PreCIErr 应该返回正确的错误码",
			err:  ErrInvalidAccessToken,
			want: CodeInvalidAccessToken,
		},
		{
			name: "包装的 PreCIErr 应该返回正确的错误码",
			err:  fmt.Errorf("wrapped: %w", ErrInvalidAccessToken),
			want: CodeInvalidAccessToken,
		},
		{
			name: "普通错误应该返回 0",
			err:  errors.New("normal error"),
			want: 0,
		},
		{
			name: "nil 错误应该返回 0",
			err:  nil,
			want: 0,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetErrorCode(tt.err); got != tt.want {
				t.Errorf("GetErrorCode() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsPreCIError(t *testing.T) {
	tests := []struct {
		name string
		err  error
		want bool
	}{
		{
			name: "PreCIErr 应该返回 true",
			err:  ErrInvalidAccessToken,
			want: true,
		},
		{
			name: "包装的 PreCIErr 应该返回 true",
			err:  fmt.Errorf("wrapped: %w", ErrInvalidAccessToken),
			want: true,
		},
		{
			name: "普通错误应该返回 false",
			err:  errors.New("normal error"),
			want: false,
		},
		{
			name: "nil 错误应该返回 false",
			err:  nil,
			want: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsPreCIError(tt.err); got != tt.want {
				t.Errorf("IsPreCIError() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestErrorsAs(t *testing.T) {
	err := fmt.Errorf("wrapped: %w", ErrInvalidAccessToken)

	var pErr *PreCIErr
	if !errors.As(err, &pErr) {
		t.Error("errors.As() should return true for wrapped PreCIErr")
	}

	if pErr.Code != CodeInvalidAccessToken {
		t.Errorf("Code = %v, want %v", pErr.Code, CodeInvalidAccessToken)
	}

	if pErr.Msg != "access token无效" {
		t.Errorf("Msg = %v, want %v", pErr.Msg, "access token无效")
	}
}
