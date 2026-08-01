package vn.edu.fpt.seal.modules.user.dto;

import lombok.Builder;

import java.util.List;

/**
 * Kết quả import tài khoản hàng loạt từ file Excel.
 * Tổng hợp số dòng tạo thành công / bỏ qua / lỗi, kèm chi tiết từng dòng để EC rà soát.
 *
 * @param totalRows tổng số dòng dữ liệu đã đọc (không tính dòng tiêu đề)
 * @param created   số tài khoản tạo mới thành công
 * @param skipped   số dòng bỏ qua (ví dụ email đã tồn tại)
 * @param failed    số dòng lỗi (thiếu dữ liệu bắt buộc, sai định dạng...)
 * @param results   chi tiết kết quả từng dòng
 */
@Builder
public record ImportUsersResponse(
        int totalRows,
        int created,
        int skipped,
        int failed,
        List<RowResult> results
) {
    /**
     * Kết quả xử lý một dòng trong file.
     *
     * @param row     số thứ tự dòng trong file (1-based, tính cả dòng tiêu đề)
     * @param email   email ở dòng đó (nếu đọc được)
     * @param status  trạng thái xử lý: "created" | "skipped" | "error"
     * @param message mô tả thêm (lý do bỏ qua / lỗi)
     */
    @Builder
    public record RowResult(
            int row,
            String email,
            String status,
            String message
    ) {}
}
