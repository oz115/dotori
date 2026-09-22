import { Ionicons } from '@expo/vector-icons';
import { useMemo, useState } from 'react';
import { ScrollView, Pressable, StyleSheet, Text, View, RefreshControl } from 'react-native';

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
  Checkbox,
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
  const childIds = useMemo(() => childList.map((child) => child.childId), [childList]);
  const { refreshing, onRefresh } = useChoreRefresh(familyId, childIds);
  const [repeatType, setRepeatType] = useState<ChoreRepeatType | null>(null);
  const [onlyActive, setOnlyActive] = useState(false);

  const { items, isPending, isError } = useChildrenChores(familyId, childList);
  const requests = useChildrenPendingRequests(familyId, childList);

  /**
   * 목록에 남길 미션. 아이 화면(ChildChoreList)과 같은 규칙이다 - 한쪽에만 남으면 부모와
   * 아이가 서로 다른 목록을 보게 된다.
   *
   * 끝낸 반복 미션(매일·매주·매월)은 흐리게 남긴다. 부모는 자기가 무엇을 걸어 뒀는지
   * 기억하지 못하는데, 끝나자마자 사라지면 그 미션이 있었다는 사실 자체가 안 보인다 (QA).
   * 한번짜리는 다시 할 일이 아니라 뺀다.
   */
  const listed = useMemo(
    () => items.filter((chore) => !chore.done || chore.repeatType !== 'ONCE'),
    [items],
  );

  const visible = useMemo(() => {
    const byRepeat =
      repeatType == null ? listed : listed.filter((chore) => chore.repeatType === repeatType);
    const filtered = onlyActive ? byRepeat.filter((chore) => !chore.done) : byRepeat;
    // 아직 안 끝난 것이 위로. 섞여 있으면 무엇이 남았는지 훑어야 한다.
    return [...filtered].sort((a, b) => Number(a.done) - Number(b.done));
  }, [listed, repeatType, onlyActive]);

  // 아이 화면(ChildChoreList)과 같은 pill 이고, 값은 상점의 "구매 내역"(StoreCatalog)을 따른다.
  const historyButton = onHistoryPress ? (
    <HeaderChipButton label="완료 이력" onPress={onHistoryPress} />
  ) : undefined;

  return (
    <Screen
      header={<Header title="미션" right={historyButton} />}
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
      style={{ paddingTop: theme.space.sm }}
    >
      {/* 칩이 스크롤해도 상단에 붙어 있어야 해서 ScrollView + stickyHeaderIndices 다.
          FlatList 는 헤더 전체(배너+요청 카드+칩)를 한 덩어리로만 붙일 수 있다.
          한 가족의 미션은 많아야 수십 개라 가상화를 포기해도 괜찮다 (상점 목록과 같은 이유). */}
      <ScrollView
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        // 1 = 필터 칩. 위쪽(0)은 그대로 밀려 올라가고 칩만 상단에 붙는다.
        stickyHeaderIndices={[1]}
        // 좌우 여백은 각 자식이 갖는다 — contentContainer 에 주면 붙어 있는 칩 줄의 배경이
        // 화면 끝까지 닿지 않아, 그 여백 틈으로 지나가는 카드가 보인다.
        contentContainerStyle={styles.content}
      >
        <View
          style={{
            paddingHorizontal: theme.layout.screenX,
            marginBottom: theme.space.lg,
            gap: theme.space.lg,
          }}
        >
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
        </View>

        {/* 칩과 체크박스는 한 줄이다 - 둘 다 이 목록을 걸러내는 장치다 (아이 화면과 같다).
            붙어 있는 동안 카드가 뒤로 지나가므로 배경을 불투명하게 깔아 준다. */}
        <View
          style={{
            paddingHorizontal: theme.layout.screenX,
            paddingBottom: theme.space.lg,
            backgroundColor: theme.color.background.canvas,
          }}
        >
          <View style={[styles.filterRow, { gap: theme.space.sm }]}>
            <View style={styles.chips}>
              <FilterChips options={FILTERS} selected={repeatType} onSelect={setRepeatType} />
            </View>
            <Checkbox checked={onlyActive} onChange={setOnlyActive} label="미완료만" />
          </View>
        </View>

        {visible.length === 0 ? (
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
        ) : (
          <View style={{ paddingHorizontal: theme.layout.screenX, gap: theme.space.md }}>
            {visible.map((item) => (
              <ChoreListRow
                key={item.choreId}
                title={item.title}
                repeatType={item.repeatType}
                rewardPoints={item.rewardPoints}
                childName={item.childName}
                endDate={item.endDate}
                // 부모 목록은 대기·거절을 행마다 보여 주지 않는다 (위의 "완료 요청" 카드가 맡는다).
                // 끝난 것만 배지와 흐린 카드로 구분한다.
                status={item.done ? 'DONE' : undefined}
                dimmed={item.done}
                onPress={onChorePress ? () => onChorePress(item) : undefined}
              />
            ))}
          </View>
        )}
      </ScrollView>

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
  filterRow: { flexDirection: 'row', alignItems: 'center' },
  // 칩이 남은 폭을 다 쓴다. 가로 스크롤이라 폭을 주지 않으면 체크박스를 밀어낸다
  chips: { flex: 1 },
  // 위쪽 여백을 주지 않는다. Screen에 style={{ paddingTop: theme.space.sm }}로 헤더-배너
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
