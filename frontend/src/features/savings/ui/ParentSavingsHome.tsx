import { useMemo, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';

import { buildSavingsCards } from '@/features/savings/lib/savingsView';
import { useSavingsProducts } from '@/features/savings/model/useSavingsProducts';
import type { SavingsGroup } from '@/features/savings/model/types';
import { SavingsGroupSegment, savingsGroupLabel } from '@/features/savings/ui/SavingsGroupSegment';
import { SavingsIntroBanner } from '@/features/savings/ui/SavingsIntroBanner';
import { SavingsProductCard } from '@/features/savings/ui/SavingsProductCard';
import { useTheme } from '@/shared/theme';
import { Button, Header, HeaderChipButton, Screen } from '@/shared/ui';

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
 * 가입하지 않으므로 여기 카드는 언제나 상품 스펙(이자율·기간·최대 금액)을 보여 준다.
 * 누가 무엇을 들고 있는지는 헤더의 "자녀 예적금 보유 현황"에서 본다 - 자녀가 여럿이면
 * 이 목록 한 장에 다 담을 수 없어서다.
 *
 * "보유한 것만 보기" 체크박스도 두지 않는다. 부모에게는 보유가 없어 늘 빈 목록이 된다.
 */
export function ParentSavingsHome({
  familyId,
  childId,
  onPressProduct,
  onPressChildrenStatus,
}: Props) {
  const theme = useTheme();
  const [group, setGroup] = useState<SavingsGroup>('DEPOSIT');

  const products = useSavingsProducts(familyId, childId);

  // 가입 목록을 비워 넘기면 모든 카드가 스펙 모드로 조립된다 (buildSavingsCards).
  const cards = useMemo(
    () =>
      buildSavingsCards(products.data ?? [], [], new Date()).filter((card) => card.group === group),
    [products.data, group],
  );

  const groupLabel = savingsGroupLabel(group);

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
      padded={false}
      style={{ paddingVertical: theme.space.sm }}
    >
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingBottom: theme.layout.screenY,
          gap: theme.space.lg,
        }}
      >
        {/* 순서는 아이 화면(SavingsHome)과 같다 - 안내 배너를 읽고 나서 세그먼트를 만난다 */}
        <SavingsIntroBanner group={group} />

        <SavingsGroupSegment value={group} onChange={setGroup} />

        {childId == null ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            아직 연결된 아이가 없어요.{'\n'}아이를 초대하면 상품을 볼 수 있어요.
          </Text>
        ) : products.isPending ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            {groupLabel} 상품을 불러오는 중이에요.
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
