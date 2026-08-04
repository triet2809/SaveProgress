package vn.edu.fpt.seal.common.enums;

/**
 * Tên các role chuẩn trong hệ thống — khớp với bảng `roles` seed sẵn trong schema.sql.
 */
public final class RoleName {
    public static final String COORDINATOR = "coordinator";
    public static final String TEAM_LEADER = "team_leader";
    public static final String TEAM_MEMBER = "team_member";
    public static final String MENTOR = "mentor";
    public static final String JUDGE = "judge";

    private RoleName() {
    }
}
