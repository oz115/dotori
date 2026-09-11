import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { mockPendingApprovals, usePendingApprovals } from '@/features/chore';
import { mockCashoutRequests, useCashoutRequests } from '@/features/wallet';
import { useTheme } from '@/shared/theme';
import { MockBadge } from '@/shared/ui';

type ApprovalsCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 — 이때는 예시로 넘어가지 않는다 */
  familyPending: boolean;
  /** 목록 화면(할 일 탭)으로 보낸다 */
  onOpenChores: () => void;
  onOpenCashouts: () => void;
};

/**
 * 부모 홈의 핵심 섹션. 아이가 올린 집안일 완료 인증과 환전 요청은 부모가 승인해야
 * 다음으로 넘어가므로, 홈에서 바로 몇 건이 밀려 있는지 보이게 한다.
 *
 * 홈은 요약만 맡고 실제 처리(사진 확인·승인·거절)는 탭 안 목록 화면이 한다.
 */
export function ApprovalsCard({
  familyId,
  childId,
  familyPending,
  onOpenChores,
  onOpenCashouts,
}: ApprovalsCardProps) {
  const theme = useTheme();
  const chores = usePendingApprovals(familyId, childId);
  const cashouts = useCashoutRequests(familyId, childId, 'PENDING');

  // 아이가 없으면 두 조회가 아예 돌지 않아 isPending 이 계속 true 라,
  // "아이 없음"과 "불러오는 중"을 따로 봐야 한다
  const isPending = familyPending || (childId != null && (chores.isPending || cashouts.isPending));
  const isError = chores.isError || cashouts.isError;

  // 대기 건이 하나도 없으면(아이 미연결·조회 실패 포함) 예시로 자리를 채운다 —
  // 그러지 않으면 이 섹션이 부모 홈에서 가장 중요한데도 늘 빈 칸으로만 보인다
  const usingMock = !isPending && (chores.data?.length ?? 0) + (cashouts.data?.length ?? 0) === 0;
  const choreCount = usingMock ? mockPendingApprovals.length : (chores.data?.length ?? 0);
  const cashoutCount = usingMock ? mockCashoutRequests.length : (cashouts.data?.length ?? 0);
  const total = choreCount + cashoutCount;

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headRow}>
        <View style={[styles.titleRow, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            확인해주세요
          </Text>
          {usingMock ? <MockBadge /> : null}
        </View>
        {!isPending && !usingMock && total > 0 ? (
          <View
            style={[
              styles.badge,
              {
                backgroundColor: theme.color.status.danger.subtle,
                borderRadius: theme.radius.full,
              },
            ]}
          >
            <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
              {total}건
            </Text>
          </View>
        ) : null}
      </View>

      {isPending ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          불러오는 중...
        </Text>
      ) : (
        <View style={{ gap: theme.space.sm }}>
          {isError ? (
            <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
              승인 대기 목록을 불러오지 못해 예시를 보여주고 있어요.
            </Text>
          ) : null}
          {choreCount > 0 ? (
            <ApprovalRow
              icon="checkmark-done-outline"
              title="집안일 완료 승인"
              description="아이가 올린 인증을 확인해주세요"
              count={choreCount}
              onPress={onOpenChores}
            />
          ) : null}
          {cashoutCount > 0 ? (
            <ApprovalRow
              icon="swap-horizontal-outline"
              title="환전 요청"
              description="승인하면 아이 계좌로 송금돼요"
              count={cashoutCount}
              onPress={onOpenCashouts}
            />
          ) : null}
        </View>
      )}
    </View>
  );
}

type ApprovalRowProps = {
  icon: keyof typeof Ionicons.glyphMap;
  title: string;
  description: string;
  count: number;
  onPress: () => void;
};

function ApprovalRow({ icon, title, description, count, onPress }: ApprovalRowProps) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      style={[
        styles.row,
        {
          borderColor: theme.color.brand.solid,
          backgroundColor: theme.color.brand.tint,
          borderRadius: theme.radius.lg,
          padding: theme.space.md,
          gap: theme.space.md,
        },
      ]}
    >
      <View
        style={[
          styles.iconBox,
          { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.md },
        ]}
      >
        <Ionicons name={icon} size={20} color={theme.color.brand.solid} />
      </View>
      <View style={{ flex: 1 }}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>{title}</Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>
          {description}
        </Text>
      </View>
      <Text style={[theme.typography.label, { color: theme.color.brand.subtleText }]}>
        {count}건
      </Text>
      <Ionicons name="chevron-forward" size={16} color={theme.color.text.muted} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  headRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  badge: { paddingHorizontal: 10, paddingVertical: 3 },
  row: { flexDirection: 'row', alignItems: 'center', borderWidth: 1.5 },
  iconBox: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
});
