import { useMemo } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { savingsGroupOf } from '@/features/savings/lib/savingsView';
import { useSavingsProduct } from '@/features/savings/model/useSavingsProduct';
import {
  heldSubscriptions,
  useChildrenSavingsSubscriptions,
  type SavingsChild,
} from '@/features/savings/model/useChildrenSavingsSubscriptions';
import type { SavingsProductResponse } from '@/features/savings/model/types';
import { savingsGroupLabel } from '@/features/savings/ui/SavingsGroupSegment';
import { SavingsProductHero } from '@/features/savings/ui/SavingsProductHero';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { Button, ChildBadge, Header, Screen } from '@/shared/ui';

type Props = {
  familyId: number | undefined;
  /** 상품 조회용. 상품은 가족 단위 값이라 아무 자녀로 물어도 같다 (라우트가 첫째를 넘긴다) */
  childId: number | undefined;
  productId: number;
  /** 보유 자녀를 찾으려면 가족의 자녀 전체가 필요하다. 가족 슬라이스 값이라 라우트가 넘긴다 */
  childList: SavingsChild[];
  onBack: () => void;
};

/**
 * 부모가 보는 예적금 상품 상세.
 *
 * 아이 상세와 달리 가입·납입·해지를 하지 않는다 - 예적금은 아이 이름으로 드는 것이라
 * 부모가 대신 가입할 경로가 서버에 없다. 그래서 이 화면은 **상품이 어떤 것인지**와
 * **누가 들고 있는지**만 말한다.
 */
export function ParentSavingsProductDetail({
  familyId,
  childId,
  productId,
  childList,
  onBack,
}: Props) {
  const theme = useTheme();
  const product = useSavingsProduct(familyId, childId, productId);
  const children = useChildrenSavingsSubscriptions(familyId, childList);

  // 이 상품을 지금 들고 있는 자녀. 해지·수령 완료 건은 "보유"로 치지 않는다.
  const holders = useMemo(
    () =>
      children.items.filter((child) =>
        heldSubscriptions(child.subscriptions).some((s) => s.savingsProductId === productId),
      ),
    [children.items, productId],
  );

  const title =
    product.data != null
      ? savingsGroupLabel(savingsGroupOf(product.data.productType))
      : '저축 상품';

  return (
    <Screen header={<Header title={title} onBack={onBack} />} padded={false}>
      {product.isPending ? (
        <Centered message="상품 정보를 불러오는 중이에요." />
      ) : product.isError || product.data == null ? (
        <View style={[styles.center, { gap: theme.space.lg }]}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            상품 정보를 불러오지 못했어요.
          </Text>
          <Button label="다시 시도" variant="outline" onPress={() => product.refetch()} />
        </View>
      ) : (
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingVertical: theme.layout.screenY,
            gap: theme.space.lg,
          }}
        >
          <SavingsProductHero product={product.data} />

          <InfoCard title="상품 정보">
            {productSpecs(product.data).map((spec) => (
              <InfoRow key={spec.label} label={spec.label} value={spec.value} />
            ))}
          </InfoCard>

          <InfoCard title="보유하고 있는 아이">
            {children.isPending ? (
              <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                불러오는 중이에요.
              </Text>
            ) : holders.length === 0 ? (
              <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                아직 이 상품을 든 아이가 없어요.
              </Text>
            ) : (
              <View style={[styles.holders, { gap: theme.space.sm }]}>
                {holders.map((child) => (
                  <ChildBadge key={child.childId} name={child.name} />
                ))}
              </View>
            )}
          </InfoCard>
        </ScrollView>
      )}
    </Screen>
  );
}

/**
 * 부모가 궁금해하는 것만 추린다 - 얼마까지 넣을 수 있고, 얼마나 묶이며, 얼마가 붙는가.
 *
 * 금액 라벨이 상품마다 다른 이유는 amount 의 의미가 달라서다. 거치식은 한 번에 맡기는
 * 돈, 정액 적립식은 매주 내는 돈, 목표적금은 모으려는 목표 금액이다.
 */
function productSpecs(product: SavingsProductResponse): { label: string; value: string }[] {
  const specs: { label: string; value: string }[] = [];

  if (product.contributionStyle === 'FREE') {
    // 보통예금 - 만기가 없고 이자가 매주 지갑으로 바로 들어간다
    specs.push({ label: '이자', value: `매주 ${product.interestRate}%` });
    specs.push({ label: '예치 기간', value: '자유롭게 (만기 없음)' });
  } else {
    specs.push({ label: '이자율', value: `${product.interestRate}%` });
    if (product.termWeeks != null) {
      specs.push({
        label: '예치 기간',
        value:
          product.contributionStyle === 'FLEXIBLE_GOAL'
            ? `${product.termWeeks}주 안에`
            : `${product.termWeeks}주`,
      });
    }
    if (product.installmentCount != null) {
      specs.push({ label: '납입 횟수', value: `매주 1번, 총 ${product.installmentCount}회` });
    }
  }

  specs.push({ label: maxAmountLabel(product), value: formatPoint(product.maxAmount) });
  return specs;
}

function maxAmountLabel(product: SavingsProductResponse): string {
  switch (product.contributionStyle) {
    case 'FREE':
      return '최대 보유 금액';
    case 'LUMP_SUM':
      return '최대 예치 금액';
    case 'FIXED_INSTALLMENT':
      return '매주 최대 납입 금액';
    case 'FLEXIBLE_GOAL':
      return '최대 목표 금액';
  }
}

/** 흰 정보 카드 - 아이 상세(SavingsProductDetail)와 같은 문법 */
function InfoCard({ title, children }: { title: string; children: React.ReactNode }) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.infoCard,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.xl,
          borderColor: theme.color.border.card,
          padding: theme.space.xl,
          gap: theme.space.lg,
        },
      ]}
    >
      <Text style={[theme.typography.label, { color: theme.color.text.sub }]}>{title}</Text>
      {children}
    </View>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  const theme = useTheme();

  return (
    <View style={styles.infoRow}>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>{label}</Text>
      <Text style={[theme.typography.label, styles.infoValue, { color: theme.color.text.strong }]}>
        {value}
      </Text>
    </View>
  );
}

function Centered({ message }: { message: string }) {
  const theme = useTheme();

  return (
    <View style={styles.center}>
      <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  infoCard: { borderWidth: 1 },
  infoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 12 },
  infoValue: { flexShrink: 1, textAlign: 'right' },
  holders: { flexDirection: 'row', flexWrap: 'wrap' },
});
