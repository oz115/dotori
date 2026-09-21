import { Image, Pressable, StyleSheet, Text, type ImageSourcePropType } from 'react-native';

import { useTheme } from '@/shared/theme';

type Props = {
  title: string;
  subtitle: string;
  icon: ImageSourcePropType;
  /** 카드 배경 — 섹션마다 다른 옅은 색을 줘서 한눈에 구분한다 */
  tint: string;
  onPress: () => void;
};

/**
 * 부모 투자 탭에서 하위 페이지로 들어가는 색 카드. 홈의 활동 보드 카드처럼
 * 제목·부제는 왼쪽 위, 아이콘은 오른쪽 아래에 걸친다.
 * 아이콘이 absolute 라서 글자가 길어져도 자리가 밀리지 않는다.
 */
export function InvestmentMenuCard({ title, subtitle, icon, tint, onPress }: Props) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${title}, ${subtitle}`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        {
          backgroundColor: tint,
          borderRadius: theme.radius.lg,
          padding: theme.space.xl,
          gap: theme.space.xs,
          opacity: pressed ? 0.85 : 1,
        },
      ]}
    >
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>{title}</Text>
      <Text style={[theme.typography.bodySm, styles.subtitle, { color: theme.color.text.sub }]}>
        {subtitle}
      </Text>
      <Image source={icon} style={styles.icon} resizeMode="contain" />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { flex: 1, minHeight: 132 },
  // 오른쪽 아래 아이콘과 겹치지 않게 글자 폭을 조금 줄여 둔다
  subtitle: { paddingRight: 44 },
  icon: { position: 'absolute', right: 14, bottom: 14, width: 56, height: 56 },
});
