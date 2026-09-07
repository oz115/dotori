import { fireEvent, render, screen } from '@testing-library/react-native';

import { Checkbox } from './Checkbox';

describe('Checkbox', () => {
  it('toggles on press', async () => {
    const onChange = jest.fn();
    await render(<Checkbox checked={false} onChange={onChange} label="아이디 저장" />);

    fireEvent.press(screen.getByText('아이디 저장'));

    expect(onChange).toHaveBeenCalledWith(true);
  });

  it('reflects checked state to accessibility', async () => {
    await render(<Checkbox checked onChange={() => {}} label="전체 동의" shape="square" />);

    expect(screen.getByRole('checkbox').props.accessibilityState).toMatchObject({ checked: true });
  });
});
