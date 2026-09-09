import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Text, View } from 'react-native';

import { EmailVerification, resetPasswordErrorMessage, useResetPassword } from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Header, Input, Screen } from '@/shared/ui';

export default function ResetPasswordScreen() {
  const theme = useTheme();
  const resetPassword = useResetPassword();

  const [loginId, setLoginId] = useState('');
  const [verified, setVerified] = useState<{ email: string; verificationToken: string } | null>(
    null,
  );
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
        onError: (e) => setSubmitError(resetPasswordErrorMessage(e)),
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

        <EmailVerification purpose="PASSWORD_RESET" onVerified={setVerified} onReset={onReset} />

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

      <Button
        label="비밀번호 재설정"
        variant="primary"
        onPress={submit}
        loading={resetPassword.isPending}
        disabled={!canSubmit}
        style={{ marginTop: theme.space['2xl'] }}
      />
    </Screen>
  );
}
