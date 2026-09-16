import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Image, StyleSheet, Text, View, RefreshControl } from 'react-native';

import { REPEAT_TYPES, repeatLabel, sumRewards } from '@/features/chore/lib/choreLabels';
import { useTodayChores } from '@/features/chore/model/useTodayChores';
import type { ChoreRepeatType } from '@/features/chore/model/types';
import { ChildChoreDetailModal } from '@/features/chore/ui/ChildChoreDetailModal';
import { ChoreFilterChips } from '@/features/chore/ui/ChoreFilterChips';
import { ChoreHeroBanner } from '@/features/chore/ui/ChoreHeroBanner';
import { ChoreListRow } from '@/features/chore/ui/ChoreListRow';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type ChildChoreListProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

const FILTERS = REPEAT_TYPES.map((key) => ({ key, label: repeatLabel(key) }));

/**
 * 아이의 할 일 목록 (할 일 탭). 탭 루트라 헤더에 뒤로가기를 두지 않는다.
 *
 * 서버가 활성 집안일만 주므로 기간이 지난 것은 애초에 내려오지 않는다 - 화면에서 따로
 * 거르지 않는다. 반복 주기 필터는 이미 받아 둔 목록을 클라이언트에서 거르는 것이다.
 */
export function ChildChoreList({ familyId, childId }: ChildChoreListProps) {
  const theme = useTheme();
  const { refreshing, onRefresh } = useChoreRefresh();
  const [repeatType, setRepeatType] = useState<ChoreRepeatType | null>(null);
  const [openChoreId, setOpenChoreId] = useState<number | null>(null);
  const { items, isPending, isError } = useTodayChores(familyId, childId);

  // 모달은 열려 있는 동안에도 목록이 갱신되면(요청 보냄·취소함) 같이 바뀌어야 한다.
  const openChore = items.find((chore) => chore.choreId === openChoreId);

  const visible = useMemo(
    () => (repeatType == null ? items : items.filter((chore) => chore.repeatType === repeatType)),
    [items, repeatType],
  );

  const { total, pendingCount } = useMemo(
    () => sumRewards(items.map((chore) => chore.rewardPoints)),
    [items],
  );

  return (
    <Screen
      header={
        <Header
          title="할 일"
          action={{ label: '완료 이력', onPress: () => router.push('/task/history') }}
        />
      }
      padded={false}
    >
      <FlatList
        data={visible}
        keyExtractor={(item) => String(item.choreId)}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space.md },
        ]}
        ListHeaderComponent={
          <View style={{ gap: theme.space.lg, marginBottom: theme.space.lg }}>
            <ChoreHeroBanner
              title={`해야 할 일이\n${items.length}개 남았어요!`}
              caption={rewardCaption(total, pendingCount)}
              mascot={require('@/assets/images/chore/squirrel-cheer.png')}
            />
            <ChoreFilterChips options={FILTERS} selected={repeatType} onSelect={setRepeatType} />
          </View>
        }
        renderItem={({ item }) => (
          <ChoreListRow
            title={item.title}
            repeatType={item.repeatType}
            rewardPoints={item.rewardPoints}
            pending={item.status === 'PENDING'}
            onPress={() => setOpenChoreId(item.choreId)}
          />
        )}
        ListEmptyComponent={
          <EmptyState
            message={
              isPending
                ? '할 일을 불러오는 중이에요.'
                : isError
                  ? '할 일을 불러오지 못했어요.'
                  : repeatType != null
                    ? '이 주기의 할 일이 없어요.'
                    : '아직 할 일이 없어요.\n부모님이 등록해 주실 거예요!'
            }
            showMascot={!isPending && !isError}
          />
        }
      />

      <ChildChoreDetailModal
        key={openChoreId}
        familyId={familyId}
        childId={childId}
        choreId={openChoreId}
        status={openChore?.status}
        onClose={() => setOpenChoreId(null)}
      />
    </Screen>
  );
}

/** 보상이 아직 산정 중인 건이 있으면 합계만 보여 주지 않는다 - 실제보다 적은 금액이 확정처럼 보인다 */
function rewardCaption(total: number, pendingCount: number): string | undefined {
  if (total === 0 && pendingCount === 0) return undefined;
  const sum = `다 하면 + ${total.toLocaleString('ko-KR')} 도토리`;
  return pendingCount > 0 ? `${sum} (${pendingCount}개는 계산 중)` : sum;
}

function EmptyState({ message, showMascot }: { message: string; showMascot: boolean }) {
  const theme = useTheme();

  return (
    <View style={[styles.empty, { gap: theme.space.lg }]}>
      {showMascot ? (
        <Image
          source={require('@/assets/images/chore/squirrel-flag.png')}
          style={styles.mascot}
          resizeMode="contain"
        />
      ) : null}
      <Text style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}>
        {message}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  content: { paddingVertical: 16, flexGrow: 1 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
  mascot: { width: 140, height: 140 },
});
