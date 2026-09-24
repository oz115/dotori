import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import {
  EmailVerification,
  recoveryHandoff,
  resetPasswordErrorMessage,
  useResetPassword,
} from '@/features/auth';
import { apiErrorCode } from '@/shared/api';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Header, Input, Screen } from '@/shared/ui';

export default function ResetPasswordScreen() {
  const theme = useTheme();
  const resetPassword = useResetPassword();

  const [loginId, setLoginId] = useState('');
  // 아이디 찾기에서 이미 이메일 인증을 마치고 넘어왔다면 그 결과를 이어 쓴다 (인증을 또 받지 않게)
  const [handoff, setHandoff] = useState(() => recoveryHandoff.peek());
  const [verifyKey, setVerifyKey] = useState(0);
  const [verified, setVerified] = useState<{ email: string; verificationToken: string } | null>(
    handoff,
  );

  // 토큰은 한 번 받아 state 에 옮겼으면 전달 슬롯에서 비운다
  useEffect(() => recoveryHandoff.clear(), []);
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [submitError, setSubmitError] = useState('');

  const onReset = () => {
    setVerified(null);
    setSubmitError('');
  };

  const submit = () => {
    if (resetPassword.isPending || !verified || loginId.trim().length === 0) return;

    if (password.length < 8 || password.length > 64) {
      setFieldError('비밀번호는 공백 없이 8자 이상 64자 이하여야 해요.');
      return;
    }
    if (password !== passwordConfirm) {
      setFieldError('비밀번호가 일치하지 않아요.');
      return;
    }
    setFieldError('');
    setSubmitError('');

    resetPassword.mutate(
      {
        loginId: loginId.trim(),
        email: verified.email,
        verificationToken: verified.verificationToken,
        newPassword: password,
      },
      {
        onSuccess: () => {
          Alert.alert('완료', '비밀번호가 변경됐어요. 새 비밀번호로 로그인해주세요.', [
            { text: '확인', onPress: () => router.replace('/') },
          ]);
        },
        onError: (e) => {
          setSubmitError(resetPasswordErrorMessage(e));
          // 인증이 만료됐으면 인증 블록을 처음 상태로 되돌려 다시 받게 한다
          if (apiErrorCode(e) === 'INVALID_VERIFICATION_TOKEN') {
            setHandoff(null);
            setVerified(null);
            setVerifyKey((k) => k + 1);
          }
        },
      },
    );
  };

  const canSubmit =
    !!verified &&
    loginId.trim().length > 0 &&
    password.length > 0 &&
    passwordConfirm.length > 0 &&
    !resetPassword.isPending;

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="비밀번호 재설정" onBack={() => router.back()} />}
    >
      <Callout
        icon={<Ionicons name="lock-closed-outline" size={22} color={theme.color.brand.solid} />}
        title="본인인증 후 새 비밀번호를 설정해요"
        description="아이디와 이메일 본인인증이 필요해요."
      />

      <View style={{ marginTop: theme.space.xl, gap: theme.space.xl }}>
        <Input
          variant="box"
          label="아이디"
          placeholder="아이디를 입력하세요"
          autoCapitalize="none"
          autoCorrect={false}
          value={loginId}
          onChangeText={setLoginId}
        />

        <EmailVerification
          key={verifyKey}
          purpose="PASSWORD_RESET"
          initialVerified={handoff ?? undefined}
          onVerified={setVerified}
          onReset={onReset}
        />

        {verified ? (
          <View style={{ gap: theme.space.xl }}>
            <Input
              variant="box"
              label="새 비밀번호"
              placeholder="새 비밀번호를 입력하세요"
              secureTextEntry
              value={password}
              onChangeText={setPassword}
            />
            <Input
              variant="box"
              label="새 비밀번호 확인"
              placeholder="새 비밀번호를 한번 더 입력하세요"
              secureTextEntry
              value={passwordConfirm}
              onChangeText={setPasswordConfirm}
              error={fieldError}
            />
          </View>
        ) : null}
      </View>

      {submitError ? (
        <Text
          style={[
            theme.typography.caption,
            { color: theme.color.status.danger.text, marginTop: theme.space.sm },
          ]}
        >
          {submitError}
        </Text>
      ) : null}

      <View style={[styles.spacer, { minHeight: theme.space['2xl'] }]} />

      <Button
        label="비밀번호 재설정"
        variant="primary"
        onPress={submit}
        loading={resetPassword.isPending}
        disabled={!canSubmit}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  spacer: { flex: 1 },
});
