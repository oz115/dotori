import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { mockChildBalance, useWalletBalance } from '@/features/wallet';
import { formatPoint } from '@/shared/lib/money';
import { fontFamily, useTheme } from '@/shared/theme';
import { MockBadge } from '@/shared/ui';

type ChildBalanceCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  childName: string | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 — 이때는 예시로 넘어가지 않는다 */
  familyPending: boolean;
  /** 환전 요청 처리 화면으로 — 지금은 승인 대기 목록(할 일 탭)으로 보낸다 */
  onCashout: () => void;
};

/**
 * 부모 홈 상단 카드. 자녀 홈의 잔액 카드와 같은 자리지만 보여주는 값이 다르다 —
 * 부모가 가장 먼저 알고 싶은 건 자기 돈이 아니라 "우리 아이가 지금 얼마를 갖고 있나" 다.
 * 도토리 브라운(accent.acorn)을 써서 바로 아래 내 계좌 카드(은행 브랜드 컬러)와 구분한다.
 */
export function ChildBalanceCard({
  familyId,
  childId,
  childName,
  familyPending,
  onCashout,
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
    <View
      style={[
        styles.card,
        { backgroundColor: theme.color.accent.acorn, borderRadius: theme.radius.lg },
      ]}
    >
      <View style={{ padding: theme.space.lg, gap: theme.space.sm }}>
        <View style={styles.headRow}>
          <View style={{ flex: 1, gap: theme.space.xs }}>
            <View style={[styles.labelRow, { gap: theme.space.xs }]}>
              <Text
                style={[
                  theme.typography.caption,
                  { color: theme.color.text.onBrand, opacity: 0.85 },
                ]}
              >
                <Text style={{ fontFamily: fontFamily.medium }}>자녀 보유 도토리</Text>
                {'\n'}
                <Text style={{ fontFamily: fontFamily.bold }}>{childName ?? '우리 아이'}</Text>
              </Text>
              {usingMock ? <MockBadge /> : null}
            </View>
            <Text style={[theme.typography.display, { color: theme.color.text.onBrand }]}>
              {loading ? '—' : formatPoint(amount)}
            </Text>
          </View>

          <View style={[styles.actionsRow, { gap: theme.space.sm }]}>
            <CardPill label="환전하기" onPress={onCashout} />
            <Image
              source={require('@/assets/images/icons/icon-dotori.png')}
              style={styles.dotoriIcon}
              resizeMode="contain"
            />
          </View>
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
          borderColor: theme.color.accent.acorn,
          borderRadius: theme.radius.lg,
          paddingHorizontal: theme.space.md,
        },
      ]}
    >
      <Text style={[theme.typography.label, { color: theme.color.accent.acorn }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { overflow: 'hidden' },
  headRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  labelRow: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap' },
  actionsRow: { flexDirection: 'row', alignItems: 'center' },
  dotoriIcon: { width: 40, height: 40 },
  pill: { minHeight: 46, alignItems: 'center', justifyContent: 'center', borderWidth: 1.5 },
});
