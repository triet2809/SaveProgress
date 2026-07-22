/**
 * TeamTimeline.jsx — Render timeline của một đội (không hiển thị tên đội trên mỗi mốc).
 * @param {Array} items - Danh sách mốc timeline.
 */
import TimelineItem from './TimelineItem';

export default function TeamTimeline({ items = [] }) {
  return <div>{items.map((item) => <TimelineItem key={item.id} item={item} />)}</div>;
}
