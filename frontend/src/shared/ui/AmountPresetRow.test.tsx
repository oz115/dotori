import { fireEvent, render, screen } from '@testing-library/react-native';

import { AmountPresetRow } from './AmountPresetRow';

describe('AmountPresetRow', () => {
  it('넘긴 프리셋을 순서대로 보여준다', async () => {
    await render(
      <AmountPresetRow
        presets={[
          { key: '1000', label: '+1,000원', onPress: () => {} },
          { key: 'full', label: '전액', onPress: () => {} },
        ]}
      />,
    );

    expect(screen.getByText('+1,000원')).toBeTruthy();
    expect(screen.getByText('전액')).toBeTruthy();
  });

  it('칩을 누르면 그 프리셋의 onPress 만 부른다', async () => {
    const addThousand = jest.fn();
    const useFull = jest.fn();
    await render(
      <AmountPresetRow
        presets={[
          { key: '1000', label: '+1,000원', onPress: addThousand },
          { key: 'full', label: '전액', onPress: useFull },
        ]}
      />,
    );

    fireEvent.press(screen.getByText('전액'));

    expect(useFull).toHaveBeenCalledTimes(1);
    expect(addThousand).not.toHaveBeenCalled();
  });
});
