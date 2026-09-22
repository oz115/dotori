import { useRef, useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { grantErrorMessage } from '@/features/wallet/lib/walletError';
import { useGrantPoints } from '@/features/wallet/model/useGrantPoints';
import { formatPoint } from '@/shared/lib/money';
import { newIdempotencyKey } from '@/shared/lib/idempotencyKey';
import { useTheme } from '@/shared/theme';
import { AmountPresetRow, Button, Header, NumericKeypad, Screen } from '@/shared/ui';

/** 한 번에 줄 수 있는 도토리 상한. 서버에는 상한이 없어서 잘못 눌러 큰 금액이 나가는 걸 화면에서 막는다 */
const MAX_POINTS = 100_000;
const PRESETS = [100, 500, 1_000];

type GrantPointsScreenProps = {
  familyId: number | undefined;
  childId: number | undefined;
  childName: string | undefined;
  onBack: () => void;
  /** 지급이 끝나고 "확인"을 눌렀을 때 */
  onDone: () => void;
};

/**
 * 도토리 주기 (부모). 실제 송금 없이 부모가 아이에게 도토리를 발행해 준다 (WLT-02).
 *
 * 돈을 새로 만드는 일이라 누르자마자 나가지 않고 한 번 더 확인받는다. 같은 금액으로 다시 보낼 때는
 * 같은 멱등키를 써서, 응답을 못 받고 다시 눌러도 두 번 지급되지 않는다 (금액을 바꾸면 새 키).
 */
export function GrantPointsScreen({
  familyId,
  childId,
  childName,
  onBack,
  onDone,
}: GrantPointsScreenProps) {
  const theme = useTheme();
  const grant = useGrantPoints(familyId, childId);

  const [raw, setRaw] = useState('0');
  const amount = Number(raw);
  const name = childName ?? '우리 아이';

  const keyRef = useRef<{ amount: number; key: string } | null>(null);

  const appendDigit = (digit: string) => {
    setRaw((prev) => {
      const next = (prev === '0' ? digit : prev + digit).replace(/^0+(?=\d)/, '');
      return Number(next) > MAX_POINTS ? prev : next || '0';
    });
  };
  const backspace = () => setRaw((prev) => (prev.length <= 1 ? '0' : prev.slice(0, -1)));
  const addPreset = (points: number) =>
    setRaw((prev) => String(Math.min(Number(prev) + points, MAX_POINTS)));

  const canSubmit = amount > 0 && childId != null && !grant.isPending;

  const send = () => {
    if (keyRef.current?.amount !== amount) {
      keyRef.current = { amount, key: newIdempotencyKey() };
    }
    grant.mutate(
      { body: { amount }, idempotencyKey: keyRef.current.key },
      {
        onSuccess: () => {
          keyRef.current = null;
          Alert.alert('도토리를 줬어요', `${name}에게 ${formatPoint(amount)}를 줬어요.`, [
            { text: '확인', onPress: onDone },
          ]);
        },
        onError: (e) => Alert.alert('지급 실패', grantErrorMessage(e)),
      },
    );
  };

  const submit = () => {
    if (!canSubmit) return;
    Alert.alert(
      `${name}에게 줄까요?`,
      `${formatPoint(amount)}가 바로 지급돼요. 되돌릴 수 없어요.`,
      [
        { text: '취소', style: 'cancel' },
        { text: '주기', onPress: send },
      ],
    );
  };

  return (
    <Screen header={<Header title="도토리 주기" onBack={onBack} />}>
      {/* 금액은 위쪽에 두고, 조작하는 것들(프리셋·키패드·버튼)은 아래에 모은다 — 충전 화면과 같은 구도 */}
      <View style={styles.amountBlock}>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
          {name}에게 줄 도토리
        </Text>
        <Text style={[theme.typography.display, { color: theme.color.text.strong }]}>
          {formatPoint(amount)}
        </Text>
        {amount >= MAX_POINTS ? (
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            한 번에 최대 {formatPoint(MAX_POINTS)}까지 줄 수 있어요.
          </Text>
        ) : null}
      </View>

      <View style={styles.spacer} />

      <View style={{ gap: theme.space.md }}>
        <AmountPresetRow
          presets={PRESETS.map((points) => ({
            key: String(points),
            label: `+${points.toLocaleString('ko-KR')}P`,
            onPress: () => addPreset(points),
          }))}
        />

        <NumericKeypad onDigit={appendDigit} onBackspace={backspace} />

        <Button
          label="주기"
          variant="primary"
          onPress={submit}
          disabled={!canSubmit}
          loading={grant.isPending}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  amountBlock: { alignItems: 'center', gap: 8, paddingTop: 48 },
  spacer: { flex: 1, minHeight: 24 },
});
