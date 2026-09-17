import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Image, StyleSheet, Text, View, RefreshControl } from 'react-native';

import {
  useChildrenPendingRequests,
  type ParentCompletionRequest,
} from '@/features/chore/model/useChildrenPendingRequests';
import type { ChoreChild } from '@/features/chore/model/useChildrenChores';
import { CompletionRequestModal } from '@/features/chore/ui/CompletionRequestModal';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import { Button, ChildBadge, ContentCard, FilterChips, Header, Screen } from '@/shared/ui';

type ParentCompletionRequestListProps = {
  familyId: number | undefined;
  childList: ChoreChild[];
  /** 승인으로 도토리가 지급되면 알린다. 지갑은 다른 슬라이스라 라우트가 갱신한다 */
  onApproved?: (childId: number, rewardPoints: number) => void;
};

/**
 * 처리 대기 중인 완료 요청 목록 (부모). 할 일 목록의 "전체보기"에서 들어온다.
 *
 * 자녀 필터는 자녀가 둘 이상일 때만 뜬다. 하나뿐이면 고를 것이 없어 자리만 차지한다.
 *
 * 행을 누르든 "확인하기"를 누르든 같은 상세가 열린다 - 사진이 작아서 목록만 보고
 * 승인·거절을 결정하기 어렵다.
 */
export function ParentCompletionRequestList({
  familyId,
  childList,
  onApproved,
}: ParentCompletionRequestListProps) {
  const theme = useTheme();
  const { refreshing, onRefresh } = useChoreRefresh();
  const [childId, setChildId] = useState<string | null>(null);
  const [openRequest, setOpenRequest] = useState<ParentCompletionRequest | null>(null);

  const { items, isPending, isError } = useChildrenPendingRequests(familyId, childList);

  // 칩 컴포넌트가 문자열 키를 쓰므로 childId 를 문자열로 오간다.
  const filters = useMemo(
    () => childList.map((child) => ({ key: String(child.childId), label: child.name })),
    [childList],
  );

  const visible = useMemo(
    () => (childId == null ? items : items.filter((r) => String(r.childId) === childId)),
    [items, childId],
  );

  return (
    <Screen
      header={
        <Header
          title="완료 요청"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/task'))}
        />
      }
      padded={false}
    >
      <FlatList
        data={visible}
        keyExtractor={(item) => String(item.choreCompletionRequestId)}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space.md },
        ]}
        ListHeaderComponent={
          childList.length > 1 ? (
            <View style={{ marginBottom: theme.space.md }}>
              <FilterChips options={filters} selected={childId} onSelect={setChildId} />
            </View>
          ) : null
        }
        renderItem={({ item }) => (
          <RequestRow request={item} onPress={() => setOpenRequest(item)} />
        )}
        ListEmptyComponent={
          <View style={[styles.empty, { gap: theme.space.lg }]}>
            {!isPending && !isError ? (
              <Image
                source={require('@/assets/images/chore/owl-thumb.png')}
                style={styles.mascot}
                resizeMode="contain"
              />
            ) : null}
            <Text
              style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}
            >
              {isPending
                ? '요청을 불러오는 중이에요.'
                : isError
                  ? '요청을 불러오지 못했어요.'
                  : childId != null
                    ? '이 아이의 확인할 요청이 없어요.'
                    : '확인할 요청이 없어요.\n모두 처리하셨네요!'}
            </Text>
          </View>
        }
      />

      <CompletionRequestModal
        familyId={familyId}
        request={openRequest}
        onClose={() => setOpenRequest(null)}
        onApproved={onApproved}
      />
    </Screen>
  );
}

function RequestRow({
  request,
  onPress,
}: {
  request: ParentCompletionRequest;
  onPress: () => void;
}) {
  const theme = useTheme();

  return (
    <ContentCard
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        padding: theme.space.md,
        gap: theme.space.md,
      }}
    >
      {request.photoUrl ? (
        <Image
          source={{ uri: request.photoUrl }}
          style={[styles.thumb, { borderRadius: theme.radius.sm }]}
          resizeMode="cover"
        />
      ) : null}

      <View style={styles.body}>
        <View style={[styles.titleRow, { gap: theme.space.xs }]}>
          <Text
            numberOfLines={1}
            style={[theme.typography.label, styles.grow, { color: theme.color.text.strong }]}
          >
            {request.choreTitle}
          </Text>
          <ChildBadge name={request.childName} />
        </View>
        <Text
          numberOfLines={1}
          style={[theme.typography.caption, { color: theme.color.text.muted }]}
        >
          {request.description ?? '사진과 함께 완료 요청을 보냈어요.'}
        </Text>
      </View>

      <Button label="확인하기" variant="outline" size="sm" onPress={onPress} />
    </ContentCard>
  );
}

const styles = StyleSheet.create({
  content: { paddingVertical: 16, flexGrow: 1 },
  thumb: { width: 52, height: 52 },
  body: { flex: 1, gap: 2 },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  grow: { flex: 1 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
  mascot: { width: 140, height: 140 },
});
