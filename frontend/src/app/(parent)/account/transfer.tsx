import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { BankBadge, findBank, useTransferStore } from '@/features/account';
import { useTheme } from '@/shared/theme';
import { Button, Header, Input, Screen } from '@/shared/ui';

/**
 * 이체 1단계 — 받는 사람의 은행·계좌번호. 서버는 계좌번호만 받지만(은행 코드 없음),
 * 실제 은행 앱과 같은 UX로 은행을 먼저 고르게 해 계좌번호 입력 실수를 줄인다.
 */
export default function TransferScreen() {
  const theme = useTheme();
  const bankKey = useTransferStore((s) => s.bankKey);
  const toAccountNo = useTransferStore((s) => s.toAccountNo);
  const patch = useTransferStore((s) => s.patch);
  const reset = useTransferStore((s) => s.reset);

  // 이체 퍼널 진입점 — 이전 회차 값 초기화. 은행 선택은 뒤로가기로 돌아오므로
  // 이 화면이 언마운트되지 않아 이 effect 가 다시 돌지 않는다.
  useEffect(() => reset(), [reset]);

  const bank = findBank(bankKey);
  const canSubmit = !!bank && toAccountNo.trim().length >= 10;

  const next = () => {
    if (!canSubmit) return;
    router.push('/account/transfer-amount');
  };

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="계좌 이체" onBack={() => router.back()} />}
    >
      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.xs }}>
        <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>
          어디로 보낼까요?
        </Text>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
          받는 분의 은행과 계좌번호를 입력해주세요.
        </Text>
      </View>

      <View style={{ marginTop: theme.space['3xl'], gap: theme.space.md }}>
        <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
          1. 은행 선택
        </Text>
        <Pressable
          onPress={() => router.push('/account/transfer-bank-select')}
          style={[
            styles.bankRow,
            {
              borderRadius: theme.radius.lg,
              borderColor: theme.color.border.default,
              backgroundColor: theme.color.background.surface,
            },
          ]}
        >
          {bank ? (
            <>
              <BankBadge bank={bank} size={32} />
              <View style={styles.bankText}>
                <Text style={[theme.typography.body, { color: theme.color.text.strong }]}>
                  {bank.name}
                </Text>
                <Text style={[theme.typography.caption, { color: theme.color.brand.solid }]}>
                  선택됨
                </Text>
              </View>
            </>
          ) : (
            <Text
              style={[theme.typography.body, styles.bankText, { color: theme.color.text.muted }]}
            >
              은행을 선택해주세요
            </Text>
          )}
          <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />
        </Pressable>
      </View>

      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.md }}>
        <Input
          variant="box"
          label="2. 계좌번호"
          placeholder="- 없이 숫자만 입력하세요"
          keyboardType="number-pad"
          value={toAccountNo}
          onChangeText={(v) => patch({ toAccountNo: v.replace(/[^0-9]/g, '') })}
          maxLength={20}
        />
      </View>

      <View style={styles.spacer} />

      <Button label="다음" variant="primary" onPress={next} disabled={!canSubmit} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  bankRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderWidth: 1.5,
    paddingVertical: 14,
    paddingHorizontal: 16,
  },
  bankText: { flex: 1 },
  spacer: { flex: 1, minHeight: 24 },
});
