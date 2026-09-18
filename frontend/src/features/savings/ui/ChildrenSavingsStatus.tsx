import { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { buildSavingsCards } from '@/features/savings/lib/savingsView';
import { useSavingsProducts } from '@/features/savings/model/useSavingsProducts';
import {
  heldSubscriptions,
  useChildrenSavingsSubscriptions,
  type ChildSavings,
  type SavingsChild,
} from '@/features/savings/model/useChildrenSavingsSubscriptions';
import { SavingsProductCard } from '@/features/savings/ui/SavingsProductCard';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { Button, FilterChips, Header, Screen } from '@/shared/ui';

type Props = {
  familyId: number | undefined;
  /** 가족의 자녀 전체. 가족 슬라이스 값이라 라우트가 넘긴다 */
  childList: SavingsChild[];
  /** 상품 조회용 - 상품은 가족 단위 값이라 아무 자녀로 물어도 같다 */
  productChildId: number | undefined;
  onPressProduct?: (productId: number) => void;
  /**
   * 자녀 구역의 "거래 내역" 탭. 자녀별로 따로 두는 이유는 조회 API 가 아이 단위라,
   * 헤더에 하나로 두면 부모가 누구 것인지 또 골라야 하기 때문이다.
   */
  onPressHistory?: (childId: number) => void;
  onBack: () => void;
};

/**
 * 자녀 예적금 보유 현황 (부모 전용).
 *
 * 자녀별로 지금 들고 있는 상품을 보여 준다. 저축 메인은 상품 카탈로그라 누가 무엇을
 * 들었는지 알 수 없어서, 그 답만 따로 모아 둔 자리다.
 *
 * 자녀가 둘 이상일 때만 고르는 칩이 뜬다 - 한 명이면 고를 것이 없어 자리만 차지한다
 * (부모 완료 요청 목록과 같은 규칙). 칩을 안 고르면 자녀를 차례로 모두 보여 준다.
 *
 * 카드는 저축 메인과 같은 SavingsProductCard 다. 부모와 아이가 같은 상품을 이야기할 때
 * 서로 다른 모양을 보고 있으면 말이 어긋난다.
 */
export function ChildrenSavingsStatus({
  familyId,
  childList,
  productChildId,
  onPressProduct,
  onPressHistory,
  onBack,
}: Props) {
  const theme = useTheme();
  const [selected, setSelected] = useState<string | null>(null);

  const products = useSavingsProducts(familyId, productChildId);
  const children = useChildrenSavingsSubscriptions(familyId, childList);

  // 칩 컴포넌트가 문자열 키를 쓰므로 childId 를 문자열로 오간다 (부모 완료 요청과 같다).
  const filters = useMemo(
    () => childList.map((child) => ({ key: String(child.childId), label: child.name })),
    [childList],
  );

  const visible = useMemo(
    () =>
      selected == null
        ? children.items
        : children.items.filter((child) => String(child.childId) === selected),
    [children.items, selected],
  );

  const isPending = products.isPending || children.isPending;
  const isError = products.isError || children.isError;

  return (
    <Screen header={<Header title="자녀 예적금 보유 현황" onBack={onBack} />} padded={false}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingVertical: theme.layout.screenY,
          gap: theme.space.lg,
        }}
      >
        {childList.length > 1 ? (
          <FilterChips options={filters} selected={selected} onSelect={setSelected} />
        ) : null}

        {childList.length === 0 ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            아직 연결된 아이가 없어요.{'\n'}아이를 초대하면 보유 현황을 볼 수 있어요.
          </Text>
        ) : isPending ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            보유 현황을 불러오는 중이에요.
          </Text>
        ) : isError ? (
          <View style={{ gap: theme.space.md, alignItems: 'flex-start' }}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              보유 현황을 불러오지 못했어요.
            </Text>
            <Button
              label="다시 시도"
              variant="outline"
              onPress={() => {
                products.refetch();
                children.refetch();
              }}
            />
          </View>
        ) : (
          visible.map((child) => (
            <ChildSection
              key={child.childId}
              child={child}
              products={products.data ?? []}
              onPressProduct={onPressProduct}
              onPressHistory={onPressHistory}
            />
          ))
        )}
      </ScrollView>
    </Screen>
  );
}

/** 자녀 한 명의 구역 - 이름 · 합계 한 줄 · 보유 카드들 */
function ChildSection({
  child,
  products,
  onPressProduct,
  onPressHistory,
}: {
  child: ChildSavings;
  products: Parameters<typeof buildSavingsCards>[0];
  onPressProduct?: (productId: number) => void;
  onPressHistory?: (childId: number) => void;
}) {
  const theme = useTheme();
  const held = heldSubscriptions(child.subscriptions);

  // 보유한 상품만 남긴다. 카드 조립은 메인과 같은 함수를 쓰므로 진행바·만기 표시가 같다.
  const cards = useMemo(
    () => buildSavingsCards(products, held, new Date()).filter((card) => card.subscribed),
    [products, held],
  );

  // 원금 + 아직 받지 않은 이자. 보통예금 이자는 매주 지갑으로 바로 나가 여기 잡히지 않는다.
  const total = held.reduce((sum, s) => sum + s.totalAmount, 0);

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={[styles.sectionHead, { gap: theme.space.sm }]}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          {child.name}
        </Text>
        {held.length > 0 ? (
          <Text style={[theme.typography.bodySm, styles.grow, { color: theme.color.text.sub }]}>
            {held.length}개 · {formatPoint(total)}
          </Text>
        ) : (
          <View style={styles.grow} />
        )}
        {/* 가입이 없어도 내역은 남는다(해지·수령한 건) - 그래서 보유 여부와 무관하게 둔다 */}
        {onPressHistory ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${child.name}의 저축 거래 내역`}
            hitSlop={8}
            onPress={() => onPressHistory(child.childId)}
          >
            <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
              거래 내역 ›
            </Text>
          </Pressable>
        ) : null}
      </View>

      {cards.length === 0 ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          아직 가입한 예적금이 없어요.
        </Text>
      ) : (
        <View style={{ gap: theme.space.md }}>
          {cards.map((card) => (
            <SavingsProductCard
              key={card.productId}
              card={card}
              onPress={onPressProduct ? () => onPressProduct(card.productId) : undefined}
            />
          ))}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  sectionHead: { flexDirection: 'row', alignItems: 'baseline' },
  grow: { flex: 1 },
});
