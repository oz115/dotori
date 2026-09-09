import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { EmailVerification, findIdErrorMessage, useFindId } from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Header, Input, Screen } from '@/shared/ui';

export default function FindIdScreen() {
  const theme = useTheme();
  const findId = useFindId();

  const [name, setName] = useState('');
  const [verified, setVerified] = useState<{ email: string; verificationToken: string } | null>(
    null,
  );
  const [foundId, setFoundId] = useState('');
  const [error, setError] = useState('');

  const resetVerification = () => {
    setVerified(null);
    setFoundId('');
    setError('');
  };

  const submit = () => {
    if (findId.isPending || !verified || name.trim().length === 0) return;
    setError('');
    findId.mutate(
      { name: name.trim(), email: verified.email, verificationToken: verified.verificationToken },
      {
        onSuccess: (res) => setFoundId(res.loginId),
        onError: (e) => setError(findIdErrorMessage(e)),
      },
    );
  };

  const canSubmit = !!verified && name.trim().length > 0 && !findId.isPending;

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="아이디 찾기" onBack={() => router.back()} />}
    >
      {foundId ? (
        <>
          <View style={{ gap: theme.space.xl }}>
            <Callout
              icon={<Ionicons name="checkmark-circle" size={22} color={theme.color.brand.solid} />}
              title="회원님의 아이디예요"
              description="보안을 위해 일부만 표시돼요."
            />
            <View
              style={[
                styles.result,
                {
                  borderColor: theme.color.border.default,
                  borderRadius: theme.radius.lg,
                  backgroundColor: theme.color.background.surface,
                },
              ]}
            >
              <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>
                {foundId}
              </Text>
            </View>
          </View>

          <View style={styles.spacer} />

          <View style={{ gap: theme.space.md }}>
            <Button
              label="로그인하러 가기"
              variant="gradient"
              onPress={() => router.replace('/')}
            />
            <Button
              label="비밀번호 재설정"
              variant="outline"
              onPress={() => router.replace('/reset-password')}
            />
          </View>
        </>
      ) : (
        <>
          <Callout
            icon={<Ionicons name="key-outline" size={22} color={theme.color.brand.solid} />}
            title="가입 정보로 아이디를 찾아요"
            description="이름과 이메일 본인인증이 필요해요."
          />

          <View style={{ marginTop: theme.space.xl, gap: theme.space.xl }}>
            <Input
              variant="box"
              label="이름"
              placeholder="이름을 입력하세요"
              value={name}
              onChangeText={setName}
            />

            <EmailVerification
              purpose="FIND_ID"
              onVerified={setVerified}
              onReset={resetVerification}
            />
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

          <View style={[styles.spacer, { minHeight: theme.space['2xl'] }]} />

          <Button
            label="아이디 찾기"
            variant="primary"
            onPress={submit}
            loading={findId.isPending}
            disabled={!canSubmit}
          />
        </>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  spacer: { flex: 1 },
  result: {
    borderWidth: 1,
    alignItems: 'center',
    paddingVertical: 28,
  },
});
