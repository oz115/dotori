import { router } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { RefreshControl, SectionList, StyleSheet, Text, View } from 'react-native';

import { splitCashoutRequests } from '@/features/wallet/lib/cashoutStatus';
import {
  useChildrenCashoutRequests,
  type CashoutChild,
  type ParentCashoutRequest,
} from '@/features/wallet/model/useChildrenCashoutRequests';
import { CashoutRequestModal } from '@/features/wallet/ui/CashoutRequestModal';
import { CashoutRequestRow } from '@/features/wallet/ui/CashoutRequestRow';
import { useTheme } from '@/shared/theme';
import { FilterChips, Header, Screen } from '@/shared/ui';

type ParentCashoutRequestListProps = {
  familyId: number | undefined;
  childList: CashoutChild[];
  /** 지금 보고 있는 부모의 userId. 상세가 "내 계좌"인지 가리는 데 쓴다 */
  viewerId?: number;
  /** 승인으로 부모 계좌에서 돈이 나가면 알린다. 계좌는 다른 슬라이스라 라우트가 갱신한다 */
  onApproved?: (childId: number, pointAmount: number) => void;
};

/**
 * 자녀 환전 요청 목록 · 처리 (부모). 부모 홈의 도토리 카드 "환전하기"에서 들어온다.
 *
 * 승인 대기와 지난 내역을 한 화면에 세로로 둔다. 탭으로 가르지 않는 이유는, 부모가 방금
 * 승인한 건이 어디로 갔는지 바로 눈에 보여야 "처리됐다"고 납득하기 때문이다 — 처리하면
 * 위 묶음에서 빠져 아래 묶음 맨 위로 옮겨 간다.
 *
 * 자녀 필터는 자녀가 둘 이상일 때만 뜬다 (완료 요청 목록과 같은 규칙).
 */
export function ParentCashoutRequestList({
  familyId,
  childList,
  viewerId,
  onApproved,
}: ParentCashoutRequestListProps) {
  const theme = useTheme();
  const [childId, setChildId] = useState<string | null>(null);
  const [openRequest, setOpenRequest] = useState<ParentCashoutRequest | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const { items, isPending, isError, refetch } = useChildrenCashoutRequests(familyId, childList);

  // 칩 컴포넌트가 문자열 키를 쓰므로 childId 를 문자열로 오간다 (완료 요청 목록과 같다).
  const filters = useMemo(
    () => childList.map((child) => ({ key: String(child.childId), label: child.name })),
    [childList],
  );

  const sections = useMemo(() => {
    const visible = childId == null ? items : items.filter((r) => String(r.childId) === childId);
    const { pending, history } = splitCashoutRequests(visible);
    return [
      { title: '승인 대기', data: pending },
      { title: '지난 내역', data: history },
    ].filter((section) => section.data.length > 0);
  }, [items, childId]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await refetch();
    } finally {
      setRefreshing(false);
    }
  }, [refetch]);

  return (
    <Screen
      header={
        <Header
          title="환전 요청"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/'))}
        />
      }
      padded={false}
    >
      <SectionList
        sections={sections}
        keyExtractor={(item) => String(item.cashoutRequestId)}
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
        renderSectionHeader={({ section }) => (
          <Text
            style={[
              theme.typography.label,
              { color: theme.color.text.sub, marginBottom: theme.space.xs },
            ]}
          >
            {section.title}
          </Text>
        )}
        renderItem={({ item }) => (
          <CashoutRequestRow
            request={item}
            childName={item.childName}
            onPress={() => setOpenRequest(item)}
          />
        )}
        ListEmptyComponent={
          <View style={[styles.empty, { gap: theme.space.md }]}>
            <Text
              style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}
            >
              {isPending
                ? '요청을 불러오는 중이에요.'
                : isError
                  ? '요청을 불러오지 못했어요.\n아래로 당겨 다시 시도해주세요.'
                  : childId != null
                    ? '이 아이의 환전 요청이 없어요.'
                    : '아직 환전 요청이 없어요.\n아이가 요청하면 여기에 보여요.'}
            </Text>
          </View>
        }
      />

      <CashoutRequestModal
        familyId={familyId}
        request={openRequest}
        viewerId={viewerId}
        onClose={() => setOpenRequest(null)}
        onApproved={onApproved}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { paddingTop: 16, paddingBottom: 40, flexGrow: 1 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  emptyText: { textAlign: 'center' },
});
