import { Image, Pressable, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import {
  previewHoldings,
  signedPoints,
  summarizeHoldings,
  useInvestmentHoldings,
  type Holding,
  type HoldingPreviewItem,
} from '@/features/investment';
import {
  ongoingSavingsAccounts,
  previewSavingsAccounts,
  savingsMaturityMarker,
  savingsProductImage,
  savingsProductName,
  useSavingsSubscriptions,
  useSavingsSummary,
  type SavingsAccountPreviewItem,
  type SavingsSubscription,
} from '@/features/savings';
import { useTheme } from '@/shared/theme';
import { ContentCard } from '@/shared/ui';

type ChildAssetsSectionProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·자녀 조회가 끝나기 전 — 아직 조회에 쓸 id 가 없다 */
  familyPending: boolean;
  onOpenSavings: () => void;
  onOpenInvestment: () => void;
  /** 제목 옆 "리포트" — 수입·소비 통계와 자산 현황 화면으로 보낸다 */
  onOpenReport: () => void;
};

/**
 * "자녀 자산" 섹션. 예적금·투자 모두 자녀 기준 요약 API를 읽어 집계만 보여 준다.
 *
 * 카드를 누르면 해당 탭(저축·투자)으로 보낸다.
 */
export function ChildAssetsSection({
  familyId,
  childId,
  familyPending,
  onOpenSavings,
  onOpenInvestment,
  onOpenReport,
}: ChildAssetsSectionProps) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          자녀 자산
        </Text>
        <Pressable onPress={onOpenReport} hitSlop={8} accessibilityRole="button">
          <Text style={[theme.typography.label, { color: theme.color.text.sub }]}>리포트 ›</Text>
        </Pressable>
      </View>

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
 * 예적금 요약 카드. 납입 중인 계좌를 만기까지 남은 날과 함께 늘어놓고, 아래에 총 납입액을 둔다.
 *
 * <p>원래는 집계만 보여 주고 계좌별 진행은 저축 탭에 맡겼다. 이름만 나열해 두면 부모가
 * "언제 뭐가 끝나나"를 알 수 없어서, 그 판단을 뒤집고 계좌별 만기까지 올렸다.
 * 금액·진행률까지는 올리지 않는다 — 카드가 화면 절반 폭이라 한 줄에 더 들어가지 않는다.
 */
function SavingsCard({ familyId, childId, familyPending, onPress }: SavingsCardProps) {
  const theme = useTheme();
  const summary = useSavingsSummary(familyId, childId);
  const subscriptions = useSavingsSubscriptions(familyId, childId);

  // 두 쿼리를 합쳐서 판단한다. 한쪽만 보면 목록은 왔는데 금액이 비거나 그 반대인 중간 상태가
  // 화면에 그려진다. 예적금 탭이 같은 쿼리를 쓰므로 캐시는 공유된다.
  const isPending = familyPending || summary.isPending || subscriptions.isPending;
  const isError = summary.isError || subscriptions.isError;
  const accounts = subscriptions.data != null ? ongoingSavingsAccounts(subscriptions.data) : [];
  const hasAccounts = !isPending && !isError && accounts.length > 0;

  return (
    <Pressable style={styles.card} onPress={onPress} accessibilityRole="button">
      <ContentCard style={[styles.cardBody, { padding: theme.space.lg }]}>
        <View style={{ gap: theme.space.md }}>
          <View style={[styles.cardHeadRow, { gap: theme.space.xs }]}>
            <SectionIcon source={require('@/assets/images/invest/deposit.png')} />
            <Text style={[theme.typography.label, { color: theme.color.text.strong, flex: 1 }]}>
              저축
            </Text>
          </View>

          <SavingsCardBody isPending={isPending} isError={isError} accounts={accounts} />
        </View>

        {/* 총 납입액은 계좌가 있을 때만 — 0원을 크게 띄워 봐야 읽을 게 없다 (투자 카드와 같은 규칙) */}
        {hasAccounts && summary.data != null ? (
          <View style={styles.cardFootBlock}>
            {/* 색과 표기를 투자 카드의 합계와 맞춘다 — 두 카드가 나란히 서 있어서 한쪽만
                부호나 색이 다르면 같은 자리의 숫자가 다른 종류의 값처럼 보인다.
                포매터도 signedPoints 하나로 모아 두 카드가 같은 모양을 내게 한다 */}
            <Text style={[theme.typography.label, { color: theme.color.status.success.text }]}>
              {signedPoints(summary.data.totalCurrentAmount)}
            </Text>
            {/* 이자는 쌓였을 때만. 보통예금 이자는 매주 지갑으로 바로 나가 여기 안 잡힌다 */}
            {summary.data.totalAccruedInterest > 0 ? (
              <Text style={[theme.typography.label, { color: theme.color.status.success.text }]}>
                {signedPoints(summary.data.totalAccruedInterest)}
              </Text>
            ) : null}
          </View>
        ) : null}
      </ContentCard>
    </Pressable>
  );
}

type SavingsCardBodyProps = {
  isPending: boolean;
  isError: boolean;
  /** 이미 ACTIVE·MATURED 로 걸러 둔 가입 건 */
  accounts: SavingsSubscription[];
};

function SavingsCardBody({ isPending, isError, accounts }: SavingsCardBodyProps) {
  const theme = useTheme();

  if (isPending) {
    return <CardNotice text="불러오는 중이에요" />;
  }
  if (isError) {
    return <CardNotice text="잠시 후 다시 확인해 주세요" />;
  }
  if (accounts.length === 0) {
    return <CardNotice text="아직 가입한 상품이 없어요" />;
  }

  const preview = previewSavingsAccounts(accounts);

  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
        {accounts.length}계좌 납입 중
      </Text>

      {/* 아이콘이 28px 이라 줄 사이는 xs 로 좁혀 둔다 — 계좌들이 한 덩어리로 읽힌다 */}
      <View style={{ gap: theme.space.xs }}>
        {preview.items.map((item) => (
          <SavingsAccountPreviewRow key={item.savingsSubscriptionId} item={item} />
        ))}
        {preview.restCount > 0 ? (
          <Text
            style={[
              theme.typography.caption,
              // 아이콘 폭 + 간격만큼 들여써서 위 계좌 이름들과 왼쪽을 맞춘다
              { color: theme.color.text.muted, marginLeft: ASSET_ICON_SIZE + theme.space.sm },
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
 * 예적금 카드의 계좌 한 줄 — 상품 아이콘과 이름.
 *
 * 키를 이름이 아니라 가입 id 로 잡는다. 같은 상품을 두 번 가입할 수 있어서 이름이 겹치면
 * React 가 한 줄을 건너뛴다.
 */
function SavingsAccountPreviewRow({ item }: { item: SavingsAccountPreviewItem }) {
  const theme = useTheme();
  const marker = savingsMaturityMarker(item);

  return (
    <View style={[styles.assetRow, { gap: theme.space.sm }]}>
      <Image
        source={savingsProductImage(item.productType)}
        style={styles.assetIcon}
        resizeMode="contain"
      />
      {/* 이름이 남는 자리를 전부 가져가고 넘치면 말줄임 — 만기 표시에 밀리지 않게 한다.
          서버 productName(단리예금) 이 아니라 화면 이름(도토리 항아리)을 쓴다. 저축 탭과
          같은 이름이어야 한다 — 여섯·일곱 글자라 좁은 기기에서는 말줄임이 걸린다 */}
      <Text
        style={[theme.typography.caption, { color: theme.color.text.sub, flex: 1 }]}
        numberOfLines={1}
      >
        {savingsProductName(item.productType)}
      </Text>
      {marker != null ? (
        <Text
          style={[
            // caption(16) 으로는 이름과 나란히 둘 자리가 없다. bodySm(14) 이 토큰 중 다음 크기다
            theme.typography.bodySm,
            {
              // muted(neutral400) 는 흰 배경에서 2.06:1 로 읽히지 않는다. sub(neutral500) 는
              // 5.51:1 로 AA 를 넘기면서 이름과 같은 색이라, 14px 라는 크기 차이만으로
              // 이름이 주(主)로 남는다 — default(neutral700) 로 올리면 만기가 이름을 압도한다
              color:
                marker.kind === 'MATURED' ? theme.color.status.warning.text : theme.color.text.sub,
            },
          ]}
        >
          {marker.kind === 'MATURED' ? '만기' : `D-${marker.days}`}
        </Text>
      ) : null}
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
              { color: theme.color.text.muted, marginLeft: ASSET_ICON_SIZE + theme.space.sm },
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
    <View style={[styles.assetRow, { gap: theme.space.sm }]}>
      {/* 아이콘 원본이 흰 배경이라 배경색 없이 둥근 모서리로만 자른다 (HoldingCard 와 같은 처리) */}
      <Image source={item.image} style={styles.assetIcon} resizeMode="cover" />
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
 * 자산 줄(종목·계좌)의 아이콘 크기. caption 글자 높이(16)보다 크게 잡아 회사가 먼저 눈에 들어오게 한다 —
 * 줄 높이가 아이콘에 끌려 늘어나는 것을 받아들인 값이다.
 *
 * 카드는 minHeight 240 에 space-between 이라 이 정도는 흡수한다. 더 키우려면 이 상수만
 * 만지면 된다.
 */
const ASSET_ICON_SIZE = 28;

/**
 * 자산 아이콘의 모서리. radius 토큰(8, 12 …)은 이 크기에 주면 너무 둥글다.
 * HoldingCard 의 비율(52px 에 12)을 그대로 옮긴 값이라 토큰을 쓰지 않고 직접 적는다.
 */
const ASSET_ICON_RADIUS = 6;

function SectionIcon({ source }: { source: ImageSourcePropType }) {
  return <Image source={source} style={styles.iconImage} resizeMode="contain" />;
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  row: { flexDirection: 'row' },
  card: { flex: 1 },
  // flex:1 이 카드(Pressable)가 늘어난 만큼 안쪽 ContentCard도 따라 늘어나게 한다 —
  // 그러지 않으면 두 카드 중 내용이 짧은 쪽만 흰 박스가 짧아 보인다.
  cardBody: { flex: 1, minHeight: 240, justifyContent: 'space-between' },
  cardHeadRow: { flexDirection: 'row', alignItems: 'center' },
  iconImage: { width: 40, height: 40 },
  cardFootBlock: { alignItems: 'flex-end' },
  assetRow: { flexDirection: 'row', alignItems: 'center' },
  assetIcon: {
    width: ASSET_ICON_SIZE,
    height: ASSET_ICON_SIZE,
    borderRadius: ASSET_ICON_RADIUS,
  },
});
