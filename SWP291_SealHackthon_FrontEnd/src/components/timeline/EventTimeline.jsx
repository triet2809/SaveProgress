/**
 * EventTimeline.jsx — Render timeline toàn sự kiện (hiển thị tên đội trên mỗi mốc vì gồm nhiều đội).
 * @param {Array} items - Danh sách mốc timeline.
 */
import TimelineItem from './TimelineItem';

export default function EventTimeline({ items = [] }) {
  return <div>{items.map((item) => <TimelineItem key={item.id} item={item} showTeam />)}</div>;
}
