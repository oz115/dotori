import type { PriceHistoryResponse, PricePoint } from '@/features/investment/model/types';
import { parseServerDate } from '@/shared/lib/datetime';

const DAY = 86_400_000;
const KST_OFFSET = 9 * 60 * 60 * 1000;
const dateKey = (time: number) => new Date(time + KST_OFFSET).toISOString().slice(0, 10);

/** 구버전의 변동일만 있는 응답도, 신버전의 일별 응답도 같은 날짜 축으로 표시한다. */
export function dailyPricePoints(
  history: PriceHistoryResponse['history'],
  now = new Date(),
): PricePoint[] {
  const today = dateKey(now.getTime());
  const rows = history
    .map((row) => ({ ...row, time: parseServerDate(row.recordedAt)?.getTime() }))
    .filter(
      (row): row is typeof row & { time: number } => row.time != null && row.time <= now.getTime(),
    )
    .sort((a, b) => a.time - b.time);
  if (rows.length === 0) return [];
  const byDate = new Map(rows.map((row) => [dateKey(row.time), row.price]));
  const points: PricePoint[] = [];
  let price = rows[0].price;
  // 최초 실제 기록 이전 가격은 추측하지 않는다.
  for (
    let time = Date.parse(dateKey(rows[0].time) + 'T00:00:00Z');
    time <= Date.parse(today + 'T00:00:00Z');
    time += DAY
  ) {
    const day = new Date(time).toISOString().slice(0, 10);
    price = byDate.get(day) ?? price;
    points.push({ label: `${Number(day.slice(5, 7))}/${day.slice(8, 10)}`, price });
  }
  return points;
}
