import { Image, Pressable, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import {
  previewHoldings,
  signedPoints,
  summarizeHoldings,
  useInvestmentHoldings,
  type Holding,
  type HoldingPreviewItem,
} from '@/features/investment';
import { daysUntilMaturity, useSavingsSummary, type SavingsSummary } from '@/features/savings';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { ContentCard } from '@/shared/ui';

type ChildAssetsSectionProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·자녀 조회가 끝나기 전 — 아직 조회에 쓸 id 가 없다 */
  familyPending: boolean;
  onOpenSavings: () => void;
  onOpenInvestment: () => void;
};

/**
 * "자녀 자산" 섹션. 예적금·투자 모두 자녀 기준 요약 API를 읽어 집계만 보여 준다.
 *
 * 카드를 누르면 해당 탭(저축·투자)으로 보낸다 — 투자 탭은 아직 준비 중 안내뿐이지만
 * 자리는 실제 라우트라 화면이 채워져도 이쪽은 그대로 둔다.
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
        <InvestmentCard
          childId={childId}
          familyPending={familyPending}
          onPress={onOpenInvestment}
        />
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

type InvestmentCardProps = {
  childId: number | undefined;
  familyPending: boolean;
  onPress: () => void;
};

/**
 * 투자 요약 카드. 자녀가 들고 있는 종목과 총 수익만 보여 준다 — 종목별 손익·차트는 투자 탭 몫이다.
 * 종목당 1주 규칙이라 수량은 항상 1이므로 줄마다 수량은 적지 않는다.
 */
function InvestmentCard({ childId, familyPending, onPress }: InvestmentCardProps) {
  const theme = useTheme();
  const holdings = useInvestmentHoldings(childId, { enabled: childId != null });
  const isPending = familyPending || childId == null || holdings.isPending;

  const totalEarned = holdings.data != null ? summarizeHoldings(holdings.data).totalEarned : 0;
  const hasHoldings = holdings.data != null && holdings.data.length > 0;

  return (
    <Pressable style={styles.card} onPress={onPress} accessibilityRole="button">
      <ContentCard style={[styles.cardBody, { padding: theme.space.lg }]}>
        <View style={{ gap: theme.space.md }}>
          <View style={[styles.cardHeadRow, { gap: theme.space.xs }]}>
            <SectionIcon source={require('@/assets/images/invest/candles.png')} />
            <Text style={[theme.typography.label, { color: theme.color.text.strong, flex: 1 }]}>
              투자
            </Text>
          </View>

          <InvestmentCardBody
            isPending={isPending}
            isError={holdings.isError}
            holdings={holdings.data}
          />
        </View>

        {/* 수익은 종목이 있을 때만 — 0원 손익을 크게 띄워 봐야 읽을 게 없다 */}
        {hasHoldings ? (
          <Text
            style={[
              theme.typography.label,
              {
                color:
                  totalEarned >= 0
                    ? theme.color.status.success.text
                    : theme.color.status.danger.text,
                alignSelf: 'flex-end',
              },
            ]}
          >
            {signedPoints(totalEarned)}
          </Text>
        ) : null}
      </ContentCard>
    </Pressable>
  );
}

type InvestmentCardBodyProps = {
  isPending: boolean;
  isError: boolean;
  holdings: Holding[] | undefined;
};

function InvestmentCardBody({ isPending, isError, holdings }: InvestmentCardBodyProps) {
  const theme = useTheme();

  if (isPending) {
    return <CardNotice text="불러오는 중이에요" />;
  }
  if (isError || holdings == null) {
    return <CardNotice text="잠시 후 다시 확인해 주세요" />;
  }
  if (holdings.length === 0) {
    return <CardNotice text="아직 투자한 종목이 없어요" />;
  }

  const preview = previewHoldings(holdings);

  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
        {holdings.length}개 보유 중
      </Text>

      {/* 아이콘이 28px 이라 줄 사이는 xs 로 좁혀 둔다 — 종목들이 한 덩어리로 읽힌다 */}
      <View style={{ gap: theme.space.xs }}>
        {preview.items.map((item) => (
          <HoldingPreviewRow key={item.id} item={item} />
        ))}
        {preview.restCount > 0 ? (
          <Text
            style={[
              theme.typography.caption,
              // 아이콘 폭 + 간격만큼 들여써서 위 종목 이름들과 왼쪽을 맞춘다
              { color: theme.color.text.muted, marginLeft: HOLDING_ICON_SIZE + theme.space.sm },
            ]}
          >
            외 {preview.restCount}개
          </Text>
        ) : null}
      </View>
    </View>
  );
}

/**
 * 투자 카드의 종목 한 줄 — 회사 아이콘과 이름.
 *
 * 키를 이름이 아니라 종목 id 로 잡는다. 이름은 서버 값이라 같은 이름이 둘 올 가능성을
 * 배제할 수 없고, 그때 React 가 한 줄을 건너뛴다.
 */
function HoldingPreviewRow({ item }: { item: HoldingPreviewItem }) {
  const theme = useTheme();

  return (
    <View style={[styles.holdingRow, { gap: theme.space.sm }]}>
      {/* 아이콘 원본이 흰 배경이라 배경색 없이 둥근 모서리로만 자른다 (HoldingCard 와 같은 처리) */}
      <Image source={item.image} style={styles.holdingIcon} resizeMode="cover" />
      <Text
        style={[theme.typography.caption, { color: theme.color.text.sub, flex: 1 }]}
        numberOfLines={1}
      >
        {item.name}
      </Text>
    </View>
  );
}

/**
 * 종목 줄의 아이콘 크기. caption 글자 높이(16)보다 크게 잡아 회사가 먼저 눈에 들어오게 한다 —
 * 줄 높이가 아이콘에 끌려 늘어나는 것을 받아들인 값이다.
 *
 * 카드는 minHeight 240 에 space-between 이라 이 정도는 흡수한다. 더 키우려면 이 상수만
 * 만지면 된다.
 */
const HOLDING_ICON_SIZE = 28;

/**
 * 종목 아이콘의 모서리. radius 토큰(8, 12 …)은 이 크기에 주면 너무 둥글다.
 * HoldingCard 의 비율(52px 에 12)을 그대로 옮긴 값이라 토큰을 쓰지 않고 직접 적는다.
 */
const HOLDING_ICON_RADIUS = 6;

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
  holdingRow: { flexDirection: 'row', alignItems: 'center' },
  holdingIcon: {
    width: HOLDING_ICON_SIZE,
    height: HOLDING_ICON_SIZE,
    borderRadius: HOLDING_ICON_RADIUS,
  },
});
