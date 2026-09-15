import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { useStoreProducts } from '@/features/store/model/useStoreProducts';
import type { StoreProductSummary } from '@/features/store/model/types';
import { CategoryFilterBar, type CategoryFilterKey } from '@/features/store/ui/CategoryFilterBar';
import { StoreBanner } from '@/features/store/ui/StoreBanner';
import { StoreProductCard } from '@/features/store/ui/StoreProductCard';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type StoreCatalogProps = {
  familyId: number | undefined;
  /**
   * 넘기면 헤더 우측에 "+ 상품 등록" 버튼이 뜬다 (부모 전용). 자녀 화면은 이 prop을
   * 아예 넘기지 않는다 — role 을 이 feature 가 직접 알 필요가 없도록 라우트가 결정한다.
   */
  onRegisterPress?: () => void;
};

/** 상점 목록 화면 (SHOP-01). 부모·자녀 공용이고, 등록 버튼 유무만 라우트가 결정한다. */
export function StoreCatalog({ familyId, onRegisterPress }: StoreCatalogProps) {
  const theme = useTheme();
  const [category, setCategory] = useState<CategoryFilterKey>('ALL');
  const { data, isPending, isError } = useStoreProducts(familyId);

  const products = useMemo(() => filterByCategory(data ?? [], category), [data, category]);

  const registerButton = onRegisterPress ? (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel="상품 등록"
      onPress={onRegisterPress}
      hitSlop={8}
    >
      <Ionicons name="add" size={26} color={theme.color.text.strong} />
    </Pressable>
  ) : undefined;

  return (
    <Screen header={<Header title="상점" right={registerButton} />} padded={false}>
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
