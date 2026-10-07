package vn.aeoc.base.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.jooq.exception.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import vn.aeoc.base.constant.ErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DEFAULT_VALIDATION_ERROR = "Dữ liệu không hợp lệ";

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : resolveErrorCode(ex.getStatusCode());
        String message = ex.getMessage() != null ? ex.getMessage() : errorCode.message;
        return buildResponse(request, errorCode, message, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String errorMessage = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : DEFAULT_VALIDATION_ERROR;

        return buildResponse(request, ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.message, errorMessage);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccessException(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        return buildResponse(request, ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.message, DEFAULT_VALIDATION_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        return buildResponse(request, ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message, ex.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpServletRequest request, ErrorCode errorCode, String message, String error) {
        return ResponseEntity.status(errorCode.code).body(buildBody(request, errorCode, message, error));
    }

    private ApiErrorResponse buildBody(HttpServletRequest request, ErrorCode errorCode, String message, String error) {
        return ApiErrorResponse.of(resolvePath(request), errorCode, message, error);
    }

    private ErrorCode resolveErrorCode(int statusCode) {
        for (ErrorCode value : ErrorCode.values()) {
            if (value.code == statusCode) {
                return value;
            }
        }
        return ErrorCode.INTERNAL_ERROR;
    }

    private String resolvePath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "unknown";
    }
}
