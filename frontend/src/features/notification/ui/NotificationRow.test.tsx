import { fireEvent, render, screen } from '@testing-library/react-native';
import { Alert } from 'react-native';

import type { NotificationItem } from '@/features/notification/model/types';

import { NotificationRow } from './NotificationRow';

const item: NotificationItem = {
  notificationId: 1,
  type: 'PURCHASE',
  title: '새 상품이 등록됐어요',
  body: '포켓몬 카드이(가) 상점에 새로 등록됐어요',
  refId: 10,
  isRead: false,
  createdAt: '2026-09-16T05:31:00+09:00',
};

/** Alert.alert 로 뜬 확인창의 버튼을 라벨로 눌러 준다. */
function pressAlertButton(label: string) {
  const spy = Alert.alert as jest.Mock;
  const buttons = spy.mock.calls[spy.mock.calls.length - 1][2];
  buttons.find((button: { text: string }) => button.text === label).onPress?.();
}

describe('NotificationRow', () => {
  beforeEach(() => {
    jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('제목을 누르면 onPress 가 호출된다', async () => {
    const onPress = jest.fn();
    await render(<NotificationRow item={item} onPress={onPress} onDelete={jest.fn()} />);

    fireEvent.press(screen.getByText(item.title));

    expect(onPress).toHaveBeenCalled();
  });

  it('휴지통은 밀지 않아도 보인다', async () => {
    // 스와이프로만 드러나던 때는 지울 수 있다는 것 자체를 몰랐다 (QA).
    await render(<NotificationRow item={item} onPress={jest.fn()} onDelete={jest.fn()} />);

    expect(screen.getByLabelText(`${item.title} 알림 삭제`)).toBeTruthy();
  });

  it('휴지통을 눌러도 확인 전에는 지우지 않는다', async () => {
    const onDelete = jest.fn().mockResolvedValue(undefined);
    await render(<NotificationRow item={item} onPress={jest.fn()} onDelete={onDelete} />);

    fireEvent.press(screen.getByLabelText(`${item.title} 알림 삭제`));

    expect(Alert.alert).toHaveBeenCalled();
    expect(onDelete).not.toHaveBeenCalled();
  });

  it('확인창에서 삭제를 고르면 지운다', async () => {
    const onDelete = jest.fn().mockResolvedValue(undefined);
    await render(<NotificationRow item={item} onPress={jest.fn()} onDelete={onDelete} />);

    fireEvent.press(screen.getByLabelText(`${item.title} 알림 삭제`));
    pressAlertButton('삭제');

    expect(onDelete).toHaveBeenCalled();
  });

  it('확인창에서 취소를 고르면 지우지 않는다', async () => {
    const onDelete = jest.fn().mockResolvedValue(undefined);
    await render(<NotificationRow item={item} onPress={jest.fn()} onDelete={onDelete} />);

    fireEvent.press(screen.getByLabelText(`${item.title} 알림 삭제`));
    pressAlertButton('취소');

    expect(onDelete).not.toHaveBeenCalled();
  });

  it('휴지통을 눌러도 행 자체의 이동은 일어나지 않는다', async () => {
    // 휴지통이 행 안에 있어서, 이벤트가 위로 새면 확인창을 띄우면서 상세로도 넘어간다.
    const onPress = jest.fn();
    await render(<NotificationRow item={item} onPress={onPress} onDelete={jest.fn()} />);

    fireEvent.press(screen.getByLabelText(`${item.title} 알림 삭제`));

    expect(onPress).not.toHaveBeenCalled();
  });
});
