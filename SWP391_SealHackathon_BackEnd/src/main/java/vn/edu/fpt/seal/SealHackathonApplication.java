package vn.edu.fpt.seal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Lớp khởi động chính của ứng dụng SEAL Hackathon Backend (Spring Boot).
 *
 * <ul>
 *   <li>{@code @SpringBootApplication} – bật auto-configuration, component scan và cấu hình Spring Boot.</li>
 *   <li>{@code @EnableJpaAuditing} – bật cơ chế audit của JPA để tự động gán
 *       createdAt/updatedAt cho các entity kế thừa {@code BaseEntity}.</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
public class SealHackathonApplication {
    /**
     * Điểm vào (entry point) của ứng dụng; khởi chạy Spring application context.
     *
     * @param args tham số dòng lệnh truyền cho Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(SealHackathonApplication.class, args);
    }
}
