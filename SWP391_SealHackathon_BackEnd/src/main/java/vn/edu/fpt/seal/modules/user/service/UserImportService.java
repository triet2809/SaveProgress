package vn.edu.fpt.seal.modules.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.enums.RoleName;
import vn.edu.fpt.seal.common.enums.StudentType;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.university.entity.University;
import vn.edu.fpt.seal.modules.university.repository.UniversityRepository;
import vn.edu.fpt.seal.modules.user.dto.ImportUsersResponse;
import vn.edu.fpt.seal.modules.user.entity.Role;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.RoleRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nhập tài khoản sinh viên trường ngoài hàng loạt từ file Excel (.xlsx/.xls).
 *
 * <p>Dùng cho bối cảnh các trường bạn gửi danh sách thí sinh giao lưu: EC upload
 * một file thay vì nhập tay từng tài khoản. Mỗi dòng tạo một sinh viên external
 * ở trạng thái approved (đã được EC duyệt qua chính hành động import danh sách
 * đối tác), đăng nhập được ngay bằng mật khẩu trong file.</p>
 *
 * <p>Cột yêu cầu (dòng đầu là tiêu đề, thứ tự cố định):</p>
 * <ol>
 *   <li>fullName — họ tên</li>
 *   <li>studentId — mã số sinh viên</li>
 *   <li>email — email đăng nhập</li>
 *   <li>university — tên trường</li>
 *   <li>password — mật khẩu ban đầu</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserImportService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UniversityRepository universityRepo;
    private final PasswordEncoder passwordEncoder;

    private static final DataFormatter FORMATTER = new DataFormatter();

    @Transactional
    public ImportUsersResponse importExternalStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("File is empty");
        }
        String name = file.getOriginalFilename();
        if (name != null && !(name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"))) {
            throw ApiException.badRequest("File must be an Excel spreadsheet (.xlsx or .xls)");
        }

        Role defaultRole = roleRepo.findByName(RoleName.TEAM_MEMBER)
                .orElseThrow(() -> new IllegalStateException("Default role team_member not seeded"));

        List<ImportUsersResponse.RowResult> results = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        int failed = 0;
        int totalRows = 0;

        // Chống trùng email ngay trong cùng một file
        Set<String> seenEmails = new HashSet<>();

        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            int firstRow = sheet.getFirstRowNum();

            for (int r = firstRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                totalRows++;
                int humanRow = r + 1; // 1-based cho người dùng

                String fullName = cell(row, 0);
                String studentId = cell(row, 1);
                String email = cell(row, 2);
                String universityName = cell(row, 3);
                String password = cell(row, 4);

                // Kiểm tra dữ liệu bắt buộc
                if (isBlank(fullName) || isBlank(email) || isBlank(universityName) || isBlank(password)) {
                    failed++;
                    results.add(ImportUsersResponse.RowResult.builder()
                            .row(humanRow).email(email)
                            .status("error")
                            .message("Missing required field (fullName, email, university, password)")
                            .build());
                    continue;
                }

                String normalizedEmail = email.toLowerCase().trim();

                if (password.length() < 8) {
                    failed++;
                    results.add(ImportUsersResponse.RowResult.builder()
                            .row(humanRow).email(normalizedEmail)
                            .status("error")
                            .message("Password must be at least 8 characters")
                            .build());
                    continue;
                }

                // Trùng email trong file
                if (!seenEmails.add(normalizedEmail)) {
                    skipped++;
                    results.add(ImportUsersResponse.RowResult.builder()
                            .row(humanRow).email(normalizedEmail)
                            .status("skipped")
                            .message("Duplicate email within file")
                            .build());
                    continue;
                }

                // Trùng email đã có trong hệ thống
                if (userRepo.existsByEmail(normalizedEmail)) {
                    skipped++;
                    results.add(ImportUsersResponse.RowResult.builder()
                            .row(humanRow).email(normalizedEmail)
                            .status("skipped")
                            .message("Email already registered")
                            .build());
                    continue;
                }

                University university = resolveUniversity(universityName.trim());

                User user = User.builder()
                        .email(normalizedEmail)
                        .passwordHash(passwordEncoder.encode(password))
                        .fullName(fullName.trim())
                        .studentType(StudentType.external)
                        .studentId(studentId == null || studentId.isBlank() ? null : studentId.trim())
                        .university(university)
                        .campus(null)
                        .isGuest(false)
                        .status(AccountStatus.approved)
                        .authProvider("local")
                        .roles(new HashSet<>(List.of(defaultRole)))
                        .build();
                userRepo.save(user);
                created++;
                results.add(ImportUsersResponse.RowResult.builder()
                        .row(humanRow).email(normalizedEmail)
                        .status("created")
                        .message(null)
                        .build());
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Excel import file", e);
            throw ApiException.badRequest("Could not read Excel file: " + e.getMessage());
        }

        log.info("User import finished: total={}, created={}, skipped={}, failed={}",
                totalRows, created, skipped, failed);
        return ImportUsersResponse.builder()
                .totalRows(totalRows)
                .created(created)
                .skipped(skipped)
                .failed(failed)
                .results(results)
                .build();
    }

    /** Tìm trường theo tên (không phân biệt hoa thường); tạo mới nếu chưa có. */
    private University resolveUniversity(String universityName) {
        return universityRepo.findByNameIgnoreCase(universityName)
                .orElseGet(() -> universityRepo.save(University.builder()
                        .name(universityName)
                        .country("Vietnam")
                        .build()));
    }

    private static String cell(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (c == null) {
            return null;
        }
        return FORMATTER.formatCellValue(c).trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean isRowEmpty(Row row) {
        for (int c = 0; c < 5; c++) {
            String v = cell(row, c);
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
