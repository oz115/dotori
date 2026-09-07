import { Ionicons } from '@expo/vector-icons';
import { useEffect, useState } from 'react';
import { Alert, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { BrandHero, loginErrorMessage, savedLoginId, useLogin } from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Checkbox, IdIcon, Input, PasswordIcon, Screen } from '@/shared/ui';

const ACORN = require('@/assets/images/acorn.png'); // 734 x 806

const soon = () => Alert.alert('준비 중', '곧 제공될 기능이에요.');

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
          leftIcon={<IdIcon size={20} color={theme.color.text.muted} />}
          placeholder="아이디를 입력하세요"
          autoCapitalize="none"
          autoCorrect={false}
          value={loginId}
          onChangeText={setLoginId}
          returnKeyType="next"
        />
        <Input
          variant="box"
          leftIcon={<PasswordIcon size={20} color={theme.color.text.muted} />}
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
          <Pressable onPress={soon} hitSlop={6}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              아이디 찾기
            </Text>
          </Pressable>
          <Text style={[theme.typography.bodySm, { color: theme.color.border.strong }]}>|</Text>
          <Pressable onPress={soon} hitSlop={6}>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
              비밀번호 재설정
            </Text>
          </Pressable>
        </View>
      </View>

      <View style={styles.spacer} />

      <View style={{ gap: theme.space.md }}>
        <Button label="로그인" variant="gradient" onPress={submit} loading={login.isPending} />
        <Callout
          icon={<Image source={ACORN} style={styles.acorn} resizeMode="contain" />}
          title="도토리가 처음이신가요?"
          description="회원가입하고 우리만의 경제 습관을 시작해보세요!"
          action={
            <Button
              label="회원가입"
              variant="outline"
              size="sm"
              onPress={soon}
              trailingIcon={
                <Ionicons name="chevron-forward" size={14} color={theme.color.brand.solid} />
              }
            />
          }
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  topSpacer: { flex: 0.5 },
  spacer: { flex: 1 },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
  },
  links: { flexDirection: 'row', alignItems: 'center', flexShrink: 1 },
  acorn: { width: 30, height: 33 },
});
