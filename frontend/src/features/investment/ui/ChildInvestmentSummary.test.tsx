import { render, screen } from '@testing-library/react-native';

import { ChildInvestmentSummary } from './ChildInvestmentSummary';

const held = { count: 2, investedPoints: 20000, earnedPoints: 300 };
const noSales = { status: 'ready', count: 0, total: 0, incomplete: false } as const;

it('지금 가치와 산 값 대비 수익, 최근 7일 판매 수익을 함께 보여준다', async () => {
  await render(
    <ChildInvestmentSummary
      summary={held}
      days={7}
      sales={{ status: 'ready', count: 1, total: 500, incomplete: false }}
    />,
  );
  expect(screen.getByText('들고 있는 주식 2개의 지금 가치')).toBeTruthy();
  expect(screen.getByText('20,300 도토리')).toBeTruthy();
  expect(screen.getByText('산 값보다 300 도토리 올랐어요')).toBeTruthy();
  expect(screen.getByText('최근 7일 판매 수익')).toBeTruthy();
  expect(screen.getByText('수익 500 도토리')).toBeTruthy();
});

it('산 주식이 없으면 숫자 대신 안내를 보여준다', async () => {
  await render(
    <ChildInvestmentSummary
      summary={{ count: 0, investedPoints: 0, earnedPoints: 0 }}
      days={7}
      sales={noSales}
    />,
  );
  expect(screen.getByText('아직 산 주식이 없어요')).toBeTruthy();
  expect(screen.queryByText(/지금 가치/)).toBeNull();
});

it('판 주식이 없으면 0 도토리 대신 "판 주식이 없어요"라고 알려준다', async () => {
  await render(<ChildInvestmentSummary summary={held} days={7} sales={noSales} />);
  expect(screen.getByText('판 주식이 없어요')).toBeTruthy();
});

it('원가를 모르는 판매가 섞이면 합계를 확정값처럼 보여주지 않는다', async () => {
  await render(
    <ChildInvestmentSummary
      summary={held}
      days={7}
      sales={{ status: 'ready', count: 2, total: 500, incomplete: true }}
    />,
  );
  expect(screen.getByText('계산할 정보가 부족해요')).toBeTruthy();
  expect(screen.queryByText('수익 500 도토리')).toBeNull();
});

it('거래 내역을 못 받았을 때는 판매 수익 자리에만 상태를 표시하고 보유 정보는 그대로 둔다', async () => {
  const { rerender } = await render(
    <ChildInvestmentSummary summary={held} days={7} sales={{ status: 'pending' }} />,
  );
  expect(screen.getByText('불러오는 중이에요')).toBeTruthy();
  await rerender(<ChildInvestmentSummary summary={held} days={7} sales={{ status: 'error' }} />);
  expect(screen.getByText('불러오지 못했어요')).toBeTruthy();
  expect(screen.getByText('20,300 도토리')).toBeTruthy();
});

it('내린 경우에도 문구(내렸어요)로 알려줘서 색만으로 구분하지 않는다', async () => {
  await render(
    <ChildInvestmentSummary
      summary={{ count: 1, investedPoints: 10000, earnedPoints: -300 }}
      days={7}
      sales={{ status: 'ready', count: 0, total: 0, incomplete: false }}
    />,
  );
  expect(screen.getByText('산 값보다 300 도토리 내렸어요')).toBeTruthy();
});

it('변동이 없으면 산 값과 같다고 알려준다', async () => {
  await render(
    <ChildInvestmentSummary
      summary={{ count: 1, investedPoints: 10000, earnedPoints: 0 }}
      days={7}
      sales={{ status: 'ready', count: 0, total: 0, incomplete: false }}
    />,
  );
  expect(screen.getByText('산 값과 같아요')).toBeTruthy();
});
