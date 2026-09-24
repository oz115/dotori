import { useMemo } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import {
  ChoreListRow,
  todayDate,
  useChoreRefresh,
  useTodayChores,
  type TodayChore,
} from '@/features/chore';
import { useTheme } from '@/shared/theme';

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
 * "오늘의 미션" 섹션. 자녀 홈의 "오늘 등록된 미션"과 같은 데이터(`useTodayChores`)를 쓰지만,
 * 부모 홈 전용으로 따로 둔다 — widgets는 서로를 import하지 않는 게 이 프로젝트 규칙이라
 * child-home 컴포넌트를 그대로 가져다 쓸 수 없다.
 *
 * 행은 두 홈과 미션 탭이 모두 ChoreListRow 를 쓴다. 부모와 아이가 같은 미션을 이야기할 때
 * 서로 다른 모양을 보고 있으면 말이 어긋난다.
 *
 * 오늘 등록된 걸 아이가 다 끝낸 경우와 애초에 등록된 게 없는 경우를 가른다 — 둘 다 목록은 비지만
 * 부모가 알아야 할 소식은 정반대다.
 *
 * "오늘 등록된"은 생성일(createdAt)이 아니라 활성 기간(startDate~endDate)에 오늘이 들어있는지로
 * 가른다. 반복 미션은 크론이 매일 자정 기간을 오늘로 밀어 주는데, createdAt 은 최초 등록일에
 * 고정돼 있어서 그 기준으로 걸렀으면 등록 다음 날부터 반복 미션이 영원히 이 자리에서 사라진다.
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
  const childIds = useMemo(() => (childId != null ? [childId] : []), [childId]);

  // 홈으로 돌아올 때마다 미션 캐시를 턴다. 보상은 등록 직후가 아니라 AI 가 산정한 뒤에
  // 채워지는데, 탭 화면은 전환해도 언마운트되지 않고 staleTime 이 60초라 다시 와도
  // "산정 중"이 그대로 남아 있었다. 미션 탭은 이미 같은 훅을 쓴다.
  useChoreRefresh(familyId, childIds);

  // 아이가 없으면 useTodayChores 가 아예 돌지 않아 isPending 이 계속 true 라,
  // "아이 없음"과 "불러오는 중"을 따로 봐야 한다
  const loading = familyPending || (childId != null && isPending);
  const todayChores = useMemo(() => activeToday(items), [items]);
  const rows = useMemo(() => unfinishedPreview(todayChores), [todayChores]);
  // 오늘 등록된 걸 다 끝내면 rows 가 비는데, 그때 "없어요"라고 하면 방금 해낸 일을 부정하는 말이 된다.
  const allDone = rows.length === 0 && todayChores.length > 0;

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <View style={[styles.titleRow, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            오늘의 미션
          </Text>
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
              미션을 불러오지 못했어요. 잠시 후 다시 시도해주세요.
            </Text>
          ) : null}
          {rows.length === 0 ? (
            <Text
              style={[
                theme.typography.bodySm,
                { color: allDone ? theme.color.brand.subtleText : theme.color.text.muted },
              ]}
            >
              {allDone ? '오늘 등록된 미션을 모두 완료했어요!' : '오늘 등록된 미션이 없어요.'}
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
                  onPress={() => onChorePress(chore)}
                />
              ))}
            </View>
          )}
        </>
      )}
    </View>
  );
}

/**
 * 오늘이 활성 기간(startDate~endDate)에 들어있는 것 전부. 끝낸 것까지 남겨야
 * "없다"와 "다 했다"를 가를 수 있다.
 */
function activeToday(chores: TodayChore[]): TodayChore[] {
  const today = todayDate();
  return chores.filter((chore) => chore.startDate <= today && today <= chore.endDate);
}

/** 그중 아직 안 끝난 것을, 최근 등록 순으로 앞의 몇 개 */
function unfinishedPreview(chores: TodayChore[]): TodayChore[] {
  return [...chores]
    .filter((chore) => chore.status !== 'DONE')
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .slice(0, PREVIEW_COUNT);
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
});
