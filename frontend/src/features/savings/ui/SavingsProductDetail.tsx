import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, View } from 'react-native';

import {
  buildSavingsCards,
  estimateReturn,
  savingsGroupOf,
} from '@/features/savings/lib/savingsView';
import { useSavingsExpectedReturn } from '@/features/savings/model/useSavingsExpectedReturn';
import { useSavingsProduct } from '@/features/savings/model/useSavingsProduct';
import {
  useContributeSavings,
  useReceiveSavings,
  useSubscribeSavings,
  useTerminateSavings,
} from '@/features/savings/model/useSavingsMutations';
import { useSavingsSubscriptions } from '@/features/savings/model/useSavingsSubscriptions';
import type {
  ContributionStyle,
  SavingsProductResponse,
  SavingsSubscription,
} from '@/features/savings/model/types';
import { apiErrorCode, isNetworkError } from '@/shared/api';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { SavingsProductHero } from '@/features/savings/ui/SavingsProductHero';
import { Button, Header, Input, ProgressBar, Screen } from '@/shared/ui';

type Props = {
  familyId: number | undefined;
  childId: number | undefined;
  productId: number;
};

/** 가입 금액 입력란의 라벨 — amount 의 의미가 상품마다 다르다 */
const AMOUNT_LABELS: Record<ContributionStyle, string> = {
  FREE: '먼저 넣을 금액',
  LUMP_SUM: '맡길 금액',
  FIXED_INSTALLMENT: '매주 납입할 금액',
  FLEXIBLE_GOAL: '목표 금액',
};

/**
 * 예적금 상품 상세 (Figma: 상단 연녹색 배너 + 상품 정보 카드 + 가입하기).
 * 미가입이면 스펙·금액 입력·만기 수령 미리보기와 가입하기 버튼을,
 * 가입 중이면 진행 정보·만기 예상과 납입/수령/해지 버튼을 보여준다.
 */
export function SavingsProductDetail({ familyId, childId, productId }: Props) {
  const theme = useTheme();
  const product = useSavingsProduct(familyId, childId, productId);
  const subscriptions = useSavingsSubscriptions(familyId, childId);

  // 진행 중(ACTIVE·MATURED) 가입이 있으면 이 화면은 "내 저축" 모드가 된다
  const subscription = (subscriptions.data ?? []).find(
    (s) => s.savingsProductId === productId && (s.status === 'ACTIVE' || s.status === 'MATURED'),
  );

  const goBack = () => (router.canGoBack() ? router.back() : router.replace('/save'));

  if (product.isPending || subscriptions.isPending) {
    return (
      <Shell title="저축 상품" onBack={goBack}>
        <View style={styles.center}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            상품 정보를 불러오는 중이에요.
          </Text>
        </View>
      </Shell>
    );
  }

  if (product.isError || subscriptions.isError || product.data == null) {
    return (
      <Shell title="저축 상품" onBack={goBack}>
        <View style={[styles.center, { gap: theme.space.lg }]}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            상품 정보를 불러오지 못했어요.
          </Text>
          <Button
            label="다시 시도"
            variant="outline"
            onPress={() => {
              product.refetch();
              subscriptions.refetch();
            }}
          />
        </View>
      </Shell>
    );
  }

  const groupLabel = savingsGroupOf(product.data.productType) === 'DEPOSIT' ? '예금' : '적금';

  return (
    <Shell title={groupLabel} onBack={goBack}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingVertical: theme.layout.screenY,
          gap: theme.space.lg,
        }}
      >
        <SavingsProductHero product={product.data} />

        {subscription != null ? (
          <SubscribedSection
            familyId={familyId}
            childId={childId}
            product={product.data}
            subscription={subscription}
          />
        ) : (
          <SignupSection familyId={familyId} childId={childId} product={product.data} />
        )}
      </ScrollView>
    </Shell>
  );
}

/** 헤더를 포함한 화면 틀. 로딩·오류·본문이 같은 틀을 쓴다 */
function Shell({
  title,
  onBack,
  children,
}: {
  title: string;
  onBack: () => void;
  children: React.ReactNode;
}) {
  return (
    <Screen header={<Header title={title} onBack={onBack} />} padded={false}>
      {children}
    </Screen>
  );
}

/** 라벨 왼쪽·값 오른쪽 정보 한 줄 (Figma: 상품 정보 카드) */
function InfoRow({ label, value, sub }: { label: string; value: string; sub?: string }) {
  const theme = useTheme();
  return (
    <View style={styles.infoRow}>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>{label}</Text>
      <View style={styles.infoValueBlock}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>{value}</Text>
        {sub ? (
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>{sub}</Text>
        ) : null}
      </View>
    </View>
  );
}

/** 흰 정보 카드 — 상점 카드와 같은 문법 (1px 테두리 + 옅은 그림자) */
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

/** 미가입 — 상품 스펙, 금액 입력, 만기 수령 미리보기, 가입하기 */
function SignupSection({
  familyId,
  childId,
  product,
}: {
  familyId: number | undefined;
  childId: number | undefined;
  product: SavingsProductResponse;
}) {
  const theme = useTheme();
  const subscribe = useSubscribeSavings(familyId, childId);
  const [raw, setRaw] = useState('');

  const amount = Number(raw.replace(/[^0-9]/g, '') || '0');
  const tooBig = amount > product.maxAmount;
  const valid = amount > 0 && !tooBig;
  const preview = valid ? estimateReturn(product, amount) : null;

  const specs: { label: string; value: string }[] = [];
  if (product.contributionStyle === 'FREE') {
    specs.push({ label: '이자', value: `매주 ${product.interestRate}%` });
    specs.push({ label: '기간', value: '자유롭게' });
  } else {
    if (product.termWeeks != null) {
      specs.push({
        label: '가입 기간',
        value:
          product.contributionStyle === 'FLEXIBLE_GOAL'
            ? `${product.termWeeks}주 안에`
            : `${product.termWeeks}주`,
      });
    }
    if (product.installmentCount != null) {
      specs.push({ label: '납입 횟수', value: `매주 1번, 총 ${product.installmentCount}회` });
    }
    specs.push({ label: '이자율', value: `${product.interestRate}%` });
  }
  specs.push({
    label: AMOUNT_LABELS[product.contributionStyle],
    value: `최대 ${formatPoint(product.maxAmount)}`,
  });

  const confirmSubscribe = () => {
    const summary =
      product.contributionStyle === 'FLEXIBLE_GOAL'
        ? `목표 ${formatPoint(amount)}로 가입할까요?\n납입은 가입 후에 자유롭게 해요.`
        : `${formatPoint(amount)}로 가입할까요?\n가입하면 바로 ${formatPoint(amount)}가 나가요.`;
    const warning =
      product.contributionStyle === 'FREE' ? '' : '\n중도에 해지하면 이자는 받을 수 없어요.';
    Alert.alert(`${product.name} 가입`, summary + warning, [
      { text: '아니요', style: 'cancel' },
      {
        text: '가입할래요',
        onPress: () =>
          subscribe.mutate(
            { savingsProductId: product.savingsProductId, amount },
            {
              onSuccess: () => {
                setRaw('');
                Alert.alert('가입했어요! 🎉', '약속한 날까지 차곡차곡 모아 보세요.');
              },
              onError: (error: unknown) =>
                Alert.alert('가입할 수 없어요', savingsErrorMessage(error)),
            },
          ),
      },
    ]);
  };

  return (
    <>
      <InfoCard title="상품 정보">
        <View style={{ gap: theme.space.lg }}>
          {specs.map((row) => (
            <InfoRow key={row.label} label={row.label} value={row.value} />
          ))}
        </View>
      </InfoCard>

      <InfoCard title={AMOUNT_LABELS[product.contributionStyle]}>
        <Input
          value={raw}
          onChangeText={(text) => setRaw(text.replace(/[^0-9]/g, ''))}
          keyboardType="number-pad"
          placeholder={`최대 ${formatPoint(product.maxAmount)}`}
          error={tooBig ? `최대 ${formatPoint(product.maxAmount)}까지 가입할 수 있어요` : undefined}
        />
        {preview ? (
          preview.weekly ? (
            <InfoRow
              label="매주 받는 이자"
              value={formatPoint(preview.interest)}
              sub="금액을 다음 주까지 유지하면 받아요"
            />
          ) : (
            <InfoRow
              label="만기 수령"
              value={formatPoint(preview.total)}
              sub={`원금 ${formatPoint(preview.principal)} + 이자 ${formatPoint(preview.interest)}`}
            />
          )
        ) : null}
      </InfoCard>

      {product.preSignupMessage ? (
        <View
          style={[
            styles.note,
            {
              backgroundColor: theme.color.background.surfaceSunken,
              borderRadius: theme.radius.full,
            },
          ]}
        >
          <Text
            style={[theme.typography.caption, styles.noteText, { color: theme.color.text.sub }]}
          >
            {product.preSignupMessage}
          </Text>
        </View>
      ) : null}

      <Button
        label="가입하기"
        variant="primary"
        disabled={!valid || subscribe.isPending}
        loading={subscribe.isPending}
        onPress={confirmSubscribe}
      />
    </>
  );
}

/** 가입 중 — 진행 정보·만기 예상과 납입/수령/해지 버튼 */
function SubscribedSection({
  familyId,
  childId,
  product,
  subscription,
}: {
  familyId: number | undefined;
  childId: number | undefined;
  product: SavingsProductResponse;
  subscription: SavingsSubscription;
}) {
  const theme = useTheme();
  const expected = useSavingsExpectedReturn(familyId, childId, subscription.savingsSubscriptionId);
  const contribute = useContributeSavings(familyId, childId);
  const receive = useReceiveSavings(familyId, childId);
  const terminate = useTerminateSavings(familyId, childId);
  const [raw, setRaw] = useState('');

  const busy = contribute.isPending || receive.isPending || terminate.isPending;
  const matured = subscription.status === 'MATURED';

  // 메인 카드와 같은 뷰모델을 그대로 써서 진행 정보 표기가 화면끼리 어긋나지 않게 한다
  const card = buildSavingsCards([product], [subscription], new Date())[0];

  // 자유 납입(보통예금·목표적금)만 금액을 입력받는다. 정액 적립식은 약정액 고정.
  const freeAmount =
    subscription.contributionStyle === 'FREE' || subscription.contributionStyle === 'FLEXIBLE_GOAL';
  const contributable = subscription.contributionStyle !== 'LUMP_SUM' && !matured;
  const amount = freeAmount ? Number(raw.replace(/[^0-9]/g, '') || '0') : subscription.principal;

  const runContribute = () => {
    Alert.alert('납입하기', `${formatPoint(amount)}를 납입할까요?`, [
      { text: '아니요', style: 'cancel' },
      {
        text: '납입할래요',
        onPress: () =>
          contribute.mutate(
            { savingsSubscriptionId: subscription.savingsSubscriptionId, amount },
            {
              onSuccess: (updated) => {
                setRaw('');
                if (updated.status === 'COMPLETED') {
                  Alert.alert(
                    '목표 달성! 🎉',
                    '원금과 이자를 바로 받았어요. 지갑을 확인해 보세요!',
                  );
                } else {
                  Alert.alert(
                    '납입했어요! 🌰',
                    `지금까지 ${formatPoint(updated.currentAmount)} 모았어요.`,
                  );
                }
              },
              onError: (error: unknown) =>
                Alert.alert('납입할 수 없어요', savingsErrorMessage(error)),
            },
          ),
      },
    ]);
  };

  const runReceive = () => {
    Alert.alert('만기 수령', `${formatPoint(subscription.totalAmount)}를 받을까요?`, [
      { text: '아니요', style: 'cancel' },
      {
        text: '받을래요',
        onPress: () =>
          receive.mutate(subscription.savingsSubscriptionId, {
            onSuccess: () => Alert.alert('받았어요! 🎉', '원금과 이자가 지갑으로 들어왔어요.'),
            onError: (error: unknown) => Alert.alert('받을 수 없어요', savingsErrorMessage(error)),
          }),
      },
    ]);
  };

  const runTerminate = () => {
    const lost = subscription.accruedInterest;
    Alert.alert(
      '중도 해지',
      `지금 해지하면 원금 ${formatPoint(subscription.currentAmount)}만 돌려받고` +
        (lost > 0
          ? `\n쌓인 이자 ${formatPoint(lost)}는 받을 수 없어요.`
          : '\n이자는 받을 수 없어요.'),
      [
        { text: '그만두기', style: 'cancel' },
        {
          text: '해지할래요',
          style: 'destructive',
          onPress: () =>
            terminate.mutate(subscription.savingsSubscriptionId, {
              onSuccess: (updated) =>
                Alert.alert(
                  '해지했어요',
                  `원금 ${formatPoint(updated.currentAmount)}가 지갑으로 돌아왔어요.`,
                ),
              onError: (error: unknown) =>
                Alert.alert('해지할 수 없어요', savingsErrorMessage(error)),
            }),
        },
      ],
    );
  };

  return (
    <>
      <InfoCard title="내 저축 현황">
        {matured ? (
          <View
            style={[
              styles.maturedBadge,
              {
                backgroundColor: theme.color.status.success.subtle,
                borderRadius: theme.radius.full,
              },
            ]}
          >
            <Text style={[theme.typography.caption, { color: theme.color.status.success.text }]}>
              🎉 만기 달성! 아래에서 받아 가세요
            </Text>
          </View>
        ) : null}
        <View style={{ gap: theme.space.lg }}>
          {card.lines.map((line) => (
            <InfoRow key={line.label} label={line.label} value={line.value} />
          ))}
          {expected.data != null ? (
            <InfoRow
              label={matured ? '받을 금액' : '만기에 받아요'}
              value={formatPoint(expected.data.expectedTotal)}
              sub={`원금 ${formatPoint(expected.data.currentAmount)} + 이자 ${formatPoint(expected.data.expectedInterest)}`}
            />
          ) : null}
        </View>
        {card.progress != null ? <ProgressBar value={card.progress} /> : null}
      </InfoCard>

      {contributable && freeAmount ? (
        <InfoCard title="납입할 금액">
          <Input
            value={raw}
            onChangeText={(text) => setRaw(text.replace(/[^0-9]/g, ''))}
            keyboardType="number-pad"
            placeholder="넣을 만큼 입력해요"
          />
        </InfoCard>
      ) : null}

      {matured ? (
        <Button
          label="만기 수령하기"
          variant="primary"
          disabled={busy}
          loading={receive.isPending}
          onPress={runReceive}
        />
      ) : contributable ? (
        <Button
          label={
            freeAmount ? '납입하기' : `이번 주 ${formatPoint(subscription.principal)} 납입하기`
          }
          variant="primary"
          disabled={busy || (freeAmount && amount <= 0)}
          loading={contribute.isPending}
          onPress={runContribute}
        />
      ) : null}

      {!matured ? (
        <Button
          label="중도 해지하기"
          variant="outline"
          disabled={busy}
          loading={terminate.isPending}
          onPress={runTerminate}
        />
      ) : null}
    </>
  );
}

/** 실패를 아이 눈높이 문구로 바꾼다. 네트워크 단절은 결과를 모르는 상태라 따로 안내. */
function savingsErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '인터넷 연결을 확인하고 다시 시도해 주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INSUFFICIENT_POINT':
      return '도토리가 부족해요. 할 일을 하고 도토리를 모아 보세요!';
    case 'SAVINGS_AMOUNT_EXCEEDS_LIMIT':
      return '가입할 수 있는 최대 금액을 넘었어요.';
    case 'SAVINGS_CONTRIBUTION_NOT_ALLOWED':
      return '지금은 납입할 수 없어요. 이번 주 납입을 이미 마쳤는지 확인해 보세요.';
    case 'SAVINGS_INVALID_CONTRIBUTION_AMOUNT':
      return '납입할 수 있는 금액이 아니에요. 금액을 다시 확인해 주세요.';
    case 'SAVINGS_NOT_MATURED':
      return '아직 만기가 되지 않았어요. 조금만 더 기다려요!';
    case 'SAVINGS_SUBSCRIPTION_NOT_ACTIVE':
      return '이미 끝난 저축이에요.';
    case 'SAVINGS_PRODUCT_INACTIVE':
      return '지금은 가입할 수 없는 상품이에요.';
    case 'DUPLICATE_POINT_REQUEST':
      return '방금 요청이 처리 중이에요. 잠시 후 확인해 주세요.';
    default:
      return '잠시 후 다시 시도해 주세요.';
  }
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  infoCard: {
    borderWidth: 1,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 12,
  },
  infoValueBlock: {
    alignItems: 'flex-end',
    gap: 2,
    flexShrink: 1,
  },
  note: {
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  noteText: {
    textAlign: 'center',
  },
  maturedBadge: {
    alignSelf: 'flex-start',
    paddingHorizontal: 10,
    paddingVertical: 3,
  },
});
