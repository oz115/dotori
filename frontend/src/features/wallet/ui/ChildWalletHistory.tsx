import { router } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  RefreshControl,
  SectionList,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { splitCashoutRequests } from '@/features/wallet/lib/cashoutStatus';
import { pointTransactionReasonLabel } from '@/features/wallet/lib/pointTransactionLabels';
import { useCashoutRequests } from '@/features/wallet/model/useCashoutRequests';
import { usePointTransactions } from '@/features/wallet/model/usePointTransactions';
import type { PointTransactionItem } from '@/features/wallet/model/types';
import { CashoutRequestRow } from '@/features/wallet/ui/CashoutRequestRow';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import {
  Header,
  PeriodFilterControl,
  Screen,
  defaultPeriodFilter,
  periodDateRange,
} from '@/shared/ui';

type Tab = 'POINT' | 'CASHOUT';
type DirectionFilter = 'ALL' | 'IN' | 'OUT';

const TABS: { key: Tab; label: string }[] = [
  { key: 'POINT', label: '도토리 내역' },
  { key: 'CASHOUT', label: '환전 내역' },
];

const DIRECTIONS: { key: DirectionFilter; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'IN', label: '입금만' },
  { key: 'OUT', label: '출금만' },
];

type ChildWalletHistoryProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 직접설정 달력의 하한 (가입일). 포인트는 가입 전 기록이 있을 수 없다 */
  minPickableDate?: Date;
};

/**
 * 아이 거래 내역 화면 (홈 도토리 카드의 "거래 내역"). 도토리 내역 탭은 포인트
 * 입출금 원장(WLT-06)을 실계좌 거래내역처럼 기간·입출금 조건으로 조회하고,
 * 환전 내역 탭은 기존 환전 요청 목록을 그대로 보여준다.
 *
 * 입금/출금 필터는 서버 파라미터가 사유(type) 단위라 서버로 보내지 않고 받은
 * 목록에서 거른다 — 아이 한 명의 거래는 많지 않아 클라이언트 필터로 충분하다.
 */
export function ChildWalletHistory({
  familyId,
  childId,
  minPickableDate,
}: ChildWalletHistoryProps) {
  const theme = useTheme();
  const [tab, setTab] = useState<Tab>('POINT');

  return (
    <Screen
      header={
        <Header
          title="거래 내역"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/'))}
        />
      }
      padded={false}
    >
      <View
        style={{
          paddingHorizontal: theme.layout.screenX,
          paddingTop: theme.space.sm,
          paddingBottom: theme.space.sm,
        }}
      >
        <ChipRow options={TABS} selected={tab} onSelect={setTab} />
      </View>

      {tab === 'POINT' ? (
        <PointHistoryTab familyId={familyId} childId={childId} minPickableDate={minPickableDate} />
      ) : (
        <CashoutHistoryTab familyId={familyId} childId={childId} />
      )}
    </Screen>
  );
}

/** 탭·입출금 공용 칩 행. 단어장·상점의 필터 칩과 같은 모양이다. */
function ChipRow<T extends string>({
  options,
  selected,
  onSelect,
}: {
  options: { key: T; label: string }[];
  selected: T;
  onSelect: (key: T) => void;
}) {
  const theme = useTheme();

  return (
    <View style={[styles.chipRow, { gap: theme.space.sm }]}>
      {options.map((option) => {
        const active = option.key === selected;
        return (
          <Pressable
            key={option.key}
            accessibilityRole="button"
            accessibilityState={{ selected: active }}
            onPress={() => onSelect(option.key)}
            style={[
              styles.chip,
              {
                borderRadius: theme.radius.full,
                backgroundColor: active
                  ? theme.color.brand.solid
                  : theme.color.background.surfaceSunken,
              },
            ]}
          >
            <Text
              style={[
                theme.typography.label,
                { color: active ? theme.color.text.onBrand : theme.color.text.default },
              ]}
            >
              {option.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

function PointHistoryTab({ familyId, childId, minPickableDate }: ChildWalletHistoryProps) {
  const theme = useTheme();
  const [period, setPeriod] = useState(defaultPeriodFilter());
  const [direction, setDirection] = useState<DirectionFilter>('ALL');
  const [refreshing, setRefreshing] = useState(false);

  const range = periodDateRange(period);
  const query = usePointTransactions(familyId, childId, range);

  const items = useMemo(
    () => (query.data?.pages ?? []).flatMap((page) => page.page.items),
    [query.data],
  );
  const visible = useMemo(
    () => (direction === 'ALL' ? items : items.filter((i) => i.transactionDirection === direction)),
    [items, direction],
  );
  const sections = useMemo(() => groupByDate(visible), [visible]);

  const { refetch } = query;
  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await refetch();
    } finally {
      setRefreshing(false);
    }
  }, [refetch]);

  return (
    <SectionList
      sections={sections}
      keyExtractor={(item) => String(item.pointTransactionId)}
      showsVerticalScrollIndicator={false}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
      contentContainerStyle={styles.listContent}
      stickySectionHeadersEnabled={false}
      ListHeaderComponent={
        <View
          style={{
            gap: theme.space.md,
            paddingHorizontal: theme.layout.screenX,
            marginBottom: theme.space.sm,
          }}
        >
          <PeriodFilterControl
            value={period}
            onChange={setPeriod}
            minPickableDate={minPickableDate}
          />
          <ChipRow options={DIRECTIONS} selected={direction} onSelect={setDirection} />
        </View>
      }
      renderSectionHeader={({ section }) => (
        <Text
          style={[
            theme.typography.label,
            {
              color: theme.color.text.sub,
              paddingHorizontal: theme.layout.screenX,
              paddingTop: theme.space.md,
              paddingBottom: theme.space.xs,
            },
          ]}
        >
          {section.title}
        </Text>
      )}
      renderItem={({ item }) => <PointTransactionRow item={item} />}
      onEndReached={() => {
        if (query.hasNextPage && !query.isFetchingNextPage) {
          query.fetchNextPage();
        }
      }}
      onEndReachedThreshold={0.4}
      ListFooterComponent={
        query.isFetchingNextPage ? (
          <ActivityIndicator
            style={{ marginTop: theme.space.md }}
            color={theme.color.brand.solid}
          />
        ) : null
      }
      ListEmptyComponent={
        <View style={styles.empty}>
          <Text style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}>
            {query.isPending
              ? '내역을 불러오는 중이에요.'
              : query.isError
                ? '내역을 불러오지 못했어요.\n아래로 당겨 다시 시도해주세요.'
                : direction === 'ALL'
                  ? '이 기간에는 도토리가 움직인 기록이 없어요.'
                  : direction === 'IN'
                    ? '이 기간에는 들어온 도토리가 없어요.'
                    : '이 기간에는 나간 도토리가 없어요.'}
          </Text>
        </View>
      }
    />
  );
}

/** 거래 한 줄 — 실계좌 거래내역과 같은 배치 (설명·시각 / ±금액·잔액). */
function PointTransactionRow({ item }: { item: PointTransactionItem }) {
  const theme = useTheme();
  const isIn = item.transactionDirection === 'IN';
  const amountColor = isIn ? theme.color.status.success.text : theme.color.status.danger.text;
  const reasonLabel = pointTransactionReasonLabel(item.transactionType);

  return (
    <View
      style={[
        styles.row,
        {
          borderBottomColor: theme.color.border.card,
          paddingVertical: theme.space.md,
          paddingHorizontal: theme.layout.screenX,
        },
      ]}
    >
      <View style={styles.rowText}>
        <Text
          style={[theme.typography.label, { color: theme.color.text.strong }]}
          numberOfLines={1}
        >
          {item.description || reasonLabel}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          {formatTime(item.createdAt)} · {reasonLabel}
        </Text>
      </View>
      <View style={styles.rowAmount}>
        <Text style={[theme.typography.label, { color: amountColor }]}>
          {isIn ? '+' : '-'}
          {formatPoint(item.amount)}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          잔액 {formatPoint(item.balanceAfter)}
        </Text>
      </View>
    </View>
  );
}

/** 환전 내역 탭 — 기존 환전 요청 목록과 같은 구성 (기다리는 중 / 지난 내역). */
function CashoutHistoryTab({
  familyId,
  childId,
}: Pick<ChildWalletHistoryProps, 'familyId' | 'childId'>) {
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
    <SectionList
      sections={sections}
      keyExtractor={(item) => String(item.cashoutRequestId)}
      showsVerticalScrollIndicator={false}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
      contentContainerStyle={[
        styles.listContent,
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
        <View style={styles.empty}>
          <Text style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}>
            {isPending
              ? '내역을 불러오는 중이에요.'
              : isError
                ? '내역을 불러오지 못했어요.\n아래로 당겨 다시 시도해주세요.'
                : '아직 환전을 신청한 적이 없어요.\n도토리를 현금으로 바꿔 보세요!'}
          </Text>
        </View>
      }
    />
  );
}

/**
 * 날짜(KST 표기 기준)가 바뀔 때마다 새 섹션을 만든다. 서버가 최신순으로 주므로
 * 같은 날짜는 항상 붙어 있다 — 실계좌 거래내역과 같은 방식이다.
 */
function groupByDate(items: PointTransactionItem[]) {
  const sections: { title: string; data: PointTransactionItem[] }[] = [];
  for (const item of items) {
    const title = formatSectionDate(item.createdAt);
    const last = sections[sections.length - 1];
    if (last && last.title === title) {
      last.data.push(item);
    } else {
      sections.push({ title, data: [item] });
    }
  }
  return sections;
}

/** "2026-09-21T13:45:30+09:00" → "09.21" */
function formatSectionDate(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getMonth() + 1)}.${pad(d.getDate())}`;
}

/** "2026-09-21T13:45:30+09:00" → "13:45" */
function formatTime(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

const styles = StyleSheet.create({
  chipRow: { flexDirection: 'row', flexWrap: 'wrap' },
  chip: { paddingHorizontal: 16, paddingVertical: 8 },
  listContent: { paddingTop: 8, paddingBottom: 40, flexGrow: 1 },
  row: { flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1 },
  rowText: { flex: 1, gap: 2 },
  rowAmount: { alignItems: 'flex-end', gap: 2 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  emptyText: { textAlign: 'center' },
});
