import { render, screen } from '@testing-library/react-native';

import { InvestmentSummaryCards } from './InvestmentSummaryCards';

it('산 주식이 없으면 숫자 대신 뉴스를 읽고 골라 보라는 안내를 보여준다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 0, investedPoints: 0, earnedPoints: 0 }} />,
  );
  expect(screen.getByText('아직 산 주식이 없어요')).toBeTruthy();
  expect(screen.getByText('뉴스를 읽고 오를 회사를 찾아보세요!')).toBeTruthy();
  expect(screen.queryByText(/지금 가치/)).toBeNull();
});

it('산 값에 수익을 더한 지금 가치와 산 값보다 오른 만큼을 보여준다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 2, investedPoints: 20000, earnedPoints: 300 }} />,
  );
  expect(screen.getByText('내 주식 2개의 지금 가치')).toBeTruthy();
  expect(screen.getByText('20,300 도토리')).toBeTruthy();
  expect(screen.getByText('산 값보다 300 도토리 올랐어요')).toBeTruthy();
});

it('가격이 내렸으면 부호 대신 "내렸어요"로 알려준다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 1, investedPoints: 10000, earnedPoints: -200 }} />,
  );
  expect(screen.getByText('산 값보다 200 도토리 내렸어요')).toBeTruthy();
});

it('변동이 없으면 산 값과 같다고 알려주고, 가격이 하루 한 번 바뀐다는 안내를 항상 보여준다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 1, investedPoints: 10000, earnedPoints: 0 }} />,
  );
  expect(screen.getByText('산 값과 같아요')).toBeTruthy();
  expect(screen.getByText('가격은 하루에 한 번, 자정 직후에 바뀌어요.')).toBeTruthy();
});

it('잔액이 있으면 내 도토리를 맨 위에 크게 보여주고 주식 가치와 수익을 함께 보여준다', async () => {
  await render(
    <InvestmentSummaryCards
      summary={{ count: 2, investedPoints: 20000, earnedPoints: 300 }}
      balance={{ amount: 5000 }}
    />,
  );
  expect(screen.getByText('내 도토리')).toBeTruthy();
  expect(screen.getByText('5,000 도토리')).toBeTruthy();
  expect(screen.getByText('투자 중인 주식 2개')).toBeTruthy();
  expect(screen.getByText('20,300 도토리')).toBeTruthy();
  expect(screen.getByText('산 값보다 300 도토리 올랐어요')).toBeTruthy();
});

it('잔액을 아직 못 받았거나 실패해도 잔액 자리에 상태를 알려주고 투자 정보는 그대로 보여준다', async () => {
  const summary = { count: 1, investedPoints: 10000, earnedPoints: -200 };
  const { rerender } = await render(<InvestmentSummaryCards summary={summary} balance={{}} />);
  expect(screen.getByText('불러오는 중이에요')).toBeTruthy();
  expect(screen.getByText('산 값보다 200 도토리 내렸어요')).toBeTruthy();

  await rerender(<InvestmentSummaryCards summary={summary} balance={{ failed: true }} />);
  expect(screen.getByText('잔액을 불러오지 못했어요')).toBeTruthy();
});

it('잔액이 있어도 산 주식이 없으면 수익 대신 뉴스를 읽어 보라는 안내를 보여준다', async () => {
  await render(
    <InvestmentSummaryCards
      summary={{ count: 0, investedPoints: 0, earnedPoints: 0 }}
      balance={{ amount: 5000 }}
    />,
  );
  expect(screen.getByText('5,000 도토리')).toBeTruthy();
  expect(screen.getByText('아직 산 주식이 없어요')).toBeTruthy();
  expect(screen.queryByText(/투자 중인 주식/)).toBeNull();
});
