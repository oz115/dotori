import { render, screen } from '@testing-library/react-native';

import { InvestmentSummaryCards } from './InvestmentSummaryCards';

it('산 주식이 없으면 숫자 대신 뉴스를 읽고 골라 보라는 안내를 보여준다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 0, investedPoints: 0, earnedPoints: 0 }} />,
  );
  expect(screen.getByText('아직 산 주식이 없어요')).toBeTruthy();
  expect(screen.getByText('뉴스를 읽고 오를 회사를 찾아보세요!')).toBeTruthy();
  expect(screen.queryByText('지금 가치')).toBeNull();
});

it('산 값·지금 가치·수익을 세 칸으로 나란히 보여준다', async () => {
  // 숫자 하나와 문장만 있던 때는 "얼마를 넣어서 얼마가 됐는지" 를 한눈에 못 봤다 (QA).
  await render(
    <InvestmentSummaryCards summary={{ count: 2, investedPoints: 20000, earnedPoints: 300 }} />,
  );
  expect(screen.getByText('내 주식 2개')).toBeTruthy();

  expect(screen.getByText('산 값')).toBeTruthy();
  expect(screen.getByText('20,000 도토리')).toBeTruthy();

  expect(screen.getByText('지금 가치')).toBeTruthy();
  expect(screen.getByText('20,300 도토리')).toBeTruthy();

  expect(screen.getByText('수익')).toBeTruthy();
  expect(screen.getByText('+300 도토리')).toBeTruthy();
});

it('수익이 마이너스면 부호로 보여준다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 1, investedPoints: 10000, earnedPoints: -200 }} />,
  );
  expect(screen.getByText('-200 도토리')).toBeTruthy();
  // 지금 가치는 산 값에서 내린 만큼을 뺀 값이다
  expect(screen.getByText('9,800 도토리')).toBeTruthy();
});

it('변동이 없으면 수익 0으로 두고, 가격이 하루 한 번 바뀐다는 안내를 항상 보여준다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 1, investedPoints: 10000, earnedPoints: 0 }} />,
  );
  expect(screen.getByText('0 도토리')).toBeTruthy();
  // 실제 문구는 두 줄(줄바꿈)이지만, 매처가 공백을 정규화하므로 한 줄로 비교한다
  expect(screen.getByText('⏰ 가격은 하루에 한 번, 자정 직후에 바뀌어요.')).toBeTruthy();
});

it('지갑 잔액(포인트)은 투자 화면 요약에 보여주지 않는다', async () => {
  await render(
    <InvestmentSummaryCards summary={{ count: 2, investedPoints: 20000, earnedPoints: 300 }} />,
  );
  expect(screen.queryByText('내 도토리')).toBeNull();
});
