import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { emailVerificationErrorMessage } from '@/features/auth/lib/emailVerificationError';
import { isValidEmail } from '@/features/auth/lib/signupValidation';
import { useCountdown } from '@/features/auth/lib/useCountdown';
import type { EmailVerificationPurpose } from '@/features/auth/model/types';
import { useConfirmEmailCode, useSendEmailCode } from '@/features/auth/model/useEmailVerification';
import { useTheme } from '@/shared/theme';
import { Button, Input } from '@/shared/ui';

const CODE_TTL_SECONDS = 10 * 60; // 인증코드 유효시간 10분
const RESEND_COOLDOWN_SECONDS = 60; // 재전송 쿨다운

type Props = {
  /** 발송 용도. 서버가 용도에 따라 중복 이메일 처리 등을 달리한다. */
  purpose: EmailVerificationPurpose;
  /** 인증코드 확인에 성공하면 호출된다. 이후 단계는 이 토큰을 쓴다. */
  onVerified: (result: { email: string; verificationToken: string }) => void;
  /** 인증 완료 뒤 이메일이 수정돼 인증이 무효화되면 호출된다. */
  onReset?: () => void;
  /** 앞 단계에서 넘어온 초기 이메일 값 */
  initialEmail?: string;
};

/**
 * 이메일 본인인증 블록 (이메일 입력 → 코드 발송 → 코드 확인).
 * 회원가입·아이디 찾기·비밀번호 재설정이 공유한다. `purpose` 만 바꿔 끼운다.
 *
 * 발송은 서버가 SMTP 를 거쳐 느릴 수 있어 응답을 기다리지 않고 코드 입력 UI 로
 * 바로 넘어간다. 실패하면 되돌린다.
 */
export function EmailVerification({ purpose, onVerified, onReset, initialEmail = '' }: Props) {
  const theme = useTheme();

  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [error, setError] = useState('');

  const codeTtl = useCountdown();
  const resendCooldown = useCountdown();
  const send = useSendEmailCode();
  const confirm = useConfirmEmailCode();

  const emailOk = isValidEmail(email);

  const clearVerification = () => {
    setCodeSent(false);
    setCode('');
    setVerified(false);
    codeTtl.stop();
    resendCooldown.stop();
  };

  const onChangeEmail = (value: string) => {
    setEmail(value);
    setError('');
    if (codeSent || verified) {
      clearVerification();
      onReset?.();
    }
  };

  const sendCode = () => {
    if (!emailOk || resendCooldown.running) return;
    setError('');
    setCodeSent(true);
    setCode('');
    codeTtl.start(CODE_TTL_SECONDS);
    resendCooldown.start(RESEND_COOLDOWN_SECONDS);
    send.mutate(
      { email: email.trim(), purpose },
      {
        onError: (e) => {
          setError(emailVerificationErrorMessage(e));
          clearVerification();
        },
      },
    );
  };

  const verifyCode = () => {
    if (confirm.isPending || verified || !codeSent || code.trim().length === 0) return;
    setError('');
    confirm.mutate(
      { email: email.trim(), code: code.trim() },
      {
        onSuccess: (res) => {
          setVerified(true);
          codeTtl.stop();
          resendCooldown.stop();
          onVerified({ email: email.trim(), verificationToken: res.verificationToken });
        },
        onError: (e) => setError(emailVerificationErrorMessage(e)),
      },
    );
  };

  return (
    <View style={{ gap: theme.space.xl }}>
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
          editable={!verified}
        />
        {!verified ? (
          <Button
            label={codeSent ? '인증코드 재전송' : '인증코드 받기'}
            variant="outline"
            onPress={sendCode}
            disabled={!emailOk || (codeSent && resendCooldown.running)}
          />
        ) : null}
      </View>

      {codeSent && !verified ? (
        <View style={{ gap: theme.space.sm }}>
          <Input
            variant="box"
            label="인증코드"
            placeholder="메일로 받은 코드를 입력하세요"
            keyboardType="number-pad"
            value={code}
            onChangeText={setCode}
            rightSlot={
              <Button
                label="확인"
                variant="outline"
                size="sm"
                onPress={verifyCode}
                loading={confirm.isPending}
                disabled={code.trim().length === 0}
              />
            }
          />
          <View style={styles.metaRow}>
            <View style={styles.timer}>
              <Ionicons name="time-outline" size={14} color={theme.color.text.muted} />
              <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                남은 시간 <Text style={{ color: theme.color.brand.solid }}>{codeTtl.label}</Text>
              </Text>
            </View>
            {resendCooldown.running ? (
              <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                재전송 {resendCooldown.label}
              </Text>
            ) : null}
          </View>
        </View>
      ) : null}

      {verified ? (
        <View style={styles.verifiedRow}>
          <Ionicons name="checkmark-circle" size={16} color={theme.color.brand.solid} />
          <Text style={[theme.typography.caption, { color: theme.color.brand.solid }]}>
            이메일 인증이 완료됐어요.
          </Text>
        </View>
      ) : null}

      {error ? (
        <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
          {error}
        </Text>
      ) : null}
    </View>
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
  verifiedRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
});
