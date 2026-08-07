-- ============================================================
-- SEED: Ranking & Appeal Flow Demo
-- Database: seal_hackathon (PostgreSQL)
-- ============================================================
-- Scenario được dựng sẵn:
--   • 1 event "SEAL Ranking & Appeal Demo 2026" đang diễn ra
--   • 1 track "AI Innovation", 1 Qualification round
--   • 3 đội: Alpha (#1 83.80đ), Gamma (#2 79.25đ), Beta (#3 78.60đ)
--   • Criteria: Innovation 40% | Technical 35% | Presentation 25%
--   • Judge đã chấm điểm, Coordinator đã Recalculate + Publish
--   • Beta (đứng cuối) đã nộp Appeal → round ở PAUSED_FOR_APPEAL
--
-- FLOW DEMO (phần sống):
--   1. Đăng nhập demo.ec@seal.local → vào Appeals
--   2. Xem appeal của Beta → phản hồi → resolve
--      - ACCEPT + recalculation_required → recalculate → republish → advance
--      - REJECT → round tự unblock → advance ngay
--
-- Tài khoản đăng nhập (mật khẩu giống demo.ec@seal.local):
--   Coordinator : demo.ec@seal.local
--   Judge       : judge.appeal.demo@seal.local
--   Leader Alpha: alpha.leader@seal.local
--   Leader Beta : beta.leader@seal.local  ← người đã nộp appeal
--   Leader Gamma: gamma.leader@seal.local
-- ============================================================

BEGIN;

-- ──────────────────────────────────────────────────────────────
-- 1. EVENT
-- ──────────────────────────────────────────────────────────────
INSERT INTO events (id, title, description, status, term, prize_pool,
                    registration_start, registration_end,
                    event_start, event_end,
                    created_at, updated_at)
VALUES (
    'b0e00000-0000-4000-8000-000000000001',
    'SEAL Ranking & Appeal Demo 2026',
    'Event demo luồng ranking, publish kết quả và appeal giải quyết khiếu nại.',
    'ongoing',
    'Summer 2026',
    '50,000,000 VNĐ',
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '15 days',
    NOW() - INTERVAL '10 days',
    NOW() + INTERVAL '30 days',
    NOW(), NOW()
);

-- ──────────────────────────────────────────────────────────────
-- 2. TRACK
-- ──────────────────────────────────────────────────────────────
INSERT INTO tracks (id, event_id, name, description, max_teams, created_at, updated_at)
VALUES (
    'b0e00000-0000-4000-8000-000000000002',
    'b0e00000-0000-4000-8000-000000000001',
    'AI Innovation',
    'Ứng dụng trí tuệ nhân tạo giải quyết vấn đề thực tế.',
    10,
    NOW(), NOW()
);

-- ──────────────────────────────────────────────────────────────
-- 3. ROUND DEFINITION (logical round, không phải final)
-- State = PAUSED_FOR_APPEAL vì Beta đang có appeal PENDING
-- ──────────────────────────────────────────────────────────────
INSERT INTO round_definitions (id, event_id, name, sequence_number, is_final,
                                default_top_n_to_promote, lifecycle_state,
                                created_at, updated_at)
VALUES (
    'b0e00000-0000-4000-8000-000000000003',
    'b0e00000-0000-4000-8000-000000000001',
    'Qualification',
    1,
    false,
    1,
    'PAUSED_FOR_APPEAL',
    NOW(), NOW()
);

-- ──────────────────────────────────────────────────────────────
-- 4. PHYSICAL ROUND (execution round)
--    submission_deadline: đã qua (đội đã nộp xong)
--    result_published_at: hôm qua (coordinator đã publish)
--    appeal_deadline    : 2 ngày nữa (cửa sổ appeal vẫn mở)
--    lifecycle_state    : PAUSED_FOR_APPEAL (Beta đang appeal)
-- ──────────────────────────────────────────────────────────────
INSERT INTO rounds (id, logical_round_id, track_id, name, sequence_number,
                    submission_deadline, top_n_to_promote,
                    result_published_at, appeal_deadline,
                    lifecycle_state, lifecycle_version,
                    created_at, updated_at)
VALUES (
    'b0e00000-0000-4000-8000-000000000004',
    'b0e00000-0000-4000-8000-000000000003',
    'b0e00000-0000-4000-8000-000000000002',
    'Qualification',
    1,
    NOW() - INTERVAL '2 days',
    1,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '2 days',
    'PAUSED_FOR_APPEAL',
    0,
    NOW(), NOW()
);

-- ──────────────────────────────────────────────────────────────
-- 5. USERS (judge + 3 team leaders)
--    Password: cùng mật khẩu với demo.ec@seal.local
-- ──────────────────────────────────────────────────────────────
INSERT INTO users (id, email, password_hash, auth_provider, full_name,
                   student_type, status, is_guest, security_version,
                   must_change_password, terms_accepted_at, terms_version, privacy_version,
                   created_at, updated_at)
VALUES
-- Judge
('b0e00000-0000-4000-8000-000000000010',
 'judge.appeal.demo@seal.local',
 '$2b$10$weaFNOxVoxJVfBO8lSLG7.9Y57gyBIg0vnj6x0gI7RhhaU67xqZ1K',
 'local', 'Judge Demo', 'none', 'approved',
 false, 1, false, NOW(), '2026-01', '2026-01', NOW(), NOW()),

-- Team Alpha leader
('b0e00000-0000-4000-8000-000000000030',
 'alpha.leader@seal.local',
 '$2b$10$weaFNOxVoxJVfBO8lSLG7.9Y57gyBIg0vnj6x0gI7RhhaU67xqZ1K',
 'local', 'Alpha Leader', 'fpt', 'approved',
 false, 1, false, NOW(), '2026-01', '2026-01', NOW(), NOW()),

-- Team Beta leader (người nộp appeal)
('b0e00000-0000-4000-8000-000000000031',
 'beta.leader@seal.local',
 '$2b$10$weaFNOxVoxJVfBO8lSLG7.9Y57gyBIg0vnj6x0gI7RhhaU67xqZ1K',
 'local', 'Beta Leader', 'fpt', 'approved',
 false, 1, false, NOW(), '2026-01', '2026-01', NOW(), NOW()),

-- Team Gamma leader
('b0e00000-0000-4000-8000-000000000032',
 'gamma.leader@seal.local',
 '$2b$10$weaFNOxVoxJVfBO8lSLG7.9Y57gyBIg0vnj6x0gI7RhhaU67xqZ1K',
 'local', 'Gamma Leader', 'fpt', 'approved',
 false, 1, false, NOW(), '2026-01', '2026-01', NOW(), NOW());

-- ──────────────────────────────────────────────────────────────
-- 6. USER ROLES
--    Role IDs lấy từ bảng roles hiện có trong DB:
--      judge       : ed0ca372-ef82-4f26-bf88-474156fd7d67
--      team_leader : 6f4f98b4-aff1-42df-801b-11f61e93db48
-- ──────────────────────────────────────────────────────────────
INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES
('b0e00000-0000-4000-8000-000000000010', 'ed0ca372-ef82-4f26-bf88-474156fd7d67', NOW()),
('b0e00000-0000-4000-8000-000000000030', '6f4f98b4-aff1-42df-801b-11f61e93db48', NOW()),
('b0e00000-0000-4000-8000-000000000031', '6f4f98b4-aff1-42df-801b-11f61e93db48', NOW()),
('b0e00000-0000-4000-8000-000000000032', '6f4f98b4-aff1-42df-801b-11f61e93db48', NOW());

-- ──────────────────────────────────────────────────────────────
-- 7. TEAM PROFILES
-- ──────────────────────────────────────────────────────────────
INSERT INTO team_profiles (id, canonical_name, created_by, status, row_version,
                           created_at, updated_at)
VALUES
('b0e00000-0000-4000-8000-000000000020', 'Team Alpha',
 'b0e00000-0000-4000-8000-000000000030', 'active', 0, NOW(), NOW()),
('b0e00000-0000-4000-8000-000000000021', 'Team Beta',
 'b0e00000-0000-4000-8000-000000000031', 'active', 0, NOW(), NOW()),
('b0e00000-0000-4000-8000-000000000022', 'Team Gamma',
 'b0e00000-0000-4000-8000-000000000032', 'active', 0, NOW(), NOW());

-- ──────────────────────────────────────────────────────────────
-- 8. TEAMS
-- ──────────────────────────────────────────────────────────────
INSERT INTO teams (id, team_profile_id, track_id, name, status, invite_code,
                   created_at, updated_at)
VALUES
('b0e00000-0000-4000-8000-000000000040',
 'b0e00000-0000-4000-8000-000000000020',
 'b0e00000-0000-4000-8000-000000000002',
 'Team Alpha', 'active', 'ALPHA1', NOW(), NOW()),
('b0e00000-0000-4000-8000-000000000041',
 'b0e00000-0000-4000-8000-000000000021',
 'b0e00000-0000-4000-8000-000000000002',
 'Team Beta',  'active', 'BETA11', NOW(), NOW()),
('b0e00000-0000-4000-8000-000000000042',
 'b0e00000-0000-4000-8000-000000000022',
 'b0e00000-0000-4000-8000-000000000002',
 'Team Gamma', 'active', 'GAMMA1', NOW(), NOW());

-- ──────────────────────────────────────────────────────────────
-- 9. TEAM MEMBERS (mỗi đội 1 leader)
-- ──────────────────────────────────────────────────────────────
INSERT INTO team_members (id, team_id, user_id, role, joined_at)
VALUES
('b0e00000-0000-4000-8000-000000000120',
 'b0e00000-0000-4000-8000-000000000040',
 'b0e00000-0000-4000-8000-000000000030', 'leader', NOW()),
('b0e00000-0000-4000-8000-000000000121',
 'b0e00000-0000-4000-8000-000000000041',
 'b0e00000-0000-4000-8000-000000000031', 'leader', NOW()),
('b0e00000-0000-4000-8000-000000000122',
 'b0e00000-0000-4000-8000-000000000042',
 'b0e00000-0000-4000-8000-000000000032', 'leader', NOW());

-- ──────────────────────────────────────────────────────────────
-- 10. ROUND CRITERIA (tổng weight = 100)
--     Innovation 40% | Technical Excellence 35% | Presentation 25%
-- ──────────────────────────────────────────────────────────────
INSERT INTO round_criteria (id, round_id, name, weight, description, status,
                            created_at, updated_at)
VALUES
('b0e00000-0000-4000-8000-000000000050',
 'b0e00000-0000-4000-8000-000000000004',
 'Innovation', 40.00,
 'Tính sáng tạo và độ mới lạ của giải pháp.', 'active', NOW(), NOW()),
('b0e00000-0000-4000-8000-000000000051',
 'b0e00000-0000-4000-8000-000000000004',
 'Technical Excellence', 35.00,
 'Chất lượng kỹ thuật, kiến trúc hệ thống và code.', 'active', NOW(), NOW()),
('b0e00000-0000-4000-8000-000000000052',
 'b0e00000-0000-4000-8000-000000000004',
 'Presentation', 25.00,
 'Khả năng thuyết trình và demo sản phẩm.', 'active', NOW(), NOW());

-- ──────────────────────────────────────────────────────────────
-- 11. ROUND JUDGE ASSIGNMENT
-- ──────────────────────────────────────────────────────────────
INSERT INTO round_judges (id, round_id, user_id, assigned_at)
VALUES (
    'b0e00000-0000-4000-8000-000000000110',
    'b0e00000-0000-4000-8000-000000000004',
    'b0e00000-0000-4000-8000-000000000010',
    NOW()
);

-- ──────────────────────────────────────────────────────────────
-- 12. SUBMISSIONS (status = submitted, đã nộp trước deadline)
-- ──────────────────────────────────────────────────────────────
INSERT INTO submissions (id, round_id, team_id,
                         repo_url, demo_url, slide_url,
                         status, review_status,
                         submitted_at, updated_at)
VALUES
('b0e00000-0000-4000-8000-000000000060',
 'b0e00000-0000-4000-8000-000000000004',
 'b0e00000-0000-4000-8000-000000000040',
 'https://github.com/alpha/seal2026', 'https://alpha-demo.app', 'https://slides.com/alpha',
 'submitted', 'pending',
 NOW() - INTERVAL '3 days', NOW()),

('b0e00000-0000-4000-8000-000000000061',
 'b0e00000-0000-4000-8000-000000000004',
 'b0e00000-0000-4000-8000-000000000041',
 'https://github.com/beta/seal2026', 'https://beta-demo.app', 'https://slides.com/beta',
 'submitted', 'pending',
 NOW() - INTERVAL '3 days' + INTERVAL '2 hours', NOW()),

('b0e00000-0000-4000-8000-000000000062',
 'b0e00000-0000-4000-8000-000000000004',
 'b0e00000-0000-4000-8000-000000000042',
 'https://github.com/gamma/seal2026', 'https://gamma-demo.app', 'https://slides.com/gamma',
 'submitted', 'pending',
 NOW() - INTERVAL '3 days' + INTERVAL '1 hour', NOW());

-- ──────────────────────────────────────────────────────────────
-- 13. SCORES (9 scores = 3 đội × 3 tiêu chí, 1 judge)
--
--   Đội       Innovation(40%) Technical(35%) Presentation(25%)  TOTAL
--   Alpha     85 → 34.00      78 → 27.30     90 → 22.50         83.80  ← #1 PROMOTED
--   Gamma     90 → 36.00      65 → 22.75     82 → 20.50         79.25  ← #2 eliminated
--   Beta      72 → 28.80      88 → 30.80     76 → 19.00         78.60  ← #3 eliminated (appealing)
-- ──────────────────────────────────────────────────────────────
INSERT INTO scores (id, submission_id, judge_id, criterion_id,
                    score, weighted_score, created_at, updated_at)
VALUES
-- ── Alpha
('b0e00000-0000-4000-8000-000000000070',
 'b0e00000-0000-4000-8000-000000000060',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000050',
 85.00, 34.00, NOW(), NOW()),   -- Innovation 85 × 40/100 = 34.00

('b0e00000-0000-4000-8000-000000000071',
 'b0e00000-0000-4000-8000-000000000060',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000051',
 78.00, 27.30, NOW(), NOW()),   -- Technical 78 × 35/100 = 27.30

('b0e00000-0000-4000-8000-000000000072',
 'b0e00000-0000-4000-8000-000000000060',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000052',
 90.00, 22.50, NOW(), NOW()),   -- Presentation 90 × 25/100 = 22.50

-- ── Beta
('b0e00000-0000-4000-8000-000000000073',
 'b0e00000-0000-4000-8000-000000000061',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000050',
 72.00, 28.80, NOW(), NOW()),   -- Innovation 72 × 40/100 = 28.80

('b0e00000-0000-4000-8000-000000000074',
 'b0e00000-0000-4000-8000-000000000061',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000051',
 88.00, 30.80, NOW(), NOW()),   -- Technical 88 × 35/100 = 30.80

('b0e00000-0000-4000-8000-000000000075',
 'b0e00000-0000-4000-8000-000000000061',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000052',
 76.00, 19.00, NOW(), NOW()),   -- Presentation 76 × 25/100 = 19.00

-- ── Gamma
('b0e00000-0000-4000-8000-000000000076',
 'b0e00000-0000-4000-8000-000000000062',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000050',
 90.00, 36.00, NOW(), NOW()),   -- Innovation 90 × 40/100 = 36.00

('b0e00000-0000-4000-8000-000000000077',
 'b0e00000-0000-4000-8000-000000000062',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000051',
 65.00, 22.75, NOW(), NOW()),   -- Technical 65 × 35/100 = 22.75

('b0e00000-0000-4000-8000-000000000078',
 'b0e00000-0000-4000-8000-000000000062',
 'b0e00000-0000-4000-8000-000000000010',
 'b0e00000-0000-4000-8000-000000000052',
 82.00, 20.50, NOW(), NOW());   -- Presentation 82 × 25/100 = 20.50

-- ──────────────────────────────────────────────────────────────
-- 14. ROUND RANKINGS (đã recalculate, topN=1 → Alpha promoted)
-- ──────────────────────────────────────────────────────────────
INSERT INTO round_rankings (id, round_id, team_id,
                            total_score, rank, status,
                            tie_breaker_reason,
                            calculated_at, updated_at)
VALUES
('b0e00000-0000-4000-8000-000000000090',
 'b0e00000-0000-4000-8000-000000000004',
 'b0e00000-0000-4000-8000-000000000040',
 83.80, 1, 'promoted',
 'Ranked by total weighted score; team name used for deterministic ordering on ties',
 NOW() - INTERVAL '1 day', NOW()),

('b0e00000-0000-4000-8000-000000000091',
 'b0e00000-0000-4000-8000-000000000004',
 'b0e00000-0000-4000-8000-000000000042',
 79.25, 2, 'eliminated',
 'Ranked by total weighted score; team name used for deterministic ordering on ties',
 NOW() - INTERVAL '1 day', NOW()),

('b0e00000-0000-4000-8000-000000000092',
 'b0e00000-0000-4000-8000-000000000004',
 'b0e00000-0000-4000-8000-000000000041',
 78.60, 3, 'eliminated',
 'Ranked by total weighted score; team name used for deterministic ordering on ties',
 NOW() - INTERVAL '1 day', NOW());

-- ──────────────────────────────────────────────────────────────
-- 15. RESULT VERSION (snapshot bất biến khi coordinator publish)
-- ──────────────────────────────────────────────────────────────
INSERT INTO round_result_versions (id, round_id, version_number, status,
                                   published_at, appeal_deadline,
                                   published_by, reason, created_at)
VALUES (
    'b0e00000-0000-4000-8000-000000000080',
    'b0e00000-0000-4000-8000-000000000004',
    1,
    'published',
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '2 days',
    'a1000000-0000-4000-8000-000000000001',  -- demo.ec@seal.local (coordinator đã có sẵn)
    'Initial publication',
    NOW() - INTERVAL '1 day'
);

-- ──────────────────────────────────────────────────────────────
-- 16. RESULT VERSION ENTRIES (snapshot ranking tại thời điểm publish)
-- ──────────────────────────────────────────────────────────────
INSERT INTO round_result_version_entries (id, result_version_id, team_id,
                                          rank, total_score, promotion_status,
                                          tie_breaker_reason, created_at)
VALUES
('b0e00000-0000-4000-8000-000000000093',
 'b0e00000-0000-4000-8000-000000000080',
 'b0e00000-0000-4000-8000-000000000040',
 1, 83.80, 'promoted',
 'Ranked by total weighted score', NOW() - INTERVAL '1 day'),

('b0e00000-0000-4000-8000-000000000094',
 'b0e00000-0000-4000-8000-000000000080',
 'b0e00000-0000-4000-8000-000000000042',
 2, 79.25, 'eliminated',
 'Ranked by total weighted score', NOW() - INTERVAL '1 day'),

('b0e00000-0000-4000-8000-000000000095',
 'b0e00000-0000-4000-8000-000000000080',
 'b0e00000-0000-4000-8000-000000000041',
 3, 78.60, 'eliminated',
 'Ranked by total weighted score', NOW() - INTERVAL '1 day');

-- ──────────────────────────────────────────────────────────────
-- 17. APPEAL (Beta leader đã nộp, đang PENDING)
--     → Round đang bị khóa ở PAUSED_FOR_APPEAL
--     → Coordinator cần review để unblock
-- ──────────────────────────────────────────────────────────────
INSERT INTO appeals (id, event_id, round_id, team_id, submitted_by,
                     result_version_id, reason, status,
                     result_published_at, appeal_deadline,
                     recalculation_required,
                     created_at, updated_at)
VALUES (
    'b0e00000-0000-4000-8000-000000000100',
    'b0e00000-0000-4000-8000-000000000001',
    'b0e00000-0000-4000-8000-000000000004',
    'b0e00000-0000-4000-8000-000000000041',   -- Team Beta
    'b0e00000-0000-4000-8000-000000000031',   -- Beta Leader (người nộp)
    'b0e00000-0000-4000-8000-000000000080',   -- result version bị khiếu nại
    'Điểm Technical Excellence của chúng tôi đạt 88/100 — cao nhất trong tất cả các đội — nhưng chúng tôi lại xếp hạng #3. Chúng tôi yêu cầu BTC xem lại công thức tính điểm có trọng số và đảm bảo kết quả phản ánh đúng năng lực kỹ thuật của đội.',
    'PENDING',
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '2 days',
    false,
    NOW() - INTERVAL '20 hours',
    NOW() - INTERVAL '20 hours'
);

COMMIT;

-- ============================================================
-- HƯỚNG DẪN SỬ DỤNG
-- ============================================================
-- Chạy script:
--   psql -U postgres -d seal_hackathon -f seed_ranking_appeal_demo.sql
--
-- Kiểm tra dữ liệu:
--   SELECT e.title, t.name as track, r.name as round,
--          r.lifecycle_state, r.appeal_deadline
--   FROM events e
--   JOIN tracks t ON t.event_id = e.id
--   JOIN rounds r ON r.track_id = t.id
--   WHERE e.id = 'b0e00000-0000-4000-8000-000000000001';
--
--   SELECT tm.name as team, rr.rank, rr.total_score, rr.status
--   FROM round_rankings rr
--   JOIN teams tm ON tm.id = rr.team_id
--   WHERE rr.round_id = 'b0e00000-0000-4000-8000-000000000004'
--   ORDER BY rr.rank;
--
-- Xóa dữ liệu demo (rollback sạch):
--   DELETE FROM appeals    WHERE id = 'b0e00000-0000-4000-8000-000000000100';
--   DELETE FROM round_result_version_entries WHERE result_version_id = 'b0e00000-0000-4000-8000-000000000080';
--   DELETE FROM round_result_versions WHERE id = 'b0e00000-0000-4000-8000-000000000080';
--   DELETE FROM round_rankings WHERE round_id = 'b0e00000-0000-4000-8000-000000000004';
--   DELETE FROM scores WHERE submission_id IN ('b0e00000-0000-4000-8000-000000000060','b0e00000-0000-4000-8000-000000000061','b0e00000-0000-4000-8000-000000000062');
--   DELETE FROM submissions WHERE round_id = 'b0e00000-0000-4000-8000-000000000004';
--   DELETE FROM round_judges WHERE round_id = 'b0e00000-0000-4000-8000-000000000004';
--   DELETE FROM round_criteria WHERE round_id = 'b0e00000-0000-4000-8000-000000000004';
--   DELETE FROM team_members WHERE team_id IN ('b0e00000-0000-4000-8000-000000000040','b0e00000-0000-4000-8000-000000000041','b0e00000-0000-4000-8000-000000000042');
--   DELETE FROM teams WHERE track_id = 'b0e00000-0000-4000-8000-000000000002';
--   DELETE FROM team_profiles WHERE id IN ('b0e00000-0000-4000-8000-000000000020','b0e00000-0000-4000-8000-000000000021','b0e00000-0000-4000-8000-000000000022');
--   DELETE FROM user_roles WHERE user_id IN ('b0e00000-0000-4000-8000-000000000010','b0e00000-0000-4000-8000-000000000030','b0e00000-0000-4000-8000-000000000031','b0e00000-0000-4000-8000-000000000032');
--   DELETE FROM users WHERE id IN ('b0e00000-0000-4000-8000-000000000010','b0e00000-0000-4000-8000-000000000030','b0e00000-0000-4000-8000-000000000031','b0e00000-0000-4000-8000-000000000032');
--   DELETE FROM rounds WHERE id = 'b0e00000-0000-4000-8000-000000000004';
--   DELETE FROM round_definitions WHERE id = 'b0e00000-0000-4000-8000-000000000003';
--   DELETE FROM tracks WHERE id = 'b0e00000-0000-4000-8000-000000000002';
--   DELETE FROM events WHERE id = 'b0e00000-0000-4000-8000-000000000001';
-- ============================================================
