import { router } from 'expo-router';
import { useEffect, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View, RefreshControl } from 'react-native';

import { REPEAT_TYPES, repeatLabel, sumRewards } from '@/features/chore/lib/choreLabels';
import { useTodayChores } from '@/features/chore/model/useTodayChores';
import type { ChoreRepeatType } from '@/features/chore/model/types';
import { ChoreHeroBanner } from '@/features/chore/ui/ChoreHeroBanner';
import { ChoreListRow } from '@/features/chore/ui/ChoreListRow';
import { useChoreRefresh } from '@/features/chore/model/useChoreRefresh';
import { useTheme } from '@/shared/theme';
import { Checkbox, FilterChips, Header, HeaderChipButton, Screen } from '@/shared/ui';

type ChildChoreListProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /**
   * 바깥(알림)에서 넘긴 choreId. 아직 할 미션이면 상세 화면을, 이번 주기에 이미 받은
   * 미션이면 완료 이력을 연다. 받자마자 onRequestedChoreConsumed 로 알려서 부르는 쪽이
   * 이 값을 비우게 한다 — 안 비우면 같은 알림을 다시 눌렀을 때 값이 그대로라 반응하지 않는다.
   */
  requestedChoreId?: number;
  onRequestedChoreConsumed?: () => void;
};

const FILTERS = REPEAT_TYPES.map((key) => ({ key, label: repeatLabel(key) }));

/**
 * 아이의 할 일 목록 (할 일 탭). 탭 루트라 헤더에 뒤로가기를 두지 않는다.
 *
 * 서버가 활성 집안일만 주므로 기간이 지난 것은 애초에 내려오지 않는다 - 화면에서 따로
 * 거르지 않는다. 반복 주기 필터는 이미 받아 둔 목록을 클라이언트에서 거르는 것이다.
 */
export function ChildChoreList({
  familyId,
  childId,
  requestedChoreId,
  onRequestedChoreConsumed,
}: ChildChoreListProps) {
  const theme = useTheme();
  const childIds = useMemo(() => (childId != null ? [childId] : []), [childId]);
  const { refreshing, onRefresh, refresh } = useChoreRefresh(familyId, childIds);
  const [repeatType, setRepeatType] = useState<ChoreRepeatType | null>(null);
  const [onlyActive, setOnlyActive] = useState(false);
  const { items, remainingCount, isPending, isError } = useTodayChores(familyId, childId);

  // 알림에서 넘어온 미션은 최신 목록을 받은 뒤에 갈 곳을 정한다. 캐시(최대 60초)로 판단하면
  // 방금 승인된 미션이 아직 TODO 로 보여서, 이미 받은 미션에 완료 요청 폼이 열린다.
  const [resolvingChoreId, setResolvingChoreId] = useState<number | null>(null);
  /**
   * 알림 처리가 끝나고 갈 곳. 이동은 effect 에서 한다 (렌더 중에 라우터를 건드리면 경고가 난다).
   *
   * 매번 새 객체를 넣어 둔다 - 같은 알림을 다시 눌러 같은 곳으로 가야 할 때도 effect 가
   * 다시 돌아야 해서, 값이 같아도 바뀐 것으로 쳐야 한다.
   */
  const [goTo, setGoTo] = useState<{ choreId: number | null } | null>(null);

  useEffect(() => {
    if (requestedChoreId == null) return;

    onRequestedChoreConsumed?.();
    // 값이 비워지면서 이 effect 가 다시 돌아도 진행 중인 새로고침은 취소하지 않는다.
    refresh().finally(() => setResolvingChoreId(requestedChoreId));
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 요청 값이 바뀔 때만 처리한다
  }, [requestedChoreId]);

  // 새로고침이 끝나면 렌더 중에 갈 곳을 정한다 (effect 에서 setState 하면 한 번 더 그린다).
  // 이번 주기에 이미 받은 미션(승인 알림 등)은 다시 할 게 없으니 완료 이력으로 보내고,
  // 아직 할 미션(새 미션·거절 알림)은 상세 화면을 연다.
  if (resolvingChoreId != null && !isPending) {
    setResolvingChoreId(null);
    const target = items.find((chore) => chore.choreId === resolvingChoreId);
    // 상태를 모르면(조회 실패) 아무 데도 안 간다 - 모르고 열면 완료 요청 폼이 뜰 수 있다.
    if (!isError) {
      setGoTo({
        choreId: target != null && target.status !== 'DONE' ? resolvingChoreId : null,
      });
    }
  }

  useEffect(() => {
    if (goTo == null) return;
    if (goTo.choreId == null) {
      router.push('/task/history');
      return;
    }
    router.push({
      pathname: '/(child)/(tabs)/task/[choreId]',
      params: { choreId: String(goTo.choreId) },
    });
  }, [goTo]);

  /**
   * 목록에 남길 미션.
   *
   * 끝낸 반복 미션(매일·매주·매월)은 흐리게 남긴다. 사라지면 아이도 부모도 무엇을 걸어
   * 놨는지 알 수 없어서, 이번 주기에 이미 했다는 사실 자체가 보이지 않는다 (QA).
   *
   * <p>한번짜리는 끝나면 뺀다. 다음 주기가 없어 다시 할 일이 아니고, 남겨 두면 "해야 할
   * 미션" 자리에 영영 쌓인다. 기록은 완료 이력에 있다.
   */
  const listed = useMemo(
    () => items.filter((chore) => chore.status !== 'DONE' || chore.repeatType !== 'ONCE'),
    [items],
  );

  /** 도토리 합계·남은 개수는 아직 할 것 기준이다 - 끝낸 것을 세면 "남았어요" 가 거짓이 된다 */
  const active = useMemo(() => items.filter((chore) => chore.status !== 'DONE'), [items]);

  const visible = useMemo(() => {
    const byRepeat =
      repeatType == null ? listed : listed.filter((chore) => chore.repeatType === repeatType);
    const filtered = onlyActive ? byRepeat.filter((chore) => chore.status !== 'DONE') : byRepeat;
    // 아직 할 미션이 위, 끝낸 것은 아래. 섞여 있으면 할 일을 찾으려고 목록을 훑어야 한다.
    // 같은 무리 안의 순서는 서버가 준 대로 둔다 (최근 등록 순).
    return [...filtered].sort((a, b) => Number(a.status === 'DONE') - Number(b.status === 'DONE'));
  }, [listed, repeatType, onlyActive]);

  const { total, pendingCount } = useMemo(
    () => sumRewards(active.map((chore) => chore.rewardPoints)),
    [active],
  );

  /**
   * 미션 상세로. 그룹(child)까지 적는다 - "/task/[choreId]" 는 부모 화면에도 있어서
   * 짧게 쓰면 부모 쪽 경로로 잡히고, 아이 계정에서는 아무 일도 일어나지 않는다.
   */
  const openDetail = (choreId: number) =>
    router.push({
      pathname: '/(child)/(tabs)/task/[choreId]',
      params: { choreId: String(choreId) },
    });

  const historyButton = (
    <HeaderChipButton label="완료 이력" onPress={() => router.push('/task/history')} />
  );

  return (
    <Screen
      header={<Header title="미션" right={historyButton} />}
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
      style={{ paddingTop: theme.space.sm }}
    >
      {/* 칩이 스크롤해도 상단에 붙어 있어야 해서 ScrollView + stickyHeaderIndices 다.
          FlatList 는 헤더 전체(배너+칩)를 한 덩어리로만 붙일 수 있어 배너까지 따라 붙는다.
          한 아이의 미션은 많아야 수십 개라 가상화를 포기해도 괜찮다 (상점 목록과 같은 이유). */}
      <ScrollView
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        // 1 = 필터 칩. 배너(0)는 그대로 밀려 올라가고 칩만 상단에 붙는다.
        stickyHeaderIndices={[1]}
        // 좌우 여백은 각 자식이 갖는다 — contentContainer 에 주면 붙어 있는 칩 줄의 배경이
        // 화면 끝까지 닿지 않아, 그 여백 틈으로 지나가는 카드가 보인다.
        contentContainerStyle={styles.content}
      >
        <View style={{ paddingHorizontal: theme.layout.screenX, marginBottom: theme.space.lg }}>
          <ChoreHeroBanner
            eyebrow="오늘 해야 할 미션이"
            title={`${remainingCount}개 남았어요!`}
            caption={rewardCaption(total, pendingCount) ?? '하나씩 완료하고 도토리를 모아봐요!'}
            mascot={require('@/assets/images/chore/squirrel-win.png')}
          />
        </View>

        {/* 주기 칩은 다섯 칸이라 체크박스와 한 줄에 두면 좁은 기기에서 "매월"이 잘렸다 —
            칩 줄을 위에, 체크박스를 그 아래 줄에 둔다. 칩은 상점(CategoryFilterBar)과
            같은 자리·같은 크기다. 붙어 있는 동안 카드가 뒤로 지나가므로 배경을
            불투명하게 깔아 준다. */}
        <View
          style={{
            paddingHorizontal: theme.layout.screenX,
            paddingBottom: theme.space.lg,
            backgroundColor: theme.color.background.canvas,
            // 칩↔체크박스 사이도 배너↔칩·체크박스↔목록과 같은 간격(lg)으로 — 한 칸만
            // 좁으면 붙어 보인다
            gap: theme.space.lg,
          }}
        >
          <FilterChips options={FILTERS} selected={repeatType} onSelect={setRepeatType} />
          <View style={styles.onlyActiveRow}>
            <Checkbox checked={onlyActive} onChange={setOnlyActive} label="미완료된 미션 보기" />
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
                    : // 등록된 미션이 있는데 목록이 비었으면 전부 끝낸 것이다.
                      // "아직 미션이 없어요" 를 내보내면 방금 해낸 일을 부정하는 말이 된다.
                      items.length > 0
                      ? '미션을 모두 해냈어요!\n완료 이력에서 볼 수 있어요.'
                      : '아직 미션이 없어요.\n부모님이 등록해 주실 거예요!'
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
                status={item.status}
                endDate={item.endDate}
                // 끝낸 미션은 흐리게. 눌러서 내용은 볼 수 있지만 요청 버튼은 상세에서 빠진다
                dimmed={item.status === 'DONE'}
                onPress={() => openDetail(item.choreId)}
              />
            ))}
          </View>
        )}
      </ScrollView>
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
  // 체크박스는 칩 아래 줄의 오른쪽 끝 — 목록을 거르는 옵션이라 목록 바로 위에 붙인다
  onlyActiveRow: { flexDirection: 'row', justifyContent: 'flex-end' },
  // 위쪽 여백을 주지 않는다. Screen에 style={{ paddingTop: theme.space.sm }}로 헤더-배너
  // 간격을 이미 줬으니, 여기서 또 주면 중복으로 벌어진다 (상점·저축·투자와 같은 값).
  content: { paddingBottom: 16, flexGrow: 1 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
});
