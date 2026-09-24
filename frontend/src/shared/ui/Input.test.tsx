import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';

import { theme } from '@/shared/theme';

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

  it('toggles password visibility in box variant', async () => {
    await render(
      <Input
        variant="box"
        placeholder="비밀번호를 입력하세요"
        secureTextEntry
        value="secret"
        onChangeText={() => {}}
      />,
    );

    const field = screen.getByPlaceholderText('비밀번호를 입력하세요');
    expect(field.props.secureTextEntry).toBe(true);

    fireEvent.press(screen.getByLabelText('비밀번호 표시'));

    await waitFor(() =>
      expect(screen.getByPlaceholderText('비밀번호를 입력하세요').props.secureTextEntry).toBe(
        false,
      ),
    );
  });

  it('shows a muted, read-only look when editable is false', async () => {
    await render(
      <Input
        variant="box"
        label="아이디"
        value="dotori"
        editable={false}
        onChangeText={() => {}}
      />,
    );

    const field = screen.getByDisplayValue('dotori');
    expect(field.props.editable).toBe(false);

    const container = field.parent;
    const backgroundColor = container?.props.style
      .flat()
      .find((s: { backgroundColor?: string }) => s?.backgroundColor)?.backgroundColor;
    expect(backgroundColor).toBe(theme.color.background.surfaceSunken);
  });
});
