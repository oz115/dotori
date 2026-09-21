import { fireEvent, render, screen } from '@testing-library/react-native';

import type { AccountInfo } from '@/features/account';

import { AccountCard } from './AccountCard';

const account = {
  bankCode: '088',
  bankName: '신한은행',
  accountNo: '110123456789',
  accountName: '김도토',
  accountTypeName: '입출금',
  accountBalance: 1250000,
  dailyTransferLimit: 0,
  oneTimeTransferLimit: 0,
  currency: 'KRW',
} as AccountInfo;

function setup(props: Partial<React.ComponentProps<typeof AccountCard>> = {}) {
  const handlers = {
    onLinkAccount: jest.fn(),
    onTransfer: jest.fn(),
    onHistory: jest.fn(),
  };
  return {
    handlers,
    view: render(<AccountCard account={account} isPending={false} {...handlers} {...props} />),
  };
}

describe('AccountCard', () => {
  it('연동된 계좌의 은행·계좌·잔액을 보여준다', async () => {
    await setup().view;

    expect(screen.getByText('신한은행')).toBeTruthy();
    expect(screen.getByText('1,250,000원')).toBeTruthy();
    expect(screen.getByText(/110123456789/)).toBeTruthy();
  });

  it('이체하기와 거래내역 조회 버튼이 각 화면으로 이어진다', async () => {
    const { handlers, view } = setup();
    await view;

    await fireEvent.press(screen.getByText('이체하기'));
    await fireEvent.press(screen.getByText('거래내역 조회'));

    expect(handlers.onTransfer).toHaveBeenCalledTimes(1);
    expect(handlers.onHistory).toHaveBeenCalledTimes(1);
  });

  it('showActions 가 꺼져 있으면 버튼을 그리지 않는다', async () => {
    await setup({ showActions: false }).view;

    expect(screen.queryByText('이체하기')).toBeNull();
    expect(screen.queryByText('거래내역 조회')).toBeNull();
  });

  it('계좌가 없으면 연동 안내와 연동 버튼을 보여준다', async () => {
    const { handlers, view } = setup({ account: null });
    await view;

    expect(screen.getByText('연동된 계좌 없음')).toBeTruthy();
    await fireEvent.press(screen.getByText('계좌 연동'));
    expect(handlers.onLinkAccount).toHaveBeenCalledTimes(1);
  });

  it('조회 중에는 잔액 자리에 대시를 보여주고 연동 안내는 띄우지 않는다', async () => {
    await setup({ account: null, isPending: true }).view;

    expect(screen.getByText('—')).toBeTruthy();
    expect(screen.queryByText('연동된 계좌 없음')).toBeNull();
  });
});
