/**
 * 알림 카드에 쓰는 상대 시간 표시.
 * - 1분 미만: "방금 전"
 * - 1시간 미만: "N분 전"
 * - 오늘 안(자정 전): "N시간 전"
 * - 어제: "어제 오후 4:21"
 * - 그 밖: "9월 11일 오전 10:14" (연도가 다르면 앞에 붙는다)
 */
export function formatNotificationTime(iso: string, now: Date = new Date()): string {
  const date = new Date(iso);
  const diffMs = now.getTime() - date.getTime();
  const diffMinutes = Math.floor(diffMs / 60_000);

  if (diffMinutes < 1) return '방금 전';
  if (diffMinutes < 60) return `${diffMinutes}분 전`;

  const isSameDay = isSameCalendarDay(date, now);
  if (isSameDay) {
    const diffHours = Math.floor(diffMinutes / 60);
    return `${diffHours}시간 전`;
  }

  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (isSameCalendarDay(date, yesterday)) {
    return `어제 ${formatClock(date)}`;
  }

  const yearPrefix = date.getFullYear() === now.getFullYear() ? '' : `${date.getFullYear()}년 `;
  return `${yearPrefix}${date.getMonth() + 1}월 ${date.getDate()}일 ${formatClock(date)}`;
}

function isSameCalendarDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

/** "오후 4:21" */
function formatClock(date: Date): string {
  const hours24 = date.getHours();
  const period = hours24 < 12 ? '오전' : '오후';
  const hours12 = hours24 % 12 === 0 ? 12 : hours24 % 12;
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${period} ${hours12}:${minutes}`;
}
