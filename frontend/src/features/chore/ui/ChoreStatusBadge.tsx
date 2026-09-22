import { StyleSheet, Text, View } from 'react-native';

import type { TodayChoreStatus } from '@/features/chore/lib/choreStatus';
import { useTheme, type Theme } from '@/shared/theme';

/**
 * 미션 진행 상태 배지 (승인 대기중·다시 해볼까요·완료).
 *
 * 전에는 카드 맨 아랫줄에 회색 캡션으로 "확인 중"만 적었다. 날짜와 같은 크기·같은 색이라
 * 아이도 부모도 요청이 갔는지 안 갔는지 알아보지 못했다 (QA). 그래서 반복 주기 배지와
 * 같은 자리·같은 모양으로 올려 색으로 먼저 읽히게 한다.
 *
 * <p>색은 뜻을 따른다 - 기다리는 중은 노랑, 돌려보낸 것은 빨강, 끝난 것은 브랜드 초록.
 * TODO 는 배지가 없다: 아직 아무 일도 일어나지 않은 상태라 붙일 말이 없고, 모든 행에
 * 배지가 붙으면 "무언가 일어난 행"이 눈에 띄지 않는다.
 */
export type ChoreStatusBadgeStyle = { label: string; bg: string; text: string };

export function choreStatusBadge(
  theme: Theme,
  status: TodayChoreStatus,
): ChoreStatusBadgeStyle | null {
  switch (status) {
    case 'PENDING':
      return {
        label: '승인 대기중',
        bg: theme.color.status.warning.subtle,
        text: theme.color.status.warning.text,
      };
    case 'REJECTED':
      return {
        label: '다시 해볼까요',
        bg: theme.color.status.danger.subtle,
        text: theme.color.status.danger.text,
      };
    case 'DONE':
      return {
        label: '완료',
        bg: theme.color.brand.subtle,
        text: theme.color.brand.subtleText,
      };
    case 'TODO':
      return null;
  }
}

export function ChoreStatusBadge({ status }: { status: TodayChoreStatus }) {
  const theme = useTheme();
  const badge = choreStatusBadge(theme, status);

  if (badge == null) {
    return null;
  }

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: badge.bg,
          borderRadius: theme.radius.full,
          paddingHorizontal: theme.space.sm,
        },
      ]}
    >
      <Text style={[theme.typography.caption, { color: badge.text }]}>{badge.label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  // 반복 주기 배지(RepeatBadge)와 같은 값 — 한 줄에 나란히 서므로 높이가 같아야 한다
  badge: { paddingVertical: 3 },
});
