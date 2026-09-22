import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { BrandHero, loginErrorMessage, savedLoginId, useLogin } from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Checkbox, IdIcon, Input, PasswordIcon, Screen } from '@/shared/ui';

const ACORN = require('@/assets/images/acorn.png'); // 734 x 806

export default function LoginScreen() {
  const theme = useTheme();
  const login = useLogin();

  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [rememberId, setRememberId] = useState(false);
  const [validationError, setValidationError] = useState('');

  useEffect(() => {
    savedLoginId.get().then((saved) => {
      if (saved) {
        setLoginId(saved);
        setRememberId(true);
      }
    });
  }, []);

  const submit = () => {
    if (login.isPending) return;
    if (loginId.trim().length === 0 || password.length === 0) {
      setValidationError('아이디와 비밀번호를 입력해주세요.');
      return;
    }
    setValidationError('');
    login.mutate({ loginId: loginId.trim(), password, rememberId });
  };

  const errorText = validationError || (login.isError ? loginErrorMessage(login.error) : '');

  return (
    <Screen scroll keyboardAvoiding>
      <View style={[styles.topSpacer, { minHeight: theme.space['3xl'] }]} />

      <BrandHero />

      <View style={{ marginTop: theme.space['4xl'], gap: theme.space.md }}>
        <Input
          variant="box"
          elevated
          leftIcon={<IdIcon size={17} color={theme.color.text.muted} />}
          placeholder="아이디를 입력하세요"
          autoCapitalize="none"
          autoCorrect={false}
          value={loginId}
          onChangeText={setLoginId}
          returnKeyType="next"
        />
        <Input
          variant="box"
          elevated
          leftIcon={<PasswordIcon size={18} color={theme.color.text.muted} />}
          placeholder="비밀번호를 입력하세요"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
          returnKeyType="done"
          onSubmitEditing={submit}
        />
        {errorText ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            {errorText}
          </Text>
        ) : null}
      </View>

      <View style={[styles.metaRow, { marginTop: theme.space.lg, gap: theme.space.sm }]}>
        <Checkbox
          shape="circle"
          checked={rememberId}
          onChange={setRememberId}
          label="아이디 저장"
        />
        <View style={[styles.links, { gap: theme.space.sm }]}>
          <Pressable onPress={() => router.push('/find-id')} hitSlop={6}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              아이디 찾기
            </Text>
          </Pressable>
          <Text style={[theme.typography.bodySm, { color: theme.color.border.strong }]}>|</Text>
          <Pressable onPress={() => router.push('/reset-password')} hitSlop={6}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              비밀번호 재설정
            </Text>
          </Pressable>
        </View>
      </View>

      {/* 로그인 버튼은 폼에 딸린 것이라 폼 바로 아래 고정 간격에 둔다.
          전에는 회원가입 안내와 한 덩어리로 묶여 화면 맨 아래에 있었는데, 그 사이 여백이
          flex 라 화면 여유를 다 먹었다 — 키보드가 올라오면 0으로 줄어 비밀번호 칸에
          버튼이 달라붙고, 내리면 다시 멀어져서 간격이 계속 달라졌다 (QA). */}
      <View style={{ marginTop: theme.space['3xl'] }}>
        <Button label="로그인" variant="gradient" onPress={submit} loading={login.isPending} />
      </View>

      <View style={styles.spacer} />

      <Callout
        icon={<Image source={ACORN} style={styles.acorn} resizeMode="contain" />}
        title="도토리가 처음이신가요?"
        description="회원가입하고 우리만의 경제 습관을 시작해보세요!"
        action={
          <Button
            label="회원가입"
            variant="outline"
            size="sm"
            onPress={() => router.push('/agreement')}
            trailingIcon={
              <Ionicons name="chevron-forward" size={14} color={theme.color.brand.solid} />
            }
          />
        }
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  topSpacer: { flex: 0.5 },
  // 남는 높이를 먹어 회원가입 안내를 맨 아래로 내린다. 키보드가 올라와 여유가 없어져도
  // 로그인 버튼과 12(= space.md)는 떨어뜨린다 — 붙으면 두 버튼이 한 덩어리로 보인다.
  spacer: { flex: 1, minHeight: 12 },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
  },
  links: { flexDirection: 'row', alignItems: 'center', flexShrink: 1 },
  acorn: { width: 30, height: 33 },
});
