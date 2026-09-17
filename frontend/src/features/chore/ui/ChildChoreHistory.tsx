import { router } from 'expo-router';
import { useMemo } from 'react';
import { FlatList, Image, StyleSheet, Text, View, RefreshControl } from 'react-native';

import { summarizeWeek } from '@/features/chore/lib/choreHistory';
import { useCompletions } from '@/features/chore/model/useCompletions';
import type { ChoreCompletionHistoryItem } from '@/features/chore/model/types';
import { ChoreHeroBanner } from '@/features/chore/ui/ChoreHeroBanner';
import { RewardChip } from '@/features/chore/ui/RewardChip';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import { ContentCard, Header, Screen } from '@/shared/ui';

type ChildChoreHistoryProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

/**
 * 아이가 보는 완료 이력. 기간을 안 넘겨 서버 기본값(최근 30일)을 그대로 쓴다.
 *
 * 행에서 상세로 넘어가지 않는다 - 이 응답은 포인트 원장에서 와서 완료 요청 ID 도 사진도
 * 없다. 눌러도 갈 곳이 없으므로 화살표를 두지 않았다.
 */
export function ChildChoreHistory({ familyId, childId }: ChildChoreHistoryProps) {
  const theme = useTheme();
  const { refreshing, onRefresh } = useChoreRefresh();
  const { data, isPending, isError } = useCompletions(familyId, childId);

  const completions = useMemo(() => data ?? [], [data]);
  const week = useMemo(() => summarizeWeek(completions, new Date()), [completions]);

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
        data={completions}
        keyExtractor={(item, index) => `${item.choreId}-${item.processedAt}-${index}`}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space.md },
        ]}
        ListHeaderComponent={
          <View style={{ marginBottom: theme.space.lg }}>
            <ChoreHeroBanner
              title={`이번 주에\n${week.count}개 해냈어요!`}
              caption={
                week.count > 0
                  ? `${week.total.toLocaleString('ko-KR')} 도토리를 모았어요`
                  : undefined
              }
              mascot={require('@/assets/images/chore/squirrel-jump.png')}
            />
          </View>
        }
        renderItem={({ item }) => <CompletionRow completion={item} />}
        ListEmptyComponent={
          <View style={[styles.empty, { gap: theme.space.lg }]}>
            {!isPending && !isError ? (
              <Image
                source={require('@/assets/images/chore/squirrel-think.png')}
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
                  : '아직 완료한 할 일이 없어요.\n첫 할 일에 도전해볼까요?'}
            </Text>
          </View>
        }
      />
    </Screen>
  );
}

function CompletionRow({ completion }: { completion: ChoreCompletionHistoryItem }) {
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
        <Text
          numberOfLines={1}
          style={[theme.typography.titleMd, { color: theme.color.text.strong }]}
        >
          {completion.choreTitle}
        </Text>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          {formatProcessedAt(completion.processedAt)}
        </Text>
      </View>
      <RewardChip rewardPoints={completion.rewardPoints} compact />
    </ContentCard>
  );
}

/** "9월 12일 (토) 오후 6:23". 기기 로컬 시간으로 보여 준다 */
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
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
  mascot: { width: 140, height: 140 },
});
