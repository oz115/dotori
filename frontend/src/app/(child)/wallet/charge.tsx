import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { useMyAccount } from '@/features/account';
import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { chargeErrorMessage, useChargeWallet } from '@/features/wallet';
import { formatPoint, formatWonNumber, wonToPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { AmountPresetRow, Button, Header, NumericKeypad, Screen } from '@/shared/ui';

const PRESETS_WON = [100, 1_000, 10_000];
const MAX_DIGITS = 9; // 억 단위까지

/**
 * 포인트 충전. 연동된 본인 계좌에서 실제로 송금하고, 10원당 1포인트를 받는다
 * (PointExchangePolicy — 10원 단위만 허용).
 */
export default function ChargeScreen() {
  const theme = useTheme();
  const familyId = useFamilyId().data ?? undefined;
  const childId = useAuthStore((s) => s.user?.userId);
  const account = useMyAccount();
  const charge = useChargeWallet(familyId, childId);

  const [raw, setRaw] = useState('0');
  const amount = Number(raw);
  const accountBalance = account.data?.accountBalance ?? 0;

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

  const isUnitValid = amount % 10 === 0;
  const canSubmit = amount > 0 && isUnitValid && amount <= accountBalance && !charge.isPending;

  const submit = () => {
    if (!canSubmit) return;
    charge.mutate(
      { amount },
      {
        onSuccess: () => {
          Alert.alert(
            '충전 완료',
            `${formatWonNumber(amount)}원을 보내고 ${formatPoint(wonToPoint(amount))}를 받았어요.`,
            [{ text: '확인', onPress: () => router.back() }],
          );
        },
        onError: (e) => Alert.alert('충전 실패', chargeErrorMessage(e)),
      },
    );
  };

  return (
    <Screen header={<Header title="포인트 충전" onBack={() => router.back()} />}>
      {/* 금액은 위쪽에 두고, 조작하는 것들(잔액·프리셋·키패드·버튼)은 아래에 모은다 —
          손가락이 닿는 영역과 읽는 영역을 나눈 은행 송금 화면과 같은 구도 */}
      <View style={styles.amountBlock}>
        <Text style={[theme.typography.display, { color: theme.color.text.strong }]}>
          {formatWonNumber(amount)}원
        </Text>
        <Text style={[theme.typography.body, { color: theme.color.brand.solid }]}>
          도토리 {formatPoint(wonToPoint(amount))} 받아요
        </Text>
        {!isUnitValid ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            10원 단위로만 충전할 수 있어요.
          </Text>
        ) : amount > accountBalance ? (
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
          <Text style={[theme.typography.label, { color: theme.color.text.sub }]}>
            계좌 잔액 {formatWonNumber(accountBalance)}원
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

        <Button
          label="충전하기"
          variant="primary"
          onPress={submit}
          disabled={!canSubmit}
          loading={charge.isPending}
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
