import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { isValidEmail, SignupStepLayout, useCountdown, useSignupStore } from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { Button, Input } from '@/shared/ui';

const CODE_TTL_SECONDS = 10 * 60; // 인증코드 유효시간 10분

export default function SignupEmailScreen() {
  const theme = useTheme();
  const patch = useSignupStore((s) => s.patch);
  const savedEmail = useSignupStore((s) => s.email);

  const [email, setEmail] = useState(savedEmail);
  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const countdown = useCountdown();

  const emailOk = isValidEmail(email);

  // TODO(S15P21A505-92): 이메일 인증 API 연동. 지금은 UI 흐름만.
  const sendCode = () => {
    if (!emailOk) return;
    setCodeSent(true);
    countdown.start(CODE_TTL_SECONDS);
    Alert.alert(
      '준비 중',
      '이메일 인증은 백엔드 작업 후 연결됩니다. 지금은 아무 코드로 넘어갈 수 있어요.',
    );
  };

  const next = () => {
    patch({ email: email.trim(), emailVerified: true });
    router.push('/signup/account');
  };

  const canProceed = emailOk && codeSent && code.trim().length > 0;

  return (
    <SignupStepLayout
      step={1}
      title="이메일 본인인증"
      subtitle="회원가입 전, 이메일을 확인해주세요."
      footer={<Button label="다음" variant="primary" onPress={next} disabled={!canProceed} />}
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
          onChangeText={setEmail}
        />
        <Button
          label={codeSent ? '인증코드 재전송' : '인증코드 받기'}
          variant="outline"
          onPress={sendCode}
          disabled={!emailOk}
        />
      </View>

      <View style={{ gap: theme.space.sm, marginTop: theme.space['2xl'] }}>
        <Input
          variant="box"
          label="인증코드"
          placeholder="메일로 받은 코드를 입력하세요"
          keyboardType="number-pad"
          value={code}
          onChangeText={setCode}
          editable={codeSent}
        />
        {codeSent ? (
          <View style={styles.metaRow}>
            <View style={styles.timer}>
              <Ionicons name="time-outline" size={14} color={theme.color.text.muted} />
              <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                남은 시간 <Text style={{ color: theme.color.brand.solid }}>{countdown.label}</Text>
              </Text>
            </View>
            <Pressable onPress={sendCode} hitSlop={6}>
              <Text
                style={[theme.typography.caption, styles.resend, { color: theme.color.text.muted }]}
              >
                인증코드 다시 보내기
              </Text>
            </Pressable>
          </View>
        ) : null}
      </View>
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
