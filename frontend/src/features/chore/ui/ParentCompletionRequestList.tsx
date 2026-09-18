import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Image, Pressable, StyleSheet, Text, View, RefreshControl } from 'react-native';

import {
  useChildrenPendingRequests,
  type ParentCompletionRequest,
} from '@/features/chore/model/useChildrenPendingRequests';
import type { ChoreChild } from '@/features/chore/model/useChildrenChores';
import { ChildBadge } from '@/features/chore/ui/ChildBadge';
import { ChoreFilterChips } from '@/features/chore/ui/ChoreFilterChips';
import { CompletionRequestModal } from '@/features/chore/ui/CompletionRequestModal';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import { ContentCard, Header, Screen } from '@/shared/ui';

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
 * 행을 누르면 상세가 열린다 - 목록의 사진이 작아서 여기서 바로 승인·거절을 결정하기
 * 어렵다. 그래서 행에는 처리 버튼을 두지 않고 상세로 보낸다.
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
              <ChoreFilterChips options={filters} selected={childId} onSelect={setChildId} />
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

  // 카드 전체가 버튼이다. 우측 화살표는 미션 목록 행·메인의 완료 요청 카드와 같은 모양이다.
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${request.choreTitle} 완료 요청 확인하기`}
      onPress={onPress}
    >
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

        {/* 이름 배지는 제목 줄이 아니라 행에 직접 둔다. 제목 옆에 두면 두 줄짜리 글의
            윗줄에만 붙어 행 가운데에서 벗어나 보이고, 이름이 길면 제목을 밀어낸다. */}
        <View style={styles.body}>
          <Text
            numberOfLines={1}
            style={[theme.typography.label, { color: theme.color.text.strong }]}
          >
            {request.choreTitle}
          </Text>
          <Text
            numberOfLines={1}
            style={[theme.typography.caption, { color: theme.color.text.muted }]}
          >
            {request.description ?? '사진과 함께 완료 요청을 보냈어요.'}
          </Text>
        </View>

        <ChildBadge name={request.childName} />
        <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />
      </ContentCard>
    </Pressable>
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
