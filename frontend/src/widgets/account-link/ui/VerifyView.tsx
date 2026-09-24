import { Ionicons } from '@expo/vector-icons';
import { useEffect, useRef, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import {
  accountAuthRequestErrorMessage,
  accountVerifyErrorMessage,
  findBank,
  useLinkAccountStore,
  useRequestAccountAuth,
  useVerifyAccountAuth,
} from '@/features/account';
import { useCountdown } from '@/shared/lib/useCountdown';
import { useTheme } from '@/shared/theme';
import { Button, CodeInput, Header, Screen } from '@/shared/ui';

const CODE_LENGTH = 4;

type VerifyViewProps = {
  /** 입금자명 앞부분 (예: "도토리") */
  authText: string;
  /** 인증 만료까지 남은 초 */
  expiresInSeconds: number;
  onBack: () => void;
  /** 연동 완료 — 각 역할의 홈으로 보낸다 */
  onVerified: () => void;
};

/** 1원 인증 코드 확인 (부모·자녀 공용). */
export function VerifyView({ authText, expiresInSeconds, onBack, onVerified }: VerifyViewProps) {
  const theme = useTheme();

  const bankKey = useLinkAccountStore((s) => s.bankKey);
  const accountNo = useLinkAccountStore((s) => s.accountNo);
  const bank = findBank(bankKey);

  const verify = useVerifyAccountAuth();
  const resend = useRequestAccountAuth();
  const ttl = useCountdown();
  const started = useRef(false);

  const [code, setCode] = useState('');
  const [error, setError] = useState('');

  // 진입 시 카운트다운 시작 (앞 화면에서 넘겨준 만료 시간 기준)
  useEffect(() => {
    if (!started.current) {
      started.current = true;
      ttl.start(expiresInSeconds || 180);
    }
  }, [expiresInSeconds, ttl]);

  const canSubmit = code.length === CODE_LENGTH && !verify.isPending && !!bank;
  const expired = !ttl.running;

  const submit = () => {
    if (!bank || !canSubmit) return;
    setError('');
    verify.mutate(
      { bankCode: bank.code, bankName: bank.name, accountNo, authCode: code },
      {
        onSuccess: () => {
          useLinkAccountStore.getState().reset();
          Alert.alert('계좌 연결 완료', '이제 도토리를 충전하고 관리할 수 있어요.', [
            { text: '확인', onPress: onVerified },
          ]);
        },
        onError: (e) => setError(accountVerifyErrorMessage(e)),
      },
    );
  };

  const resendOneWon = () => {
    if (!bank || resend.isPending) return;
    setError('');
    setCode('');
    resend.mutate(
      { bankCode: bank.code, bankName: bank.name, accountNo },
      {
        onSuccess: (res) => ttl.start(res.expiresInSeconds),
        onError: (e) => setError(accountAuthRequestErrorMessage(e)),
      },
    );
  };

  return (
    <Screen scroll keyboardAvoiding header={<Header title="계좌 인증" onBack={onBack} />}>
      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.sm, alignItems: 'center' }}>
        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>
          1원을 보냈습니다!
        </Text>
        <Text style={[theme.typography.body, styles.center, { color: theme.color.text.sub }]}>
          <Text style={{ color: theme.color.brand.solid }}>{authText}</Text> 뒤 숫자 {CODE_LENGTH}
          자리를 입력해주세요.
        </Text>
      </View>

      <View
        style={[
          styles.example,
          {
            marginTop: theme.space['2xl'],
            borderRadius: theme.radius.lg,
            borderColor: theme.color.border.default,
            backgroundColor: theme.color.background.surface,
          },
        ]}
      >
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          예시 입금내역
        </Text>
        <View style={styles.exampleRow}>
          <Text style={[theme.typography.label, { color: theme.color.brand.solid }]}>입금</Text>
          <Text
            style={[theme.typography.body, styles.exampleName, { color: theme.color.text.strong }]}
          >
            {authText} 1234
          </Text>
          <Text style={[theme.typography.body, { color: theme.color.status.success.text }]}>
            +1원
          </Text>
        </View>
      </View>

      <View style={{ marginTop: theme.space['3xl'], gap: theme.space.md, alignItems: 'center' }}>
        <CodeInput
          value={code}
          onChangeText={(v) => {
            setCode(v);
            if (error) setError('');
          }}
          length={CODE_LENGTH}
          mode="numeric"
          onFilled={submit}
          error={!!error}
          autoFocus
        />
        <View style={styles.timer}>
          <Ionicons
            name="time-outline"
            size={14}
            color={expired ? theme.color.status.danger.text : theme.color.brand.solid}
          />
          <Text
            style={[
              theme.typography.caption,
              { color: expired ? theme.color.status.danger.text : theme.color.brand.solid },
            ]}
          >
            {expired ? '시간이 만료됐어요' : `남은 시간 ${ttl.label}`}
          </Text>
        </View>
        <Pressable onPress={resendOneWon} disabled={resend.isPending} hitSlop={6}>
          <Text style={[theme.typography.bodySm, styles.resend, { color: theme.color.text.muted }]}>
            1원 다시 보내기
          </Text>
        </Pressable>
      </View>

      {error ? (
        <Text
          style={[
            theme.typography.caption,
            styles.center,
            { color: theme.color.status.danger.text, marginTop: theme.space.md },
          ]}
        >
          {error}
        </Text>
      ) : null}

      <View style={styles.spacer} />

      <Button
        label="인증하기"
        variant="primary"
        onPress={submit}
        loading={verify.isPending}
        disabled={!canSubmit}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: { textAlign: 'center' },
  example: { borderWidth: 1, padding: 16, gap: 10 },
  exampleRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  exampleName: { flex: 1 },
  timer: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  resend: { textDecorationLine: 'underline' },
  spacer: { flex: 1, minHeight: 24 },
});
