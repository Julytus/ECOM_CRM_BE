package vn.aeoc.base.exception;

import vn.aeoc.base.constant.ErrorCode;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class AppException extends RuntimeException {
    protected int statusCode = INTERNAL_SERVER_ERROR.value();
    protected ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;

    public AppException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AppException(ErrorCode errorCode) {
        super(errorCode.message);
        this.statusCode = errorCode.code;
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.statusCode = errorCode.code;
        this.errorCode = errorCode;
    }

    public AppException(String message) {
        super(message);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
