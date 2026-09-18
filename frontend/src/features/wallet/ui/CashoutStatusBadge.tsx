import { StyleSheet, Text, View } from 'react-native';

import { cashoutStatusLabel, cashoutStatusTone } from '@/features/wallet/lib/cashoutStatus';
import type { CashoutStatus } from '@/features/wallet/model/types';
import { useTheme } from '@/shared/theme';

/**
 * 환전 요청 상태 배지. 목록의 행과 상세 모달이 같은 배지를 쓴다 — 목록에서 본 상태와
 * 눌러서 들어간 상세의 상태가 다른 모양이면 같은 값인지 매번 읽어서 확인해야 한다.
 */
export function CashoutStatusBadge({ status }: { status: CashoutStatus }) {
  const theme = useTheme();
  const tone = cashoutStatusTone(status);

  // neutral 은 status 팔레트에 없다 — 색으로 의미를 줄 것이 없는 상태(대기 중·아이가 취소)라
  // 회색으로 물러나게 한다.
  const { bg, fg } =
    tone === 'neutral'
      ? { bg: theme.color.background.surfaceSunken, fg: theme.color.text.sub }
      : { bg: theme.color.status[tone].subtle, fg: theme.color.status[tone].text };

  return (
    <View
      style={[
        styles.badge,
        { backgroundColor: bg, borderRadius: theme.radius.full, paddingHorizontal: theme.space.sm },
      ]}
    >
      <Text style={[theme.typography.caption, { color: fg }]}>{cashoutStatusLabel(status)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { paddingVertical: 2 },
});
