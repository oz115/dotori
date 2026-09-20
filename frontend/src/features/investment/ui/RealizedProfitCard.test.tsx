import { render, screen } from '@testing-library/react-native';

import type { InvestmentTransactionItem } from '@/features/investment/model/types';
import { RealizedProfitCard } from './RealizedProfitCard';

const sale: InvestmentTransactionItem = {
  id: 1,
  productName: '고소한빵',
  image: 0,
  type: 'SELL',
  totalAmount: 1100,
  date: '2026. 09. 19',
  realizedProfit: 100,
};

it('매도 대금 대신 원금을 뺀 확정 이익과 손해를 합산한다', async () => {
  await render(
    <RealizedProfitCard
      transactions={[sale, { ...sale, id: 2, totalAmount: 800, realizedProfit: -200 }]}
    />,
  );
  expect(screen.getByText('손해 100 도토리')).toBeTruthy();
});

it('원가가 빠진 거래가 있으면 불완전한 합계를 확정값처럼 보여주지 않는다', async () => {
  await render(
    <RealizedProfitCard transactions={[sale, { ...sale, id: 2, realizedProfit: null }]} />,
  );
  expect(screen.getByText('판매 수익을 계산할 정보가 부족해요.')).toBeTruthy();
  expect(screen.queryByText('수익 100 도토리')).toBeNull();
});
