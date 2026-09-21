import { router } from 'expo-router';
import { useMemo } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import {
  ChoreListRow,
  mockTodayChores,
  todayDate,
  useChoreRefresh,
  useTodayChores,
  type TodayChore,
} from '@/features/chore';
import { useTheme } from '@/shared/theme';
import { MockBadge } from '@/shared/ui';

type TodayChoresProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 제목 옆 "전체보기"와 각 항목 탭 — 미션 탭으로 보낸다 */
  onMorePress: () => void;
};

/** 미리보기라 앞의 몇 개만 보여 준다. 나머지는 미션 탭에 있다 */
const PREVIEW_COUNT = 3;

/**
 * 홈의 "오늘 등록된 미션" 섹션. 미션 탭의 목록을 앞부분만 미리 보여 주는 자리다.
 *
 * 행은 미션 탭과 같은 컴포넌트(ChoreListRow)를 쓴다 - 같은 미션이 화면마다 다르게
 * 생겼으면 아이가 같은 것인지 알아보기 어렵다. 그래서 제목·반복 주기·도토리가 똑같이
 * 나오고, 예전에 있던 아이콘 상자는 뺐다(목록에는 없던 장식이다).
 *
 * 완료한 미션은 뺀다. 미션 탭이 감추는 것을 홈에서만 보여 주면 두 화면의 개수가 어긋나고,
 * 여기 행에는 "완료" 표시가 없어 안 한 것처럼 보인다.
 *
 * 아직 등록된 미션이 없으면(또는 조회에 실패하면) 예시 데이터로 자리를 채운다 —
 * 화면 배치를 확인하려는 것이라 "예시" 표시를 함께 달아 실제 데이터와 구분한다.
 */
export function TodayChores({ familyId, childId, onMorePress }: TodayChoresProps) {
  const theme = useTheme();
  const { items, isPending, isError } = useTodayChores(familyId, childId);
  const childIds = useMemo(() => (childId != null ? [childId] : []), [childId]);

  // 홈으로 돌아올 때마다 미션 캐시를 턴다. 보상은 등록 직후가 아니라 AI 가 산정한 뒤에
  // 채워지는데, 탭 화면은 전환해도 언마운트되지 않고 staleTime 이 60초라 다시 와도
  // "산정 중"이 그대로 남아 있었다. 미션 탭은 이미 같은 훅을 쓴다.
  // (당겨서 새로고침은 홈에 없어 반환값은 쓰지 않는다)
  useChoreRefresh(familyId, childIds);

  const usingMock = !isPending && items.length === 0;
  const source = usingMock ? mockTodayChores : items;

  const rows = useMemo(() => registeredToday(source), [source]);

  // 상세는 미션 탭 안의 화면이다 (부모와 같은 구조). from=home 을 넘겨 그 화면의 뒤로가기가
  // 미션 목록이 아니라 홈으로 돌아오게 한다 - 여기서 들어갔으니 여기로 나와야 한다.
  const openDetail = (choreId: number) =>
    router.push({
      pathname: '/task/[choreId]',
      params: { choreId: String(choreId), from: 'home' },
    });

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <View style={[styles.titleRow, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            오늘 등록된 미션
          </Text>
          {usingMock ? <MockBadge /> : null}
        </View>
        <Pressable onPress={onMorePress} hitSlop={8} accessibilityRole="button">
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>전체보기 ›</Text>
        </Pressable>
      </View>

      {isPending ? (
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
                  // 예시 데이터는 서버에 없는 id 라 상세를 열 수 없다.
                  onPress={usingMock ? undefined : () => openDetail(chore.choreId)}
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
