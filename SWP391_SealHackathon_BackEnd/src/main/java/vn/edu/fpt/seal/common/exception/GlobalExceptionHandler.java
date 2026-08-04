package vn.edu.fpt.seal.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Bộ xử lý ngoại lệ tập trung cho toàn bộ REST controller.
 * Chuyển các loại ngoại lệ khác nhau thành phản hồi lỗi JSON thống nhất
 * ({@link ErrorResponse}) với mã HTTP status và mã lỗi phù hợp.
 *
 * <p>Nhờ {@link RestControllerAdvice}, mọi exception ném ra từ controller đều được
 * bắt tại đây thay vì trả về stack trace thô, giúp client xử lý lỗi nhất quán.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý ngoại lệ nghiệp vụ tự định nghĩa, giữ nguyên status/code/message đã khai báo.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), ex.getCode(), ex.getMessage()));
    }

    /**
     * Xử lý vi phạm ràng buộc DB (trùng unique, khóa ngoại...) → trả về 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrityConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, "CONFLICT",
                        "The requested change conflicts with an existing unique or dependent record"));
    }

    /**
     * Xử lý lỗi validation dữ liệu đầu vào (@Valid) → 400 Bad Request.
     * Gom lỗi của từng trường vào map fieldErrors để client hiển thị chi tiết từng ô nhập.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        // Duyệt từng lỗi trường: tên trường → thông điệp lỗi mặc định
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed");
        body.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Xử lý body request không đọc được (JSON sai định dạng) → 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request body"));
    }

    /**
     * Xử lý thiếu tham số bắt buộc trên request → 400 Bad Request, kèm tên tham số thiếu.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing required parameter: " + ex.getParameterName()));
    }

    /**
     * Xử lý HTTP method không được hỗ trợ trên endpoint → 405 Method Not Allowed.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed"));
    }

    /**
     * Xử lý truy cập URL không tồn tại (không có static resource/handler) → 404 Not Found.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found"));
    }

    /**
     * Xử lý lỗi xác thực (sai thông tin đăng nhập, token không hợp lệ) → 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage()));
    }

    /**
     * Xử lý lỗi từ chối quyền truy cập.
     * Phân biệt hai trường hợp: chưa đăng nhập (401) và đã đăng nhập nhưng thiếu quyền (403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        // Method-security (@PreAuthorize) ném AccessDeniedException kể cả với request
        // ẩn danh/chưa xác thực. Những trường hợp đó phải trả 401 (không phải 403)
        // để giữ hợp đồng xác thực nhất quán với các endpoint được bảo vệ bởi filter chain.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean unauthenticated = auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken;
        if (unauthenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied"));
    }

    /**
     * Lưới an toàn cuối cùng: bắt mọi exception chưa được xử lý → 500, kèm ghi log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "An unexpected error occurred"));
    }

    /**
     * Cấu trúc phản hồi lỗi chuẩn trả về cho client dưới dạng JSON.
     */
    public static class ErrorResponse {
        /**
         * Thời điểm phát sinh lỗi.
         */
        public LocalDateTime timestamp;
        /**
         * Mã HTTP status dạng số (ví dụ 404, 500).
         */
        public int status;
        /**
         * Mô tả chuẩn của HTTP status (ví dụ "Not Found").
         */
        public String error;
        /**
         * Mã lỗi nội bộ để client xử lý theo loại.
         */
        public String code;
        /**
         * Thông điệp lỗi cho người dùng.
         */
        public String message;
        /**
         * Chi tiết lỗi theo từng trường (chỉ có khi lỗi validation).
         */
        public Map<String, String> fieldErrors;

        /**
         * Tạo {@link ErrorResponse} với timestamp hiện tại và các thông tin lỗi cơ bản.
         */
        public static ErrorResponse of(HttpStatus status, String code, String message) {
            ErrorResponse r = new ErrorResponse();
            r.timestamp = LocalDateTime.now();
            r.status = status.value();
            r.error = status.getReasonPhrase();
            r.code = code;
            r.message = message;
            return r;
        }

        /**
         * Gán map lỗi chi tiết theo từng trường.
         */
        public void setFieldErrors(Map<String, String> fieldErrors) {
            this.fieldErrors = fieldErrors;
        }
    }
}
