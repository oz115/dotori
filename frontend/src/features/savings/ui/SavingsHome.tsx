import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { buildSavingsCards } from '@/features/savings/lib/savingsView';
import { useSavingsProducts } from '@/features/savings/model/useSavingsProducts';
import { useSavingsSubscriptions } from '@/features/savings/model/useSavingsSubscriptions';
import type { SavingsGroup } from '@/features/savings/model/types';
import { SavingsIntroBanner } from '@/features/savings/ui/SavingsIntroBanner';
import { SavingsProductCard } from '@/features/savings/ui/SavingsProductCard';
import { useTheme } from '@/shared/theme';
import { Button, Checkbox, Header, Screen } from '@/shared/ui';

type Props = {
  familyId: number | undefined;
  childId: number | undefined;
  /**
   * 카드 탭 시 이동. 상세 화면 태스크가 아직이라 라우트가 안 넘기면 카드는 표시만 한다.
   */
  onPressCard?: (productId: number, subscriptionId?: number) => void;
};

const GROUPS: { key: SavingsGroup; label: string }[] = [
  { key: 'DEPOSIT', label: '예금' },
  { key: 'INSTALLMENT', label: '적금' },
];

/**
 * 저축 탭 메인 (Figma: 예적금 메인). 예금/적금 세그먼트 → 안내 배너 →
 * 상품 카드 2열 그리드. 가입된 상품은 진행 정보가, 미가입 상품은 스펙이 실리고,
 * "보유한 것만 보기"를 켜면 가입된 카드만 남는다.
 */
export function SavingsHome({ familyId, childId, onPressCard }: Props) {
  const theme = useTheme();
  const [group, setGroup] = useState<SavingsGroup>('DEPOSIT');
  const [onlyMine, setOnlyMine] = useState(false);

  const products = useSavingsProducts(familyId, childId);
  const subscriptions = useSavingsSubscriptions(familyId, childId);

  // 탭 화면은 전환해도 언마운트되지 않는다 — 돌아올 때마다 가입 현황을 다시 받아온다
  const refetchSubscriptions = subscriptions.refetch;
  useFocusEffect(
    useCallback(() => {
      refetchSubscriptions();
    }, [refetchSubscriptions]),
  );

  const cards = useMemo(() => {
    const all = buildSavingsCards(products.data ?? [], subscriptions.data ?? [], new Date());
    return all.filter((card) => card.group === group && (!onlyMine || card.subscribed));
  }, [products.data, subscriptions.data, group, onlyMine]);

  const groupLabel = group === 'DEPOSIT' ? '예금' : '적금';
  const isPending = products.isPending || subscriptions.isPending;
  const isError = products.isError || subscriptions.isError;

  return (
    <Screen header={<Header title="저축" />} padded={false}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingVertical: theme.layout.screenY,
          gap: theme.space.lg,
        }}
      >
        {/* 예금 | 적금 세그먼트 */}
        <View
          style={[
            styles.segment,
            {
              backgroundColor: theme.color.background.surface,
              borderColor: theme.color.border.card,
              borderRadius: theme.radius.full,
            },
          ]}
        >
          {GROUPS.map(({ key, label }) => {
            const active = key === group;
            return (
              <Pressable
                key={key}
                accessibilityRole="button"
                accessibilityState={{ selected: active }}
                onPress={() => setGroup(key)}
                style={[
                  styles.segmentItem,
                  active && {
                    backgroundColor: theme.color.brand.solid,
                    borderRadius: theme.radius.full,
                  },
                ]}
              >
                <Text
                  style={[
                    theme.typography.label,
                    { color: active ? theme.color.text.onBrand : theme.color.text.sub },
                  ]}
                >
                  {label}
                </Text>
              </Pressable>
            );
          })}
        </View>

        <View style={styles.filterRow}>
          <Checkbox
            checked={onlyMine}
            onChange={setOnlyMine}
            label={`보유한 ${groupLabel}만 보기`}
          />
        </View>

        <SavingsIntroBanner group={group} />

        {isPending ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            {groupLabel} 상품을 불러오는 중이에요.
          </Text>
        ) : isError ? (
          <View style={{ gap: theme.space.md, alignItems: 'flex-start' }}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              상품을 불러오지 못했어요.
            </Text>
            <Button
              label="다시 시도"
              variant="outline"
              onPress={() => {
                products.refetch();
                subscriptions.refetch();
              }}
            />
          </View>
        ) : cards.length === 0 ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            아직 가입한 {groupLabel}이 없어요. 마음에 드는 상품을 골라 시작해 보세요!
          </Text>
        ) : (
          <View style={[styles.grid, { gap: theme.space.md }]}>
            {cards.map((card) => (
              <View key={card.productId} style={styles.gridItem}>
                <SavingsProductCard
                  card={card}
                  onPress={
                    onPressCard ? () => onPressCard(card.productId, card.subscriptionId) : undefined
                  }
                />
              </View>
            ))}
          </View>
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  segment: {
    flexDirection: 'row',
    borderWidth: 1,
    padding: 4,
  },
  segmentItem: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 8,
  },
  filterRow: {
    alignItems: 'flex-end',
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  // 2열 그리드 — gap 을 고려해 절반보다 살짝 작게
  gridItem: {
    flexBasis: '47%',
    flexGrow: 1,
  },
});
