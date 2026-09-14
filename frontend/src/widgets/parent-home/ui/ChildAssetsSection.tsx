import { Image, Pressable, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { mockHoldings, summarizeHoldings } from '@/features/investment';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { ContentCard, MockBadge, ProgressBar } from '@/shared/ui';

type ChildAssetsSectionProps = {
  onOpenSavings: () => void;
  onOpenInvestment: () => void;
};

/**
 * "자녀 투자 내역" 섹션. 예적금·투자 둘 다 아직 백엔드 도메인이 없어(예적금은 아예 없고,
 * 투자는 mock 데이터뿐) 전부 예시로 채운다. 두 도메인이 생기면 각자 실제 훅으로 바꾼다.
 *
 * 카드를 누르면 해당 탭(저축·투자)으로 보낸다 — 탭 화면 자체는 아직 준비 중 안내뿐이지만
 * 자리는 실제 라우트라 도메인이 생기면 이 화면은 그대로 두고 탭 내용만 채우면 된다.
 */
export function ChildAssetsSection({ onOpenSavings, onOpenInvestment }: ChildAssetsSectionProps) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={[styles.titleRow, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          자녀 투자 내역
        </Text>
        <MockBadge />
      </View>

      <View style={[styles.row, { gap: theme.space.md }]}>
        <SavingsCard onPress={onOpenSavings} />
        <InvestmentCard onPress={onOpenInvestment} />
      </View>
    </View>
  );
}

/** 예적금 목업 — 백엔드에 예적금 도메인 자체가 아직 없어 이 화면 전용으로 둔다. */
const mockSavingsGoals = [
  { id: 1, name: '도토리 첫 예금', currentDay: 16, totalDay: 30 },
  { id: 2, name: '소원 성취 적금', currentDay: 6, totalDay: 7 },
];
const mockSavingsReward = 500;

function SavingsCard({ onPress }: { onPress: () => void }) {
  const theme = useTheme();

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
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            {mockSavingsGoals.length}계좌 납입 중
          </Text>

          <View style={{ gap: theme.space.md }}>
            {mockSavingsGoals.map((goal) => (
              <View key={goal.id} style={{ gap: theme.space.xs }}>
                <View style={styles.goalRow}>
                  <Text
                    style={[theme.typography.caption, { color: theme.color.text.sub, flex: 1 }]}
                    numberOfLines={1}
                  >
                    {goal.name}
                  </Text>
                  <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                    {goal.currentDay}일 / {goal.totalDay}일
                  </Text>
                </View>
                <ProgressBar value={goal.currentDay / goal.totalDay} />
              </View>
            ))}
          </View>
        </View>

        <Text
          style={[
            theme.typography.label,
            { color: theme.color.status.success.text, alignSelf: 'flex-end' },
          ]}
        >
          +{formatPoint(mockSavingsReward)}
        </Text>
      </ContentCard>
    </Pressable>
  );
}

function InvestmentCard({ onPress }: { onPress: () => void }) {
  const theme = useTheme();
  const summary = summarizeHoldings(mockHoldings);

  return (
    <Pressable style={styles.card} onPress={onPress} accessibilityRole="button">
      <ContentCard style={[styles.cardBody, { padding: theme.space.lg }]}>
        <View style={{ gap: theme.space.md }}>
          <View style={[styles.cardHeadRow, { gap: theme.space.xs }]}>
            <SectionIcon source={require('@/assets/images/invest/sprout.png')} />
            <Text style={[theme.typography.label, { color: theme.color.text.strong, flex: 1 }]}>
              투자
            </Text>
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
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  row: { flexDirection: 'row' },
  card: { flex: 1 },
  // flex:1 이 카드(Pressable)가 늘어난 만큼 안쪽 ContentCard도 따라 늘어나게 한다 —
  // 그러지 않으면 두 카드 중 내용이 짧은 쪽만 흰 박스가 짧아 보인다.
  cardBody: { flex: 1, minHeight: 240, justifyContent: 'space-between' },
  cardHeadRow: { flexDirection: 'row', alignItems: 'center' },
  goalRow: { flexDirection: 'row', alignItems: 'center' },
  iconImage: { width: 40, height: 40 },
});
