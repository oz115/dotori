import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { useMyAccount } from '@/features/account';
import { signOut, useAuthStore, WithdrawButton } from '@/features/auth';
import { useFamilyStatus } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { Button, Screen, StepCard } from '@/shared/ui';

/**
 * 자녀 온보딩 허브. 자녀가 아직 앱을 쓸 준비가 안 됐을 때((child)/(tabs) 게이트) 진입한다.
 * 1) 가족 연결(초대 코드) → 2) 본인 계좌 연동 순서로 안내한다.
 */
export default function ChildOnboardingHomeScreen() {
  const theme = useTheme();
  const name = useAuthStore((s) => s.user?.name);
  const family = useFamilyStatus();
  const account = useMyAccount();

  const hasFamily = family.data?.hasFamily === true;
  const hasAccount = account.data != null;

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
          title="가족 연결하기"
          description="부모님께 받은 6자리 코드를 넣으면 끝나요."
          done={hasFamily}
          enabled={!hasFamily}
          action={
            <Button
              label="코드 입력하기"
              variant="gradient"
              onPress={() => router.push('/setup/connect')}
            />
          }
        />
        <StepCard
          title="계좌 연동하기"
          description={
            hasFamily
              ? '본인의 계좌를 연동해주세요.'
              : '가족 연결 후에 할 수 있어요. 본인의 계좌를 연동해주세요.'
          }
          done={hasAccount}
          enabled={hasFamily && !hasAccount}
          action={
            <Button
              label="연동 하러 가기"
              variant={hasFamily ? 'primary' : 'secondary'}
              onPress={() => router.push('/setup/account/link')}
              disabled={!hasFamily}
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
