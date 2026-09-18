import { Ionicons } from '@expo/vector-icons';
import { useMemo, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View, RefreshControl } from 'react-native';

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
import { ChoreHeroBanner } from '@/features/chore/ui/ChoreHeroBanner';
import { ChoreListRow } from '@/features/chore/ui/ChoreListRow';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import {
  ChildBadge,
  ContentCard,
  FilterChips,
  Header,
  HeaderChipButton,
  Screen,
} from '@/shared/ui';

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

  // 지급까지 끝난 미션은 목록에서 뺀다. 아이 화면과 같은 기준이다 - 한쪽에만 남으면
  // 부모와 아이가 서로 다른 목록을 보게 된다. 지난 건은 완료 이력에서 본다.
  const active = useMemo(() => items.filter((chore) => !chore.done), [items]);

  const visible = useMemo(
    () => (repeatType == null ? active : active.filter((chore) => chore.repeatType === repeatType)),
    [active, repeatType],
  );

  // 아이 화면(ChildChoreList)과 같은 pill 이고, 값은 상점의 "구매 내역"(StoreCatalog)을 따른다.
  const historyButton = onHistoryPress ? (
    <HeaderChipButton label="완료 이력" onPress={onHistoryPress} />
  ) : undefined;

  return (
    <Screen
      header={<Header title="미션" right={historyButton} />}
      padded={false}
      style={{ paddingVertical: theme.space.sm }}
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
              title="우리 가족 미션"
              caption={'아이가 해낼 미션을 만들고 도토리로\n칭찬해주세요!'}
              mascot={require('@/assets/images/chore/owl-cheer.png')}
            />
            {requests.items.length > 0 ? (
              <PendingRequestsCard
                requests={requests.items}
                onRequestPress={onRequestPress}
                onSeeAll={onSeeAllRequests}
              />
            ) : null}
            <FilterChips options={FILTERS} selected={repeatType} onSelect={setRepeatType} />
          </View>
        }
        renderItem={({ item }) => (
          <ChoreListRow
            title={item.title}
            repeatType={item.repeatType}
            rewardPoints={item.rewardPoints}
            childName={item.childName}
            endDate={item.endDate}
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
                    : // 등록된 것이 있는데 비었으면 아이가 전부 끝낸 것이다.
                      items.length > 0
                      ? '아이가 미션을 모두 끝냈어요.\n새 미션을 만들어주세요!'
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

      {/* 행 전체가 버튼이다. 오른쪽 끝의 화살표는 목록 행(ChoreListRow)과 같은 모양이라,
          "눌러서 들어간다"는 것이 같은 방식으로 읽힌다. 예전에는 "확인하기" 버튼이었는데
          버튼만 누를 수 있어 표적이 좁았고, 버튼 높이와 두 줄 글이 서로 안 맞아 보였다. */}
      {requests.slice(0, PREVIEW_COUNT).map((request) => (
        <Pressable
          key={request.choreCompletionRequestId}
          accessibilityRole="button"
          accessibilityLabel={`${request.choreTitle} 완료 요청 확인하기`}
          disabled={onRequestPress == null}
          onPress={() => onRequestPress?.(request)}
          style={[styles.requestRow, { gap: theme.space.md }]}
        >
          {/* 이름 배지는 제목 줄이 아니라 행에 직접 둔다. 제목 옆에 두면 두 줄짜리 글의
              윗줄에만 붙어 행 가운데에서 벗어나 보이고, 이름이 길면 제목을 밀어낸다. */}
          <View style={styles.requestBody}>
            <Text
              numberOfLines={1}
              style={[theme.typography.label, { color: theme.color.text.strong }]}
            >
              {request.choreTitle}
            </Text>
            <Text
              numberOfLines={1}
              style={[theme.typography.bodySm, { color: theme.color.text.muted }]}
            >
              {request.description ?? '사진과 함께 완료 요청을 보냈어요.'}
            </Text>
          </View>
          <ChildBadge name={request.childName} />
          <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />
        </Pressable>
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
  // 위쪽 여백을 주지 않는다. Screen에 style={{ paddingVertical: theme.space.sm }}로 헤더-배너
  // 간격을 이미 줬으니, 여기서 또 주면 중복으로 벌어진다 (상점·저축·투자와 같은 값).
  content: { paddingBottom: 16, flexGrow: 1 },
  cardHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  count: { minWidth: 20, paddingHorizontal: 6, paddingVertical: 1, alignItems: 'center' },
  requestRow: { flexDirection: 'row', alignItems: 'center' },
  requestBody: { flex: 1, gap: 2 },
  // 제목이 길어도 아이 이름 배지를 밀어내지 않게 한다

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
