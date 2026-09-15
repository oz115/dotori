import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { formatWonNumber } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { Button, Screen } from '@/shared/ui';

/**
 * 이체 4단계 — 완료. 되돌릴 수 없는 거래라 `Alert` 대신 화면으로 결과를 남겨서
 * 사용자가 다시 확인할 수 있게 한다.
 */
export default function TransferCompleteScreen() {
  const theme = useTheme();
  const params = useLocalSearchParams<{
    toAccountNo?: string;
    amount?: string;
    transactedAt?: string;
  }>();

  const amount = Number(params.amount) || 0;
  const transactedAt = params.transactedAt ? new Date(params.transactedAt) : null;

  return (
    <Screen edges={['top', 'bottom']}>
      <View style={styles.body}>
        <Ionicons name="checkmark-circle" size={64} color={theme.color.status.success.solid} />
        <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>
          이체가 완료됐어요
        </Text>
        <Text style={[theme.typography.display, { color: theme.color.text.strong }]}>
          {formatWonNumber(amount)}원
        </Text>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
          {params.toAccountNo} 로 보냈어요
        </Text>
        {transactedAt ? (
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            {transactedAt.toLocaleString('ko-KR')}
          </Text>
        ) : null}
      </View>

      <Button label="확인" variant="primary" onPress={() => router.replace('/')} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  body: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
});
