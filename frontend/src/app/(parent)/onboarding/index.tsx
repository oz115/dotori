import { Redirect, router } from 'expo-router';
import { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { signOut, useAuthStore, WithdrawButton } from '@/features/auth';
import { useFamilyReady, useOnboardingStore } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { Button, Screen, StepCard } from '@/shared/ui';

/** 아이가 코드를 입력해 합류했는지 확인하는 주기 */
const JOIN_POLL_MS = 5_000;

/**
 * 부모 온보딩 허브. 가족을 만들고 아이가 합류하기 전까지 부모는 탭 화면 대신 이 화면에 머문다
 * ((parent)/(tabs) 게이트). 1) 가족 만들기(경제 규칙 설정 포함) → 2) 아이 초대(코드 공유 →
 * 아이가 코드를 입력해 합류) 순서로 안내하고, 아이가 합류하면 자동으로 홈으로 넘어간다.
 * 자녀의 (child)/setup 허브와 같은 모양이다.
 */
export default function OnboardingHomeScreen() {
  const theme = useTheme();
  const name = useAuthStore((s) => s.user?.name);
  const reset = useOnboardingStore((s) => s.reset);
  const family = useFamilyReady(JOIN_POLL_MS);

  // 온보딩 퍼널의 진입점 — 이전 회차 입력값 초기화
  useEffect(() => reset(), [reset]);

  // 아이가 합류했다 — 폴링이 이를 알아채면 바로 홈으로 보낸다
  if (family.ready) {
    return <Redirect href="/" />;
  }

  return (
    <Screen scroll>
      <View style={[styles.topSpacer, { minHeight: theme.space['2xl'] }]} />

      <View style={{ gap: theme.space.xs }}>
        <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>
          {name ? `${name}님, 반가워요` : '반가워요'}
        </Text>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
          두 가지만 하면 도토리를 시작할 수 있어요.
        </Text>
      </View>

      <View style={{ marginTop: theme.space['4xl'], gap: theme.space.lg }}>
        <StepCard
          title="우리 가족 만들기"
          description="가족 이름과 용돈·보상 규칙을 정하면 우리집 경제가 만들어져요."
          done={family.hasFamily}
          enabled={!family.hasFamily}
          action={
            <Button
              label="우리집 경제 만들기 시작"
              variant="gradient"
              onPress={() => router.push('/onboarding/family-name')}
            />
          }
        />
        <StepCard
          title="아이 초대하기"
          description={
            family.hasFamily
              ? '아이에게 6자리 코드를 보내주세요. 아이가 코드를 입력하면 바로 시작할 수 있어요.'
              : '가족을 만든 뒤 아이에게 6자리 초대 코드를 보낼 수 있어요.'
          }
          enabled={family.hasFamily}
          action={
            <Button
              label="초대 코드 보내기"
              variant={family.hasFamily ? 'gradient' : 'secondary'}
              onPress={() => router.push('/onboarding/invite')}
              disabled={!family.hasFamily}
            />
          }
        />
      </View>

      <View style={styles.spacer} />

      <View style={{ marginTop: theme.space.lg, gap: theme.space.xs }}>
        <Button label="로그아웃" variant="ghost" onPress={signOut} />
        <WithdrawButton />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  topSpacer: { flex: 0.4 },
  spacer: { flex: 1, minHeight: 24 },
});
