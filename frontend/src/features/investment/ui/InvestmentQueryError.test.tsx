import { fireEvent, render, screen } from '@testing-library/react-native';

import { InvestmentQueryError } from './InvestmentQueryError';

it('재조회 중에는 진행 상태를 알리고 중복 요청을 막는다', async () => {
  const retry = jest.fn();
  await render(<InvestmentQueryError error={new Error('offline')} fetching onRetry={retry} />);
  expect(screen.getByText('다시 불러오는 중이에요.')).toBeTruthy();
  fireEvent.press(screen.getByRole('button'));
  expect(retry).not.toHaveBeenCalled();
});

it('로그인 만료는 반복 재조회 대신 로그인 안내를 표시한다', async () => {
  await render(
    <InvestmentQueryError
      error={{ response: { status: 401 } }}
      fetching={false}
      onRetry={jest.fn()}
    />,
  );
  expect(screen.getByText('로그인이 만료됐어요. 다시 로그인해 주세요.')).toBeTruthy();
  expect(screen.queryByText('다시 불러오기')).toBeNull();
});
