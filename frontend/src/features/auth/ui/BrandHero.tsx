import { Image, StyleSheet, Text, useWindowDimensions, View } from 'react-native';

import { useTheme } from '@/shared/theme';

const LOGO = require('@/assets/images/logo-wordmark.png'); // 308 x 113
const MASCOT = require('@/assets/images/mascot-login.png'); // 272 x 367

const LOGO_RATIO = 113 / 308;
const MASCOT_RATIO = 367 / 272;

/**
 * 인증 플로우 상단 브랜드 영역 — 로고 + 태그라인 + 마스코트.
 * 이미지 크기는 화면 너비에 비례(useWindowDimensions)하되 상한을 둬서
 * 태블릿 등 큰 화면에서 과도하게 커지지 않게 한다.
 */
export function BrandHero() {
  const theme = useTheme();
  const { width } = useWindowDimensions();

  const logoW = Math.min(width * 0.38, 150);
  const mascotW = Math.min(width * 0.34, 145);

  return (
    <View>
      <Image
        source={LOGO}
        style={{ width: logoW, height: logoW * LOGO_RATIO }}
        resizeMode="contain"
      />

      <View style={[styles.row, { marginTop: theme.space.md, marginLeft: theme.space.sm }]}>
        <View style={[styles.text, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>
            도토리로 시작하는{'\n'}우리의{' '}
            <Text style={{ color: theme.color.brand.solid }}>경제 습관</Text>
          </Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.default }]}>
            즐겁게 배우고, 똑똑하게 모아요
          </Text>
        </View>

        <Image
          source={MASCOT}
          style={{ width: mascotW, height: mascotW * MASCOT_RATIO, marginTop: theme.space.lg }}
          resizeMode="contain"
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'flex-start' },
  text: { flex: 1 },
});
