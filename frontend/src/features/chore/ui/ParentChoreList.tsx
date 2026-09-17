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
  onRequestPress?: (request: ParentCompletionRequest) => void;
  onSeeAllRequests?: () => void;
  onHistoryPress?: () => void;
};

const FILTERS = REPEAT_TYPES.map((key) => ({ key, label: repeatLabel(key) }));

/**
 * 부모가 보는 미션 목록 (미션 탭). 자녀 여러 명의 미션이 한 목록에 섞인다.
 *
 * 맨 위에 설명 배너, 그 아래 처리할 완료 요청, 그다음이 목록이다 - 부모가 이 화면에 오는
 * 이유가 대개 "확인해 줄 것이 있나"라서 목록보다 위에 둔다.
 *
 * 수정·삭제는 여기 두지 않는다. 행마다 달려 있던 "..." 메뉴를 없애고 상세로 옮겼다 -
 * 목록에서 곧바로 지울 수 있으면 어느 미션을 지우는지 확인하지 않은 채 누르기 쉽다.
 */
export function ParentChoreList({
  familyId,
  childList,
  onRegisterPress,
  onChorePress,
  onRequestPress,
  onSeeAllRequests,
  onHistoryPress,
}: ParentChoreListProps) {
  const theme = useTheme();
  const { refreshing, onRefresh } = useChoreRefresh();
  const [repeatType, setRepeatType] = useState<ChoreRepeatType | null>(null);

  const { items, isPending, isError } = useChildrenChores(familyId, childList);
  const requests = useChildrenPendingRequests(familyId, childList);

  const visible = useMemo(
    () => (repeatType == null ? items : items.filter((chore) => chore.repeatType === repeatType)),
    [items, repeatType],
  );

  // 아이 화면(ChildChoreList)과 같은 pill 이고, 값은 상점의 "구매 내역"(StoreCatalog)을 따른다.
  const historyButton = onHistoryPress ? (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel="완료 이력"
      onPress={onHistoryPress}
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
  ) : undefined;

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
            <IntroBanner />
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
                    : '아직 등록한 미션이 없어요.\n오른쪽 아래 + 버튼으로 만들어보세요.'
            }
          />
        }
      />

      {onRegisterPress ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="미션 등록"
          onPress={onRegisterPress}
          style={({ pressed }) => [
            styles.fab,
            theme.shadow.card,
            {
              backgroundColor: pressed ? theme.color.brand.solidPressed : theme.color.brand.solid,
              borderRadius: theme.radius.full,
            },
          ]}
        >
          <Ionicons name="add" size={28} color={theme.color.text.onBrand} />
        </Pressable>
      ) : null}
    </Screen>
  );
}

/**
 * 화면 맨 위의 설명 배너. 상점(StoreBanner)과 같은 자리·같은 높이다.
 *
 * 누르는 배너가 아니다. 예전에는 이것이 등록 버튼이었는데, 목록을 보러 온 부모가 맨 위
 * 큰 박스를 눌러 등록 화면으로 빠지는 일이 잦았다. 등록은 우측 하단 + 버튼으로 옮겼다.
 */
function IntroBanner() {
  const theme = useTheme();

  return (
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
        <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>
          우리 가족 미션
        </Text>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
          {'아이가 해낼 미션을 만들고 도토리로\n칭찬해주세요!'}
        </Text>
      </View>
      <Image
        source={require('@/assets/images/chore/owl-cheer.png')}
        style={styles.mascotSmall}
        resizeMode="contain"
      />
    </View>
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
  banner: { flexDirection: 'row', alignItems: 'center' },
  bannerBody: { flex: 1, gap: 4 },
  historyChip: { paddingHorizontal: 12, paddingVertical: 6 },
  // 상점 배너(StoreBanner)와 같은 88px. 글 영역이 52px 라 높이는 마스코트가 정한다 - 이 값이
  // 두 탭의 배너를 똑같이 120px 로 맞춘다.
  mascotSmall: { width: 88, height: 88 },
  cardHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  count: { minWidth: 20, paddingHorizontal: 6, paddingVertical: 1, alignItems: 'center' },
  requestRow: { flexDirection: 'row', alignItems: 'center' },
  requestBody: { flex: 1, gap: 2 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
  // 상점의 등록 버튼(StoreCatalog)과 같은 자리·같은 크기다.
  fab: {
    position: 'absolute',
    right: 20,
    bottom: 24,
    width: 56,
    height: 56,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
