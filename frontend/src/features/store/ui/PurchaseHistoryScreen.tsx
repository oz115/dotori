import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { categoryIcon } from '@/features/store/lib/categoryIcon';
import { categoryLabel } from '@/features/store/lib/categoryLabel';
import { categoryBadgeColors } from '@/features/store/lib/categoryTheme';
import {
  currentYearMonth,
  formatYearMonthRange,
  shiftYearMonth,
  yearMonthDateRange,
  type YearMonth,
} from '@/features/store/lib/purchaseHistoryFilter';
import type { PurchaseHistoryItem } from '@/features/store/model/types';
import { usePurchaseHistory } from '@/features/store/model/usePurchaseHistory';
import { CategoryFilterBar, type CategoryFilterKey } from '@/features/store/ui/CategoryFilterBar';
import { PurchaseDetailModal } from '@/features/store/ui/PurchaseDetailModal';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type PurchaseHistoryScreenProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

type SortOrder = 'LATEST' | 'OLDEST';

/**
 * 구매 내역 (SHOP-07). 자녀 본인, 그 가족의 부모가 볼 수 있다.
 *
 * <p>가족당 구매 건수가 많지 않을 걸로 보고, 기간 조건 없이 전체를 한 번에 받아서 월
 * 이동·카테고리 필터·정렬을 전부 클라이언트에서 처리한다 — 상단 배너의 "지금까지 총 N번"은
 * 전체 기간 누적이라 이렇게 해야 월 필터와 무관하게 따로 계산할 수 있고, API 호출도 한 번으로
 * 끝난다.
 */
export function PurchaseHistoryScreen({ familyId, childId }: PurchaseHistoryScreenProps) {
  const theme = useTheme();
  const [yearMonth, setYearMonth] = useState<YearMonth>(currentYearMonth());
  const [category, setCategory] = useState<CategoryFilterKey>('ALL');
  const [sort, setSort] = useState<SortOrder>('LATEST');
  const [selected, setSelected] = useState<PurchaseHistoryItem | null>(null);
  const { data, isPending, isError } = usePurchaseHistory(familyId, childId);

  const monthRange = yearMonthDateRange(yearMonth);
  const visibleItems = useMemo(() => {
    const monthItems = (data ?? []).filter((item) => isWithinRange(item.purchasedAt, monthRange));
    return filterByCategory(monthItems, category);
  }, [data, monthRange, category]);
  // 백엔드가 이미 최신순(DESC)으로 내려주니, 오래된순은 뒤집기만 하면 된다.
  const sortedItems = sort === 'LATEST' ? visibleItems : [...visibleItems].reverse();

  const isNextMonthDisabled = compareYearMonth(yearMonth, currentYearMonth()) >= 0;

  return (
    <Screen header={<Header title="구매 내역" onBack={() => router.back()} />} padded={false}>
      <FlatList
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

            <View style={styles.monthBar}>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="이전 달"
                onPress={() => setYearMonth((prev) => shiftYearMonth(prev, -1))}
                hitSlop={8}
              >
                <Ionicons name="chevron-back" size={20} color={theme.color.text.default} />
              </Pressable>
              <View style={styles.monthLabel}>
                <Ionicons name="calendar-outline" size={16} color={theme.color.text.muted} />
                <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                  {formatYearMonthRange(yearMonth)}
                </Text>
              </View>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="다음 달"
                disabled={isNextMonthDisabled}
                onPress={() => setYearMonth((prev) => shiftYearMonth(prev, 1))}
                hitSlop={8}
              >
                <Ionicons
                  name="chevron-forward"
                  size={20}
                  color={isNextMonthDisabled ? theme.color.text.muted : theme.color.text.default}
                />
              </Pressable>
            </View>

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
    </Screen>
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

      <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
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

/** a가 b보다 이전이면 음수, 같으면 0, 이후면 양수. */
function compareYearMonth(a: YearMonth, b: YearMonth): number {
  return a.year * 12 + a.month - (b.year * 12 + b.month);
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
}

const styles = StyleSheet.create({
  content: { paddingBottom: 40, flexGrow: 1, paddingTop: 16 },
  monthBar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 16 },
  monthLabel: { flexDirection: 'row', alignItems: 'center', gap: 6 },
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
});
