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
  expect(screen.getByText('산 값 대비 0 도토리')).toBeTruthy();
  expect(screen.getByText('가격은 하루에 한 번, 자정 직후에 바뀌어요.')).toBeTruthy();
});

it('지갑 잔액(포인트)은 투자 화면 요약에 보여주지 않는다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 2, investedPoints: 20000, earnedPoints: 300 }} />,
  );
  expect(screen.queryByText('내 도토리')).toBeNull();
});
