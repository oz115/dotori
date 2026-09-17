import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { CashoutRequest } from '@/features/wallet/model/types';
import { CashoutStatusBadge } from '@/features/wallet/ui/CashoutStatusBadge';
import { CashoutStatusIcon } from '@/features/wallet/ui/CashoutStatusIcon';
import { formatPoint, formatWon, pointToWon } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { ChildBadge, ContentCard } from '@/shared/ui';

type CashoutRequestRowProps = {
  request: CashoutRequest;
  /** 부모 화면처럼 여러 자녀가 한 목록에 섞일 때만 넘긴다. 자녀 본인 화면에는 없다 */
  childName?: string;
  onPress?: () => void;
};

/**
 * 환전 요청 한 건. 부모의 처리 목록과 자녀의 내 요청 목록이 같은 행을 쓴다.
 *
 * 포인트와 원을 같이 적는다 — 아이는 도토리로 요청하지만 계좌에서 움직이는 것은 원이라,
 * 한쪽만 보이면 얼마인지 매번 환산해야 한다. 오른쪽 아이콘은 목록을 훑을 때 글자를 읽기
 * 전에 됐다/안 됐다가 먼저 보이라고 둔다.
 */
export function CashoutRequestRow({ request, childName, onPress }: CashoutRequestRowProps) {
  const theme = useTheme();

  const card = (
    <ContentCard
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        padding: theme.space.lg,
        gap: theme.space.md,
      }}
    >
      <View style={[styles.grow, { gap: theme.space.xs }]}>
        <View style={[styles.headRow, { gap: theme.space.sm }]}>
          {childName ? <ChildBadge name={childName} /> : null}
          <CashoutStatusBadge status={request.status} />
        </View>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          {formatWon(pointToWon(request.pointAmount))}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          {formatPoint(request.pointAmount)} · {formatRequestedAt(request.requestedAt)}
        </Text>
      </View>

      <CashoutStatusIcon status={request.status} />
    </ContentCard>
  );

  if (onPress == null) return card;

  return (
    <Pressable onPress={onPress} accessibilityRole="button">
      {card}
    </Pressable>
  );
}

/** '9월 15일 오후 6:23' — 기기 로컬 시간 */
function formatRequestedAt(requestedAt: string): string {
  return new Date(requestedAt).toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

const styles = StyleSheet.create({
  headRow: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap' },
  grow: { flex: 1 },
});
