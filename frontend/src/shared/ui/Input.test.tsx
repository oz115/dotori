import { fireEvent, render, screen } from '@testing-library/react-native';

import { Input } from './Input';

describe('Input', () => {
  it('renders its label', async () => {
    await render(<Input label="이메일" value="" onChangeText={() => {}} />);

    expect(screen.getByText('이메일')).toBeTruthy();
  });

  it('calls onChangeText when typing', async () => {
    const onChangeText = jest.fn();
    await render(
      <Input
        label="이메일"
        placeholder="이메일을 입력하세요"
        value=""
        onChangeText={onChangeText}
      />,
    );

    fireEvent.changeText(screen.getByPlaceholderText('이메일을 입력하세요'), 'a@b.com');

    expect(onChangeText).toHaveBeenCalledWith('a@b.com');
  });

  it('shows the error message', async () => {
    await render(
      <Input label="이메일" value="" onChangeText={() => {}} error="형식이 올바르지 않아요" />,
    );

    expect(screen.getByText('형식이 올바르지 않아요')).toBeTruthy();
  });
});
