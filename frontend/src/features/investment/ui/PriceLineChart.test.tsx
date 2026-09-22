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

it('31일 가격 이력의 날짜 눈금을 다섯 개 이하로 줄이고 양끝 날짜를 보존한다', async () => {
  await render(
    <PriceLineChart
      history={Array.from({ length: 31 }, (_, index) => ({ label: `8/${index + 1}`, price: 100 }))}
    />,
  );
  expect(screen.getByText('8/1')).toBeTruthy();
  expect(screen.getByText('8/31')).toBeTruthy();
  expect(screen.getAllByText(/^8\//).length).toBeLessThanOrEqual(5);
});
