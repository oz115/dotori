import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { SAVINGS_GROUP_FILTERS, buildSavingsCards } from '@/features/savings/lib/savingsView';
import { useSavingsProducts } from '@/features/savings/model/useSavingsProducts';
import { useSavingsSubscriptions } from '@/features/savings/model/useSavingsSubscriptions';
import type { SavingsGroup } from '@/features/savings/model/types';
import { SavingsIntroBanner } from '@/features/savings/ui/SavingsIntroBanner';
import { SavingsProductCard } from '@/features/savings/ui/SavingsProductCard';
import { useTheme } from '@/shared/theme';
import {
  Button,
  FilterChips,
  Header,
  HeaderChipButton,
  Screen,
  Toggle,
  usePullToRefresh,
} from '@/shared/ui';

type Props = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 카드 탭 시 상세 화면으로 이동 */
  onPressCard?: (productId: number, subscriptionId?: number) => void;
  /** 헤더 "거래 내역" 탭 시 이동 */
  onPressHistory?: () => void;
};

/**
 * 저축 탭 메인 (Figma: 예적금 메인). 안내 배너 → 분류 칩 → 상품 카드를 세로로 쌓는다.
 * 가입된 상품은 진행 정보가, 미가입 상품은 스펙이 실리고, "보유한 것만"을 켜면
 * 가입된 카드만 남는다.
 *
 * 분류는 상점·미션과 같은 FilterChips 다 (전체 · 예금 · 적금). 예금/적금 세그먼트를
 * 쓰던 때는 한쪽을 반드시 골라야 해서 여섯 상품을 나란히 비교할 수가 없었고, 같은 앱
 * 안에서 목록을 걸러내는 장치가 화면마다 다른 모양이 됐다.
 */
export function SavingsHome({ familyId, childId, onPressCard, onPressHistory }: Props) {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  const [group, setGroup] = useState<SavingsGroup | null>(null);
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
    // group 이 null 이면 '전체' — 예금·적금을 한 목록에서 비교할 수 있다
    return all.filter(
      (card) => (group == null || card.group === group) && (!onlyMine || card.subscribed),
    );
  }, [products.data, subscriptions.data, group, onlyMine]);

  const isPending = products.isPending || subscriptions.isPending;
  const isError = products.isError || subscriptions.isError;

  return (
    <Screen
      header={
        <Header
          title="저축"
          right={
            onPressHistory ? (
              <HeaderChipButton label="거래 내역" onPress={onPressHistory} />
            ) : undefined
          }
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
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingBottom: theme.space['4xl'],
          gap: theme.space.lg,
        }}
      >
        {/* 안내 배너가 맨 위다. 아이는 예금·적금이 무엇인지부터 알아야 고를 수 있어서,
            둘의 차이를 읽고 나서 분류 칩을 만나는 순서로 둔다. */}
        <SavingsIntroBanner />

        {/* 칩과 체크박스는 한 줄이다 — 둘 다 이 목록을 걸러내는 장치라 위아래로 떼어
            놓으면 체크박스가 목록에 붙은 다른 기능처럼 보인다. */}
        <View style={[styles.filterRow, { gap: theme.space.md }]}>
          <View style={styles.chips}>
            <FilterChips options={SAVINGS_GROUP_FILTERS} selected={group} onSelect={setGroup} />
          </View>
          <Toggle checked={onlyMine} onChange={setOnlyMine} label="보유한 상품 보기" />
        </View>

        {isPending ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            상품을 불러오는 중이에요.
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
            {onlyMine
              ? '아직 가입한 상품이 없어요. 마음에 드는 상품을 골라 시작해 보세요!'
              : '보여 줄 상품이 없어요.'}
          </Text>
        ) : (
          <View style={{ gap: theme.space.md }}>
            {cards.map((card) => (
              <SavingsProductCard
                key={card.productId}
                card={card}
                onPress={
                  onPressCard ? () => onPressCard(card.productId, card.subscriptionId) : undefined
                }
              />
            ))}
          </View>
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  filterRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  // 칩이 남은 폭을 다 쓴다. 가로 스크롤이라 폭을 주지 않으면 체크박스를 밀어낸다.
  chips: {
    flex: 1,
  },
});
