import { Image, StyleSheet, Text, View } from 'react-native';

import { mascotImage, type StoreMascotCharacter } from '@/features/store/lib/mascotImage';
import { useTheme } from '@/shared/theme';

type StoreBannerProps = {
  /** 부모/자녀 화면이 서로 다른 마스코트·설명 문구를 보여준다. role 을 이 feature 가 직접
   * 알 필요가 없도록, 어떤 캐릭터를 쓸지는 호출하는 쪽(StoreCatalog → 라우트)이 정해서 넘긴다. */
  character: StoreMascotCharacter;
};

/** 부모는 상품을 등록·관리하는 입장, 자녀는 둘러보고 사는 입장이라 문구가 다르다. */
const BANNER_DESCRIPTION: Record<StoreMascotCharacter, string> = {
  child: '도토리로 원하는 보상을 골라보세요!',
  // 자연스러운 줄바꿈 위치 없이 그대로 두면 "등록해보세" + "요!" 처럼 단어 중간에서
  // 끊겨 보여서, "등록해보세요!" 앞에서만 줄바꿈되게 직접 지정한다.
  parent: '아이에게 줄 보상을 자유롭게\n등록해보세요!',
};

/** 상점 화면 상단 배너. */
export function StoreBanner({ character }: StoreBannerProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.banner,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.xl,
          padding: theme.space.lg,
          gap: theme.space.md,
        },
      ]}
    >
      <View style={{ flex: 1, gap: theme.space.xs }}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          우리 가족 상점
        </Text>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
          {BANNER_DESCRIPTION[character]}
        </Text>
      </View>
      <Image source={mascotImage(character)} style={styles.mascot} resizeMode="contain" />
    </View>
  );
}

const styles = StyleSheet.create({
  banner: { flexDirection: 'row', alignItems: 'center' },
  mascot: { width: 88, height: 88 },
});
