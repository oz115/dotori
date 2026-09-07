import { fireEvent, render, screen } from '@testing-library/react-native';

import { Header } from './Header';

describe('Header', () => {
  it('renders the title', async () => {
    await render(<Header title="본인인증" />);

    expect(screen.getByText('본인인증')).toBeTruthy();
  });

  it('calls onBack when the back button is pressed', async () => {
    const onBack = jest.fn();
    await render(<Header title="본인인증" onBack={onBack} />);

    fireEvent.press(screen.getByLabelText('뒤로가기'));

    expect(onBack).toHaveBeenCalledTimes(1);
  });

  it('renders the right action and calls its handler', async () => {
    const onPress = jest.fn();
    await render(<Header title="본인인증" action={{ label: '취소', onPress }} />);

    fireEvent.press(screen.getByText('취소'));

    expect(onPress).toHaveBeenCalledTimes(1);
  });
});
