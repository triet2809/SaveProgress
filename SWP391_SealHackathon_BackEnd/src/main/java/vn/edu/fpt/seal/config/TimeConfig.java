package vn.edu.fpt.seal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Cấu hình đồng hồ hệ thống dùng chung cho toàn ứng dụng.
 *
 * <p>Đăng ký một bean {@link Clock} theo múi giờ UTC. Việc inject {@code Clock}
 * thay vì gọi trực tiếp {@code Instant.now()}/{@code LocalDateTime.now()} giúp
 * dễ kiểm thử (có thể thay bằng đồng hồ cố định trong test) và đảm bảo mọi
 * mốc thời gian nghiệp vụ đều tính theo cùng một chuẩn UTC.</p>
 */
@Configuration
public class TimeConfig {
    /**
     * Bean đồng hồ hệ thống theo UTC, dùng cho các tính toán thời gian nghiệp vụ.
     */
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}
