import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View, RefreshControl } from 'react-native';

import { REPEAT_TYPES, repeatLabel, sumRewards } from '@/features/chore/lib/choreLabels';
import { useTodayChores } from '@/features/chore/model/useTodayChores';
import type { ChoreRepeatType } from '@/features/chore/model/types';
import { ChildChoreDetailModal } from '@/features/chore/ui/ChildChoreDetailModal';
import { ChoreHeroBanner } from '@/features/chore/ui/ChoreHeroBanner';
import { ChoreListRow } from '@/features/chore/ui/ChoreListRow';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import { FilterChips, Header, Screen } from '@/shared/ui';

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
  const { items, remainingCount, isPending, isError } = useTodayChores(familyId, childId);

  // 모달은 열려 있는 동안에도 목록이 갱신되면(요청 보냄·취소함) 같이 바뀌어야 한다.
  // items 는 완료한 것까지 들고 있다 - 모달이 열린 사이에 부모가 승인해도 상태를 읽을 수
  // 있어야 하므로, 걸러내는 일은 목록에만 적용한다.
  const openChore = items.find((chore) => chore.choreId === openChoreId);

  // 지급까지 끝난 미션은 목록에서 뺀다. 아이가 다시 할 것이 없는데 남겨 두면 "해야 할
  // 미션"에 섞여 무엇이 남았는지 흐려지고, 눌러 봐야 아무것도 못 한다. 기록은 완료
  // 이력에 있다. 반복 미션은 다음 주기가 시작되면 TODO 로 돌아와 다시 나타난다.
  const active = useMemo(() => items.filter((chore) => chore.status !== 'DONE'), [items]);

  const visible = useMemo(
    () => (repeatType == null ? active : active.filter((chore) => chore.repeatType === repeatType)),
    [active, repeatType],
  );

  const { total, pendingCount } = useMemo(
    () => sumRewards(active.map((chore) => chore.rewardPoints)),
    [active],
  );

  // 상점 탭의 "구매 내역"(StoreCatalog)과 같은 pill 이다. 두 탭이 헤더 같은 자리에 같은
  // 성격의 버튼을 두는데 한쪽만 맨 텍스트라 탭을 옮길 때마다 어긋나 보였다. 값(pill 배경·
  // 여백·caption)은 StoreCatalog 쪽을 그대로 따른다 - 세 번째 화면이 생기면 shared 로 올린다.
  const historyButton = (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel="완료 이력"
      onPress={() => router.push('/task/history')}
      hitSlop={8}
      style={[
        styles.historyChip,
        {
          borderRadius: theme.radius.full,
          backgroundColor: theme.color.background.surfaceSunken,
        },
      ]}
    >
      <Text
        numberOfLines={1}
        style={[theme.typography.caption, { color: theme.color.text.default }]}
      >
        완료 이력
      </Text>
    </Pressable>
  );

  return (
    <Screen header={<Header title="미션" right={historyButton} />} padded={false}>
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
              title={`해야 할 미션이\n${remainingCount}개 남았어요!`}
              caption={rewardCaption(total, pendingCount)}
              mascot={require('@/assets/images/chore/squirrel-cheer.png')}
            />
            <FilterChips options={FILTERS} selected={repeatType} onSelect={setRepeatType} />
          </View>
        }
        renderItem={({ item }) => (
          <ChoreListRow
            title={item.title}
            repeatType={item.repeatType}
            rewardPoints={item.rewardPoints}
            status={item.status}
            onPress={() => setOpenChoreId(item.choreId)}
          />
        )}
        ListEmptyComponent={
          <EmptyState
            message={
              isPending
                ? '미션을 불러오는 중이에요.'
                : isError
                  ? '미션을 불러오지 못했어요.'
                  : repeatType != null
                    ? '이 주기의 미션이 없어요.'
                    : // 등록된 미션이 있는데 목록이 비었으면 전부 끝낸 것이다.
                      // "아직 미션이 없어요" 를 내보내면 방금 해낸 일을 부정하는 말이 된다.
                      items.length > 0
                      ? '미션을 모두 해냈어요!\n완료 이력에서 볼 수 있어요.'
                      : '아직 미션이 없어요.\n부모님이 등록해 주실 거예요!'
            }
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

function EmptyState({ message }: { message: string }) {
  const theme = useTheme();

  return (
    <View style={styles.empty}>
      <Text style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}>
        {message}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  // 위쪽 여백을 주지 않는다. Screen 이 padded={false} 여도 paddingVertical(16)은 항상
  // 넣기 때문에, 여기서 또 주면 헤더와 배너 사이가 32px 이 되어 상점(16px)보다 벌어진다.
  content: { paddingBottom: 16, flexGrow: 1 },
  historyChip: { paddingHorizontal: 12, paddingVertical: 6 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
});
