import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { RepeatBadge } from '@/features/chore/ui/RepeatBadge';
import { RewardChip } from '@/features/chore/ui/RewardChip';
import type { TodayChoreStatus } from '@/features/chore/lib/choreStatus';
import type { ChoreRepeatType } from '@/features/chore/model/types';
import { useTheme } from '@/shared/theme';
import { ChildBadge, ContentCard } from '@/shared/ui';

type ChoreListRowProps = {
  title: string;
  repeatType: ChoreRepeatType;
  rewardPoints: number | null;
  /** 부모 화면에서만 넘긴다. 아이 화면은 자기 것만 보므로 이름이 필요 없다 */
  childName?: string;
  /**
   * 아이 화면에서만 넘긴다. 부모 목록은 상태를 겹쳐 받지 않아 비워 둔다.
   *
   * 불리언을 늘리지 않고 상태를 그대로 받는다 - "확인 중"과 "다시 해볼까요"는 동시에
   * 성립할 수 없는데 불리언 두 개로 두면 그 사실이 타입에서 사라진다.
   */
  status?: TodayChoreStatus;
  onPress?: () => void;
  /** 우측 끝 슬롯. 아이 화면은 chevron, 부모 화면은 더보기 버튼이 들어간다 */
  trailing?: ReactNode;
};

/**
 * 할 일 목록의 한 줄. 아이·부모 화면이 같은 카드를 쓰고 우측 슬롯과 이름 배지만 다르다.
 *
 * "확인 중"은 아이가 이미 완료 요청을 보낸 상태다. 이 표시가 없으면 아이가 같은 할 일을
 * 다시 눌러 PENDING_REQUEST_EXISTS 오류를 보게 된다.
 *
 * "다시 해볼까요"는 부모가 거절한 상태다. 거절하면 그 할 일은 아무 흔적 없이 목록으로
 * 돌아오는데, 아이는 알림을 받고 들어오므로 화면에 표시가 없으면 알림과 앞뒤가 맞지 않는다.
 */
export function ChoreListRow({
  title,
  repeatType,
  rewardPoints,
  childName,
  status,
  onPress,
  trailing,
}: ChoreListRowProps) {
  const theme = useTheme();

  return (
    <Pressable onPress={onPress} disabled={onPress == null} accessibilityRole="button">
      <ContentCard
        style={{
          flexDirection: 'row',
          alignItems: 'center',
          paddingVertical: theme.space.md,
          paddingHorizontal: theme.space.lg,
          gap: theme.space.md,
        }}
      >
        <View style={styles.body}>
          <View style={[styles.meta, { gap: theme.space.xs }]}>
            {childName ? <ChildBadge name={childName} /> : null}
            <RepeatBadge repeatType={repeatType} />
            {status === 'PENDING' ? (
              <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                확인 중
              </Text>
            ) : null}
            {status === 'REJECTED' ? (
              <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
                다시 해볼까요
              </Text>
            ) : null}
          </View>
          <Text
            numberOfLines={1}
            style={[theme.typography.titleMd, { color: theme.color.text.strong }]}
          >
            {title}
          </Text>
        </View>

        <RewardChip rewardPoints={rewardPoints} compact={childName != null} />
        {trailing ?? <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />}
      </ContentCard>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  body: { flex: 1, gap: 6 },
  meta: { flexDirection: 'row', alignItems: 'center' },
});
