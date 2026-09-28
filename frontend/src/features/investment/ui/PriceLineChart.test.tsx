import { fireEvent, render, screen } from '@testing-library/react-native';

import { PriceLineChart } from './PriceLineChart';

it('빈 이력은 축 계산 대신 안내를 보여준다', async () => {
  await render(<PriceLineChart history={[]} />);
  expect(screen.getByText('아직 가격 기록을 모으고 있어요.')).toBeTruthy();
});

it('기본엔 안내만 보이고, 그래프를 누르면 그 지점의 날짜·가격을 보여준다', async () => {
  await render(
    <PriceLineChart
      history={[
        { label: '9/1', price: 100 },
        { label: '9/2', price: 200 },
        { label: '9/3', price: 300 },
      ]}
    />,
  );
  expect(screen.getByText('그래프를 누르면 그날의 가격을 볼 수 있어요.')).toBeTruthy();

  const plot = screen.getByTestId('price-chart-plot');
  await fireEvent(plot, 'layout', { nativeEvent: { layout: { width: 300 } } });
  await fireEvent(plot, 'responderGrant', { nativeEvent: { locationX: 150 } });

  expect(screen.getByText('9/2 · 200 도토리')).toBeTruthy();
});

it('긴 이력의 날짜 눈금은 마지막 날을 보존하고 같은 간격으로 거슬러 솎아낸다', async () => {
  await render(
    <PriceLineChart
      history={Array.from({ length: 31 }, (_, index) => ({ label: `8/${index + 1}`, price: 100 }))}
    />,
  );
  // 31일: 간격 5일 — 8/31, 8/26, ..., 8/1. 끝의 두 라벨만 간격이 다른 일이 없다.
  expect(screen.getByText('8/31')).toBeTruthy();
  expect(screen.getByText('8/26')).toBeTruthy();
  expect(screen.getByText('8/1')).toBeTruthy();
  expect(screen.queryByText('8/30')).toBeNull();
  expect(screen.getAllByText(/^8\//).length).toBeLessThanOrEqual(8);
});

it('일주일(8점) 이력은 모든 날짜가 찍힌다', async () => {
  await render(
    <PriceLineChart
      history={Array.from({ length: 8 }, (_, index) => ({ label: `9/${index + 15}`, price: 100 }))}
    />,
  );
  for (let day = 15; day <= 22; day += 1) {
    expect(screen.getByText(`9/${day}`)).toBeTruthy();
  }
});
