import { fireEvent, render, screen } from '@testing-library/react-native';

import { BottomNav } from './BottomNav';

const items = [
  { key: 'task', label: '할 일' },
  { key: 'store', label: '상점' },
  { key: 'home', label: '홈' },
] as const;

describe('BottomNav', () => {
  it('renders a tab per item', async () => {
    await render(<BottomNav items={[...items]} activeKey="home" onSelect={() => {}} />);

    expect(screen.getByText('할 일')).toBeTruthy();
    expect(screen.getByText('상점')).toBeTruthy();
  });

  it('calls onSelect with the tapped key', async () => {
    const onSelect = jest.fn();
    await render(<BottomNav items={[...items]} activeKey="home" onSelect={onSelect} />);

    fireEvent.press(screen.getByLabelText('상점'));

    expect(onSelect).toHaveBeenCalledWith('store');
  });

  it('marks the active tab as selected', async () => {
    await render(<BottomNav items={[...items]} activeKey="home" onSelect={() => {}} />);

    expect(screen.getByLabelText('홈').props.accessibilityState).toMatchObject({ selected: true });
  });
});
