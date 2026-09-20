import { useFocusEffect } from 'expo-router';
import { useCallback } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { parentQuizStatus } from '@/features/quiz/lib/parentQuiz';
import { useParentTodayQuiz } from '@/features/quiz/model/useParentTodayQuiz';
import { ParentQuizStatusBadge } from '@/features/quiz/ui/ParentQuizStatusBadge';
import { useTheme } from '@/shared/theme';
import { ContentCard } from '@/shared/ui';

type ParentQuizEntryCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 눌렀을 때 이동. 내용은 상세 화면(ParentQuizScreen)이 보여 준다 */
  onPress: () => void;
};

/**
 * 부모 홈 "오늘의 경제 퀴즈" 진입 카드. 문제·정답·아이의 제출 내용은 여기서 펼치지 않고
 * 눌러서 들어가는 전용 화면에서 보여 준다 — 홈에는 아이가 풀었는지만 알려 주는 배지를 둔다.
 *
 * 배지는 상세 화면과 같은 쿼리를 쓰므로 캐시를 공유한다. 조회가 실패하거나 아직 퀴즈가
 * 없어도 카드는 그대로 눌러서 들어갈 수 있고(안내는 상세 화면 몫), 배지만 빠진다.
 */
export function ParentQuizEntryCard({ familyId, childId, onPress }: ParentQuizEntryCardProps) {
  const theme = useTheme();
  const query = useParentTodayQuiz(familyId, childId);

  // 탭 화면은 전환해도 언마운트되지 않는다 — 홈으로 돌아올 때마다 아이가 그새 푼 결과를 다시 받는다.
  // 마운트 직후의 첫 조회와 겹쳐도 진행 중인 요청을 끊지 않는다.
  const refetch = query.refetch;
  const canQuery = familyId != null && childId != null;
  useFocusEffect(
    useCallback(() => {
      if (canQuery) {
        refetch({ cancelRefetch: false });
      }
    }, [canQuery, refetch]),
  );

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel="오늘의 경제 퀴즈 열기"
      onPress={onPress}
      style={({ pressed }) => ({ opacity: pressed ? 0.85 : 1 })}
    >
      <ContentCard style={[styles.card, { padding: theme.space.lg, gap: theme.space.md }]}>
        <Image
          source={require('@/assets/images/icons/icon-quiz.png')}
          style={styles.icon}
          resizeMode="contain"
        />
        <View style={styles.text}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            오늘의 경제 퀴즈
          </Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            문제와 아이가 낸 답을 확인해요
          </Text>
        </View>
        {query.data != null ? (
          <ParentQuizStatusBadge status={parentQuizStatus(query.data)} />
        ) : null}
      </ContentCard>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { flexDirection: 'row', alignItems: 'center', minHeight: 88 },
  text: { flex: 1, gap: 2 },
  icon: { width: 56, height: 56 },
});
