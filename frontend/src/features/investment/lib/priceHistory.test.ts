import { dailyPricePoints } from './priceHistory';

it('변동일만 내려오는 기존 서버에서도 보합 날짜와 마지막 보합 구간을 유지한다', () => {
  const result = dailyPricePoints(
    [
      { recordedAt: '2026-09-15T00:05:00+09:00', price: 100 },
      { recordedAt: '2026-09-18T00:05:00+09:00', price: 110 },
    ],
    new Date('2026-09-19T10:00:00+09:00'),
  );
  expect(result.map((row) => row.price)).toEqual([100, 100, 100, 110, 110]);
});

it('UTC 기록을 KST 날짜로 맞추고 같은 날은 마지막 가격을 쓴다', () => {
  const result = dailyPricePoints(
    [
      { recordedAt: '2026-09-18T15:05:00Z', price: 100 },
      { recordedAt: '2026-09-19T02:00:00Z', price: 110 },
    ],
    new Date('2026-09-19T12:00:00+09:00'),
  );
  expect(result).toEqual([{ label: '9/19', price: 110 }]);
});
