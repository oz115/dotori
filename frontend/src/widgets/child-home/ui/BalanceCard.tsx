import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text } from 'react-native';

import { AccountCard, type AccountInfo } from '@/features/account';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { PagedCards, WalletButton, WalletCard } from '@/shared/ui';

import type { AssetRowText } from '@/widgets/child-home/model/assetRows';

type BalanceCardProps = {
  balance: number | undefined;
  isPending: boolean;
  onCharge: () => void;
  onCashout: () => void;
  /** 거래 내역(포인트 입출금·환전)으로 */
  onCashoutHistory: () => void;
  /** 넷째 장 "실계좌"에 쓸 실제 계좌 정보 (없으면 연동 전) */
  account: AccountInfo | null | undefined;
  /** 둘째·셋째 장 "예적금"·"투자" 문구 (조회 상태까지 반영된 것) */
  assets: { savings: AssetRowText; investment: AssetRowText };
  /** 예적금 탭으로 */
  onOpenSavings: () => void;
  /** 투자 탭으로 */
  onOpenInvestment: () => void;
  /** 계좌 관리(연동된 경우) 또는 계좌 연동(아직 안 한 경우) 화면으로 */
  onOpenAccount: () => void;
  /**
   * 지갑 첫 장에서 자산 장(예적금·투자·실계좌)으로 넘어왔을 때 — 계좌·자산을 최신값으로 새로고침한다.
   * 승인된 환전이 실계좌 잔액에 바로 보이게 한다.
   */
  onViewAssets?: () => void;
};

/**
 * 자녀 홈 맨 위 지갑. 옆으로 넘기면 네 장이다 — 내 도토리, 예적금, 투자, 실계좌.
 * 부모 홈의 지갑과 같은 틀(WalletCard)이고, 장마다 포인트 색만 다르다.
 * 슬라이드 동작(폭 측정·스냅·점 표시)은 shared/ui 의 PagedCards 가 맡는다.
 */
export function BalanceCard({
  balance,
  isPending,
  onCharge,
  onCashout,
  onCashoutHistory,
  account,
  assets,
  onOpenSavings,
  onOpenInvestment,
  onOpenAccount,
  onViewAssets,
}: BalanceCardProps) {
  const theme = useTheme();

  return (
    <PagedCards
      testID="balance-card-pages"
      onPageChange={(next, previous) => {
        if (previous === 0 && next > 0) onViewAssets?.();
      }}
      pages={[
        <WalletCard
          key="wallet"
          fill
          inverted
          // 자녀 도토리 카드의 초록 — 팔레트의 그라디언트(deep)를 그대로 쓴다
          gradient={theme.palette.gradient.deep}
          accent={theme.color.brand.solid}
          icon={
            <Image
              source={require('@/assets/images/icons/icon-dotori.png')}
              style={styles.acornIcon}
              resizeMode="contain"
            />
          }
          title="내 도토리"
          subtitle="현재 사용 가능"
          headerRight={<HistoryLink label="거래 내역" onPress={onCashoutHistory} />}
          amount={isPending ? '—' : formatPoint(balance ?? 0)}
        >
          <WalletButton label="도토리 충전" onPress={onCharge} />
          <WalletButton label="환전하기" onPress={onCashout} tone="soft" />
        </WalletCard>,

        <WalletCard
          key="savings"
          fill
          accent={theme.color.status.success.solid}
          icon={
            <Ionicons name="wallet-outline" size={24} color={theme.color.status.success.solid} />
          }
          title="예적금"
          subtitle={assets.savings.subtitle}
          amount={assets.savings.amount}
        >
          <WalletButton label="예적금 보러가기" onPress={onOpenSavings} />
        </WalletCard>,

        <WalletCard
          key="investment"
          fill
          accent={theme.color.status.info.solid}
          icon={
            <Ionicons name="trending-up-outline" size={24} color={theme.color.status.info.solid} />
          }
          title="투자"
          subtitle={assets.investment.subtitle}
          amount={assets.investment.amount}
        >
          <WalletButton label="투자 보러가기" onPress={onOpenInvestment} />
        </WalletCard>,

        <AccountCard
          key="account"
          fill
          account={account}
          isPending={false}
          onLinkAccount={onOpenAccount}
          onManage={onOpenAccount}
          showActions={false}
          unlinkedNote="계좌를 연결하면 도토리를 충전하고 환전할 수 있어요."
        />,
      ]}
    />
  );
}

/** 카드 오른쪽 위의 작은 이동 링크 ("거래 내역 ›"). 버튼처럼 보이지 않게 글씨와 화살표만 둔다. */
function HistoryLink({ label, onPress }: { label: string; onPress: () => void }) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`${label} 보기`}
      hitSlop={10}
      style={styles.historyLink}
    >
      <Text style={[theme.typography.caption, { color: theme.color.text.onBrand, opacity: 0.9 }]}>
        {label}
      </Text>
      <Ionicons name="chevron-forward" size={14} color={theme.color.text.onBrand} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  acornIcon: { width: 28, height: 28 },
  historyLink: { flexDirection: 'row', alignItems: 'center', gap: 2 },
});
