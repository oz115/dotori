import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import {
  EMAIL_CODE_LENGTH,
  emailVerificationErrorMessage,
  isCompleteEmailCode,
  isValidEmail,
  normalizeEmailCode,
  SignupStepLayout,
  useConfirmEmailCode,
  useSendEmailCode,
  useSignupStore,
} from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { useCountdown } from '@/shared/lib/useCountdown';
import { Button, Input } from '@/shared/ui';

const CODE_TTL_SECONDS = 10 * 60; // 인증코드 유효시간 10분
const RESEND_COOLDOWN_SECONDS = 60; // 재전송 쿨다운

export default function SignupEmailScreen() {
  const theme = useTheme();
  const patch = useSignupStore((s) => s.patch);

  const [email, setEmail] = useState(() => useSignupStore.getState().email);
  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [error, setError] = useState('');
  const codeTtl = useCountdown(); // 인증코드 유효시간
  const resendCooldown = useCountdown(); // 재전송 쿨다운

  const send = useSendEmailCode();
  const confirm = useConfirmEmailCode();
  const emailOk = isValidEmail(email);

  const onChangeEmail = (v: string) => {
    setEmail(v);
    if (codeSent) {
      // 이메일이 바뀌면 이전 인증 상태·타이머 모두 초기화
      setCodeSent(false);
      setCode('');
      codeTtl.stop();
      resendCooldown.stop();
    }
  };

  const sendCode = () => {
    if (!emailOk || resendCooldown.running) return;
    setError('');
    // 메일 발송은 서버가 SMTP 를 거쳐 느릴 수 있어 응답을 기다리지 않는다.
    // 코드 입력 UI 로 바로 넘어가고, 실패하면 되돌린다.
    setCodeSent(true);
    setCode('');
    codeTtl.start(CODE_TTL_SECONDS);
    resendCooldown.start(RESEND_COOLDOWN_SECONDS);
    send.mutate(
      { email: email.trim(), purpose: 'SIGNUP' },
      {
        onError: (e) => {
          setError(emailVerificationErrorMessage(e));
          setCodeSent(false);
          codeTtl.stop();
          resendCooldown.stop();
        },
      },
    );
  };

  const next = () => {
    if (confirm.isPending || !codeSent || !isCompleteEmailCode(code)) return;
    setError('');
    confirm.mutate(
      { email: email.trim(), code },
      {
        onSuccess: (res) => {
          patch({
            email: email.trim(),
            emailVerified: true,
            verificationToken: res.verificationToken,
          });
          router.push('/signup/account');
        },
        onError: (e) => setError(emailVerificationErrorMessage(e)),
      },
    );
  };

  const canProceed = emailOk && codeSent && isCompleteEmailCode(code) && !confirm.isPending;

  return (
    <SignupStepLayout
      step={1}
      title="이메일 본인인증"
      subtitle="회원가입 전, 이메일을 확인해주세요."
      footer={
        <Button
          label="다음"
          variant="primary"
          onPress={next}
          loading={confirm.isPending}
          disabled={!canProceed}
        />
      }
    >
      <View style={{ gap: theme.space.sm }}>
        <Input
          variant="box"
          label="이메일"
          placeholder="이메일을 입력하세요"
          keyboardType="email-address"
          autoCapitalize="none"
          autoCorrect={false}
          value={email}
          onChangeText={onChangeEmail}
        />
        <Button
          label="인증코드 받기"
          variant="outline"
          onPress={sendCode}
          disabled={!emailOk || codeSent}
        />
      </View>

      {codeSent ? (
        <View style={{ gap: theme.space.sm, marginTop: theme.space['2xl'] }}>
          <Input
            variant="box"
            label="인증코드"
            placeholder="메일로 받은 코드를 입력하세요"
            // 서버 코드는 대문자·숫자 6자리라 숫자 키패드로는 입력할 수 없다
            autoCapitalize="characters"
            autoCorrect={false}
            maxLength={EMAIL_CODE_LENGTH}
            value={code}
            onChangeText={(v) => setCode(normalizeEmailCode(v))}
          />
          <View style={styles.metaRow}>
            <View style={styles.timer}>
              <Ionicons name="time-outline" size={14} color={theme.color.text.muted} />
              <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                남은 시간 <Text style={{ color: theme.color.brand.solid }}>{codeTtl.label}</Text>
              </Text>
            </View>
            <Pressable onPress={sendCode} disabled={resendCooldown.running} hitSlop={6}>
              <Text
                style={[
                  theme.typography.caption,
                  styles.resend,
                  {
                    color: resendCooldown.running
                      ? theme.color.text.muted
                      : theme.color.brand.solid,
                  },
                ]}
              >
                {resendCooldown.running
                  ? `인증코드 재전송 (${resendCooldown.label})`
                  : '인증코드 재전송'}
              </Text>
            </Pressable>
          </View>
        </View>
      ) : null}

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
    </SignupStepLayout>
  );
}

const styles = StyleSheet.create({
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: 8,
  },
  timer: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  resend: { textDecorationLine: 'underline' },
});
