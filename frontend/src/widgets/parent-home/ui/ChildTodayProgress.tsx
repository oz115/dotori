import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { mockTodayChores, useTodayChores, type TodayChore } from '@/features/chore';
import { useTheme } from '@/shared/theme';
import { ContentCard, MockBadge } from '@/shared/ui';

type ChildTodayProgressProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 — 이때는 예시로 넘어가지 않는다 */
  familyPending: boolean;
  /** "더보기"·각 항목 탭 — 할 일 탭으로 보낸다 */
  onMorePress: () => void;
};

/**
 * "미션 진척도" 섹션. 자녀 홈의 "오늘의 미션"과 같은 데이터(`useTodayChores`)를 쓰지만,
 * 부모 홈 전용으로 따로 둔다 — widgets는 서로를 import하지 않는 게 이 프로젝트 규칙이라
 * child-home 컴포넌트를 그대로 가져다 쓸 수 없다.
 *
 * 등록된 집안일이 없거나 아직 아이를 연결하지 않았으면 예시로 자리를 채운다.
 */
export function ChildTodayProgress({
  familyId,
  childId,
  familyPending,
  onMorePress,
}: ChildTodayProgressProps) {
  const theme = useTheme();
  const { items, isPending, isError } = useTodayChores(familyId, childId);

  // 아이가 없으면 useTodayChores 가 아예 돌지 않아 isPending 이 계속 true 라,
  // "아이 없음"과 "불러오는 중"을 따로 봐야 한다
  const loading = familyPending || (childId != null && isPending);
  const usingMock = !loading && items.length === 0;
  const rows = usingMock ? mockTodayChores : items;

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <View style={[styles.titleRow, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            미션 진척도
          </Text>
          {usingMock ? <MockBadge /> : null}
        </View>
        <Pressable onPress={onMorePress} hitSlop={8}>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>더보기</Text>
        </Pressable>
      </View>

      {loading ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          불러오는 중...
        </Text>
      ) : (
        <>
          {isError ? (
            <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
              미션을 불러오지 못해 예시를 보여주고 있어요.
            </Text>
          ) : null}
          <View style={{ gap: theme.space.sm }}>
            {rows.map((chore) => (
              <ChoreRow key={chore.choreId} chore={chore} onPress={onMorePress} />
            ))}
          </View>
        </>
      )}
    </View>
  );
}

function ChoreRow({ chore, onPress }: { chore: TodayChore; onPress: () => void }) {
  const theme = useTheme();
  const done = chore.status === 'DONE';
  const pending = chore.status === 'PENDING';

  const icon = done ? 'checkmark-circle' : pending ? 'time-outline' : 'help-circle-outline';
  const trailing = pending ? '승인 대기' : rewardLabel(chore.rewardPoints);
  const trailingColor = pending ? theme.color.text.muted : theme.color.status.success.text;

  return (
    <Pressable onPress={onPress} accessibilityRole="button">
      <ContentCard style={[styles.row, { padding: theme.space.md, gap: theme.space.md }]}>
        <View
          style={[
            styles.iconBox,
            { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.md },
          ]}
        >
          <Ionicons name={icon} size={20} color={theme.color.brand.solid} />
        </View>
        <Text
          style={[theme.typography.body, { color: theme.color.text.strong, flex: 1 }]}
          numberOfLines={1}
        >
          {chore.title}
        </Text>
        <Text style={[theme.typography.label, { color: trailingColor }]}>{trailing}</Text>
      </ContentCard>
    </Pressable>
  );
}

function rewardLabel(rewardPoints: number | null): string {
  return rewardPoints == null ? '산정 중' : `+${rewardPoints}P`;
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  row: { flexDirection: 'row', alignItems: 'center' },
  iconBox: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
});
