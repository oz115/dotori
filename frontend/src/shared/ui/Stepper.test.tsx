import { render, screen } from '@testing-library/react-native';

import { ProgressIndicator, Steps } from './Stepper';

describe('Steps', () => {
  it('renders a circle per step', async () => {
    await render(<Steps total={3} current={1} />);

    expect(screen.getByText('1')).toBeTruthy();
    expect(screen.getByText('2')).toBeTruthy();
    expect(screen.getByText('3')).toBeTruthy();
  });
});

describe('ProgressIndicator', () => {
  it('exposes progress via accessibility value', async () => {
    await render(<ProgressIndicator total={4} current={2} />);

    expect(screen.getByRole('progressbar').props.accessibilityValue).toEqual({
      min: 1,
      max: 4,
      now: 2,
    });
  });
});
