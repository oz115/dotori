import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import type { StoreMascotCharacter } from '@/features/store/lib/mascotImage';
import { useStoreProducts } from '@/features/store/model/useStoreProducts';
import type { StoreProductSummary } from '@/features/store/model/types';
import { CategoryFilterBar, type CategoryFilterKey } from '@/features/store/ui/CategoryFilterBar';
import { StoreBanner } from '@/features/store/ui/StoreBanner';
import { StoreProductCard } from '@/features/store/ui/StoreProductCard';
import { useTheme } from '@/shared/theme';
import { Header, HeaderChipButton, Screen, usePullToRefresh } from '@/shared/ui';

type StoreCatalogProps = {
  familyId: number | undefined;
  /** 배너에 쓸 마스코트. 라우트가 role 에 맞춰 넘긴다. */
  character: StoreMascotCharacter;
  /**
   * 넘기면 우측 하단에 "+" 플로팅 버튼이 뜬다 (부모 전용). 자녀 화면은 이 prop을
   * 아예 넘기지 않는다 — role 을 이 feature 가 직접 알 필요가 없도록 라우트가 결정한다.
   */
  onRegisterPress?: () => void;
  /**
   * 카드 탭 시 상세로 이동한다. (child)/(parent) 둘 다 `/store/[productId]` 모양의
   * 동적 라우트를 갖고 있어서, 그룹 접두사 없는 경로로 push하면 expo-router가 어느
   * 쪽으로 갈지 못 정하고 조용히 무시해버린다 — 그래서 이 feature가 직접 push하지
   * 않고, 어느 그룹인지 아는 라우트가 정확한 경로를 만들어 콜백으로 넘긴다.
   */
  onPressProduct: (productId: number) => void;
  /** 헤더 우측 "구매 내역" 아이콘 버튼의 이동 동작 (SHOP-07). 부모·자녀 둘 다 쓴다. */
  onHistoryPress: () => void;
};

/**
 * 상점 목록 화면 (SHOP-01). 부모·자녀 공용이고, 등록 버튼 유무·배너 캐릭터·상세 이동은 라우트가 결정한다.
 *
 * <p>FlatList 가 아니라 ScrollView + `stickyHeaderIndices` 다. 카테고리 칩이 스크롤해도
 * 상단에 붙어 있어야 하는데(QA: 상품이 많아지면 필터를 쓰려고 맨 위까지 올라가야 한다),
 * FlatList 는 헤더 전체(배너+칩)를 한 덩어리로만 붙일 수 있어 배너까지 따라 붙는다.
 * 가족 상점의 상품 수는 많아도 수십 개라 가상화를 포기해도 괜찮다.
 */
export function StoreCatalog({
  familyId,
  character,
  onRegisterPress,
  onPressProduct,
  onHistoryPress,
}: StoreCatalogProps) {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  const [category, setCategory] = useState<CategoryFilterKey>('ALL');
  const { data, isPending, isError, refetch } = useStoreProducts(familyId);

  const products = useMemo(() => filterByCategory(data ?? [], category), [data, category]);

  // 탭 화면은 전환해도 언마운트되지 않아서, 다른 탭에서 등록/구매가 일어나도 이 화면은
  // 그 변화를 모른다. 상점 탭으로 돌아올 때마다(앱을 껐다 켜지 않아도) 다시 받아온다.
  useFocusEffect(
    useCallback(() => {
      refetch();
    }, [refetch]),
  );

  return (
    <Screen
      header={
        <Header
          title="상점"
          right={<HeaderChipButton label="구매 내역" onPress={onHistoryPress} />}
        />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
      style={{ paddingTop: theme.space.sm }}
    >
      <ScrollView
        refreshControl={refreshControl}
        showsVerticalScrollIndicator={false}
        // 1 = 카테고리 바. 배너(0)는 그대로 밀려 올라가고 칩만 상단에 붙는다.
        stickyHeaderIndices={[1]}
        // 좌우 여백은 각 자식이 갖는다 — contentContainer 에 주면 붙어 있는 카테고리 바의
        // 배경이 화면 끝까지 닿지 않아, 그 여백 틈으로 지나가는 카드가 보인다.
        contentContainerStyle={styles.content}
      >
        <View style={{ paddingHorizontal: theme.layout.screenX, marginBottom: theme.space.lg }}>
          <StoreBanner character={character} />
        </View>

        {/* 붙어 있는 동안 카드가 뒤로 지나가므로 배경을 불투명하게 깔아 준다 */}
        <View
          style={{
            paddingHorizontal: theme.layout.screenX,
            paddingBottom: theme.space.lg,
            backgroundColor: theme.color.background.canvas,
          }}
        >
          <CategoryFilterBar selected={category} onSelect={setCategory} />
        </View>

        {products.length === 0 ? (
          <View style={styles.emptyState}>
            <Text
              style={[theme.typography.body, styles.emptyText, { color: theme.color.text.sub }]}
            >
              {isPending
                ? '상품을 불러오는 중이에요.'
                : isError
                  ? '상품을 불러오지 못했어요.'
                  : onRegisterPress
                    ? '아직 등록한 상품이 없어요.\n오른쪽 아래 + 버튼으로 만들어보세요.'
                    : '아직 상품이 없어요.\n부모님이 곧 채워주실 거예요!'}
            </Text>
          </View>
        ) : (
          <View
            style={[
              styles.grid,
              { paddingHorizontal: theme.layout.screenX, rowGap: theme.space.md },
            ]}
          >
            {products.map((item) => (
              // 폭을 48%로 고정한다. flex:1 로 두면 마지막 줄에 카드가 하나만 남았을 때
              // 행 전체로 늘어나(이미지·글자까지 통째로 커짐) 다른 줄과 달라 보인다.
              <View key={item.storeProductId} style={styles.cell}>
                <StoreProductCard
                  product={item}
                  onPress={() => onPressProduct(item.storeProductId)}
                />
              </View>
            ))}
          </View>
        )}
      </ScrollView>

      {onRegisterPress ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="상품 등록"
          onPress={onRegisterPress}
          style={({ pressed }) => [
            styles.fab,
            theme.shadow.card,
            {
              backgroundColor: pressed ? theme.color.brand.solidPressed : theme.color.brand.solid,
              borderRadius: theme.radius.full,
            },
          ]}
        >
          <Ionicons name="add" size={28} color={theme.color.text.onBrand} />
        </Pressable>
      ) : null}
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
  // 위쪽 여백을 주지 않는다. Screen에 style={{ paddingTop: theme.space.sm }}로 헤더-배너
  // 간격을 이미 줬으니, 여기서 또 주면 배너가 미션·저축·투자보다 내려가 시작 위치가 어긋난다.
  content: { paddingBottom: 40, flexGrow: 1 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
  cell: { width: '48%' },
  // 할 일 화면(ParentChoreList/ChildChoreList)의 빈 상태와 같은 위치에 오도록
  // paddingBottom 만 준다 (paddingTop 을 쓰면 두 탭의 빈 문구 높이가 달라 보인다).
  emptyState: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingBottom: 80 },
  emptyText: { textAlign: 'center' },
  fab: {
    position: 'absolute',
    right: 20,
    bottom: 24,
    width: 56,
    height: 56,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
