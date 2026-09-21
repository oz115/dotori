import { fireEvent, render, screen } from '@testing-library/react-native';

import type { AccountInfo } from '@/features/account';

import { BalanceCard } from './BalanceCard';

const assets = {
  savings: { subtitle: '2개 상품에 저축 중', amount: '3,000P' },
  investment: { subtitle: '보유 종목 없음', amount: '0P' },
};

const account = {
  bankCode: '088',
  bankName: '신한은행',
  accountNo: '110123456789',
  accountName: '김도담',
  accountTypeName: '입출금',
  accountBalance: 25000,
  dailyTransferLimit: 0,
  oneTimeTransferLimit: 0,
  currency: 'KRW',
} as AccountInfo;

async function setup(overrides: Partial<React.ComponentProps<typeof BalanceCard>> = {}) {
  const handlers = {
    onCharge: jest.fn(),
    onCashout: jest.fn(),
    onCashoutHistory: jest.fn(),
    onOpenSavings: jest.fn(),
    onOpenInvestment: jest.fn(),
    onOpenAccount: jest.fn(),
  };
  await render(
    <BalanceCard
      balance={1200}
      isPending={false}
      account={account}
      assets={assets}
      {...handlers}
      {...overrides}
    />,
  );
  // 카드는 실제로 그려진 폭을 잰 뒤에야 나온다 — 테스트에서는 레이아웃 이벤트를 직접 보낸다
  await fireEvent(screen.getByTestId('balance-card-pages'), 'layout', {
    nativeEvent: { layout: { width: 320, height: 240 } },
  });
  return handlers;
}

describe('BalanceCard - 자녀 지갑 슬라이드', () => {
  it('내 도토리 → 예적금 → 투자 → 실계좌 네 장이 차례로 놓인다', async () => {
    await setup();

    for (const title of ['내 도토리', '예적금', '투자', '신한은행']) {
      expect(screen.getByText(title)).toBeTruthy();
    }
  });

  it('첫 장은 잔액과 충전·환전 버튼이 있고, 환전 내역은 작은 링크로 들어간다', async () => {
    const handlers = await setup();

    expect(screen.getByText('1,200P')).toBeTruthy();
    // 환전 내역은 큰 버튼이 아니라 오른쪽 위의 링크다
    await fireEvent.press(screen.getByLabelText('환전 내역 보기'));
    await fireEvent.press(screen.getByText('도토리 충전'));
    await fireEvent.press(screen.getByText('환전하기'));

    expect(handlers.onCashoutHistory).toHaveBeenCalledTimes(1);
    expect(handlers.onCharge).toHaveBeenCalledTimes(1);
    expect(handlers.onCashout).toHaveBeenCalledTimes(1);
  });

  it('예적금·투자 장은 금액과 설명을 보여주고 각 탭으로 보낸다', async () => {
    const handlers = await setup();

    expect(screen.getByText('3,000P')).toBeTruthy();
    expect(screen.getByText('2개 상품에 저축 중')).toBeTruthy();
    expect(screen.getByText('보유 종목 없음')).toBeTruthy();

    await fireEvent.press(screen.getByText('예적금 보러가기'));
    await fireEvent.press(screen.getByText('투자 보러가기'));

    expect(handlers.onOpenSavings).toHaveBeenCalledTimes(1);
    expect(handlers.onOpenInvestment).toHaveBeenCalledTimes(1);
  });

  it('실계좌 장은 은행·잔액을 보여주고 계좌 관리로 보낸다', async () => {
    const handlers = await setup();

    expect(screen.getByText('25,000원')).toBeTruthy();
    await fireEvent.press(screen.getByText('계좌 관리'));

    expect(handlers.onOpenAccount).toHaveBeenCalledTimes(1);
  });

  it('계좌를 연동하지 않았으면 안내와 연동 버튼을 보여준다', async () => {
    const handlers = await setup({ account: null });

    expect(screen.getByText('연동된 계좌 없음')).toBeTruthy();
    await fireEvent.press(screen.getByText('계좌 연동'));

    expect(handlers.onOpenAccount).toHaveBeenCalledTimes(1);
  });
});
