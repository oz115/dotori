import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { deadlineLabel } from '@/features/chore/lib/choreLabels';
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
  /**
   * 넘기면 한번짜리 미션에 한해 "9월 20일까지"를 함께 보여 준다.
   *
   * 반복 미션에는 쓰지 않는다 - 주기가 돌아오면 endDate 가 다음 기간으로 밀려서 그
   * 날짜가 마감을 뜻하지 않는다. 홈의 미리보기는 자리가 좁아 넘기지 않는다.
   */
  endDate?: string;
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
  endDate,
  onPress,
  trailing,
}: ChoreListRowProps) {
  const theme = useTheme();
  const deadline = repeatType === 'ONCE' && endDate ? deadlineLabel(endDate) : null;
  const note =
    status === 'PENDING'
      ? { text: '확인 중', color: theme.color.text.muted }
      : status === 'REJECTED'
        ? { text: '다시 해볼까요', color: theme.color.status.danger.text }
        : null;

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
        {/* 세 줄로 나눠 읽는다: 배지 / 제목 / 부가 정보. 한 줄에 몰아 두면 배지·문구·날짜가
            뒤섞여 무엇이 제목인지 한눈에 안 들어온다. */}
        <View style={styles.body}>
          <View style={[styles.meta, { gap: theme.space.xs }]}>
            {childName ? <ChildBadge name={childName} /> : null}
            <RepeatBadge repeatType={repeatType} />
          </View>

          <Text
            numberOfLines={1}
            style={[theme.typography.titleMd, { color: theme.color.text.strong }]}
          >
            {title}
          </Text>

          {deadline || note ? (
            <View style={[styles.meta, { gap: theme.space.sm }]}>
              {deadline ? (
                <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                  {deadline}
                </Text>
              ) : null}
              {note ? (
                <Text style={[theme.typography.caption, { color: note.color }]}>{note.text}</Text>
              ) : null}
            </View>
          ) : null}
        </View>

        {/* 목록에서는 "도토리" 글자를 뺀다 - 칩에 도토리 아이콘이 이미 있어 같은 말을 두 번 한다 */}
        <RewardChip rewardPoints={rewardPoints} compact />
        {trailing ?? <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />}
      </ContentCard>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  // 세 줄이라 줄 간격을 좁게 잡는다. 6 이면 카드가 불필요하게 길어진다.
  body: { flex: 1, gap: 4 },
  meta: { flexDirection: 'row', alignItems: 'center' },
});
