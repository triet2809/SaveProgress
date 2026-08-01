package vn.edu.fpt.seal.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.edu.fpt.seal.config.AppProperties;

import java.util.List;

/**
 * Cấu hình bảo mật trung tâm của ứng dụng (Spring Security).
 *
 * <p>Thiết lập xác thực stateless dựa trên JWT, CORS, mã hóa mật khẩu, và khai báo
 * các endpoint công khai / yêu cầu đăng nhập. Bật {@code @EnableMethodSecurity}
 * để dùng {@code @PreAuthorize} trên từng method.</p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Filter tự viết để giải mã và xác thực JWT trên mỗi request. */
    private final JwtAuthenticationFilter jwtFilter;
    /** Thuộc tính cấu hình ứng dụng (dùng để lấy danh sách CORS allowed origins). */
    private final AppProperties appProperties;

    /**
     * Bean mã hóa mật khẩu dùng BCrypt với strength 12.
     *
     * @return {@link PasswordEncoder} để hash và xác minh mật khẩu
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Cung cấp {@link AuthenticationManager} mặc định của Spring Security.
     *
     * @param cfg cấu hình xác thực do Spring quản lý
     * @return authentication manager
     * @throws Exception nếu không lấy được manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /**
     * Cấu hình CORS: cho phép origin theo config, các method thông dụng,
     * mọi header, cho phép gửi credential và cache preflight 1 giờ.
     *
     * @return nguồn cấu hình CORS áp dụng cho mọi đường dẫn
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Cho phép tất cả LAN/dev origin. Production nên ép về danh sách cứng.
        if (appProperties.getCors().getAllowedOrigins().contains("*")) {
            // Origin pattern không kết hợp được với credentials=true theo CORS spec
            // nên khi mở rộng cho LAN, tắt credentials để browser không chặn.
            config.setAllowedOriginPatterns(List.of("*"));
            config.setAllowCredentials(false);
        } else {
            config.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
            config.setAllowCredentials(true);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Xây dựng chuỗi filter bảo mật chính.
     *
     * <p>Thiết lập: bật CORS, tắt CSRF (vì dùng token stateless), phiên làm việc
     * kiểu STATELESS, trả 401 khi chưa xác thực, khai báo các endpoint công khai
     * (đăng ký/đăng nhập, swagger, health, một số GET công khai) và yêu cầu
     * xác thực cho mọi request còn lại. Cuối cùng chèn {@code jwtFilter}
     * trước {@code UsernamePasswordAuthenticationFilter}.</p>
     *
     * @param http đối tượng cấu hình HttpSecurity
     * @return chuỗi filter bảo mật đã xây dựng
     * @throws Exception nếu cấu hình lỗi
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS luôn được phép
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Các endpoint xác thực công khai: đăng ký, đăng nhập, refresh, kích hoạt
                        .requestMatchers(HttpMethod.POST,
                                "/auth/register",
                                "/auth/login",
                                "/auth/google",
                                "/auth/refresh",
                                "/auth/activate/**"
                        ).permitAll()
                        // Tài liệu API và health check mở công khai
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()
                        // Các endpoint GET công khai (dữ liệu không nhạy cảm)
                        .requestMatchers(HttpMethod.GET, "/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/events/*/timeline", "/timeline/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/universities/**", "/campuses/**").permitAll()
                        // Mọi request còn lại bắt buộc phải xác thực
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
