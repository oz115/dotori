import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Image, Pressable, StyleSheet, Text, View, RefreshControl } from 'react-native';

import { matchCompletionPhotos, summarizeWeek } from '@/features/chore/lib/choreHistory';
import { useApprovedRequests } from '@/features/chore/model/useApprovedRequests';
import { useCompletions } from '@/features/chore/model/useCompletions';
import type { ChoreCompletionHistoryItem } from '@/features/chore/model/types';
import { ChoreHeroBanner } from '@/features/chore/ui/ChoreHeroBanner';
import {
  CompletionHistoryModal,
  type CompletionHistoryDetail,
} from '@/features/chore/ui/CompletionHistoryModal';
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
 * 행을 누르면 그때 보낸 사진과 쓴 내용을 다시 본다. 이 응답(포인트 원장)에는 사진이 없어서
 * 승인된 완료 요청을 따로 받아 짝지어 붙인다 (matchCompletionPhotos).
 */
export function ChildChoreHistory({ familyId, childId }: ChildChoreHistoryProps) {
  const theme = useTheme();
  const childIds = useMemo(() => (childId != null ? [childId] : []), [childId]);
  const { refreshing, onRefresh } = useChoreRefresh(familyId, childIds);
  const { data, isPending, isError } = useCompletions(familyId, childId);
  const { data: approved } = useApprovedRequests(familyId, childId);
  const [detail, setDetail] = useState<CompletionHistoryDetail | null>(null);

  const completions = useMemo(() => data ?? [], [data]);
  const week = useMemo(() => summarizeWeek(completions, new Date()), [completions]);
  // 사진은 늦게 와도 목록은 먼저 그린다 - 아직 없으면 행이 "사진을 찾지 못했어요" 로 열린다
  const rows = useMemo(
    () => matchCompletionPhotos(completions, approved ?? []),
    [completions, approved],
  );

  return (
    <Screen
      header={
        <Header
          title="완료 이력"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/task'))}
        />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      <FlatList
        data={rows}
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
              mascot={require('@/assets/images/chore/squirrel-win.png')}
            />
          </View>
        }
        renderItem={({ item }) => (
          <CompletionRow
            completion={item}
            onPress={() =>
              setDetail({
                choreTitle: item.choreTitle,
                processedAt: item.processedAt,
                rewardPoints: item.rewardPoints,
                photo: item.photo,
              })
            }
          />
        )}
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
                  : '아직 완료한 미션이 없어요.\n첫 미션에 도전해볼까요?'}
            </Text>
          </View>
        }
      />

      <CompletionHistoryModal detail={detail} onClose={() => setDetail(null)} />
    </Screen>
  );
}

function CompletionRow({
  completion,
  onPress,
}: {
  completion: ChoreCompletionHistoryItem;
  onPress: () => void;
}) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`${completion.choreTitle} 자세히 보기`}
    >
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
        <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />
      </ContentCard>
    </Pressable>
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
