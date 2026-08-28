import { fireEvent, render, screen } from '@testing-library/react-native';

import { Button } from './Button';

describe('Button', () => {
  it('renders its label', async () => {
    await render(<Button label="증가" onPress={() => {}} />);

    expect(screen.getByText('증가')).toBeTruthy();
  });

  it('calls onPress when pressed', async () => {
    const onPress = jest.fn();
    await render(<Button label="증가" onPress={onPress} />);

    fireEvent.press(screen.getByText('증가'));

    expect(onPress).toHaveBeenCalledTimes(1);
  });
});
