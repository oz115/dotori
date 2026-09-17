import { Image, Pressable, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { summarizeHoldings, type Holding } from '@/features/investment';
import { daysUntilMaturity, useSavingsSummary, type SavingsSummary } from '@/features/savings';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { ContentCard, MockBadge } from '@/shared/ui';

type ChildAssetsSectionProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·자녀 조회가 끝나기 전 — 아직 조회에 쓸 id 가 없다 */
  familyPending: boolean;
  onOpenSavings: () => void;
  onOpenInvestment: () => void;
};

/**
 * "자녀 자산" 섹션. 예적금은 실제 요약 API를 읽고, 투자는 아직 백엔드 도메인이 없어
 * mock 데이터라 그쪽 카드에만 예시 배지를 남긴다.
 *
 * 카드를 누르면 해당 탭(저축·투자)으로 보낸다 — 탭 화면 자체는 아직 준비 중 안내뿐이지만
 * 자리는 실제 라우트라 도메인이 생기면 이 화면은 그대로 두고 탭 내용만 채우면 된다.
 */
export function ChildAssetsSection({
  familyId,
  childId,
  familyPending,
  onOpenSavings,
  onOpenInvestment,
}: ChildAssetsSectionProps) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>자녀 자산</Text>

      <View style={[styles.row, { gap: theme.space.md }]}>
        <SavingsCard
          familyId={familyId}
          childId={childId}
          familyPending={familyPending}
          onPress={onOpenSavings}
        />
        <InvestmentCard onPress={onOpenInvestment} />
      </View>
    </View>
  );
}

type SavingsCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  familyPending: boolean;
  onPress: () => void;
};

/**
 * 예적금 요약 카드. 가입 건 하나하나가 아니라 집계만 보여 준다 — 부모 홈은 "지금 어떤가"를
 * 훑는 자리고, 계좌별 진행은 저축 탭이 맡는다.
 */
function SavingsCard({ familyId, childId, familyPending, onPress }: SavingsCardProps) {
  const theme = useTheme();
  const summary = useSavingsSummary(familyId, childId);
  const isPending = familyPending || summary.isPending;

  return (
    <Pressable style={styles.card} onPress={onPress} accessibilityRole="button">
      <ContentCard style={[styles.cardBody, { padding: theme.space.lg }]}>
        <View style={{ gap: theme.space.md }}>
          <View style={[styles.cardHeadRow, { gap: theme.space.xs }]}>
            <SectionIcon source={require('@/assets/images/invest/deposit.png')} />
            <Text style={[theme.typography.label, { color: theme.color.text.strong, flex: 1 }]}>
              예적금
            </Text>
          </View>

          <SavingsCardBody isPending={isPending} isError={summary.isError} summary={summary.data} />
        </View>

        {summary.data != null && summary.data.totalAccruedInterest > 0 ? (
          <Text
            style={[
              theme.typography.label,
              { color: theme.color.status.success.text, alignSelf: 'flex-end' },
            ]}
          >
            +{formatPoint(summary.data.totalAccruedInterest)}
          </Text>
        ) : null}
      </ContentCard>
    </Pressable>
  );
}

type SavingsCardBodyProps = {
  isPending: boolean;
  isError: boolean;
  summary: SavingsSummary | undefined;
};

function SavingsCardBody({ isPending, isError, summary }: SavingsCardBodyProps) {
  const theme = useTheme();

  if (isPending) {
    return <CardNotice text="불러오는 중이에요" />;
  }
  if (isError || summary == null) {
    return <CardNotice text="잠시 후 다시 확인해 주세요" />;
  }

  const joinedCount = summary.activeCount + summary.maturedCount;
  if (joinedCount === 0) {
    return <CardNotice text="아직 가입한 상품이 없어요" />;
  }

  const remainingDays = daysUntilMaturity(summary.nearestMaturityAt);

  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
        {joinedCount}계좌 납입 중
      </Text>

      <View style={{ gap: theme.space.xs }}>
        <SummaryRow label="모은 금액" value={formatPoint(summary.totalAmount)} />
        {remainingDays != null ? (
          <SummaryRow label="가장 빠른 만기" value={`D-${remainingDays}`} />
        ) : null}
      </View>

      {/* 만기가 와도 아이가 직접 수령을 눌러야 지갑에 들어온다. 부모가 챙겨 줄 수 있게 띄운다 */}
      {summary.maturedCount > 0 ? (
        <View
          style={[
            styles.badge,
            {
              backgroundColor: theme.color.status.warning.subtle,
              borderRadius: theme.radius.full,
            },
          ]}
        >
          <Text style={[theme.typography.caption, { color: theme.color.status.warning.text }]}>
            수령 대기 {summary.maturedCount}건
          </Text>
        </View>
      ) : null}
    </View>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  const theme = useTheme();

  return (
    <View style={styles.summaryRow}>
      <Text style={[theme.typography.caption, { color: theme.color.text.sub, flex: 1 }]}>
        {label}
      </Text>
      <Text style={[theme.typography.caption, { color: theme.color.text.strong }]}>{value}</Text>
    </View>
  );
}

function CardNotice({ text }: { text: string }) {
  const theme = useTheme();

  return <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>{text}</Text>;
}

/** 투자 카드 목업 — 자녀 보유 API 연동 시 교체한다. 예적금과 달리 아직 실데이터가 없다. */
const mockHoldings: Holding[] = [
  { id: 1, name: '시원아이스', image: 0, quantity: 1, buyPrice: 10100, currentPrice: 10250 },
  { id: 8, name: '고소한빵', image: 0, quantity: 1, buyPrice: 9900, currentPrice: 9800 },
  { id: 10, name: '파워배터리', image: 0, quantity: 1, buyPrice: 10200, currentPrice: 10400 },
];

function InvestmentCard({ onPress }: { onPress: () => void }) {
  const theme = useTheme();
  const summary = summarizeHoldings(mockHoldings);

  return (
    <Pressable style={styles.card} onPress={onPress} accessibilityRole="button">
      <ContentCard style={[styles.cardBody, { padding: theme.space.lg }]}>
        <View style={{ gap: theme.space.md }}>
          <View style={[styles.cardHeadRow, { gap: theme.space.xs }]}>
            <SectionIcon source={require('@/assets/images/invest/candles.png')} />
            <Text style={[theme.typography.label, { color: theme.color.text.strong, flex: 1 }]}>
              투자
            </Text>
            <MockBadge />
          </View>
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            {summary.count}개 보유 중
          </Text>
          <View style={{ gap: theme.space.xs }}>
            {mockHoldings.map((holding) => (
              <Text
                key={holding.id}
                style={[theme.typography.caption, { color: theme.color.text.sub }]}
                numberOfLines={1}
              >
                {holding.name}
              </Text>
            ))}
          </View>
        </View>

        <Text
          style={[
            theme.typography.label,
            { color: theme.color.status.success.text, alignSelf: 'flex-end' },
          ]}
        >
          +{formatPoint(summary.totalEarned)}
        </Text>
      </ContentCard>
    </Pressable>
  );
}

function SectionIcon({ source }: { source: ImageSourcePropType }) {
  return <Image source={source} style={styles.iconImage} resizeMode="contain" />;
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  card: { flex: 1 },
  // flex:1 이 카드(Pressable)가 늘어난 만큼 안쪽 ContentCard도 따라 늘어나게 한다 —
  // 그러지 않으면 두 카드 중 내용이 짧은 쪽만 흰 박스가 짧아 보인다.
  cardBody: { flex: 1, minHeight: 240, justifyContent: 'space-between' },
  cardHeadRow: { flexDirection: 'row', alignItems: 'center' },
  summaryRow: { flexDirection: 'row', alignItems: 'center' },
  badge: { paddingHorizontal: 8, paddingVertical: 2, alignSelf: 'flex-start' },
  iconImage: { width: 40, height: 40 },
});
