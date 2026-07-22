package vn.edu.fpt.seal.security;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Đối tượng bất biến (immutable) đại diện cho người dùng đã xác thực trong request hiện tại.
 * Được đặt làm principal trong Spring Security sau khi JWT được giải mã thành công,
 * giúp các service/controller truy xuất nhanh id, email và danh sách role mà không cần truy DB.
 */
@Value
@Builder
public class CurrentUser {
    /** Định danh người dùng (UUID). */
    UUID id;
    /** Email của người dùng. */
    String email;
    /** Danh sách tên role của người dùng (ví dụ coordinator, judge). */
    List<String> roles;
}
