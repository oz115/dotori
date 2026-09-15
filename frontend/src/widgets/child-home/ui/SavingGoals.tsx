import { Ionicons } from '@expo/vector-icons';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import {
  categoryIcon,
  mockStoreProducts,
  nearestSavingGoals,
  useStoreProducts,
} from '@/features/store';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { ContentCard, MockBadge, ProgressBar } from '@/shared/ui';

type SavingGoalsProps = {
  familyId: number | undefined;
  balance: number | undefined;
  /**
   * 카드 탭 시 상품 상세로 이동한다. (child)/(parent) 둘 다 `/store/[productId]` 모양의
   * 동적 라우트가 있어서, 그룹 접두사 없는 경로로 push하면 expo-router가 조용히 무시해버린다
   * — 그래서 이 위젯이 직접 push하지 않고, 어느 그룹인지 아는 라우트가 콜백으로 넘긴다.
   * 예시 데이터일 땐(usingMock) storeProductId 가 실제 상품이 아니라 눌러도 반응하지 않는다.
   */
  onPressGoal?: (productId: number) => void;
};

const CARD_WIDTH = 180;

/**
 * "조금만 더 모으면!" 섹션. 상점 상품 중 잔액으로 아직 못 사는 것들을
 * 목표가에 가까운 순으로 보여준다. 상품엔 이미지가 없어 카테고리 아이콘으로 대신한다.
 */
export function SavingGoals({ familyId, balance, onPressGoal }: SavingGoalsProps) {
  const theme = useTheme();
  const products = useStoreProducts(familyId);

  if (products.isPending) return null;

  const currentBalance = balance ?? 0;
  const realGoals = nearestSavingGoals(products.data ?? [], currentBalance);

  // 상점에 올라온 물건이 없으면 예시로 자리를 채운다 (배치 확인용 — "예시" 표시가 붙는다)
  const usingMock = realGoals.length === 0;
  const goals = usingMock ? nearestSavingGoals(mockStoreProducts, currentBalance) : realGoals;
  if (goals.length === 0) return null;

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={[styles.titleRow, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          조금만 더 모으면!
        </Text>
        {usingMock ? <MockBadge /> : null}
      </View>
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
          {goals.map((goal) => (
            <Pressable
              key={goal.product.storeProductId}
              accessibilityRole="button"
              disabled={usingMock || !onPressGoal}
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
                    { borderRadius: theme.radius.md, borderColor: theme.color.border.default },
                  ]}
                >
                  <Ionicons
                    name={categoryIcon(goal.product.category)}
                    size={28}
                    color={theme.color.text.muted}
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
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  imageBox: {
    height: 72,
    borderWidth: 1,
    borderStyle: 'dashed',
    alignItems: 'center',
    justifyContent: 'center',
  },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  rangeRow: { flexDirection: 'row', justifyContent: 'space-between' },
});
