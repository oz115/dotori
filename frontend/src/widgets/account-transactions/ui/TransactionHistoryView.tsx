import { Ionicons } from '@expo/vector-icons';
import { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Modal,
  Pressable,
  SectionList,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {
  useAccountTransactions,
  useMyAccount,
  type AccountInfo,
  type AccountTransaction,
  type TransactionDirection,
  type TransactionPeriod,
  type TransactionRangeFilter,
} from '@/features/account';
import { useProfile } from '@/features/auth';
import { apiErrorCode } from '@/shared/api';
import { formatDateShort, fromDateParam, monthsAgo, toDateParam } from '@/shared/lib/dateRange';
import { formatWon } from '@/shared/lib/money';
import { fontFamily, useTheme } from '@/shared/theme';
import { Button, DateRangeCalendar, Header, Screen, usePullToRefresh } from '@/shared/ui';

type TransactionHistoryViewProps = {
  onBack: () => void;
};

type RangeFilterKey = TransactionPeriod | 'CUSTOM';
type TypeFilterKey = TransactionDirection | 'ALL';

const RANGE_OPTIONS: { key: RangeFilterKey; label: string }[] = [
  { key: 'MONTH', label: '1개월' },
  { key: 'THREE_MONTHS', label: '3개월' },
  { key: 'CUSTOM', label: '직접설정' },
];

const TYPE_OPTIONS: { key: TypeFilterKey; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'DEPOSIT', label: '입금만' },
  { key: 'WITHDRAWAL', label: '출금만' },
];

/** 서버가 직접 지정 기간으로 허용하는 최대 폭 (resolveDateRange의 MAX_TRANSACTION_RANGE_MONTHS와 맞춘다) */
const MAX_CUSTOM_RANGE_MONTHS = 3;

/**
 * 부모 본인 계좌의 거래내역 화면 (ACC-TX-01). 계좌 요약 + 필터 요약바를 누르면 뜨는
 * 바텀시트에서 기간·유형을 고르는 구조다. 정렬(최신순/과거순)은 서버가 항상 최신순만
 * 내려줘서 고정 표기만 하고 바꿀 수 있는 옵션으로는 안 둔다.
 */
export function TransactionHistoryView({ onBack }: TransactionHistoryViewProps) {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  const account = useMyAccount();
  // 직접설정 달력의 하한 = 도토리 가입일. 계좌 개설도 가입 후라 그 전 날짜는 볼 게 없다.
  // (서버 제약은 "최대 3개월 폭"뿐이라 가입일까지 거슬러 올라가는 조회는 허용된다)
  const profile = useProfile();
  const signupDate =
    profile.data?.createdAt != null
      ? fromDateParam(profile.data.createdAt.slice(0, 10))
      : undefined;
  // TODO: 임시 디버그 로그 — bankName이 안 보이는 원인 확인되면 지운다
  if (__DEV__) {
    console.log('[debug] account.data.bankName =', JSON.stringify(account.data?.bankName));
  }

  const [rangeFilter, setRangeFilter] = useState<RangeFilterKey>('MONTH');
  const [type, setType] = useState<TypeFilterKey>('ALL');
  const [customStart, setCustomStart] = useState(() => monthsAgo(1));
  const [customEnd, setCustomEnd] = useState(() => new Date());
  const [sheetOpen, setSheetOpen] = useState(false);

  const range: TransactionRangeFilter = useMemo(() => {
    if (rangeFilter === 'CUSTOM') {
      return {
        mode: 'custom',
        startDate: toDateParam(customStart),
        endDate: toDateParam(customEnd),
      };
    }
    return { mode: 'period', period: rangeFilter };
  }, [rangeFilter, customStart, customEnd]);

  const transactionType = type === 'ALL' ? undefined : type;
  const { data, isPending, isError, error, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useAccountTransactions(range, transactionType);

  const sections = useMemo(
    () => groupByDate(data?.pages.flatMap((page) => page.transactions) ?? []),
    [data],
  );

  const notLinked = apiErrorCode(error) === 'ACCOUNT_NOT_LINKED';
  const summaryLabel = `${rangeLabel(rangeFilter, customStart, customEnd)} · ${typeLabel(type)} · 최신순`;

  return (
    <Screen
      header={<Header title={account.data?.accountName ?? '거래내역'} onBack={onBack} />}
      padded={false}
    >
      <SectionList
        refreshControl={refreshControl}
        sections={sections}
        keyExtractor={(item) => item.transactionUniqueNo}
        renderItem={({ item }) => <TransactionRow transaction={item} />}
        renderSectionHeader={({ section }) => (
          <View
            style={{
              backgroundColor: theme.color.background.canvas,
              paddingHorizontal: theme.layout.screenX,
              paddingTop: theme.space.md,
              paddingBottom: theme.space.xs,
            }}
          >
            <Text
              style={[
                theme.typography.label,
                { color: theme.color.text.strong, fontFamily: fontFamily.bold },
              ]}
            >
              {section.title}
            </Text>
          </View>
        )}
        onEndReached={() => {
          if (hasNextPage && !isFetchingNextPage) {
            fetchNextPage();
          }
        }}
        onEndReachedThreshold={0.4}
        contentContainerStyle={styles.content}
        ListHeaderComponent={
          <View
            style={{
              gap: theme.space.lg,
              marginBottom: theme.space.lg,
              paddingHorizontal: theme.layout.screenX,
            }}
          >
            <AccountSummary account={account.data} isPending={account.isPending} />

            <Pressable
              onPress={() => setSheetOpen(true)}
              accessibilityRole="button"
              style={[
                styles.summaryBar,
                {
                  backgroundColor: theme.color.background.surfaceSunken,
                  borderRadius: theme.radius.full,
                  paddingHorizontal: theme.space.lg,
                  gap: theme.space.sm,
                },
              ]}
            >
              <Ionicons name="search" size={18} color={theme.color.text.muted} />
              <Text
                style={[theme.typography.body, { color: theme.color.text.default, flex: 1 }]}
                numberOfLines={1}
              >
                {summaryLabel}
              </Text>
              <Ionicons name="chevron-down" size={18} color={theme.color.text.muted} />
            </Pressable>
          </View>
        }
        ListFooterComponent={
          isFetchingNextPage ? (
            <ActivityIndicator
              style={{ marginTop: theme.space.md }}
              color={theme.color.brand.solid}
            />
          ) : null
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
              {isPending
                ? '거래내역을 불러오는 중이에요.'
                : notLinked
                  ? '연동된 계좌가 없어요.'
                  : isError
                    ? '거래내역을 불러오지 못했어요.'
                    : '해당 기간의 거래내역이 없어요.'}
            </Text>
          </View>
        }
        showsVerticalScrollIndicator={false}
      />

      <FilterSheet
        visible={sheetOpen}
        rangeFilter={rangeFilter}
        type={type}
        customStart={customStart}
        customEnd={customEnd}
        minPickableDate={signupDate}
        onCancel={() => setSheetOpen(false)}
        onApply={(next) => {
          setRangeFilter(next.rangeFilter);
          setType(next.type);
          setCustomStart(next.customStart);
          setCustomEnd(next.customEnd);
          setSheetOpen(false);
        }}
      />
    </Screen>
  );
}

function AccountSummary({
  account,
  isPending,
}: {
  account: AccountInfo | null | undefined;
  isPending: boolean;
}) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.xs }}>
      {account ? (
        <Text style={[theme.typography.label, { color: theme.color.text.sub }]}>
          {account.bankName}
        </Text>
      ) : null}
      <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
        {account?.accountNo ?? (isPending ? '—' : '')}
      </Text>
      <Text style={[theme.typography.display, { color: theme.color.text.strong }]}>
        {isPending ? '—' : account ? formatWon(account.accountBalance) : '0원'}
      </Text>
    </View>
  );
}

type FilterRowProps<T extends string> = {
  options: { key: T; label: string }[];
  selected: T;
  onSelect: (key: T) => void;
};

/** 조회 기간·거래 유형 공용 칩 필터. */
function FilterRow<T extends string>({ options, selected, onSelect }: FilterRowProps<T>) {
  const theme = useTheme();

  return (
    <View style={[styles.filterRow, { gap: theme.space.sm }]}>
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

type FilterSheetProps = {
  visible: boolean;
  rangeFilter: RangeFilterKey;
  type: TypeFilterKey;
  customStart: Date;
  customEnd: Date;
  /** 직접설정 달력의 하한 (가입일). 프로필 로딩 전엔 잠시 제한 없이 열린다 */
  minPickableDate?: Date;
  onCancel: () => void;
  onApply: (next: {
    rangeFilter: RangeFilterKey;
    type: TypeFilterKey;
    customStart: Date;
    customEnd: Date;
  }) => void;
};

/**
 * 조회 기간·거래 유형을 고르는 바텀시트. 시트 안에서 고른 값은 "확인"을 눌러야 실제
 * 조회에 반영된다 — 칩을 누를 때마다 바로 재조회하면 "직접설정" 캘린더를 만지는 동안
 * 계속 불필요한 요청이 나간다.
 *
 * 내용(FilterSheetContent)은 열려 있을 때만 마운트한다. 그래야 열 때마다 draft 상태가
 * "지금 적용된 값"에서 새로 시작한다 — effect로 매번 되돌리는 대신 마운트 자체를
 * 다시 하는 쪽을 택했다.
 */
function FilterSheet({
  visible,
  rangeFilter,
  type,
  customStart,
  customEnd,
  minPickableDate,
  onCancel,
  onApply,
}: FilterSheetProps) {
  const theme = useTheme();

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onCancel}>
      <Pressable
        style={[styles.backdrop, { backgroundColor: theme.color.background.overlay }]}
        onPress={onCancel}
      >
        <Pressable
          style={[
            styles.sheet,
            {
              backgroundColor: theme.color.background.canvas,
              borderTopLeftRadius: theme.radius.xl,
              borderTopRightRadius: theme.radius.xl,
              padding: theme.space.xl,
              gap: theme.space.lg,
            },
          ]}
          onPress={(e) => e.stopPropagation()}
        >
          {visible ? (
            <FilterSheetContent
              rangeFilter={rangeFilter}
              type={type}
              customStart={customStart}
              customEnd={customEnd}
              minPickableDate={minPickableDate}
              onApply={onApply}
            />
          ) : null}
        </Pressable>
      </Pressable>
    </Modal>
  );
}

type FilterSheetContentProps = Omit<FilterSheetProps, 'visible' | 'onCancel'>;

function FilterSheetContent({
  rangeFilter,
  type,
  customStart,
  customEnd,
  minPickableDate,
  onApply,
}: FilterSheetContentProps) {
  const theme = useTheme();
  const [draftRange, setDraftRange] = useState(rangeFilter);
  const [draftType, setDraftType] = useState(type);
  const [draftStart, setDraftStart] = useState(customStart);
  const [draftEnd, setDraftEnd] = useState(customEnd);

  return (
    <>
      <View
        style={[styles.grabber, { backgroundColor: theme.color.border.strong }]}
        accessibilityElementsHidden
      />

      <View style={{ gap: theme.space.sm }}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          조회 기간
        </Text>
        <FilterRow options={RANGE_OPTIONS} selected={draftRange} onSelect={setDraftRange} />
      </View>

      {draftRange === 'CUSTOM' ? (
        <DateRangeCalendar
          start={draftStart}
          end={draftEnd}
          onChange={(start, end) => {
            setDraftStart(start);
            setDraftEnd(end);
          }}
          // 서버 제약(시작~종료 최대 3개월 폭)을 달력에서 미리 막는다.
          // 시작일 자체는 가입일까지 거슬러 올라갈 수 있다.
          maxRangeMonths={MAX_CUSTOM_RANGE_MONTHS}
          minPickableDate={minPickableDate}
        />
      ) : null}

      <View style={{ gap: theme.space.sm }}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          거래 유형
        </Text>
        <FilterRow options={TYPE_OPTIONS} selected={draftType} onSelect={setDraftType} />
      </View>

      <Button
        label="확인"
        variant="primary"
        onPress={() =>
          onApply({
            rangeFilter: draftRange,
            type: draftType,
            customStart: draftStart,
            customEnd: draftEnd,
          })
        }
      />
    </>
  );
}

function TransactionRow({ transaction }: { transaction: AccountTransaction }) {
  const theme = useTheme();
  const isDeposit = transaction.transactionType === 'DEPOSIT';
  const amountColor = isDeposit ? theme.color.status.success.text : theme.color.status.danger.text;

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
      <View style={{ flex: 1, gap: 2 }}>
        <Text
          style={[theme.typography.label, { color: theme.color.text.strong }]}
          numberOfLines={1}
        >
          {transaction.description || transaction.transactionTypeName}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          {formatTime(transaction.transactedAt)} · {transaction.transactionTypeName}
        </Text>
      </View>
      <View style={{ alignItems: 'flex-end', gap: 2 }}>
        <Text style={[theme.typography.label, { color: amountColor }]}>
          {isDeposit ? '+' : '-'}
          {formatWon(transaction.amount)}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          잔액 {formatWon(transaction.balanceAfter)}
        </Text>
      </View>
    </View>
  );
}

/** 날짜(YYYY-MM-DD 로컬 기준)가 바뀔 때마다 새 섹션을 만든다. 서버가 이미 최신순으로
 * 정렬해 주므로 같은 날짜는 항상 붙어 있다 — 별도 정렬 없이 순서대로 묶기만 하면 된다. */
function groupByDate(transactions: AccountTransaction[]) {
  const sections: { title: string; data: AccountTransaction[] }[] = [];
  for (const tx of transactions) {
    const title = formatSectionDate(tx.transactedAt);
    const last = sections[sections.length - 1];
    if (last && last.title === title) {
      last.data.push(tx);
    } else {
      sections.push({ title, data: [tx] });
    }
  }
  return sections;
}

/** "2025-09-07T13:45:30+09:00" → "09.07" (섹션 헤더용) */
function formatSectionDate(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getMonth() + 1)}.${pad(d.getDate())}`;
}

/** "2025-09-07T13:45:30+09:00" → "13:45" */
function formatTime(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function rangeLabel(key: RangeFilterKey, start: Date, end: Date): string {
  switch (key) {
    case 'WEEK':
      return '1주일';
    case 'MONTH':
      return '1개월';
    case 'THREE_MONTHS':
      return '3개월';
    case 'CUSTOM':
      return `${formatDateShort(start)} ~ ${formatDateShort(end)}`;
  }
}

function typeLabel(key: TypeFilterKey): string {
  return key === 'ALL' ? '전체' : key === 'DEPOSIT' ? '입금만' : '출금만';
}

const styles = StyleSheet.create({
  content: { paddingBottom: 40, flexGrow: 1 },
  summaryBar: { flexDirection: 'row', alignItems: 'center', minHeight: 48 },
  filterRow: { flexDirection: 'row', flexWrap: 'wrap' },
  chip: { paddingHorizontal: 16, paddingVertical: 8 },
  backdrop: { flex: 1, justifyContent: 'flex-end' },
  sheet: { width: '100%' },
  grabber: { width: 40, height: 4, borderRadius: 2, alignSelf: 'center' },
  row: { flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1 },
  emptyState: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 40 },
});
