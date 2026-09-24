import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useMyAccount, useTransferStore } from '@/features/account';
import { formatWonNumber } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { AmountPresetRow, Button, Header, NumericKeypad, Screen } from '@/shared/ui';

const PRESETS_WON = [1_000, 10_000, 100_000];
const MAX_DIGITS = 9; // 억 단위까지

/** 이체 2단계 — 보낼 금액. 내 연동 계좌 잔액·1회 이체한도 안에서만 보낼 수 있다. */
export default function TransferAmountScreen() {
  const theme = useTheme();
  const account = useMyAccount();
  const patch = useTransferStore((s) => s.patch);

  const [raw, setRaw] = useState('0');
  const amount = Number(raw);
  const accountBalance = account.data?.accountBalance ?? 0;
  const oneTimeLimit = account.data?.oneTimeTransferLimit ?? Infinity;

  const appendDigit = (digit: string) => {
    setRaw((prev) => {
      const next = prev === '0' ? digit : prev + digit;
      const cleaned = next.replace(/^0+(?=\d)/, '');
      return cleaned.length > MAX_DIGITS ? prev : cleaned || '0';
    });
  };

  const backspace = () => setRaw((prev) => (prev.length <= 1 ? '0' : prev.slice(0, -1)));

  const addPreset = (won: number) => {
    setRaw((prev) => String(Math.min(Number(prev) + won, 999_999_990)));
  };

  const useFullBalance = () => setRaw(String(accountBalance));

  const overBalance = amount > accountBalance;
  const overLimit = amount > oneTimeLimit;
  const canSubmit = amount > 0 && !overBalance && !overLimit;

  const next = () => {
    if (!canSubmit) return;
    patch({ amount });
    router.push('/account/transfer-confirm');
  };

  return (
    <Screen header={<Header title="계좌 이체" onBack={() => router.back()} />}>
      <View style={styles.amountBlock}>
        <Text style={[theme.typography.display, { color: theme.color.text.strong }]}>
          {formatWonNumber(amount)}원
        </Text>
        {overLimit ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            1회 이체 한도({formatWonNumber(oneTimeLimit)}원)를 넘을 수 없어요.
          </Text>
        ) : overBalance ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            계좌 잔액을 넘을 수 없어요.
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
            내 계좌 잔액 {formatWonNumber(accountBalance)}원
          </Text>
        </View>

        <AmountPresetRow
          presets={[
            ...PRESETS_WON.map((won) => ({
              key: String(won),
              label: `+${formatWonNumber(won)}원`,
              onPress: () => addPreset(won),
            })),
            { key: 'full', label: '전액', onPress: useFullBalance },
          ]}
        />

        <NumericKeypad onDigit={appendDigit} onBackspace={backspace} />

        <Button label="다음" variant="primary" onPress={next} disabled={!canSubmit} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  amountBlock: { alignItems: 'center', gap: 8, paddingTop: 48 },
  balancePill: { alignSelf: 'center', paddingHorizontal: 14, paddingVertical: 6, marginBottom: 12 },
  spacer: { flex: 1, minHeight: 24 },
});
