import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { deadlineLabel } from '@/features/chore/lib/choreLabels';
import { ChoreStatusBadge } from '@/features/chore/ui/ChoreStatusBadge';
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
  /**
   * 이번 주기에 끝낸 미션. 목록에 남기되 "꺼진" 카드로 그린다 (QA).
   *
   * 지워 버리면 아이도 부모도 무엇을 걸어 놨는지 알 수 없다. 대신 할 일이 남은 미션과
   * 같은 밝기면 무엇이 남았는지 흐려진다.
   *
   * <p>투명도(opacity)로 낮추지 않는다. 흰 카드가 반투명해지면 배경색과 섞여 카드 자체가
   * 사라져 "덜 그려진 화면"처럼 보였다. 대신 채움을 회색으로 바꾸고 그림자를 뺀다 -
   * 상자는 상자대로 남고 한 칸 가라앉은 것처럼 읽힌다.
   */
  dimmed?: boolean;
  onPress?: () => void;
  /** 우측 끝 슬롯. 아이 화면은 chevron, 부모 화면은 더보기 버튼이 들어간다 */
  trailing?: ReactNode;
};

/**
 * 할 일 목록의 한 줄. 아이·부모 화면이 같은 카드를 쓰고 우측 슬롯과 이름 배지만 다르다.
 *
 * 상태는 배지로 보여 준다 (ChoreStatusBadge). "승인 대기중"이 없으면 아이가 같은 미션을
 * 다시 눌러 PENDING_REQUEST_EXISTS 오류를 보게 되고, "다시 해볼까요"가 없으면 거절이
 * 아무 흔적 없이 목록으로 돌아와 알림과 앞뒤가 맞지 않는다.
 */
export function ChoreListRow({
  title,
  repeatType,
  rewardPoints,
  childName,
  status,
  endDate,
  dimmed = false,
  onPress,
  trailing,
}: ChoreListRowProps) {
  const theme = useTheme();
  const deadline = repeatType === 'ONCE' && endDate ? deadlineLabel(endDate) : null;

  return (
    <Pressable onPress={onPress} disabled={onPress == null} accessibilityRole="button">
      <ContentCard
        style={[
          {
            flexDirection: 'row',
            alignItems: 'center',
            paddingVertical: theme.space.md,
            paddingHorizontal: theme.space.lg,
            gap: theme.space.md,
          },
          dimmed ? [{ backgroundColor: theme.color.background.surfaceSunken }, styles.flat] : null,
        ]}
      >
        {/* 세 줄로 나눠 읽는다: 배지 / 제목 / 부가 정보. 한 줄에 몰아 두면 배지·문구·날짜가
            뒤섞여 무엇이 제목인지 한눈에 안 들어온다. */}
        <View style={styles.body}>
          <View style={[styles.meta, { gap: theme.space.xs }]}>
            {childName ? <ChildBadge name={childName} /> : null}
            <RepeatBadge repeatType={repeatType} />
            {/* 상태는 주기 배지 옆이다. 아랫줄 캡션으로 두면 날짜와 같은 크기·색이라
                요청을 보냈는지조차 알아보기 어려웠다 (QA) */}
            {status ? <ChoreStatusBadge status={status} /> : null}
          </View>

          <Text
            numberOfLines={1}
            style={[
              theme.typography.titleMd,
              { color: dimmed ? theme.color.text.muted : theme.color.text.strong },
            ]}
          >
            {title}
          </Text>

          {deadline ? (
            <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
              {deadline}
            </Text>
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
  // 끝낸 카드는 떠 보이지 않아야 한다 - 그림자를 지워 배경에 가라앉힌다
  flat: { shadowOpacity: 0, elevation: 0 },
  // 세 줄이라 줄 간격을 좁게 잡는다. 6 이면 카드가 불필요하게 길어진다.
  body: { flex: 1, gap: 4 },
  meta: { flexDirection: 'row', alignItems: 'center' },
});
