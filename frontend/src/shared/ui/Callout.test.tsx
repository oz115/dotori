import { render, screen } from '@testing-library/react-native';
import { Text } from 'react-native';

import { Callout } from './Callout';

describe('Callout', () => {
  it('renders title, description and action', async () => {
    await render(
      <Callout
        title="도토리가 처음이신가요?"
        description="회원가입하고 시작해보세요"
        action={<Text>회원가입</Text>}
      />,
    );

    expect(screen.getByText('도토리가 처음이신가요?')).toBeTruthy();
    expect(screen.getByText('회원가입하고 시작해보세요')).toBeTruthy();
    expect(screen.getByText('회원가입')).toBeTruthy();
  });
});
