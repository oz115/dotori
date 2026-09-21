import { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { SAVINGS_GROUP_FILTERS, buildSavingsCards } from '@/features/savings/lib/savingsView';
import { useSavingsProducts } from '@/features/savings/model/useSavingsProducts';
import type { SavingsGroup } from '@/features/savings/model/types';
import { SavingsIntroBanner } from '@/features/savings/ui/SavingsIntroBanner';
import { SavingsProductCard } from '@/features/savings/ui/SavingsProductCard';
import { useTheme } from '@/shared/theme';
import { Button, FilterChips, Header, HeaderChipButton, Screen } from '@/shared/ui';

type Props = {
  familyId: number | undefined;
  /**
   * 상품 조회에 쓸 자녀. 경로가 `/children/{childId}/savings/products` 라 한 명이 필요한데,
   * 상품 6종과 maxAmount 는 **가족 단위 값**이라 누구로 물어도 같은 답이 온다. 그래서
   * 라우트가 첫째를 넘긴다 - 자녀가 아직 없으면 undefined 이고 화면은 안내만 띄운다.
   */
  childId: number | undefined;
  /** 카드 탭 시 상품 상세로 이동 */
  onPressProduct?: (productId: number) => void;
  /** 헤더 "보유 현황" 탭 시 이동 (자녀 예적금 보유 현황 화면) */
  onPressChildrenStatus?: () => void;
};

/**
 * 부모의 저축 탭 메인.
 *
 * 아이 화면(SavingsHome)과 같은 구성이지만 **가입 정보를 얹지 않는다.** 부모는 예적금에
 * 가입하지 않으므로 여기 카드는 언제나 상품 스펙(최대 금액·기간·받는 도토리)을 보여 준다.
 * 누가 무엇을 들고 있는지는 헤더의 "보유 현황"에서 본다 - 자녀가 여럿이면 이 목록 한 장에
 * 다 담을 수 없어서다.
 *
 * "보유한 것만" 체크박스도 두지 않는다. 부모에게는 보유가 없어 늘 빈 목록이 된다.
 */
export function ParentSavingsHome({
  familyId,
  childId,
  onPressProduct,
  onPressChildrenStatus,
}: Props) {
  const theme = useTheme();
  const [group, setGroup] = useState<SavingsGroup | null>(null);

  const products = useSavingsProducts(familyId, childId);

  // 가입 목록을 비워 넘기면 모든 카드가 스펙 모드로 조립된다 (buildSavingsCards).
  const cards = useMemo(
    () =>
      buildSavingsCards(products.data ?? [], [], new Date()).filter(
        (card) => group == null || card.group === group,
      ),
    [products.data, group],
  );

  return (
    <Screen
      header={
        <Header
          title="저축"
          right={
            onPressChildrenStatus ? (
              // 아이 화면의 "거래 내역"과 같은 네 글자로 맞춘다. 길면 칩이 헤더 폭을
              // 먹어 가운데 정렬인 제목이 왼쪽으로 밀린다 (누구의 보유인지는 들어간
              // 화면 제목이 "자녀 예적금 보유 현황" 으로 다시 말해 준다).
              <HeaderChipButton
                label="보유 현황"
                accessibilityLabel="자녀 예적금 보유 현황"
                onPress={onPressChildrenStatus}
              />
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
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingBottom: theme.space['4xl'],
          gap: theme.space.lg,
        }}
      >
        {/* 순서는 아이 화면(SavingsHome)과 같다 - 안내 배너를 읽고 나서 분류 칩을 만난다 */}
        <SavingsIntroBanner />

        <View style={styles.filterRow}>
          <FilterChips options={SAVINGS_GROUP_FILTERS} selected={group} onSelect={setGroup} />
        </View>

        {childId == null ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            아직 연결된 아이가 없어요.{'\n'}아이를 초대하면 상품을 볼 수 있어요.
          </Text>
        ) : products.isPending ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            상품을 불러오는 중이에요.
          </Text>
        ) : products.isError ? (
          <View style={{ gap: theme.space.md, alignItems: 'flex-start' }}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              상품을 불러오지 못했어요.
            </Text>
            <Button label="다시 시도" variant="outline" onPress={() => products.refetch()} />
          </View>
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
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  // 체크박스가 없어도 아이 화면과 같은 한 줄 자리를 쓴다 (칩 높이가 줄에 맞게 고정된다)
  filterRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
});
