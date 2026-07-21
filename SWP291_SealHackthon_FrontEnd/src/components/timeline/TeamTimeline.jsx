import TimelineItem from './TimelineItem';

export default function TeamTimeline({ items = [] }) {
  return <div>{items.map((item) => <TimelineItem key={item.id} item={item} />)}</div>;
}
