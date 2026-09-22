import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useState, type ReactNode } from 'react';
import {
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';

import { BANKS, type AccountInfo } from '@/features/account';
import { formatPoint, formatWonNumber } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { ContentCard } from '@/shared/ui';

import type { AssetRowText } from '@/widgets/child-home/model/assetRows';

/** 장과 장 사이 간격. 넘기는 도중에만 보이고, 멈추면 이만큼 화면 밖으로 빠진다 */
const PAGE_GAP = 16;

type BalanceCardProps = {
  balance: number | undefined;
  isPending: boolean;
  onCharge: () => void;
  onCashout: () => void;
  /** 내가 보낸 환전 요청 내역으로 */
  onCashoutHistory: () => void;
  /** "내 자산 현황"의 연동 계좌 줄에 쓸 실제 계좌 정보 */
  account: AccountInfo | null | undefined;
  /** "내 자산 현황"의 예적금·투자 줄 문구 (조회 상태까지 반영된 것) */
  assets: { savings: AssetRowText; investment: AssetRowText };
};

/**
 * 홈 맨 위 지갑. 옆으로 넘기면 두 장이다 — 도토리 잔액, 내 자산 현황.
 *
 * 두 장의 내용 길이가 달라서 그냥 두면 넘길 때마다 아래 섹션이 위아래로 출렁인다.
 * 가로 ScrollView 는 자식을 가장 큰 높이에 맞춰 늘리므로, 두 장 모두 flex:1 로 두어
 * 높이를 고정한다.
 */
export function BalanceCard({
  balance,
  isPending,
  onCharge,
  onCashout,
  onCashoutHistory,
  account,
  assets,
}: BalanceCardProps) {
  const theme = useTheme();
  const [page, setPage] = useState(0);

  // 한 장의 폭은 화면 폭에서 계산하지 않고 실제로 그려진 폭을 잰다 —
  // 홈 화면의 좌우 여백이 바뀌면 계산값이 어긋나 다음 장이 옆에 걸쳐 보인다.
  const [pageWidth, setPageWidth] = useState(0);

  // 넘기는 동안 두 장이 맞붙어 보이지 않게 사이를 띄우고, 그만큼 더 스크롤해서 멈춘다
  const interval = pageWidth + PAGE_GAP;

  const onScrollEnd = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    setPage(Math.round(e.nativeEvent.contentOffset.x / interval));
  };

  return (
    <View style={{ gap: theme.space.md }}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        onLayout={(e) => setPageWidth(e.nativeEvent.layout.width)}
        snapToInterval={interval}
        snapToAlignment="start"
        decelerationRate="fast"
        disableIntervalMomentum
        onMomentumScrollEnd={onScrollEnd}
        // 자산 카드 그림자가 ScrollView 경계 밖으로 번져 잘리는 걸 막는다
        contentContainerStyle={{ gap: PAGE_GAP, paddingTop: 4, paddingBottom: 8 }}
      >
        {/* 폭을 재기 전(첫 프레임)에 그리면 카드가 폭 0 으로 찌그러진 채 한 번 보인다 */}
        {pageWidth > 0 ? (
          <>
            <View style={{ width: pageWidth }}>
              <WalletPage
                balance={balance}
                isPending={isPending}
                onCharge={onCharge}
                onCashout={onCashout}
                onCashoutHistory={onCashoutHistory}
              />
            </View>
            <View style={{ width: pageWidth }}>
              <AssetsPage account={account} assets={assets} />
            </View>
          </>
        ) : null}
      </ScrollView>

      <PageDots count={2} active={page} />
    </View>
  );
}

type WalletPageProps = Omit<BalanceCardProps, 'account' | 'assets'>;

/** 첫 장 — 보유 도토리와 충전·환전 버튼. */
function WalletPage({
  balance,
  isPending,
  onCharge,
  onCashout,
  onCashoutHistory,
}: WalletPageProps) {
  const theme = useTheme();

  return (
    <View style={[styles.walletCard, { borderRadius: theme.radius.lg }]}>
      <LinearGradient
        colors={theme.palette.gradient.deep}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={StyleSheet.absoluteFill}
      />
      <View style={[styles.walletInner, { padding: theme.space['2xl'] }]}>
        <View style={{ gap: theme.space.sm }}>
          <View style={[styles.labelRow, { gap: theme.space.xs }]}>
            <Image
              source={require('@/assets/images/icons/icon-dotori.png')}
              style={styles.acornIcon}
              resizeMode="contain"
            />
            <Text
              style={[theme.typography.caption, { color: theme.color.text.onBrand, opacity: 0.85 }]}
            >
              현재 사용 가능 도토리
            </Text>
          </View>
          <Text style={[theme.typography.display, { color: theme.color.text.onBrand }]}>
            {isPending ? '—' : formatPoint(balance ?? 0)}
          </Text>
        </View>

        <View style={{ gap: theme.space.sm }}>
          {/* 내역은 "하러 가는" 두 버튼과 성격이 달라 위에 따로 둔다 */}
          <WalletPill label="거래 내역" onPress={onCashoutHistory} variant="cream" />

          {/* 두 버튼이 카드 폭을 반씩 나눠 갖는다 */}
          <View style={[styles.actionRow, { gap: theme.space.sm }]}>
            <View style={styles.action}>
              <WalletPill label="도토리 충전" onPress={onCharge} />
            </View>
            <View style={styles.action}>
              <WalletPill label="환전하기" onPress={onCashout} variant="tinted" />
            </View>
          </View>
        </View>
      </View>
    </View>
  );
}

/**
 * 둘째 장 — 내 자산 현황. 예적금·투자·연동 계좌 세 줄 모두 실제 데이터다.
 * 예적금·투자는 포인트, 연동 계좌는 원 단위라 한 장 안에서도 단위가 다르다.
 */
function AssetsPage({
  account,
  assets,
}: {
  account: AccountInfo | null | undefined;
  assets: BalanceCardProps['assets'];
}) {
  const theme = useTheme();

  return (
    <ContentCard style={[styles.assetsCard, { padding: theme.space.xl, gap: theme.space.sm }]}>
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
        내 자산 현황
      </Text>
      <View>
        <DetailRow
          icon={<SavingsIcon />}
          title="예적금"
          subtitle={assets.savings.subtitle}
          amount={assets.savings.amount}
        />
        <DetailRow
          icon={<InvestIcon />}
          title="투자"
          subtitle={assets.investment.subtitle}
          amount={assets.investment.amount}
        />
        <DetailRow
          icon={<AccountIcon bankCode={account?.bankCode} />}
          title="연동 계좌"
          subtitle={account?.bankName ?? '연동된 계좌 없음'}
          amount={account ? `${formatWonNumber(account.accountBalance)}원` : '-'}
          last
        />
      </View>
    </ContentCard>
  );
}

/** 몇 번째 장인지 알려주는 점. 현재 장만 길쭉한 알약이 된다. */
function PageDots({ count, active }: { count: number; active: number }) {
  const theme = useTheme();

  return (
    <View style={[styles.dots, { gap: theme.space.sm }]}>
      {Array.from({ length: count }, (_, i) => (
        <View
          key={i}
          style={[
            styles.dot,
            {
              width: i === active ? 22 : 8,
              borderRadius: theme.radius.full,
              backgroundColor: i === active ? theme.color.text.muted : theme.color.border.default,
            },
          ]}
        />
      ))}
    </View>
  );
}

type WalletPillProps = {
  label: string;
  onPress: () => void;
  /**
   * light: 흰 배경 (도토리 충전) · tinted: 연한 연두 배경 (환전하기) ·
   * cream: 크림 배경 + 도토리 브라운 글씨 (거래 내역). 배경만 보면 light 와 거의 같은 색이라
   * 글씨색으로 가른다
   */
  variant?: 'light' | 'tinted' | 'cream';
};

/**
 * 잔액 카드 위 알약 버튼. shared/ui Button 대신 직접 그린다 —
 * 배경색이 버튼마다 다르게 고정돼야 하는데, Button 의 outline 배경은 눌림 상태를 따라가서
 * 눌림 타이밍에 따라 색이 흔들린다.
 */
function WalletPill({ label, onPress, variant = 'light' }: WalletPillProps) {
  const theme = useTheme();

  // 테두리는 셋 다 브랜드 그린으로 같다 — 한 카드 안에 나란히 놓이는 버튼이라 테두리까지
  // 갈라지면 서로 다른 종류의 버튼처럼 보인다. 구분은 배경과 글씨색이 맡는다.
  const { background, label: labelColor } = {
    light: { background: theme.color.background.surface, label: theme.color.brand.solid },
    tinted: { background: theme.color.brand.subtle, label: theme.color.brand.solid },
    cream: { background: theme.color.accent.cream, label: theme.color.accent.acorn },
  }[variant];

  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.pill,
        {
          backgroundColor: background,
          borderColor: theme.color.brand.solid,
          borderRadius: theme.radius.lg,
          paddingHorizontal: theme.space.md,
        },
      ]}
    >
      <Text style={[theme.typography.label, { color: labelColor }]}>{label}</Text>
    </Pressable>
  );
}

type DetailRowProps = {
  icon: ReactNode;
  title: string;
  subtitle: string;
  amount: string;
  last?: boolean;
};

/** 자산 목록 한 줄 (예적금·투자·연동 계좌 공용). */
function DetailRow({ icon, title, subtitle, amount, last }: DetailRowProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.detailRow,
        {
          paddingVertical: theme.space.md,
          borderBottomWidth: last ? 0 : 1,
          borderBottomColor: theme.color.border.card,
        },
      ]}
    >
      <View style={[styles.detailIcon, { borderRadius: theme.radius.full }]}>{icon}</View>
      <View style={{ flex: 1 }}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>{title}</Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          {subtitle}
        </Text>
      </View>
      <Text style={[theme.typography.label, { color: theme.color.status.success.text }]}>
        {amount}
      </Text>
    </View>
  );
}

const MOCK_CIRCLE = { backgroundColor: '#F3D9D9' };

function SavingsIcon() {
  return (
    <View style={[styles.detailIcon, MOCK_CIRCLE, { borderRadius: 999 }]}>
      <Ionicons name="wallet-outline" size={16} color="#B36B6B" />
    </View>
  );
}

function InvestIcon() {
  return (
    <View style={[styles.detailIcon, MOCK_CIRCLE, { borderRadius: 999 }]}>
      <Ionicons name="trending-up-outline" size={16} color="#B36B6B" />
    </View>
  );
}

function AccountIcon({ bankCode }: { bankCode: string | undefined }) {
  const bank = BANKS.find((b) => b.code === bankCode);
  if (!bank) {
    return <View style={[styles.detailIcon, { borderRadius: 999, backgroundColor: '#E6E6E6' }]} />;
  }
  return <Image source={bank.logo} style={styles.detailIcon} resizeMode="contain" />;
}

const styles = StyleSheet.create({
  walletCard: { flex: 1, overflow: 'hidden' },
  walletInner: { flex: 1, justifyContent: 'space-between' },
  assetsCard: { flex: 1 },
  labelRow: { flexDirection: 'row', alignItems: 'center' },
  acornIcon: { width: 18, height: 18 },
  actionRow: { flexDirection: 'row' },
  action: { flex: 1 },
  pill: { minHeight: 46, alignItems: 'center', justifyContent: 'center', borderWidth: 1.5 },
  dots: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  dot: { height: 8 },
  detailRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  detailIcon: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
});
