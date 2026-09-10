import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { Header, ProgressIndicator, Screen } from '@/shared/ui';

type OnboardingStepLayoutProps = {
  /** 진행 표시기에서 강조할 단계 (1 가족 · 2 경제 규칙 · 3 아이 초대) */
  step: number;
  /** 헤더 가운데 제목 */
  headerTitle: string;
  /** 제목 위 작은 분류 라벨 (브랜드색) */
  eyebrow?: string;
  title: string;
  subtitle?: string;
  children: ReactNode;
  /** 하단 고정 버튼 영역 */
  footer: ReactNode;
  /** 진행 표시기와 제목 사이 간격을 좁힌다 (제목 블록이 이미 긴 경제 규칙 화면용) */
  compactHeader?: boolean;
};

/**
 * 부모 온보딩 스텝 공통 레이아웃.
 * 헤더 + 진행 표시기(1-2-3) + (분류라벨) 제목/설명 + 폼 + 하단 버튼.
 * 회원가입의 SignupStepLayout 과 같은 뼈대다.
 */
export function OnboardingStepLayout({
  step,
  headerTitle,
  eyebrow,
  title,
  subtitle,
  children,
  footer,
  compactHeader = false,
}: OnboardingStepLayoutProps) {
  const theme = useTheme();

  const goBack = () => {
    if (router.canGoBack()) router.back();
    else router.replace('/onboarding');
  };

  return (
    <Screen scroll keyboardAvoiding header={<Header title={headerTitle} onBack={goBack} />}>
      <View style={{ marginTop: theme.space.lg }}>
        <ProgressIndicator total={3} current={step} />
      </View>

      <View
        style={{
          marginTop: compactHeader ? theme.space['2xl'] : theme.space['5xl'],
          gap: theme.space.sm,
        }}
      >
        {eyebrow ? (
          <Text style={[theme.typography.label, { color: theme.color.brand.solid }]}>
            {eyebrow}
          </Text>
        ) : null}
        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>{title}</Text>
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
