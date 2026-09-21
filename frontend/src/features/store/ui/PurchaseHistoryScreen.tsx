import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useMemo, useState } from 'react';
import { FlatList, Image, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { Calendar, type DateData } from 'react-native-calendars';

import { categoryIcon } from '@/features/store/lib/categoryIcon';
import { categoryLabel } from '@/features/store/lib/categoryLabel';
import { categoryBadgeColors } from '@/features/store/lib/categoryTheme';
import type { PurchaseHistoryItem } from '@/features/store/model/types';
import { usePurchaseHistory } from '@/features/store/model/usePurchaseHistory';
import { CategoryFilterBar, type CategoryFilterKey } from '@/features/store/ui/CategoryFilterBar';
import { PurchaseDetailModal } from '@/features/store/ui/PurchaseDetailModal';
// 부작용 import — react-native-calendars 에 한글 월/요일 이름을 등록한다 (거래내역 화면과 동일)
import '@/shared/lib/calendarLocale';
import { buildCalendarTheme } from '@/shared/lib/calendarTheme';
import { useTheme, type Theme } from '@/shared/theme';
import { Button, Header, Screen, usePullToRefresh } from '@/shared/ui';

type PurchaseHistoryScreenProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

type SortOrder = 'LATEST' | 'OLDEST';

type DateRange = { start: Date; end: Date };

/**
 * 구매 내역 (SHOP-07). 자녀 본인, 그 가족의 부모가 볼 수 있다.
 *
 * <p>가족당 구매 건수가 많지 않을 걸로 보고, 기간 조건 없이 전체를 한 번에 받아서 기간
 * 필터·카테고리 필터·정렬을 전부 클라이언트에서 처리한다 — 상단 배너의 "지금까지 총 N번"은
 * 전체 기간 누적이라 이렇게 해야 기간 필터와 무관하게 따로 계산할 수 있고, API 호출도 한 번으로
 * 끝난다. 기간은 거래내역 화면과 같은 달력(react-native-calendars)으로 시작일~종료일을 고른다.
 */
export function PurchaseHistoryScreen({ familyId, childId }: PurchaseHistoryScreenProps) {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  // 기본 기간: 이번 달 1일 ~ 오늘
  const [range, setRange] = useState<DateRange>(() => ({ start: firstOfMonth(), end: new Date() }));
  const [category, setCategory] = useState<CategoryFilterKey>('ALL');
  const [sort, setSort] = useState<SortOrder>('LATEST');
  const [selected, setSelected] = useState<PurchaseHistoryItem | null>(null);
  const [pickerOpen, setPickerOpen] = useState(false);
  const { data, isPending, isError } = usePurchaseHistory(familyId, childId);

  const dateRange = useMemo(
    () => ({ startDate: toDateParam(range.start), endDate: toDateParam(range.end) }),
    [range],
  );
  const visibleItems = useMemo(() => {
    const inRange = (data ?? []).filter((item) => isWithinRange(item.purchasedAt, dateRange));
    return filterByCategory(inRange, category);
  }, [data, dateRange, category]);
  // 백엔드가 이미 최신순(DESC)으로 내려주니, 오래된순은 뒤집기만 하면 된다.
  const sortedItems = sort === 'LATEST' ? visibleItems : [...visibleItems].reverse();

  return (
    <Screen header={<Header title="구매 내역" onBack={() => router.back()} />} padded={false}>
      <FlatList
        refreshControl={refreshControl}
        data={sortedItems}
        keyExtractor={(item) => String(item.purchaseId)}
        renderItem={({ item }) => <PurchaseRow item={item} onPress={() => setSelected(item)} />}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space.sm },
        ]}
        ListHeaderComponent={
          <View style={{ gap: theme.space.lg, marginBottom: theme.space.lg }}>
            <CategoryFilterBar selected={category} onSelect={setCategory} />

            <Pressable
              accessibilityRole="button"
              accessibilityLabel="기간 선택"
              onPress={() => setPickerOpen(true)}
              hitSlop={8}
              style={[
                styles.periodBar,
                {
                  backgroundColor: theme.color.background.surface,
                  borderColor: theme.color.border.default,
                  borderRadius: theme.radius.lg,
                  paddingHorizontal: theme.space.lg,
                  paddingVertical: theme.space.md,
                },
              ]}
            >
              <Ionicons name="calendar-outline" size={16} color={theme.color.text.muted} />
              <Text
                style={[
                  theme.typography.label,
                  styles.periodLabel,
                  { color: theme.color.text.strong },
                ]}
              >
                {formatRangeLabel(range)}
              </Text>
              <Ionicons name="chevron-down" size={16} color={theme.color.text.muted} />
            </Pressable>

            <View
              style={[
                styles.summaryBanner,
                {
                  backgroundColor: theme.color.brand.subtle,
                  borderRadius: theme.radius.xl,
                  padding: theme.space.lg,
                  gap: theme.space.md,
                },
              ]}
            >
              <Image
                source={require('@/assets/images/store/mascot-purchase-summary.png')}
                style={styles.mascot}
                resizeMode="contain"
              />
              <Text
                style={[
                  theme.typography.bodySm,
                  styles.summaryText,
                  { color: theme.color.text.strong },
                ]}
              >
                {`지금까지 총 ${(data ?? []).length}번 구매했어요!\n도토리를 현명하게 사용하고 있어요!`}
              </Text>
            </View>

            <View style={styles.countRow}>
              <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                총 {sortedItems.length}건
              </Text>
              <Pressable
                accessibilityRole="button"
                onPress={() => setSort((prev) => (prev === 'LATEST' ? 'OLDEST' : 'LATEST'))}
                hitSlop={8}
                style={styles.sortToggle}
              >
                <Text style={[theme.typography.bodySm, { color: theme.color.text.default }]}>
                  {sort === 'LATEST' ? '최신순' : '오래된순'}
                </Text>
                <Ionicons name="swap-vertical" size={14} color={theme.color.text.muted} />
              </Pressable>
            </View>
          </View>
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
              {isPending
                ? '불러오는 중이에요.'
                : isError
                  ? '구매 내역을 불러오지 못했어요.'
                  : '이 기간엔 구매 내역이 없어요.'}
            </Text>
          </View>
        }
        showsVerticalScrollIndicator={false}
      />

      <PurchaseDetailModal item={selected} onClose={() => setSelected(null)} />

      <RangePickerModal
        visible={pickerOpen}
        range={range}
        onApply={(next) => {
          setRange(next);
          setPickerOpen(false);
        }}
        onClose={() => setPickerOpen(false)}
      />
    </Screen>
  );
}

/** 시작일~종료일을 달력에서 고른다 (거래내역 화면과 같은 react-native-calendars 기간 선택). */
function RangePickerModal({
  visible,
  range,
  onApply,
  onClose,
}: {
  visible: boolean;
  range: DateRange;
  onApply: (range: DateRange) => void;
  onClose: () => void;
}) {
  const theme = useTheme();
  const today = new Date();
  const [start, setStart] = useState(range.start);
  const [end, setEnd] = useState(range.end);
  const [awaitingEnd, setAwaitingEnd] = useState(false);

  // 열 때마다 현재 적용된 기간에서 시작한다 (지난번 임시 선택이 남지 않게).
  useEffect(() => {
    if (visible) {
      setStart(range.start);
      setEnd(range.end);
      setAwaitingEnd(false);
    }
  }, [visible, range.start, range.end]);

  const startStr = toDateParam(start);
  const endStr = toDateParam(end);

  // 첫 탭은 시작일, 다음 탭은 종료일. 시작일보다 앞을 누르면 다시 시작일부터 잡는다.
  const handleDayPress = (day: DateData) => {
    const picked = fromDateParam(day.dateString);
    if (!awaitingEnd || day.dateString < startStr) {
      setStart(picked);
      setEnd(picked);
      setAwaitingEnd(true);
      return;
    }
    setEnd(picked);
    setAwaitingEnd(false);
  };

  const markedDates = useMemo(
    () => buildRangeMarking(startStr, endStr, theme),
    [startStr, endStr, theme],
  );

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable style={styles.pickerBackdrop} onPress={onClose}>
        <Pressable
          style={[
            styles.pickerSheet,
            {
              backgroundColor: theme.color.background.canvas,
              borderRadius: theme.radius.xl,
              padding: theme.space.xl,
              gap: theme.space.md,
            },
          ]}
          onPress={(e) => e.stopPropagation()}
        >
          <Text
            style={[
              theme.typography.titleMd,
              styles.pickerTitle,
              { color: theme.color.text.strong },
            ]}
          >
            기간 선택
          </Text>

          <View style={styles.rangeRow}>
            <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
              {startStr.replaceAll('-', '.')}
            </Text>
            <Text style={[theme.typography.body, { color: theme.color.text.muted }]}> ~ </Text>
            <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
              {endStr.replaceAll('-', '.')}
            </Text>
          </View>
          <Text
            style={[
              theme.typography.caption,
              styles.pickerTitle,
              { color: theme.color.text.muted },
            ]}
          >
            {awaitingEnd ? '종료일을 선택해주세요.' : '시작일을 선택해주세요.'}
          </Text>

          <Calendar
            current={startStr}
            maxDate={toDateParam(today)}
            markingType="period"
            markedDates={markedDates}
            onDayPress={handleDayPress}
            theme={buildCalendarTheme(theme)}
            style={[styles.calendar, { borderColor: theme.color.border.default }]}
          />

          <View style={[styles.pickerActions, { gap: theme.space.sm }]}>
            <Button label="취소" variant="outline" onPress={onClose} style={styles.flex} />
            <Button
              label="적용"
              variant="primary"
              onPress={() => onApply({ start, end })}
              style={styles.flex}
            />
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function PurchaseRow({ item, onPress }: { item: PurchaseHistoryItem; onPress: () => void }) {
  const theme = useTheme();
  const category = item.category ?? 'ETC';
  const badge = categoryBadgeColors(theme, category);

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.lg,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.9 : 1,
        },
      ]}
    >
      <View
        style={[styles.iconWrap, { backgroundColor: badge.bg, borderRadius: theme.radius.full }]}
      >
        <Ionicons name={categoryIcon(category)} size={20} color={badge.text} />
      </View>

      <View style={{ flex: 1, gap: 4 }}>
        <Text
          numberOfLines={1}
          style={[theme.typography.label, { color: theme.color.text.strong }]}
        >
          {item.productName}
          {item.quantity > 1 ? ` x${item.quantity}` : ''}
        </Text>
        <View style={styles.metaRow}>
          <View
            style={[
              styles.categoryBadge,
              { backgroundColor: badge.bg, borderRadius: theme.radius.full },
            ]}
          >
            <Text style={[theme.typography.caption, { color: badge.text }]}>
              {categoryLabel(category)}
            </Text>
          </View>
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            {formatDate(item.purchasedAt)}
          </Text>
        </View>
      </View>

      <View style={styles.priceRow}>
        <Image
          source={require('@/assets/images/icons/icon-dotori.png')}
          style={styles.acornIcon}
          resizeMode="contain"
        />
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          {item.totalPrice.toLocaleString('ko-KR')}
        </Text>
      </View>
    </Pressable>
  );
}

function filterByCategory(
  items: PurchaseHistoryItem[],
  category: CategoryFilterKey,
): PurchaseHistoryItem[] {
  if (category === 'ALL') return items;
  return items.filter((item) => (item.category ?? 'ETC') === category);
}

/** purchasedAt(ISO datetime)이 "YYYY-MM-DD" 범위 안에 있는지. ISO 날짜는 문자열 비교로 충분하다. */
function isWithinRange(
  purchasedAt: string,
  range: { startDate: string; endDate: string },
): boolean {
  const date = purchasedAt.slice(0, 10);
  return date >= range.startDate && date <= range.endDate;
}

/** 이번 달 1일. */
function firstOfMonth(from = new Date()): Date {
  return new Date(from.getFullYear(), from.getMonth(), 1);
}

/** "2026.09.01 ~ 2026.09.18" */
function formatRangeLabel(range: DateRange): string {
  return `${toDateParam(range.start).replaceAll('-', '.')} ~ ${toDateParam(range.end).replaceAll('-', '.')}`;
}

/** Date → "YYYY-MM-DD" (로컬 기준, UTC 변환 없이). */
function toDateParam(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** "2026-09-07" → Date. new Date(문자열)은 UTC 자정으로 밀릴 수 있어 로컬 자정으로 만든다. */
function fromDateParam(dateString: string): Date {
  const [year, month, day] = dateString.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function addDays(date: Date, n: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + n);
  return d;
}

/** 시작~종료 사이를 period 마킹으로 칠한다 (거래내역 화면과 같은 방식). */
function buildRangeMarking(startStr: string, endStr: string, theme: Theme) {
  const marks: Record<
    string,
    { color: string; textColor: string; startingDay?: boolean; endingDay?: boolean }
  > = {};
  let cursor = fromDateParam(startStr);
  // 'YYYY-MM-DD' 문자열은 사전순 비교가 곧 날짜순 비교라 Date 객체 없이 비교할 수 있다.
  for (let key = toDateParam(cursor); key <= endStr; key = toDateParam(cursor)) {
    marks[key] = {
      color: theme.color.brand.solid,
      textColor: theme.color.text.onBrand,
      startingDay: key === startStr,
      endingDay: key === endStr,
    };
    cursor = addDays(cursor, 1);
  }
  return marks;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
}

const styles = StyleSheet.create({
  content: { paddingBottom: 40, flexGrow: 1, paddingTop: 16 },
  periodBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1,
  },
  periodLabel: { marginHorizontal: 2 },
  summaryBanner: { flexDirection: 'row', alignItems: 'center' },
  summaryText: { flex: 1 },
  mascot: { width: 56, height: 56 },
  countRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sortToggle: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  row: { flexDirection: 'row', alignItems: 'center' },
  iconWrap: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  categoryBadge: { paddingHorizontal: 8, paddingVertical: 2 },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  acornIcon: { width: 16, height: 16 },
  emptyState: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 60 },
  pickerBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  pickerSheet: { width: '100%' },
  pickerTitle: { textAlign: 'center' },
  rangeRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  calendar: { borderWidth: 1, borderRadius: 12 },
  pickerActions: { flexDirection: 'row' },
  flex: { flex: 1 },
});
