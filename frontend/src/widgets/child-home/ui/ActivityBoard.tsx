import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { useTheme } from '@/shared/theme';

/**
 * "활동 보드" 섹션. 경제 퀴즈·단어장은 아직 백엔드 도메인이 없어 정적 안내만 보여준다
 * (탭해도 아무 동작 안 함 — 해당 기능이 생기면 실제 화면으로 연결한다).
 */
export function ActivityBoard() {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>활동 보드</Text>
      <View style={[styles.row, { gap: theme.space.md }]}>
        <Card
          icon={require('@/assets/images/icons/icon-quiz.png')}
          title="오늘의 경제 퀴즈"
          subtitle="맞히면 +50P!"
        />
        <Card
          icon={require('@/assets/images/icons/icon-word.png')}
          title="경제 단어장"
          subtitle="오늘의 단어: ???"
        />
      </View>
    </View>
  );
}

type CardProps = {
  icon: ImageSourcePropType;
  title: string;
  subtitle: string;
};

function Card({ icon, title, subtitle }: CardProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.card,
        {
          borderRadius: theme.radius.lg,
          borderColor: theme.color.border.default,
          padding: theme.space.md,
          gap: theme.space.sm,
        },
      ]}
    >
      <Image source={icon} style={styles.icon} resizeMode="contain" />
      <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>{title}</Text>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>{subtitle}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  card: { flex: 1, borderWidth: 1 },
  icon: { width: 40, height: 40 },
});
