package vn.aeoc.base.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import vn.aeoc.base.constant.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String path,
        int code,
        String errorCode,
        String message,
        String error
) {
    public static ApiErrorResponse of(String path, ErrorCode errorCode) {
        return new ApiErrorResponse(path, errorCode.code, errorCode.name(), errorCode.message, null);
    }

    public static ApiErrorResponse of(String path, ErrorCode errorCode, String error) {
        return new ApiErrorResponse(path, errorCode.code, errorCode.name(), errorCode.message, error);
    }

    public static ApiErrorResponse of(String path, ErrorCode errorCode, String message, String error) {
        return new ApiErrorResponse(path, errorCode.code, errorCode.name(), message, error);
    }
}
