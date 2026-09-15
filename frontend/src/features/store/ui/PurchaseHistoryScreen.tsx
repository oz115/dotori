import { router } from 'expo-router';
import { useState } from 'react';
import { FlatList, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import {
  PURCHASE_HISTORY_FILTERS,
  purchaseHistoryDateRange,
  type PurchaseHistoryFilterKey,
} from '@/features/store/lib/purchaseHistoryFilter';
import type { PurchaseHistoryItem } from '@/features/store/model/types';
import { usePurchaseHistory } from '@/features/store/model/usePurchaseHistory';
import { PurchaseDetailModal } from '@/features/store/ui/PurchaseDetailModal';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type PurchaseHistoryScreenProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

/** 구매 내역 (SHOP-07). 자녀 본인, 그 가족의 부모가 볼 수 있다. */
export function PurchaseHistoryScreen({ familyId, childId }: PurchaseHistoryScreenProps) {
  const theme = useTheme();
  const [filter, setFilter] = useState<PurchaseHistoryFilterKey>('ALL');
  const [selected, setSelected] = useState<PurchaseHistoryItem | null>(null);
  const range = purchaseHistoryDateRange(filter);
  const { data, isPending, isError } = usePurchaseHistory(familyId, childId, range);

  return (
    <Screen header={<Header title="구매 내역" onBack={() => router.back()} />} padded={false}>
      <FlatList
        data={data ?? []}
        keyExtractor={(item) => String(item.purchaseId)}
        renderItem={({ item }) => <PurchaseRow item={item} onPress={() => setSelected(item)} />}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space.sm },
        ]}
        ListHeaderComponent={
          <View style={[styles.filterRow, { marginBottom: theme.space.lg, gap: theme.space.sm }]}>
            {PURCHASE_HISTORY_FILTERS.map((f) => {
              const active = f.key === filter;
              return (
                <Pressable
                  key={f.key}
                  accessibilityRole="button"
                  accessibilityState={{ selected: active }}
                  onPress={() => setFilter(f.key)}
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
                    {f.label}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
              {isPending
                ? '불러오는 중이에요.'
                : isError
                  ? '구매 내역을 불러오지 못했어요.'
                  : '구매 내역이 없어요.'}
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
          opacity: pressed ? 0.9 : 1,
        },
      ]}
    >
      <View style={{ flex: 1, gap: 2 }}>
        <Text
          numberOfLines={1}
          style={[theme.typography.label, { color: theme.color.text.strong }]}
        >
          {item.productName}
          {item.quantity > 1 ? ` x${item.quantity}` : ''}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          {formatDateTime(item.purchasedAt)}
        </Text>
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

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const styles = StyleSheet.create({
  content: { paddingBottom: 40, flexGrow: 1, paddingTop: 16 },
  filterRow: { flexDirection: 'row' },
  chip: { paddingHorizontal: 16, paddingVertical: 8 },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  acornIcon: { width: 16, height: 16 },
  emptyState: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 60 },
});
