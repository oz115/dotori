import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import {
  accountAuthRequestErrorMessage,
  BankBadge,
  findBank,
  useLinkAccountStore,
  useRequestAccountAuth,
} from '@/features/account';
import { useTheme } from '@/shared/theme';
import { Button, Header, Input, Screen } from '@/shared/ui';

const NOTES = [
  '본인 명의 계좌만 연결할 수 있어요.',
  '계좌 확인을 위해 1원이 입금돼요.',
  '연결된 계좌는 도토리 충전·환전에 사용돼요.',
  '마이페이지에서 언제든 연결을 해제할 수 있어요.',
];

export default function AccountLinkScreen() {
  const theme = useTheme();
  const request = useRequestAccountAuth();

  const bankKey = useLinkAccountStore((s) => s.bankKey);
  const patch = useLinkAccountStore((s) => s.patch);
  const reset = useLinkAccountStore((s) => s.reset);
  const [accountNo, setAccountNo] = useState('');
  const [error, setError] = useState('');

  // 연동 퍼널 진입점 — 이전 회차 스토어 값 초기화. bank-select 는 router.back() 이라
  // 이 화면이 언마운트되지 않으므로 이 effect 가 다시 돌지 않는다.
  useEffect(() => reset(), [reset]);

  const bank = findBank(bankKey);
  const canSubmit = !!bank && accountNo.trim().length >= 10 && !request.isPending;

  const submit = () => {
    if (!bank || !canSubmit) return;
    setError('');
    patch({ accountNo });
    request.mutate(
      { bankCode: bank.code, bankName: bank.name, accountNo: accountNo.trim() },
      {
        onSuccess: (res) => {
          router.push({
            pathname: '/setup/account/verify',
            params: {
              authText: res.authText,
              expiresInSeconds: String(res.expiresInSeconds),
            },
          });
        },
        onError: (e) => setError(accountAuthRequestErrorMessage(e)),
      },
    );
  };

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="계좌 연동" onBack={() => router.back()} />}
    >
      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.xs }}>
        <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>
          내 계좌 연결하기
        </Text>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
          내 계좌를 연결해서 도토리를 모으고 관리할 수 있어요.
        </Text>
      </View>

      <View style={{ marginTop: theme.space['3xl'], gap: theme.space.md }}>
        <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
          1. 은행 선택
        </Text>
        <Pressable
          onPress={() => router.push('/setup/account/bank-select')}
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
          value={accountNo}
          onChangeText={(v) => {
            setAccountNo(v.replace(/[^0-9]/g, ''));
            if (error) setError('');
          }}
          maxLength={20}
        />
      </View>

      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.sm }}>
        <View style={styles.notesHead}>
          <Ionicons name="alert-circle" size={16} color={theme.color.text.strong} />
          <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
            계좌 연결 전 확인해주세요
          </Text>
        </View>
        {NOTES.map((n) => (
          <View key={n} style={styles.noteRow}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>•</Text>
            <Text
              style={[theme.typography.bodySm, styles.noteText, { color: theme.color.text.sub }]}
            >
              {n}
            </Text>
          </View>
        ))}
      </View>

      {error ? (
        <Text
          style={[
            theme.typography.caption,
            { color: theme.color.status.danger.text, marginTop: theme.space.sm },
          ]}
        >
          {error}
        </Text>
      ) : null}

      <View style={styles.spacer} />

      <Button
        label="1원 송금하기"
        variant="primary"
        onPress={submit}
        loading={request.isPending}
        disabled={!canSubmit}
      />
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
  notesHead: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  noteRow: { flexDirection: 'row', gap: 8 },
  noteText: { flex: 1 },
  spacer: { flex: 1, minHeight: 24 },
});
