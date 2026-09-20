import { fireEvent, render, screen } from '@testing-library/react-native';

import type { Holding } from '@/features/investment/model/types';
import { HoldingsListCard } from './HoldingsListCard';

const holding = (id: number, name: string, buyPrice: number, currentPrice: number): Holding => ({
  id,
  name,
  image: 0,
  quantity: 1,
  buyPrice,
  currentPrice,
});

it('종목마다 이름과 산 값, 지금 가격, 산 값 대비 변화를 한 줄에 보여준다', async () => {
  await render(
    <HoldingsListCard
      holdings={[holding(1, '고소한빵', 10000, 10057), holding(2, '튼튼건설', 10000, 9800)]}
      onPressHolding={jest.fn()}
    />,
  );
  expect(screen.getByText('고소한빵')).toBeTruthy();
  expect(screen.getAllByText('산 값 10,000 도토리')).toHaveLength(2);
  expect(screen.getByText('10,057 도토리')).toBeTruthy();
  expect(screen.getByText('+57 도토리')).toBeTruthy();
  expect(screen.getByText('-200 도토리')).toBeTruthy();
});

it('변동이 없으면 0 도토리 대신 변동 없음이라고 적는다', async () => {
  await render(
    <HoldingsListCard
      holdings={[holding(1, '고소한빵', 10000, 10000)]}
      onPressHolding={jest.fn()}
    />,
  );
  expect(screen.getByText('변동 없음')).toBeTruthy();
});

it('줄을 누르면 그 종목을 알려준다', async () => {
  const onPressHolding = jest.fn();
  const target = holding(2, '튼튼건설', 10000, 9800);
  await render(
    <HoldingsListCard
      holdings={[holding(1, '고소한빵', 1, 1), target]}
      onPressHolding={onPressHolding}
    />,
  );
  await fireEvent.press(screen.getByLabelText('튼튼건설 상세 보기'));
  expect(onPressHolding).toHaveBeenCalledWith(target);
});
