import { StyleSheet, Text, View } from 'react-native';

import { parentQuizStatusLabel, type ParentQuizStatus } from '@/features/quiz/lib/parentQuiz';
import { useTheme, type Theme } from '@/shared/theme';

/**
 * 부모가 보는 오늘의 퀴즈 진행 상태 알약(안 풀었어요·도전 중·정답·오답).
 * 홈 진입 카드와 상세 화면이 같은 모양·같은 문구를 쓰도록 한 곳에 둔다.
 */
export function ParentQuizStatusBadge({ status }: { status: ParentQuizStatus }) {
  const theme = useTheme();
  const { background, text } = statusColors(theme, status);

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: background,
          borderRadius: theme.radius.full,
          paddingHorizontal: theme.space.md,
          paddingVertical: theme.space.xs,
        },
      ]}
    >
      <Text style={[theme.typography.bodySm, { color: text }]}>
        {parentQuizStatusLabel(status)}
      </Text>
    </View>
  );
}

function statusColors(theme: Theme, status: ParentQuizStatus) {
  switch (status) {
    case 'SOLVED':
      return {
        background: theme.color.status.success.subtle,
        text: theme.color.status.success.text,
      };
    case 'FAILED':
      return { background: theme.color.status.danger.subtle, text: theme.color.status.danger.text };
    case 'IN_PROGRESS':
      return {
        background: theme.color.status.warning.subtle,
        text: theme.color.status.warning.text,
      };
    case 'UNSOLVED':
      return { background: theme.color.background.surfaceSunken, text: theme.color.text.sub };
  }
}

const styles = StyleSheet.create({
  badge: { alignSelf: 'center' },
});
