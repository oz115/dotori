import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import {
  categoryBadgeColors,
  categoryImage,
  nearestSavingGoals,
  useStoreProducts,
} from '@/features/store';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { ContentCard, ProgressBar } from '@/shared/ui';

type SavingGoalsProps = {
  familyId: number | undefined;
  balance: number | undefined;
  /**
   * 카드 탭 시 상품 상세로 이동한다. (child)/(parent) 둘 다 `/store/[productId]` 모양의
   * 동적 라우트가 있어서, 그룹 접두사 없는 경로로 push하면 expo-router가 조용히 무시해버린다
   * — 그래서 이 위젯이 직접 push하지 않고, 어느 그룹인지 아는 라우트가 콜백으로 넘긴다.
   */
  onPressGoal?: (productId: number) => void;
};

const CARD_WIDTH = 180;

/**
 * "조금만 더 모으면!" 섹션. 상점 상품 중 잔액으로 아직 못 사는 것들을
 * 목표가에 가까운 순으로 보여준다. 해당하는 상품이 없으면 섹션 자체를 숨긴다.
 */
export function SavingGoals({ familyId, balance, onPressGoal }: SavingGoalsProps) {
  const theme = useTheme();
  const products = useStoreProducts(familyId);

  if (products.isPending) return null;

  const currentBalance = balance ?? 0;
  const goals = nearestSavingGoals(products.data ?? [], currentBalance);
  if (goals.length === 0) return null;

  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
        조금만 더 모으면!
      </Text>
      {/*
        ScrollView 는 콘텐츠 높이에 딱 맞춰지는데 카드 그림자는 그 밖으로 번져서
        아래가 잘린다 (Y+4, blur 20). 안쪽 패딩으로 번질 자리를 만들어 준다.
      */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={{ paddingTop: 4, paddingBottom: theme.space.lg }}
        style={{ marginBottom: -theme.space.md }}
      >
        <View style={{ flexDirection: 'row', gap: theme.space.md }}>
          {goals.map((goal) => {
            const badge = categoryBadgeColors(theme, goal.product.category);
            return (
              <Pressable
                key={goal.product.storeProductId}
                accessibilityRole="button"
                disabled={!onPressGoal}
                onPress={() => onPressGoal?.(goal.product.storeProductId)}
                style={({ pressed }) => ({ opacity: pressed ? 0.9 : 1 })}
              >
                <ContentCard
                  style={{
                    width: CARD_WIDTH,
                    padding: theme.space.md,
                    gap: theme.space.sm,
                  }}
                >
                  <View
                    style={[
                      styles.imageBox,
                      { backgroundColor: badge.bg, borderRadius: theme.radius.md },
                    ]}
                  >
                    <Image
                      source={categoryImage(goal.product.category)}
                      style={styles.image}
                      resizeMode="contain"
                    />
                  </View>

                  <View style={styles.titleRow}>
                    <Text
                      style={[theme.typography.label, { color: theme.color.text.strong, flex: 1 }]}
                      numberOfLines={1}
                    >
                      {goal.product.name}
                    </Text>
                    <Ionicons name="chevron-forward" size={16} color={theme.color.text.muted} />
                  </View>
                  <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
                    {formatPoint(goal.remaining)} 남았어요
                  </Text>

                  <ProgressBar value={goal.progress} />
                  <View style={styles.rangeRow}>
                    <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                      {formatPoint(currentBalance)}
                    </Text>
                    <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                      {formatPoint(goal.product.price)}
                    </Text>
                  </View>
                </ContentCard>
              </Pressable>
            );
          })}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  imageBox: { height: 72, alignItems: 'center', justifyContent: 'center' },
  image: { height: '70%', aspectRatio: 1 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  rangeRow: { flexDirection: 'row', justifyContent: 'space-between' },
});
