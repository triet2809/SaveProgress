package vn.edu.fpt.seal.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ nghiệp vụ chuẩn của toàn hệ thống.
 * Mang kèm mã HTTP status và mã lỗi (code) để {@code GlobalExceptionHandler}
 * chuyển thành phản hồi lỗi thống nhất cho client.
 *
 * <p>Dùng các factory method tĩnh ({@link #notFound}, {@link #badRequest}, ...)
 * để tạo ngoại lệ với status/code phù hợp thay vì new trực tiếp.</p>
 */
@Getter
public class ApiException extends RuntimeException {
    /** Mã HTTP status trả về cho client. */
    private final HttpStatus status;
    /** Mã lỗi dạng chuỗi (ví dụ NOT_FOUND, CONFLICT) để frontend xử lý theo loại. */
    private final String code;

    /**
     * @param status  mã HTTP status
     * @param code    mã lỗi định danh
     * @param message thông điệp mô tả lỗi cho người dùng
     */
    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** Tạo lỗi 404 Not Found (tài nguyên không tồn tại). */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    /** Tạo lỗi 400 Bad Request (dữ liệu đầu vào không hợp lệ). */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /** Tạo lỗi 409 Conflict (xung đột trạng thái/dữ liệu). */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    /** Tạo lỗi 401 Unauthorized (chưa xác thực). */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    /** Tạo lỗi 403 Forbidden (đã xác thực nhưng không đủ quyền). */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
