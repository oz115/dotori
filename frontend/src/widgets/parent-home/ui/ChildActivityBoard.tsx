import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { ParentQuizCard } from '@/features/quiz';
import { useTheme } from '@/shared/theme';
import { ContentCard, useTabOverlap } from '@/shared/ui';

type ChildActivityBoardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·자녀 조회가 끝나기 전 — 아직 조회에 쓸 id 가 없다 */
  familyPending: boolean;
  /** 경제 단어장 — 아이와 같은 단어장 화면(읽기 전용)으로 보낸다 */
  onOpenWords: () => void;
};

/**
 * 부모용 "활동 보드". 자녀 홈의 것과 자리는 같지만 보여주는 내용이 다르다 — 아이는
 * "풀어볼 것"을 안내받고, 부모는 "아이가 어떻게 했는지" 결과를 본다.
 *
 * 퀴즈 카드는 아이가 풀었는지에 따라 문제·정답(·아이가 낸 답)을 그 자리에서 펼쳐 보여주고,
 * 단어장 카드는 아이와 똑같은 단어장 화면으로 들어가는 진입점이다.
 */
export function ChildActivityBoard({
  familyId,
  childId,
  familyPending,
  onOpenWords,
}: ChildActivityBoardProps) {
  const theme = useTheme();
  const tabOverlap = useTabOverlap();

  return (
    <View
      style={{
        gap: theme.space.md,
        backgroundColor: theme.color.background.surfaceSunken,
        marginHorizontal: -theme.layout.screenX,
        paddingHorizontal: theme.layout.screenX,
        paddingTop: theme.space['2xl'],
        paddingBottom: theme.space['4xl'] + tabOverlap,
      }}
    >
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>활동 보드</Text>
      <View style={[styles.column, { gap: theme.space.md }]}>
        <ParentQuizCard familyId={familyId} childId={childId} familyPending={familyPending} />
        <WordBookRow onPress={onOpenWords} />
      </View>
    </View>
  );
}

/** 경제 단어장 진입 카드. 사전이라 아이별 데이터가 없어 "오늘의 단어" 같은 값을 두지 않는다. */
function WordBookRow({ onPress }: { onPress: () => void }) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel="경제 단어장 열기"
      onPress={onPress}
      style={({ pressed }) => ({ opacity: pressed ? 0.85 : 1 })}
    >
      <ContentCard style={[styles.card, { padding: theme.space.lg, gap: theme.space.md }]}>
        <Image
          source={require('@/assets/images/icons/icon-word.png')}
          style={styles.icon}
          resizeMode="contain"
        />
        <View style={styles.cardText}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            경제 단어장
          </Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            아이와 같은 단어장을 볼 수 있어요
          </Text>
        </View>
        <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />
      </ContentCard>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  column: { flexDirection: 'column' },
  card: { flexDirection: 'row', alignItems: 'center', minHeight: 88 },
  cardText: { flex: 1, gap: 2 },
  icon: { width: 56, height: 56 },
});
