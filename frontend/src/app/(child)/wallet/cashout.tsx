import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { cashoutErrorMessage, useCashoutRequest, useWalletBalance } from '@/features/wallet';
import { formatPoint, formatWonNumber, pointToWon } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { AmountPresetRow, Button, Header, NumericKeypad, Screen } from '@/shared/ui';

const PRESETS_POINT = [100, 1_000, 10_000];
const MAX_DIGITS = 9;

/**
 * 포인트 환전 요청. 요청 즉시 포인트가 차감되고, 부모가 승인하면 실제 계좌로 송금된다
 * (WLT-07 — 이 화면은 요청만 만들고, 승인 대기 상태는 부모 쪽에서 처리한다).
 */
export default function CashoutScreen() {
  const theme = useTheme();
  const familyId = useFamilyId().data ?? undefined;
  const childId = useAuthStore((s) => s.user?.userId);
  const balance = useWalletBalance(familyId, childId);
  const cashout = useCashoutRequest(familyId, childId);

  const [raw, setRaw] = useState('0');
  const amount = Number(raw);
  const walletBalance = balance.data?.balance ?? 0;

  const appendDigit = (digit: string) => {
    setRaw((prev) => {
      const next = prev === '0' ? digit : prev + digit;
      const cleaned = next.replace(/^0+(?=\d)/, '');
      return cleaned.length > MAX_DIGITS ? prev : cleaned || '0';
    });
  };

  const backspace = () => setRaw((prev) => (prev.length <= 1 ? '0' : prev.slice(0, -1)));

  const addPreset = (points: number) => {
    setRaw((prev) => String(Math.min(Number(prev) + points, 999_999_990)));
  };

  const useFullBalance = () => setRaw(String(walletBalance));

  const canSubmit = amount > 0 && amount <= walletBalance && !cashout.isPending;

  const submit = () => {
    if (!canSubmit) return;
    cashout.mutate(
      { amount },
      {
        onSuccess: () => {
          Alert.alert(
            '환전 요청 완료',
            `${formatPoint(amount)} → ${formatWonNumber(pointToWon(amount))}원\n부모님이 승인하면 계좌로 들어와요.`,
            [{ text: '확인', onPress: () => router.back() }],
          );
        },
        onError: (e) => Alert.alert('환전 요청 실패', cashoutErrorMessage(e)),
      },
    );
  };

  return (
    <Screen header={<Header title="포인트 환전" onBack={() => router.back()} />}>
      {/* 구도는 충전 화면과 동일 — 금액은 위, 조작하는 것들은 아래 한 덩어리 */}
      <View style={styles.amountBlock}>
        <Text style={[theme.typography.display, { color: theme.color.text.strong }]}>
          {formatPoint(amount)}
        </Text>
        <Text style={[theme.typography.body, { color: theme.color.brand.solid }]}>
          현금 {formatWonNumber(pointToWon(amount))}원 받아요
        </Text>
        {amount > walletBalance ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            보유 포인트를 넘을 수 없어요.
          </Text>
        ) : null}
      </View>

      <View style={styles.spacer} />

      <View style={{ gap: theme.space.md }}>
        <View
          style={[
            styles.balancePill,
            {
              backgroundColor: theme.color.background.surfaceSunken,
              borderRadius: theme.radius.full,
            },
          ]}
        >
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            보유 포인트 {formatPoint(walletBalance)}
          </Text>
        </View>

        <AmountPresetRow
          presets={[
            ...PRESETS_POINT.map((points) => ({
              key: String(points),
              label: `+${formatPoint(points)}`,
              onPress: () => addPreset(points),
            })),
            { key: 'full', label: '전액', onPress: useFullBalance },
          ]}
        />

        <NumericKeypad onDigit={appendDigit} onBackspace={backspace} />

        <Button
          label="환전하기"
          variant="primary"
          onPress={submit}
          disabled={!canSubmit}
          loading={cashout.isPending}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  amountBlock: { alignItems: 'center', gap: 8, paddingTop: 48 },
  // 잔액 표시와 아래 조작 영역(프리셋·키패드)을 눈으로 갈라 놓는 여백
  balancePill: { alignSelf: 'center', paddingHorizontal: 14, paddingVertical: 6, marginBottom: 12 },
  spacer: { flex: 1, minHeight: 24 },
});
