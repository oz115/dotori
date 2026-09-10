import { router } from 'expo-router';
import { useEffect } from 'react';
import { Image, StyleSheet, Text, useWindowDimensions, View } from 'react-native';

import { useAuthStore } from '@/features/auth';
import { useOnboardingStore } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { Button, ProgressIndicator, Screen } from '@/shared/ui';

const MASCOT = require('@/assets/images/parent-onboarding.png'); // 253 x 294
const MASCOT_RATIO = 294 / 253;

const STEPS = ['가족 생성하기', '경제 생태계 만들기', '아이 초대하기'];

/**
 * 부모 온보딩 허브. 로그인한 부모가 아직 가족이 없을 때 진입한다.
 * 준비 단계를 안내하고 "가족 만들기" 스텝 플로우로 보낸다.
 * 배치는 로그인 화면과 같은 방식 — 위/아래 flex 스페이서로 여백을 분배한다.
 */
export default function OnboardingHomeScreen() {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const name = useAuthStore((s) => s.user?.name);
  const reset = useOnboardingStore((s) => s.reset);

  // 온보딩 퍼널의 진입점 — 이전 회차 입력값 초기화
  useEffect(() => reset(), [reset]);

  const mascotW = Math.min(width * 0.3, 132);

  return (
    <Screen scroll>
      <View style={[styles.topSpacer, { minHeight: theme.space.xl }]} />

      <View style={styles.hero}>
        <Text style={[theme.typography.h1, styles.heroTitle, { color: theme.color.text.strong }]}>
          안녕하세요,{'\n'}
          <Text style={{ color: theme.color.brand.solid }}>{name ? `${name}님` : '반가워요'}</Text>
        </Text>
        <Image
          source={MASCOT}
          style={{ width: mascotW, height: mascotW * MASCOT_RATIO }}
          resizeMode="contain"
        />
      </View>

      <Text
        style={[
          theme.typography.body,
          { color: theme.color.text.default, marginTop: theme.space.md },
        ]}
      >
        도토리와 함께 우리 아이의{' '}
        <Text style={{ color: theme.color.brand.solid }}>건강한 경제 습관</Text>을 시작해요.
      </Text>

      <View
        style={[
          styles.card,
          {
            marginTop: theme.space['3xl'],
            borderColor: theme.color.border.default,
            borderRadius: theme.radius.lg,
            padding: theme.space.lg,
            gap: theme.space.lg,
          },
        ]}
      >
        <View style={{ gap: theme.space.xs }}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            준비단계 <Text style={{ color: theme.color.brand.solid }}>0/3</Text>
          </Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            모든 단계를 완료하면 아이를 초대할 수 있어요.
          </Text>
        </View>
        <ProgressIndicator total={3} current={0} />
      </View>

      <View
        style={[
          styles.card,
          {
            marginTop: theme.space.lg,
            borderColor: theme.color.border.default,
            borderRadius: theme.radius.lg,
          },
        ]}
      >
        {STEPS.map((label, i) => (
          <View
            key={label}
            style={[
              styles.stepRow,
              {
                padding: theme.space.lg,
                gap: theme.space.md,
                borderTopWidth: i === 0 ? 0 : 1,
                borderTopColor: theme.color.border.default,
              },
            ]}
          >
            <View
              style={[
                styles.badge,
                {
                  backgroundColor: i === 0 ? theme.color.brand.solid : theme.color.border.default,
                },
              ]}
            >
              <Text
                style={[
                  theme.typography.label,
                  { color: i === 0 ? theme.color.text.onBrand : theme.color.text.muted },
                ]}
              >
                {i + 1}
              </Text>
            </View>
            <Text style={[theme.typography.body, { color: theme.color.text.strong }]}>{label}</Text>
          </View>
        ))}
      </View>

      <View style={styles.spacer} />

      <Button
        label="우리집 경제 만들기 시작"
        variant="gradient"
        onPress={() => router.push('/onboarding/family-name')}
        style={{ marginTop: theme.space.xl }}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  topSpacer: { flex: 0.4 },
  hero: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
  },
  heroTitle: { flex: 1, marginTop: 4 },
  card: { borderWidth: 1, overflow: 'hidden' },
  stepRow: { flexDirection: 'row', alignItems: 'center' },
  badge: {
    width: 28,
    height: 28,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  spacer: { flex: 1, minHeight: 24 },
});
