import type { ImageSourcePropType } from 'react-native';
import { Image, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type ChoreHeroBannerProps = {
  /** 굵게 나가는 본문. 줄바꿈은 부르는 쪽이 \n 으로 넣는다 */
  title: string;
  /** 제목 아래 한 줄 (보상 합계·설명 등) */
  caption?: string;
  mascot: ImageSourcePropType;
  /** 우측 슬롯 (부모 목록 배너의 '>' 버튼) */
  action?: React.ReactNode;
};

/**
 * 할 일 화면 상단의 초록 배너. 아이 목록·부모 목록·완료 내역이 같은 틀을 쓰고
 * 문구와 마스코트만 바꾼다.
 *
 * 마스코트는 배경이 투명한 PNG 라야 한다 (초록 위에 흰 네모가 보이면 안 된다).
 */
export function ChoreHeroBanner({ title, caption, mascot, action }: ChoreHeroBannerProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.banner,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.lg,
          paddingVertical: theme.space.lg,
          paddingHorizontal: theme.space.xl,
          gap: theme.space.md,
        },
      ]}
    >
      <View style={styles.body}>
        <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>{title}</Text>
        {caption ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.brand.subtleText }]}>
            {caption}
          </Text>
        ) : null}
      </View>
      <Image source={mascot} style={styles.mascot} resizeMode="contain" />
      {action}
    </View>
  );
}

const styles = StyleSheet.create({
  banner: { flexDirection: 'row', alignItems: 'center' },
  body: { flex: 1, gap: 4 },
  mascot: { width: 110, height: 110 },
});
