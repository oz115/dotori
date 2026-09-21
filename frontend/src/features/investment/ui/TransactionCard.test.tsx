import { render, screen } from '@testing-library/react-native';

import { toTransactionItems } from '@/features/investment/lib/transactions';
import { TransactionCard } from './TransactionCard';

const sellThenBuy = (sellPrice: number) =>
  toTransactionItems([
    {
      investmentTransactionId: 2,
      productName: '고소한빵',
      transactionType: 'SELL' as const,
      quantity: 1,
      unitPrice: sellPrice,
      totalAmount: sellPrice,
      createdAt: '2026-09-19T10:01:00+09:00',
    },
    {
      investmentTransactionId: 1,
      productName: '고소한빵',
      transactionType: 'BUY' as const,
      quantity: 1,
      unitPrice: 10000,
      totalAmount: 10000,
      createdAt: '2026-09-19T10:00:00+09:00',
    },
  ]);

// 매도 항목은 번/잃은 도토리 한 줄만 보여준다 — 살 때/팔 때 가격까지 늘어놓으면
// 정보가 많아 못 읽는다는 피드백으로 줄였다 (매도 완료 알림과 같은 문구).
it('판 항목은 번 도토리 한 줄만 보여주고 살 때/팔 때 가격은 늘어놓지 않는다', async () => {
  const [sale] = sellThenBuy(10250);
  await render(<TransactionCard item={sale} />);
  expect(screen.getByText('팔았어요')).toBeTruthy();
  expect(screen.getByText('250 도토리를 벌었어요!')).toBeTruthy();
  expect(screen.queryByText(/구매.*판매/)).toBeNull();
});

it('산 값보다 싸게 판 항목은 잃은 도토리를 보여준다', async () => {
  const [sale] = sellThenBuy(9700);
  await render(<TransactionCard item={sale} />);
  expect(screen.getByText('300 도토리를 잃었어요.')).toBeTruthy();
});

it('같은 가격에 사고팔면 산 값 그대로 팔았다고 알려준다', async () => {
  const [sale] = sellThenBuy(10000);
  await render(<TransactionCard item={sale} />);
  expect(screen.getByText('산 값 그대로 팔았어요.')).toBeTruthy();
});
