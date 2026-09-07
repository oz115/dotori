import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useSignupStore } from '@/features/auth/model/signupStore';
import { useTheme } from '@/shared/theme';
import { Header, ProgressIndicator, Screen } from '@/shared/ui';

type SignupStepLayoutProps = {
  /** 1~3 단계 */
  step: number;
  title: string;
  subtitle: string;
  children: ReactNode;
  /** 하단 고정 버튼 영역 */
  footer: ReactNode;
};

/**
 * 회원가입 3단계(이메일·계정정보·역할) 공통 레이아웃.
 * 헤더("도토리" + 취소) + 진행 표시기 + 제목/설명 + 폼 + 하단 버튼.
 */
export function SignupStepLayout({
  step,
  title,
  subtitle,
  children,
  footer,
}: SignupStepLayoutProps) {
  const theme = useTheme();
  const reset = useSignupStore((s) => s.reset);

  const cancel = () => {
    reset();
    router.replace('/');
  };

  const goBack = () => (router.canGoBack() ? router.back() : cancel());

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="도토리" onBack={goBack} action={{ label: '취소', onPress: cancel }} />}
    >
      <ProgressIndicator total={3} current={step} />

      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.xs }}>
        <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>{title}</Text>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>{subtitle}</Text>
      </View>

      <View style={[styles.form, { marginTop: theme.space['5xl'] }]}>{children}</View>

      <View style={styles.spacer} />

      <View style={{ marginTop: theme.space.xl }}>{footer}</View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  form: { flexShrink: 0 },
  spacer: { flex: 1, minHeight: 24 },
});
