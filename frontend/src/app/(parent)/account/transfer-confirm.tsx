import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import {
  BankBadge,
  findBank,
  transferErrorMessage,
  useTransfer,
  useTransferStore,
} from '@/features/account';
import { formatWonNumber } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { Button, Header, Input, Screen } from '@/shared/ui';

const DEFAULT_MEMO_PLACEHOLDER = '계좌 이체';

/**
 * 이체 3단계 — 확인. 받는 사람 계좌의 예금주를 서버가 확인해주지 않아서(1원 인증과 달리),
 * 잘못 보내는 걸 막을 마지막 지점은 이 화면뿐이다. 그래서 요약을 다시 보여주고
 * 이 버튼을 눌러야만 실제로 돈이 나간다.
 */
export default function TransferConfirmScreen() {
  const theme = useTheme();
  const bankKey = useTransferStore((s) => s.bankKey);
  const toAccountNo = useTransferStore((s) => s.toAccountNo);
  const amount = useTransferStore((s) => s.amount);
  const memo = useTransferStore((s) => s.memo);
  const patch = useTransferStore((s) => s.patch);
  const transfer = useTransfer();

  const bank = findBank(bankKey);

  const submit = () => {
    if (transfer.isPending) return;
    transfer.mutate(
      { toAccountNo, amount, memo: memo.trim() || undefined },
      {
        onSuccess: (result) =>
          router.replace({
            pathname: '/account/transfer-complete',
            params: {
              toAccountNo: result.toAccountNo,
              amount: String(result.amount),
              transactedAt: result.transactedAt,
            },
          }),
        onError: (e) => Alert.alert('이체 실패', transferErrorMessage(e)),
      },
    );
  };

  return (
    <Screen scroll header={<Header title="이체 확인" onBack={() => router.back()} />}>
      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.xs }}>
        <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>
          이 내용대로 보낼까요?
        </Text>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
          받는 분 정보는 다시 확인해주세요.{'\n'}이체는 되돌릴 수 없어요.
        </Text>
      </View>

      <View
        style={[
          styles.summary,
          {
            marginTop: theme.space['2xl'],
            borderRadius: theme.radius.lg,
            borderColor: theme.color.border.default,
            backgroundColor: theme.color.background.surface,
            padding: theme.space.lg,
            gap: theme.space.md,
          },
        ]}
      >
        <SummaryRow
          label="받는 은행"
          value={bank?.name ?? '-'}
          icon={bank ? <BankBadge bank={bank} size={20} /> : undefined}
        />
        <SummaryRow label="계좌번호" value={toAccountNo} />
        <SummaryRow label="보낼 금액" value={`${formatWonNumber(amount)}원`} strong />
      </View>

      <View style={{ marginTop: theme.space['2xl'] }}>
        <Input
          variant="box"
          label="메모 (선택)"
          placeholder={DEFAULT_MEMO_PLACEHOLDER}
          value={memo}
          onChangeText={(v) => patch({ memo: v })}
          maxLength={50}
        />
      </View>

      <View style={styles.spacer} />

      <Button label="보내기" variant="primary" onPress={submit} loading={transfer.isPending} />
    </Screen>
  );
}

type SummaryRowProps = { label: string; value: string; strong?: boolean; icon?: ReactNode };

function SummaryRow({ label, value, strong, icon }: SummaryRowProps) {
  const theme = useTheme();
  return (
    <View style={styles.summaryRow}>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>{label}</Text>
      <View style={[styles.valueRow, { gap: theme.space.xs }]}>
        <Text
          style={[
            strong ? theme.typography.h2 : theme.typography.body,
            { color: theme.color.text.strong },
          ]}
        >
          {value}
        </Text>
        {icon}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  summary: { borderWidth: 1.5 },
  summaryRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  valueRow: { flexDirection: 'row', alignItems: 'center' },
  spacer: { flex: 1, minHeight: 24 },
});
