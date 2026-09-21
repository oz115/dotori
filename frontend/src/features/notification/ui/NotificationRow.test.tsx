import { fireEvent, render, screen } from '@testing-library/react-native';

import type { NotificationItem } from '@/features/notification/model/types';

import { NotificationRow } from './NotificationRow';

// 실제 ReanimatedSwipeable은 드래그 제스처·애니메이션이라 jsdom에서 신뢰성 있게
// 재현할 수 없다. 드러나는 액션(renderRightActions)을 그냥 같이 그려 주는 가벼운 대역으로
// 바꿔서, "밀면 나오는 휴지통을 눌러야 삭제된다"는 배선만 검증한다.
jest.mock('react-native-gesture-handler/ReanimatedSwipeable', () => {
  // jest.mock 팩토리는 babel-jest-hoist 제약으로 모듈 스코프 import를 참조할 수 없어
  // require를 써야 한다.
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const React = require('react');
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { View } = require('react-native');

  const FakeSwipeable = React.forwardRef(function FakeSwipeable(
    { children, renderRightActions }: any,
    ref: any,
  ) {
    React.useImperativeHandle(ref, () => ({
      close: jest.fn(),
      openLeft: jest.fn(),
      openRight: jest.fn(),
      reset: jest.fn(),
    }));

    return (
      <View testID="fake-swipeable">
        {renderRightActions?.()}
        {children}
      </View>
    );
  });

  return { __esModule: true, default: FakeSwipeable };
});

const item: NotificationItem = {
  notificationId: 1,
  type: 'PURCHASE',
  title: '새 상품이 등록됐어요',
  body: '포켓몬 카드이(가) 상점에 새로 등록됐어요',
  refId: 10,
  isRead: false,
  createdAt: '2026-09-16T05:31:00+09:00',
};

describe('NotificationRow', () => {
  it('제목을 누르면 onPress 가 호출된다', async () => {
    const onPress = jest.fn();
    await render(<NotificationRow item={item} onPress={onPress} onDelete={jest.fn()} />);

    fireEvent.press(screen.getByText(item.title));

    expect(onPress).toHaveBeenCalled();
  });

  it('밀어서 드러난 휴지통을 눌러야 삭제된다', async () => {
    // 밀기만 해도 지워지던 때는 알림이 갑자기 사라졌다 (QA).
    const onDelete = jest.fn().mockResolvedValue(undefined);
    await render(<NotificationRow item={item} onPress={jest.fn()} onDelete={onDelete} />);

    const deleteButton = screen.getByLabelText(`${item.title} 알림 삭제`);
    expect(onDelete).not.toHaveBeenCalled();

    fireEvent.press(deleteButton);

    expect(onDelete).toHaveBeenCalled();
  });
});
