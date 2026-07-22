package vn.edu.fpt.seal.modules.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.modules.event.entity.Event;

import java.util.UUID;

/**
 * Repository truy xuất dữ liệu sự kiện {@link Event}.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Lấy danh sách sự kiện theo trạng thái, có phân trang.
     *
     * @param status   trạng thái cần lọc
     * @param pageable thông tin phân trang
     * @return trang kết quả sự kiện
     */
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    /**
     * Kiểm tra tồn tại sự kiện trùng tiêu đề (không phân biệt hoa/thường).
     *
     * @param title tiêu đề cần kiểm tra
     * @return true nếu đã tồn tại
     */
    boolean existsByTitleIgnoreCase(String title);
}
