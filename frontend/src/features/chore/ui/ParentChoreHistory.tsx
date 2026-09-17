import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Image, StyleSheet, Text, View, RefreshControl } from 'react-native';

import { summarizeWeek } from '@/features/chore/lib/choreHistory';
import {
  useChildrenCompletions,
  type ParentCompletion,
} from '@/features/chore/model/useChildrenCompletions';
import type { ChoreChild } from '@/features/chore/model/useChildrenChores';
import { ChoreHeroBanner } from '@/features/chore/ui/ChoreHeroBanner';
import { RewardChip } from '@/features/chore/ui/RewardChip';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import { ChildBadge, ContentCard, FilterChips, Header, Screen } from '@/shared/ui';

type ParentChoreHistoryProps = {
  familyId: number | undefined;
  childList: ChoreChild[];
};

/**
 * 완료 내역 (부모). 아이 여러 명의 지급 기록이 한 목록에 섞인다.
 *
 * 배너 수치는 "이번 주"이고 필터를 따라간다 - 민준을 고르면 민준 것만 센다. 필터를
 * 걸어 놓고 배너만 가족 전체를 보여 주면 두 숫자가 어긋나 보인다.
 *
 * 행에서 상세로 넘어가지 않는다. 이 응답은 포인트 원장에서 와서 완료 요청 ID 도
 * 사진도 없다 (아이 완료 이력과 같은 이유).
 */
export function ParentChoreHistory({ familyId, childList }: ParentChoreHistoryProps) {
  const theme = useTheme();
  const { refreshing, onRefresh } = useChoreRefresh();
  const [childId, setChildId] = useState<string | null>(null);
  const { items, isPending, isError } = useChildrenCompletions(familyId, childList);

  const filters = useMemo(
    () => childList.map((child) => ({ key: String(child.childId), label: child.name })),
    [childList],
  );

  const visible = useMemo(
    () => (childId == null ? items : items.filter((item) => String(item.childId) === childId)),
    [items, childId],
  );

  // 배너 수치는 필터를 따라간다 - 필터를 걸어 놓고 배너만 가족 전체를 세면 두 숫자가
  // 어긋나 보인다. 누구 것인지는 위의 칩이 이미 말해 주므로 문구에 이름을 넣지 않는다
  // (받침에 따라 조사가 갈려 '지호이가' 같은 말이 나온다).
  const week = useMemo(() => summarizeWeek(visible, new Date()), [visible]);

  return (
    <Screen
      header={
        <Header
          title="완료 이력"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/task'))}
        />
      }
      padded={false}
    >
      <FlatList
        data={visible}
        keyExtractor={(item, index) => `${item.choreId}-${item.processedAt}-${index}`}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space.md },
        ]}
        ListHeaderComponent={
          <View style={{ gap: theme.space.lg, marginBottom: theme.space.lg }}>
            <ChoreHeroBanner
              title={`이번 주에\n${week.count}개 해냈어요!`}
              caption={
                week.count > 0 ? `${week.total.toLocaleString('ko-KR')} 도토리를 줬어요` : undefined
              }
              mascot={require('@/assets/images/chore/owl-thumb.png')}
            />
            {childList.length > 1 ? (
              <FilterChips options={filters} selected={childId} onSelect={setChildId} />
            ) : null}
          </View>
        }
        renderItem={({ item }) => <CompletionRow completion={item} />}
        ListEmptyComponent={
          <View style={[styles.empty, { gap: theme.space.lg }]}>
            {!isPending && !isError ? (
              <Image
                source={require('@/assets/images/chore/icon-medal.png')}
                style={styles.mascot}
                resizeMode="contain"
              />
            ) : null}
            <Text
              style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}
            >
              {isPending
                ? '이력을 불러오는 중이에요.'
                : isError
                  ? '이력을 불러오지 못했어요.'
                  : childId != null
                    ? '이 아이의 완료 내역이 없어요.'
                    : '아직 완료한 미션이 없어요.'}
            </Text>
          </View>
        }
      />
    </Screen>
  );
}

function CompletionRow({ completion }: { completion: ParentCompletion }) {
  const theme = useTheme();

  return (
    <ContentCard
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: theme.space.md,
        paddingHorizontal: theme.space.lg,
        gap: theme.space.md,
      }}
    >
      <View style={styles.body}>
        <View style={[styles.titleRow, { gap: theme.space.xs }]}>
          <Text
            numberOfLines={1}
            style={[theme.typography.titleMd, styles.grow, { color: theme.color.text.strong }]}
          >
            {completion.choreTitle}
          </Text>
          <ChildBadge name={completion.childName} />
        </View>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          {formatProcessedAt(completion.processedAt)}
        </Text>
      </View>
      <RewardChip rewardPoints={completion.rewardPoints} compact />
    </ContentCard>
  );
}

/** '9월 12일 (토) 오후 6:23'. 기기 로컬 시간으로 보여 준다 */
function formatProcessedAt(processedAt: string): string {
  return new Date(processedAt).toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
    hour: 'numeric',
    minute: '2-digit',
  });
}

const styles = StyleSheet.create({
  content: { paddingVertical: 16, flexGrow: 1 },
  body: { flex: 1, gap: 4 },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  grow: { flex: 1 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
  mascot: { width: 120, height: 120 },
});
