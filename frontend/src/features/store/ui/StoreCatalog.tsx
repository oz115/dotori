import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';

import { useStoreProducts } from '@/features/store/model/useStoreProducts';
import type { StoreProductSummary } from '@/features/store/model/types';
import { CategoryFilterBar, type CategoryFilterKey } from '@/features/store/ui/CategoryFilterBar';
import { StoreBanner } from '@/features/store/ui/StoreBanner';
import { StoreProductCard } from '@/features/store/ui/StoreProductCard';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type StoreCatalogProps = {
  familyId: number | undefined;
};

/** 자녀용 상점 목록 화면 (SHOP-01). 지금은 조회 전용이고, 구매는 이후 작업 범위다. */
export function StoreCatalog({ familyId }: StoreCatalogProps) {
  const theme = useTheme();
  const [category, setCategory] = useState<CategoryFilterKey>('ALL');
  const { data, isPending, isError } = useStoreProducts(familyId);

  const products = useMemo(() => filterByCategory(data ?? [], category), [data, category]);

  return (
    <Screen header={<Header title="상점" />} padded={false}>
      <FlatList
        data={products}
        keyExtractor={(item) => String(item.storeProductId)}
        numColumns={2}
        renderItem={({ item }) => (
          <View style={styles.cell}>
            <StoreProductCard
              product={item}
              onPress={() =>
                router.push({
                  pathname: '/store/[productId]',
                  params: { productId: String(item.storeProductId) },
                })
              }
            />
          </View>
        )}
        columnWrapperStyle={{ gap: theme.space.md }}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space.md },
        ]}
        ListHeaderComponent={
          <View style={{ gap: theme.space.lg, marginBottom: theme.space.lg }}>
            <StoreBanner />
            <CategoryFilterBar selected={category} onSelect={setCategory} />
          </View>
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
              {isPending
                ? '상품을 불러오는 중이에요.'
                : isError
                  ? '상품을 불러오지 못했어요.'
                  : '표시할 상품이 없어요.'}
            </Text>
          </View>
        }
        showsVerticalScrollIndicator={false}
      />
    </Screen>
  );
}

function filterByCategory(
  products: StoreProductSummary[],
  category: CategoryFilterKey,
): StoreProductSummary[] {
  return category === 'ALL' ? products : products.filter((p) => p.category === category);
}

const styles = StyleSheet.create({
  content: { paddingBottom: 40, flexGrow: 1 },
  cell: { flex: 1 },
  emptyState: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 40 },
});
