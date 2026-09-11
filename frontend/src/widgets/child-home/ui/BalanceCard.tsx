import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useState } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { formatPoint } from '@/shared/lib/money';

type BalanceCardProps = {
  balance: number | undefined;
  isPending: boolean;
  onCharge: () => void;
  onCashout: () => void;
};

/**
 * 잔액 카드. 예적금·투자는 아직 없는 도메인이라 "내 계좌보기"를 펼치면
 * 실제 목록 대신 준비 중 안내만 보여준다.
 */
export function BalanceCard({ balance, isPending, onCharge, onCashout }: BalanceCardProps) {
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
      <View style={[styles.inner, { padding: theme.space.lg, gap: theme.space.md }]}>
        <View style={styles.topRow}>
          <View style={{ gap: theme.space.xs }}>
            <View style={[styles.labelRow, { gap: theme.space.xs }]}>
              <Image
                source={require('@/assets/images/icons/icon-dotori.png')}
                style={styles.acornIcon}
                resizeMode="contain"
              />
              <Text
                style={[
                  theme.typography.bodySm,
                  { color: theme.color.text.onBrand, opacity: 0.85 },
                ]}
              >
                현재 사용 가능 도토리
              </Text>
            </View>
            <Text style={[theme.typography.h1, { color: theme.color.text.onBrand }]}>
              {isPending ? '—' : formatPoint(balance ?? 0)}
            </Text>
          </View>

          <View style={{ gap: theme.space.sm }}>
            <WalletPill label="도토리 충전" onPress={onCharge} />
            <WalletPill label="환전하기" onPress={onCashout} />
          </View>
        </View>

        <View
          style={[styles.divider, { backgroundColor: theme.color.text.onBrand, opacity: 0.25 }]}
        />

        <Pressable
          onPress={() => setExpanded((v) => !v)}
          style={[styles.expandRow, { gap: theme.space.xs }]}
          accessibilityRole="button"
          accessibilityState={{ expanded }}
        >
          <Text
            style={[theme.typography.bodySm, { color: theme.color.text.onBrand, opacity: 0.85 }]}
          >
            예적금 · 투자 내 계좌보기
          </Text>
          <Ionicons
            name={expanded ? 'chevron-up' : 'chevron-down'}
            size={16}
            color={theme.color.text.onBrand}
          />
        </Pressable>

        {expanded ? (
          <Text
            style={[
              theme.typography.bodySm,
              { color: theme.color.text.onBrand, opacity: 0.85, textAlign: 'center' },
            ]}
          >
            예적금·투자는 곧 만나볼 수 있어요.
          </Text>
        ) : null}
      </View>
    </View>
  );
}

type WalletPillProps = {
  label: string;
  onPress: () => void;
};

/**
 * 잔액 카드 위 흰 알약 버튼. shared/ui Button 대신 직접 그린다 —
 * 둘 다 항상 같은 흰 배경이어야 하는데, Button의 outline 배경은 눌림 상태를 따라가서
 * 두 버튼의 눌림 타이밍이 어긋나면 순간적으로 서로 다른 색으로 보일 수 있다.
 */
function WalletPill({ label, onPress }: WalletPillProps) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.pill,
        {
          backgroundColor: theme.color.background.surface,
          borderColor: theme.color.brand.solid,
          borderRadius: theme.radius.sm,
          paddingHorizontal: theme.space.md,
        },
      ]}
    >
      <Text style={[theme.typography.label, { color: theme.color.brand.solid }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { overflow: 'hidden' },
  inner: {},
  topRow: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between' },
  labelRow: { flexDirection: 'row', alignItems: 'center' },
  acornIcon: { width: 18, height: 18 },
  pill: { minHeight: 36, alignItems: 'center', justifyContent: 'center', borderWidth: 1.5 },
  divider: { height: 1 },
  expandRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
});
