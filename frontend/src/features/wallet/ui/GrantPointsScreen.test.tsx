import { fireEvent, render, screen } from '@testing-library/react-native';
import { Alert, type AlertButton } from 'react-native';

import { GrantPointsScreen } from './GrantPointsScreen';

const mockMutate = jest.fn();

jest.mock('@/features/wallet/model/useGrantPoints', () => ({
  useGrantPoints: () => ({ mutate: mockMutate, isPending: false }),
}));

const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation(() => {});

function setup(props: Partial<React.ComponentProps<typeof GrantPointsScreen>> = {}) {
  const onDone = jest.fn();
  return {
    onDone,
    view: render(
      <GrantPointsScreen
        familyId={1}
        childId={2}
        childName="도담"
        onBack={jest.fn()}
        onDone={onDone}
        {...props}
      />,
    ),
  };
}

/** 마지막으로 띄운 알림의 버튼을 이름으로 누른다 */
function pressAlertButton(text: string) {
  const buttons = alertSpy.mock.calls.at(-1)?.[2] as AlertButton[];
  buttons.find((b) => b.text === text)?.onPress?.();
}

beforeEach(() => {
  mockMutate.mockReset();
  alertSpy.mockClear();
});

describe('GrantPointsScreen', () => {
  it('누른 금액만큼 도토리를 줄 준비를 하고, 아이 이름을 보여준다', async () => {
    await setup().view;

    expect(screen.getByText('도담에게 줄 도토리')).toBeTruthy();
    await fireEvent.press(screen.getByText('5'));
    await fireEvent.press(screen.getByText('0'));
    await fireEvent.press(screen.getByText('0'));

    expect(screen.getByText('500P')).toBeTruthy();
  });

  it('프리셋 버튼은 지금 금액에 더한다', async () => {
    await setup().view;

    await fireEvent.press(screen.getByText('+500P'));
    await fireEvent.press(screen.getByText('+100P'));

    expect(screen.getByText('600P')).toBeTruthy();
  });

  it('바로 보내지 않고 확인을 받은 뒤에 지급한다', async () => {
    await setup().view;
    await fireEvent.press(screen.getByText('+500P'));

    await fireEvent.press(screen.getByText('주기'));

    expect(mockMutate).not.toHaveBeenCalled();
    expect(alertSpy).toHaveBeenCalledWith(
      '도담에게 줄까요?',
      expect.stringContaining('500P'),
      expect.any(Array),
    );

    pressAlertButton('주기');

    expect(mockMutate).toHaveBeenCalledWith(
      { body: { amount: 500 }, idempotencyKey: expect.any(String) },
      expect.any(Object),
    );
  });

  it('확인 창에서 취소하면 지급하지 않는다', async () => {
    await setup().view;
    await fireEvent.press(screen.getByText('+100P'));
    await fireEvent.press(screen.getByText('주기'));

    pressAlertButton('취소');

    expect(mockMutate).not.toHaveBeenCalled();
  });

  it('금액이 0이면 주기를 누를 수 없다', async () => {
    await setup().view;

    await fireEvent.press(screen.getByText('주기'));

    expect(alertSpy).not.toHaveBeenCalled();
  });

  it('같은 금액으로 다시 보내면 같은 멱등키를 쓰고, 금액을 바꾸면 새 키를 쓴다', async () => {
    await setup().view;
    await fireEvent.press(screen.getByText('+500P'));

    await fireEvent.press(screen.getByText('주기'));
    pressAlertButton('주기');
    await fireEvent.press(screen.getByText('주기'));
    pressAlertButton('주기');
    await fireEvent.press(screen.getByText('+100P'));
    await fireEvent.press(screen.getByText('주기'));
    pressAlertButton('주기');

    const keys = mockMutate.mock.calls.map(([vars]) => vars.idempotencyKey);
    expect(keys[0]).toBe(keys[1]);
    expect(keys[2]).not.toBe(keys[0]);
  });
});
