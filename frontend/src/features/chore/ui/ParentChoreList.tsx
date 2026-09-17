import { Ionicons } from '@expo/vector-icons';
import { useMemo, useState } from 'react';
import { FlatList, Image, Pressable, StyleSheet, Text, View, RefreshControl } from 'react-native';

import { REPEAT_TYPES, repeatLabel } from '@/features/chore/lib/choreLabels';
import {
  useChildrenChores,
  type ChoreChild,
  type ParentChore,
} from '@/features/chore/model/useChildrenChores';
import {
  useChildrenPendingRequests,
  type ParentCompletionRequest,
} from '@/features/chore/model/useChildrenPendingRequests';
import type { ChoreRepeatType } from '@/features/chore/model/types';
import { ChildBadge } from '@/features/chore/ui/ChildBadge';
import { useDeleteChore } from '@/features/chore/model/useDeleteChore';
import { ChoreActionSheet } from '@/features/chore/ui/ChoreActionSheet';
import { DeleteChoreModal } from '@/features/chore/ui/DeleteChoreModal';
import { ChoreFilterChips } from '@/features/chore/ui/ChoreFilterChips';
import { ChoreListRow } from '@/features/chore/ui/ChoreListRow';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import { Button, ContentCard, Header, Screen } from '@/shared/ui';

/** 대기함 카드에 미리 보여 줄 건수. 나머지는 "전체보기"로 넘어간다 */
const PREVIEW_COUNT = 2;

type ParentChoreListProps = {
  familyId: number | undefined;
  /** 가족 슬라이스에서 온 값이라 라우트가 넘겨준다 */
  childList: ChoreChild[];
  /** 화면 이동은 라우트가 맡는다. 안 넘기면 그 자리는 눌러도 조용하다 */
  onRegisterPress?: () => void;
  onChorePress?: (chore: ParentChore) => void;
  onEditChore?: (chore: ParentChore) => void;
  onRequestPress?: (request: ParentCompletionRequest) => void;
  onSeeAllRequests?: () => void;
  onHistoryPress?: () => void;
};

const FILTERS = REPEAT_TYPES.map((key) => ({ key, label: repeatLabel(key) }));

/**
 * 부모가 보는 할 일 목록 (할 일 탭). 자녀 여러 명의 할 일이 한 목록에 섞인다.
 *
 * 맨 위에 등록 배너, 그 아래 처리할 완료 요청, 그다음이 목록이다 - 부모가 이 화면에 오는
 * 이유가 대개 "확인해 줄 것이 있나"라서 목록보다 위에 둔다.
 */
export function ParentChoreList({
  familyId,
  childList,
  onRegisterPress,
  onChorePress,
  onEditChore,
  onRequestPress,
  onSeeAllRequests,
  onHistoryPress,
}: ParentChoreListProps) {
  const theme = useTheme();
  const { refreshing, onRefresh } = useChoreRefresh();
  const [repeatType, setRepeatType] = useState<ChoreRepeatType | null>(null);
  const [menuTarget, setMenuTarget] = useState<ParentChore | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ParentChore | null>(null);

  // 지울 대상이 정해져야 어느 아이의 경로로 부를지 알 수 있다. 대상이 바뀌면 훅도 따라간다.
  const deleteChore = useDeleteChore(familyId, deleteTarget?.childId);

  const { items, isPending, isError } = useChildrenChores(familyId, childList);
  const requests = useChildrenPendingRequests(familyId, childList);

  const visible = useMemo(
    () => (repeatType == null ? items : items.filter((chore) => chore.repeatType === repeatType)),
    [items, repeatType],
  );

  return (
    <Screen
      header={
        <Header
          title="할 일"
          action={onHistoryPress ? { label: '완료 이력', onPress: onHistoryPress } : undefined}
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
            <RegisterBanner onPress={onRegisterPress} />
            {requests.items.length > 0 ? (
              <PendingRequestsCard
                requests={requests.items}
                onRequestPress={onRequestPress}
                onSeeAll={onSeeAllRequests}
              />
            ) : null}
            <ChoreFilterChips options={FILTERS} selected={repeatType} onSelect={setRepeatType} />
          </View>
        }
        renderItem={({ item }) => (
          <ChoreListRow
            title={item.title}
            repeatType={item.repeatType}
            rewardPoints={item.rewardPoints}
            childName={item.childName}
            onPress={onChorePress ? () => onChorePress(item) : undefined}
            trailing={
              <Pressable
                onPress={() => setMenuTarget(item)}
                hitSlop={8}
                accessibilityRole="button"
                accessibilityLabel={`${item.title} 더보기`}
              >
                <Ionicons name="ellipsis-horizontal" size={20} color={theme.color.text.muted} />
              </Pressable>
            }
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
                    : '아직 등록한 할 일이 없어요.\n위에서 첫 할 일을 만들어보세요.'
            }
            showMascot={!isPending && !isError}
          />
        }
      />

      <ChoreActionSheet
        title={menuTarget?.title ?? null}
        onEdit={() => {
          const target = menuTarget;
          setMenuTarget(null);
          if (target) onEditChore?.(target);
        }}
        onDelete={() => {
          setDeleteTarget(menuTarget);
          setMenuTarget(null);
        }}
        onClose={() => setMenuTarget(null)}
      />

      <DeleteChoreModal
        title={deleteTarget?.title ?? null}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() =>
          deleteTarget &&
          deleteChore.mutate(deleteTarget.choreId, { onSuccess: () => setDeleteTarget(null) })
        }
        loading={deleteChore.isPending}
        isError={deleteChore.isError}
      />
    </Screen>
  );
}

function RegisterBanner({ onPress }: { onPress?: () => void }) {
  const theme = useTheme();

  return (
    <Pressable onPress={onPress} disabled={onPress == null} accessibilityRole="button">
      <View
        style={[
          styles.banner,
          {
            backgroundColor: theme.color.brand.subtle,
            borderRadius: theme.radius.lg,
            paddingVertical: theme.space.lg,
            paddingHorizontal: theme.space.xl,
            gap: theme.space.md,
          },
        ]}
      >
        <View style={styles.bannerBody}>
          <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>할 일 등록</Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.brand.subtleText }]}>
            우리 아이의 새로운 할 일을 추가해보세요!
          </Text>
        </View>
        <Image
          source={require('@/assets/images/chore/owl-smile.png')}
          style={styles.mascotSmall}
          resizeMode="contain"
        />
        <Ionicons name="chevron-forward" size={22} color={theme.color.brand.subtleText} />
      </View>
    </Pressable>
  );
}

/**
 * 처리 대기 중인 완료 요청. 앞의 몇 건만 보여 주고 나머지는 전체보기로 넘긴다.
 *
 * 문구는 아이가 쓴 내용을 그대로 보여 준다. 안 썼을 때만 사진이 왔다는 사실을 대신 알린다 -
 * 고정 문구로 덮으면 아이가 남긴 말이 부모에게 닿지 않는다.
 */
function PendingRequestsCard({
  requests,
  onRequestPress,
  onSeeAll,
}: {
  requests: ParentCompletionRequest[];
  onRequestPress?: (request: ParentCompletionRequest) => void;
  onSeeAll?: () => void;
}) {
  const theme = useTheme();

  return (
    <ContentCard style={{ padding: theme.space.lg, gap: theme.space.md }}>
      <View style={styles.cardHeader}>
        <View style={[styles.titleRow, { gap: theme.space.sm }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            완료 요청
          </Text>
          <View
            style={[
              styles.count,
              { backgroundColor: theme.color.brand.solid, borderRadius: theme.radius.full },
            ]}
          >
            <Text style={[theme.typography.caption, { color: theme.color.text.onBrand }]}>
              {requests.length}
            </Text>
          </View>
        </View>
        {onSeeAll ? (
          <Pressable onPress={onSeeAll} hitSlop={8} accessibilityRole="button">
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              전체보기 ›
            </Text>
          </Pressable>
        ) : null}
      </View>

      {requests.slice(0, PREVIEW_COUNT).map((request) => (
        <View
          key={request.choreCompletionRequestId}
          style={[styles.requestRow, { gap: theme.space.md }]}
        >
          <View style={styles.requestBody}>
            <View style={[styles.titleRow, { gap: theme.space.xs }]}>
              <Text
                numberOfLines={1}
                style={[theme.typography.label, { color: theme.color.text.strong }]}
              >
                {request.choreTitle}
              </Text>
              <ChildBadge name={request.childName} />
            </View>
            <Text
              numberOfLines={1}
              style={[theme.typography.bodySm, { color: theme.color.text.muted }]}
            >
              {request.description ?? '사진과 함께 완료 요청을 보냈어요.'}
            </Text>
          </View>
          <Button
            label="확인하기"
            variant="outline"
            size="sm"
            disabled={onRequestPress == null}
            onPress={() => onRequestPress?.(request)}
          />
        </View>
      ))}
    </ContentCard>
  );
}

function EmptyState({ message, showMascot }: { message: string; showMascot: boolean }) {
  const theme = useTheme();

  return (
    <View style={[styles.empty, { gap: theme.space.lg }]}>
      {showMascot ? (
        <Image
          source={require('@/assets/images/chore/owl-cheer.png')}
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
  banner: { flexDirection: 'row', alignItems: 'center' },
  bannerBody: { flex: 1, gap: 4 },
  mascotSmall: { width: 84, height: 84 },
  cardHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  count: { minWidth: 20, paddingHorizontal: 6, paddingVertical: 1, alignItems: 'center' },
  requestRow: { flexDirection: 'row', alignItems: 'center' },
  requestBody: { flex: 1, gap: 2 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
  mascot: { width: 140, height: 140 },
});
