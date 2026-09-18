import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import {
  ChildChoreDetailModal,
  ChoreListRow,
  mockTodayChores,
  todayDate,
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
  const [openChoreId, setOpenChoreId] = useState<number | null>(null);

  const usingMock = !isPending && items.length === 0;
  const source = usingMock ? mockTodayChores : items;

  const rows = useMemo(() => registeredToday(source), [source]);

  // 모달이 열린 사이에 목록이 갱신돼도(요청 보냄·취소함) 상태가 따라가야 한다.
  const openChore = source.find((chore) => chore.choreId === openChoreId);

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
                  onPress={usingMock ? undefined : () => setOpenChoreId(chore.choreId)}
                />
              ))}
            </View>
          )}
        </>
      )}

      {/* 홈에서 바로 상세를 연다. 미션 탭으로 보내면 아이가 돌아올 길을 스스로 찾아야 하는데,
          이 모달은 닫으면 그대로 홈이라 "눌렀다가 닫으면 제자리"가 성립한다. */}
      <ChildChoreDetailModal
        key={openChoreId}
        familyId={familyId}
        childId={childId}
        choreId={openChoreId}
        status={openChore?.status}
        onClose={() => setOpenChoreId(null)}
      />
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
