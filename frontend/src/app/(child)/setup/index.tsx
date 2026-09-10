import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { signOut, useAuthStore } from '@/features/auth';
import { useFamilyStatus } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { Button, Screen } from '@/shared/ui';

/**
 * 자녀 온보딩 허브. 로그인한 자녀가 아직 앱을 쓸 준비가 안 됐을 때 진입한다.
 * 1) 가족 연결(초대 코드) → 2) 본인 계좌 연동 순서로 안내한다.
 */
export default function ChildOnboardingHomeScreen() {
  const theme = useTheme();
  const name = useAuthStore((s) => s.user?.name);
  const status = useFamilyStatus();

  const hasFamily = status.data?.hasFamily ?? false;

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
        {/* 1. 가족 연결 */}
        <View
          style={[
            styles.card,
            {
              borderColor: hasFamily ? theme.color.border.default : theme.color.brand.solid,
              backgroundColor: hasFamily ? theme.color.background.surface : theme.color.brand.tint,
              borderRadius: theme.radius.lg,
              padding: theme.space.lg,
              gap: theme.space.md,
              opacity: hasFamily ? 0.6 : 1,
            },
          ]}
        >
          <View style={styles.cardHead}>
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              가족 연결하기
            </Text>
            {hasFamily ? (
              <Ionicons name="checkmark-circle" size={20} color={theme.color.brand.solid} />
            ) : null}
          </View>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            부모님께 받은 6자리 코드를 넣으면 끝나요.
          </Text>
          {!hasFamily ? (
            <Button
              label="코드 입력하기"
              variant="gradient"
              onPress={() => router.push('/setup/connect')}
            />
          ) : null}
        </View>

        {/* 2. 계좌 연동 */}
        <View
          style={[
            styles.card,
            {
              borderColor: theme.color.border.default,
              backgroundColor: theme.color.background.surface,
              borderRadius: theme.radius.lg,
              padding: theme.space.lg,
              gap: theme.space.md,
              opacity: hasFamily ? 1 : 0.6,
            },
          ]}
        >
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            계좌 연동하기
          </Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            {hasFamily
              ? '본인의 계좌를 연동해주세요.'
              : '가족 연결 후에 할 수 있어요. 본인의 계좌를 연동해주세요.'}
          </Text>
          {/* 계좌 연동 화면은 S15P21A505-164 에서 붙인다. 지금은 항상 비활성 */}
          <Button label="연동 하러 가기" variant="secondary" onPress={() => {}} disabled />
        </View>
      </View>

      <View style={styles.spacer} />

      <Button
        label="로그아웃"
        variant="ghost"
        onPress={signOut}
        style={{ marginTop: theme.space.lg }}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  topSpacer: { flex: 0.4 },
  card: { borderWidth: 1.5 },
  cardHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  spacer: { flex: 1, minHeight: 24 },
});
