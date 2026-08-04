package vn.edu.fpt.seal.common.enums;

/**
 * Trạng thái vòng đời của một vòng thi (round), điều khiển luồng chấm điểm,
 * kháng cáo, tính lại điểm và thăng hạng.
 *
 * <ul>
 *   <li>{@code SCORING} – đang chấm điểm.</li>
 *   <li>{@code APPEAL_WINDOW_OPEN} – cửa sổ kháng cáo đang mở.</li>
 *   <li>{@code PAUSED_FOR_APPEAL} – tạm dừng để xử lý kháng cáo.</li>
 *   <li>{@code AWAITING_RECALCULATION} – chờ tính lại điểm.</li>
 *   <li>{@code READY_TO_ADVANCE} – sẵn sàng thăng hạng đội.</li>
 *   <li>{@code ADVANCED} – đã thăng hạng xong.</li>
 *   <li>{@code READY_FOR_AWARDS} – sẵn sàng trao giải.</li>
 * </ul>
 */
public enum RoundLifecycleState {SCORING, APPEAL_WINDOW_OPEN, PAUSED_FOR_APPEAL, AWAITING_RECALCULATION, READY_TO_ADVANCE, ADVANCED, READY_FOR_AWARDS}
