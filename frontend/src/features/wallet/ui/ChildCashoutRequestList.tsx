import { router } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { RefreshControl, SectionList, StyleSheet, Text, View } from 'react-native';

import { splitCashoutRequests } from '@/features/wallet/lib/cashoutStatus';
import { useCashoutRequests } from '@/features/wallet/model/useCashoutRequests';
import { CashoutRequestRow } from '@/features/wallet/ui/CashoutRequestRow';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type ChildCashoutRequestListProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

/**
 * 내가 보낸 환전 요청 목록 (자녀). 홈 도토리 카드의 "환전 내역"에서 들어온다.
 *
 * 부모 화면과 같은 행을 쓰되 자녀 이름 배지와 필터는 없다 — 여기 뜨는 것은 전부 내 요청이라
 * 누구 것인지 가릴 일이 없다.
 *
 * 요청하면 도토리가 그 자리에서 빠지기 때문에, 아이 입장에서 가장 궁금한 것은 "내 도토리가
 * 어디까지 갔나" 다. 그래서 기다리는 중인 것을 위에 모아 두고 끝난 것을 아래에 쌓는다.
 */
export function ChildCashoutRequestList({ familyId, childId }: ChildCashoutRequestListProps) {
  const theme = useTheme();
  const [refreshing, setRefreshing] = useState(false);

  const { data, isPending, isError, refetch } = useCashoutRequests(familyId, childId);

  const sections = useMemo(() => {
    const { pending, history } = splitCashoutRequests(data ?? []);
    return [
      { title: '기다리는 중', data: pending },
      { title: '지난 내역', data: history },
    ].filter((section) => section.data.length > 0);
  }, [data]);

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
          title="환전 내역"
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
        renderItem={({ item }) => <CashoutRequestRow request={item} />}
        ListEmptyComponent={
          <View style={[styles.empty, { gap: theme.space.md }]}>
            <Text
              style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}
            >
              {isPending
                ? '내역을 불러오는 중이에요.'
                : isError
                  ? '내역을 불러오지 못했어요.\n아래로 당겨 다시 시도해주세요.'
                  : '아직 환전을 신청한 적이 없어요.\n도토리를 현금으로 바꿔 보세요!'}
            </Text>
          </View>
        }
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { paddingTop: 16, paddingBottom: 40, flexGrow: 1 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  emptyText: { textAlign: 'center' },
});
