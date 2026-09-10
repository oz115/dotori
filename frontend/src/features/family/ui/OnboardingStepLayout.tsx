import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { Header, ProgressIndicator, Screen } from '@/shared/ui';

type OnboardingStepLayoutProps = {
  /** 1~3 단계 (1 가족 · 2 경제 규칙 · 3 아이 초대) */
  step: number;
  title: string;
  subtitle?: string;
  children: ReactNode;
  /** 하단 고정 버튼 영역 */
  footer: ReactNode;
};

/**
 * 부모 온보딩 스텝 공통 레이아웃.
 * 헤더("가족 만들기" + 뒤로) + 진행 표시기(1-2-3) + 제목/설명 + 폼 + 하단 버튼.
 * 회원가입의 SignupStepLayout 과 같은 뼈대다.
 */
export function OnboardingStepLayout({
  step,
  title,
  subtitle,
  children,
  footer,
}: OnboardingStepLayoutProps) {
  const theme = useTheme();

  const goBack = () => {
    if (router.canGoBack()) router.back();
    else router.replace('/onboarding');
  };

  return (
    <Screen scroll keyboardAvoiding header={<Header title="가족 만들기" onBack={goBack} />}>
      <View style={{ marginTop: theme.space.lg }}>
        <ProgressIndicator total={3} current={step} />
      </View>

      <View style={{ marginTop: theme.space['5xl'], gap: theme.space.sm }}>
        <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>{title}</Text>
        {subtitle ? (
          <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>{subtitle}</Text>
        ) : null}
      </View>

      <View style={{ marginTop: theme.space['4xl'] }}>{children}</View>

      <View style={styles.spacer} />

      <View style={{ marginTop: theme.space['2xl'] }}>{footer}</View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  spacer: { flex: 1, minHeight: 24 },
});
