import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { mockTodayChores, useTodayChores } from '@/features/chore';
import { useTheme } from '@/shared/theme';
import { MockBadge, ProgressBar } from '@/shared/ui';

type ChildSummaryCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  childName: string | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 — 이때는 예시로 넘어가지 않는다 */
  familyPending: boolean;
  /** 할 일 탭으로 보낸다 */
  onPress: () => void;
};

/**
 * 자녀의 오늘 할 일 진척도. 보유 도토리는 상단 카드(ChildBalanceCard)가 맡고
 * 여기서는 "오늘 얼마나 했나"만 보여준다. 자세한 목록은 탭에서 본다.
 *
 * 등록된 집안일이 없거나 아직 아이를 연결하지 않았으면 예시로 자리를 채운다 —
 * 아이 미연결 안내는 상단 카드가 이미 하고 있어 여기서 반복하지 않는다.
 */
export function ChildSummaryCard({
  familyId,
  childId,
  childName,
  familyPending,
  onPress,
}: ChildSummaryCardProps) {
  const theme = useTheme();
  const chores = useTodayChores(familyId, childId);

  // 아이가 없으면 useTodayChores 가 아예 돌지 않아 isPending 이 계속 true 라,
  // "아이 없음"과 "불러오는 중"을 따로 봐야 한다
  const loading = familyPending || (childId != null && chores.isPending);
  const usingMock = !loading && chores.items.length === 0;
  const rows = usingMock ? mockTodayChores : chores.items;

  const total = rows.length;
  const done = rows.filter((c) => c.status === 'DONE').length;
  const progress = total === 0 ? 0 : done / total;

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={[styles.titleRow, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          우리 아이
        </Text>
        {usingMock ? <MockBadge /> : null}
      </View>

      <Pressable
        onPress={onPress}
        accessibilityRole="button"
        style={[
          styles.card,
          {
            borderColor: theme.color.border.default,
            borderRadius: theme.radius.lg,
            padding: theme.space.lg,
            gap: theme.space.md,
          },
        ]}
      >
        <View style={styles.headRow}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            {childName ?? '우리 아이'}
          </Text>
          <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
        </View>

        <View style={{ gap: theme.space.xs }}>
          <View style={styles.headRow}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
              오늘 할 일
            </Text>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.default }]}>
              {loading ? '—' : `${done} / ${total}`}
            </Text>
          </View>
          <ProgressBar value={progress} />
        </View>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  card: { borderWidth: 1 },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  headRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
