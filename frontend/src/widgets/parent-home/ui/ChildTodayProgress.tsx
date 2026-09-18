import { useMemo } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import {
  ChoreListRow,
  mockTodayChores,
  todayDate,
  useTodayChores,
  type TodayChore,
} from '@/features/chore';
import { useTheme } from '@/shared/theme';
import { MockBadge } from '@/shared/ui';

type ChildTodayProgressProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 — 이때는 예시로 넘어가지 않는다 */
  familyPending: boolean;
  /** "전체보기" — 미션 탭으로 보낸다 */
  onMorePress: () => void;
  /** 항목 탭 — 그 미션의 상세로 보낸다 */
  onChorePress: (chore: TodayChore) => void;
};

/** 미리보기라 앞의 몇 개만 보여 준다. 나머지는 미션 탭에 있다 */
const PREVIEW_COUNT = 3;

/**
 * "미션 진척도" 섹션. 자녀 홈의 "오늘 등록된 미션"과 같은 데이터(`useTodayChores`)를 쓰지만,
 * 부모 홈 전용으로 따로 둔다 — widgets는 서로를 import하지 않는 게 이 프로젝트 규칙이라
 * child-home 컴포넌트를 그대로 가져다 쓸 수 없다.
 *
 * 행은 두 홈과 미션 탭이 모두 ChoreListRow 를 쓴다. 부모와 아이가 같은 미션을 이야기할 때
 * 서로 다른 모양을 보고 있으면 말이 어긋난다.
 *
 * 등록된 미션이 없거나 아직 아이를 연결하지 않았으면 예시로 자리를 채운다.
 */
export function ChildTodayProgress({
  familyId,
  childId,
  familyPending,
  onMorePress,
  onChorePress,
}: ChildTodayProgressProps) {
  const theme = useTheme();
  const { items, isPending, isError } = useTodayChores(familyId, childId);

  // 아이가 없으면 useTodayChores 가 아예 돌지 않아 isPending 이 계속 true 라,
  // "아이 없음"과 "불러오는 중"을 따로 봐야 한다
  const loading = familyPending || (childId != null && isPending);
  const usingMock = !loading && items.length === 0;
  const source = usingMock ? mockTodayChores : items;

  const rows = useMemo(() => registeredToday(source), [source]);

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <View style={[styles.titleRow, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            미션 진척도
          </Text>
          {usingMock ? <MockBadge /> : null}
        </View>
        <Pressable onPress={onMorePress} hitSlop={8} accessibilityRole="button">
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>전체보기 ›</Text>
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
          {rows.length === 0 ? (
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              오늘 등록된 미션이 없어요.
            </Text>
          ) : (
            <View style={{ gap: theme.space.sm }}>
              {rows.map((chore) => (
                <ChoreListRow
                  key={chore.choreId}
                  title={chore.title}
                  repeatType={chore.repeatType}
                  rewardPoints={chore.rewardPoints}
                  status={chore.status}
                  onPress={usingMock ? undefined : () => onChorePress(chore)}
                />
              ))}
            </View>
          )}
        </>
      )}
    </View>
  );
}

/** 오늘 등록된 것 중 아직 안 끝난 것을, 최근 등록 순으로 앞의 몇 개 */
function registeredToday(chores: TodayChore[]): TodayChore[] {
  const today = todayDate();
  return [...chores]
    .filter((chore) => chore.status !== 'DONE' && chore.createdAt.slice(0, 10) === today)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .slice(0, PREVIEW_COUNT);
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
});
