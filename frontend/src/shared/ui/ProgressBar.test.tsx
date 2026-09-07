import { render, screen } from '@testing-library/react-native';

import { ProgressBar } from './ProgressBar';

describe('ProgressBar', () => {
  it('reports the clamped percentage', async () => {
    await render(<ProgressBar value={0.42} />);

    expect(screen.getByRole('progressbar').props.accessibilityValue).toEqual({
      min: 0,
      max: 100,
      now: 42,
    });
  });

  it('clamps out-of-range values', async () => {
    await render(<ProgressBar value={1.8} />);

    expect(screen.getByRole('progressbar').props.accessibilityValue.now).toBe(100);
  });
});
