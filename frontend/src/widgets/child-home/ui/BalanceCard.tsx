import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useState, type ReactNode } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { BANKS, type AccountInfo } from '@/features/account';
import { formatPoint, formatWonNumber } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';

type BalanceCardProps = {
  balance: number | undefined;
  isPending: boolean;
  onCharge: () => void;
  onCashout: () => void;
  /** "예적금·투자 내 계좌보기" 펼쳤을 때 "내 계좌" 줄에 쓸 실제 연동 계좌 정보 */
  account: AccountInfo | undefined;
};

/**
 * 잔액 카드. 예적금·투자는 아직 없는 도메인이라 펼침 목록에서는 mock 값을 보여준다 —
 * "내 계좌" 한 줄만 실제 연동 계좌 데이터다.
 */
export function BalanceCard({
  balance,
  isPending,
  onCharge,
  onCashout,
  account,
}: BalanceCardProps) {
  const theme = useTheme();
  const [expanded, setExpanded] = useState(false);

  return (
    <View style={[styles.card, { borderRadius: theme.radius.lg }]}>
      <LinearGradient
        colors={theme.palette.gradient.deep}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={StyleSheet.absoluteFill}
      />
      <View style={[styles.inner, { padding: theme.space['2xl'], gap: theme.space.lg }]}>
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

        {/* 두 버튼이 카드 폭을 반씩 나눠 갖는다 */}
        <View style={[styles.actionRow, { gap: theme.space.sm }]}>
          <View style={styles.action}>
            <WalletPill label="도토리 충전" onPress={onCharge} />
          </View>
          <View style={styles.action}>
            <WalletPill label="환전하기" onPress={onCashout} tinted />
          </View>
        </View>
      </View>

      {/* 카드 아래를 가로로 꽉 채우는 띠. 위쪽 그라디언트 위에 흰색을 옅게 덮어
          한 톤 밝은 초록으로 보이게 한다 (구분선만 긋는 것보다 영역이 또렷하다) */}
      <Pressable
        onPress={() => setExpanded((v) => !v)}
        style={[
          styles.expandRow,
          {
            backgroundColor: EXPAND_BAND_OVERLAY,
            paddingVertical: theme.space.md,
            gap: theme.space.xs,
          },
        ]}
        accessibilityRole="button"
        accessibilityState={{ expanded }}
      >
        <Text style={[theme.typography.bodySm, { color: theme.color.text.onBrand }]}>
          예적금 투자 내 계좌보기
        </Text>
      </Pressable>

      {/* 펼치면 카드 아래쪽이 통째로 흰 영역이 된다 — 초록 위에 흰 상자를 얹는 것보다
          목록이 또렷하게 읽힌다. 아래 모서리는 카드의 overflow:hidden 이 잘라 준다 */}
      {expanded ? (
        <View style={[styles.detailSheet, { backgroundColor: theme.color.background.surface }]}>
          <DetailRow
            icon={<SavingsIcon />}
            title="예적금"
            subtitle="3개 상품에 저축 중"
            amount={formatPoint(3000)}
          />
          <DetailRow
            icon={<InvestIcon />}
            title="투자"
            subtitle="3개 회사 보유 중"
            amount={formatPoint(3000)}
          />
          <DetailRow
            icon={<AccountIcon bankCode={account?.bankCode} />}
            title="내 계좌"
            subtitle={account?.bankName ?? '연동된 계좌 없음'}
            amount={account ? `${formatWonNumber(account.accountBalance)}원` : '-'}
            last
          />
        </View>
      ) : null}
    </View>
  );
}

type WalletPillProps = {
  label: string;
  onPress: () => void;
  /** 연한 연두 배경 (환전하기). 기본은 흰 배경 (도토리 충전) */
  tinted?: boolean;
};

/**
 * 잔액 카드 위 알약 버튼. shared/ui Button 대신 직접 그린다 —
 * 배경색이 버튼마다 다르게 고정돼야 하는데, Button 의 outline 배경은 눌림 상태를 따라가서
 * 눌림 타이밍에 따라 색이 흔들린다.
 */
function WalletPill({ label, onPress, tinted }: WalletPillProps) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.pill,
        {
          backgroundColor: tinted ? theme.color.brand.subtle : theme.color.background.surface,
          borderColor: theme.color.brand.solid,
          borderRadius: theme.radius.lg,
          paddingHorizontal: theme.space.md,
        },
      ]}
    >
      <Text style={[theme.typography.label, { color: theme.color.brand.solid }]}>{label}</Text>
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

/** 펼침 목록 한 줄 (예적금·투자·내 계좌 공용). */
function DetailRow({ icon, title, subtitle, amount, last }: DetailRowProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.detailRow,
        {
          paddingVertical: theme.space.lg,
          borderBottomWidth: last ? 0 : 1,
          borderBottomColor: theme.color.border.default,
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
      <Ionicons name="chevron-forward" size={16} color={theme.color.text.muted} />
    </View>
  );
}

/** 잔액 카드 아래 띠 색. 그라디언트 위에 흰색을 옅게 덮어 한 톤 밝게 만든다 */
const EXPAND_BAND_OVERLAY = 'rgba(255, 255, 255, 0.18)';

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
  card: { overflow: 'hidden' },
  inner: {},
  labelRow: { flexDirection: 'row', alignItems: 'center' },
  acornIcon: { width: 18, height: 18 },
  actionRow: { flexDirection: 'row' },
  action: { flex: 1 },
  pill: { minHeight: 46, alignItems: 'center', justifyContent: 'center', borderWidth: 1.5 },
  expandRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  detailSheet: { paddingHorizontal: 20 },
  detailRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  detailIcon: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
});
