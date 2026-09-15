import { fireEvent, render, screen } from '@testing-library/react-native';

import { ChoreFilterChips } from './ChoreFilterChips';

const OPTIONS = [
  { key: 'DAY' as const, label: '매일' },
  { key: 'WEEK' as const, label: '매주' },
];

describe('ChoreFilterChips', () => {
  it('전체 칩을 앞에 붙여 보여준다', async () => {
    await render(<ChoreFilterChips options={OPTIONS} selected={null} onSelect={() => {}} />);

    expect(screen.getByText('전체')).toBeTruthy();
    expect(screen.getByText('매일')).toBeTruthy();
  });

  it('칩을 누르면 그 키를 넘긴다', async () => {
    const onSelect = jest.fn();
    await render(<ChoreFilterChips options={OPTIONS} selected={null} onSelect={onSelect} />);

    fireEvent.press(screen.getByText('매주'));

    expect(onSelect).toHaveBeenCalledWith('WEEK');
  });

  it('전체를 누르면 null 을 넘긴다 (선택 해제)', async () => {
    const onSelect = jest.fn();
    await render(<ChoreFilterChips options={OPTIONS} selected="DAY" onSelect={onSelect} />);

    fireEvent.press(screen.getByText('전체'));

    expect(onSelect).toHaveBeenCalledWith(null);
  });
});
