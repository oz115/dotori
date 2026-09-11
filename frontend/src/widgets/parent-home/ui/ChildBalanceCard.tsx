import { LinearGradient } from 'expo-linear-gradient';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { mockChildBalance, useWalletBalance } from '@/features/wallet';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { MockBadge } from '@/shared/ui';

type ChildBalanceCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  childName: string | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 — 이때는 예시로 넘어가지 않는다 */
  familyPending: boolean;
  onGrant: () => void;
};

/**
 * 부모 홈 상단 카드. 자녀 홈의 잔액 카드와 같은 자리·같은 모양이지만 보여주는 값이 다르다 —
 * 부모가 가장 먼저 알고 싶은 건 자기 돈이 아니라 "우리 아이가 지금 얼마를 갖고 있나" 다.
 */
export function ChildBalanceCard({
  familyId,
  childId,
  childName,
  familyPending,
  onGrant,
}: ChildBalanceCardProps) {
  const theme = useTheme();
  const balance = useWalletBalance(familyId, childId);

  const hasChild = childId != null;
  const loading = familyPending || (hasChild && balance.isPending);

  // 아이를 아직 연결하지 않았거나 지갑 조회가 실패하면 예시 금액으로 자리를 채운다.
  // 잔액 0 은 "아직 안 모았다"는 실제 정보라 예시로 덮지 않는다.
  const usingMock = !loading && balance.data == null;
  const amount = usingMock ? mockChildBalance : (balance.data?.balance ?? 0);

  return (
    <View style={[styles.card, { borderRadius: theme.radius.lg }]}>
      <LinearGradient
        colors={theme.palette.gradient.deep}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={StyleSheet.absoluteFill}
      />
      <View style={{ padding: theme.space['2xl'], gap: theme.space.sm }}>
        <View style={[styles.labelRow, { gap: theme.space.xs }]}>
          <Image
            source={require('@/assets/images/icons/icon-dotori.png')}
            style={styles.acornIcon}
            resizeMode="contain"
          />
          <Text
            style={[theme.typography.caption, { color: theme.color.text.onBrand, opacity: 0.85 }]}
          >
            {hasChild ? `${childName ?? '우리 아이'}의 도토리` : '우리 아이의 도토리'}
          </Text>
          {usingMock ? <MockBadge /> : null}
        </View>

        {/* 금액과 버튼을 한 줄에 — 버튼이 하나뿐이라 아래로 내리면 카드가 허전해진다 */}
        <View style={styles.amountRow}>
          <Text style={[theme.typography.display, { color: theme.color.text.onBrand }]}>
            {loading ? '—' : formatPoint(amount)}
          </Text>
          <CardPill label="용돈 주기" onPress={onGrant} />
        </View>

        {!loading && !hasChild ? (
          <Text style={[theme.typography.caption, { color: theme.color.text.onBrand }]}>
            아직 연결된 아이가 없어요. 가족 관리에서 초대 코드를 받아 알려주세요.
          </Text>
        ) : null}
      </View>
    </View>
  );
}

type CardPillProps = {
  label: string;
  onPress: () => void;
  tinted?: boolean;
};

function CardPill({ label, onPress, tinted }: CardPillProps) {
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

const styles = StyleSheet.create({
  card: { overflow: 'hidden' },
  labelRow: { flexDirection: 'row', alignItems: 'center' },
  acornIcon: { width: 18, height: 18 },
  amountRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  pill: { minHeight: 46, alignItems: 'center', justifyContent: 'center', borderWidth: 1.5 },
});
