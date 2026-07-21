import TimelineItem from './TimelineItem';

export default function EventTimeline({ items = [] }) {
  return <div>{items.map((item) => <TimelineItem key={item.id} item={item} showTeam />)}</div>;
}
