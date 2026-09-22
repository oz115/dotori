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

it('날짜 눈금은 마지막 날을 보존하고 같은 간격으로 거슬러 찍는다', async () => {
  await render(
    <PriceLineChart
      history={Array.from({ length: 31 }, (_, index) => ({ label: `8/${index + 1}`, price: 100 }))}
    />,
  );
  // 31일: 간격 8일 — 8/31, 8/23, 8/15, 8/7. 앞쪽 자투리(8/1)는 버린다.
  expect(screen.getByText('8/31')).toBeTruthy();
  expect(screen.getByText('8/23')).toBeTruthy();
  expect(screen.getByText('8/7')).toBeTruthy();
  expect(screen.queryByText('8/1')).toBeNull();
  expect(screen.getAllByText(/^8\//).length).toBeLessThanOrEqual(5);
});

it('일주일(8점) 이력은 오늘부터 이틀 간격으로 고르게 찍는다', async () => {
  await render(
    <PriceLineChart
      history={Array.from({ length: 8 }, (_, index) => ({ label: `9/${index + 15}`, price: 100 }))}
    />,
  );
  // 9/15~9/22: 라벨은 9/16, 9/18, 9/20, 9/22 — 끝의 두 라벨만 간격이 다른 일이 없다
  expect(screen.getByText('9/22')).toBeTruthy();
  expect(screen.getByText('9/20')).toBeTruthy();
  expect(screen.getByText('9/18')).toBeTruthy();
  expect(screen.getByText('9/16')).toBeTruthy();
  expect(screen.queryByText('9/15')).toBeNull();
  expect(screen.queryByText('9/21')).toBeNull();
});
