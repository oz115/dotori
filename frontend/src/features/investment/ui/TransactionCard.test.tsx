import { render, screen } from '@testing-library/react-native';

import { toTransactionItems } from '@/features/investment/lib/transactions';
import { TransactionCard } from './TransactionCard';

it('기존 서버 응답에서 같은 가격에 사고판 고소한빵은 수익 0으로 보여준다', async () => {
  const [sale] = toTransactionItems([
    {
      investmentTransactionId: 2,
      productName: '고소한빵',
      transactionType: 'SELL',
      quantity: 1,
      unitPrice: 10000,
      totalAmount: 10000,
      createdAt: '2026-09-19T10:01:00+09:00',
    },
    {
      investmentTransactionId: 1,
      productName: '고소한빵',
      transactionType: 'BUY',
      quantity: 1,
      unitPrice: 10000,
      totalAmount: 10000,
      createdAt: '2026-09-19T10:00:00+09:00',
    },
  ]);
  await render(<TransactionCard item={sale} />);
  expect(screen.getByText('수익 0 도토리')).toBeTruthy();
  expect(screen.getByText('구매 10,000 도토리 → 판매 10,000 도토리')).toBeTruthy();
  expect(screen.queryByText(/정보가 부족/)).toBeNull();
});
