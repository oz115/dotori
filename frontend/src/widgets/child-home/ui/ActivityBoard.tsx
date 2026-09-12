import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { useTheme } from '@/shared/theme';
import { ContentCard } from '@/shared/ui';

/**
 * "활동 보드" 섹션. 경제 퀴즈·단어장은 아직 백엔드 도메인이 없어 정적 안내만 보여준다
 * (탭해도 아무 동작 안 함 — 해당 기능이 생기면 실제 화면으로 연결한다).
 *
 * 홈 화면 마지막 섹션이라 연회색 배경이 화면 좌우 끝까지 깔린다 — 스크롤 콘텐츠의
 * 좌우 여백(screenX)만큼 음수 마진으로 빼고, 같은 값을 안쪽 패딩으로 되돌려 준다.
 */
export function ActivityBoard() {
  const theme = useTheme();

  return (
    <View
      style={{
        gap: theme.space.md,
        backgroundColor: theme.color.background.surfaceSunken,
        marginHorizontal: -theme.layout.screenX,
        paddingHorizontal: theme.layout.screenX,
        paddingTop: theme.space['2xl'],
        paddingBottom: theme.space['4xl'],
        paddingLeft: theme.space['2xl'],
      }}
    >
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>활동 보드</Text>
      <View style={[styles.row, { gap: theme.space.md }]}>
        <Card
          icon={require('@/assets/images/icons/icon-quiz.png')}
          title="오늘의 경제 퀴즈"
          subtitle="맞히면 +50P!"
          highlight
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
  /** 부제 강조 — 보상이 걸린 카드만 초록으로 띄운다 */
  highlight?: boolean;
};

/**
 * 제목·부제는 왼쪽 위, 아이콘은 오른쪽 아래에 걸친다.
 * 아이콘이 absolute 라서 글자가 길어져도 자리가 밀리지 않는다.
 */
function Card({ icon, title, subtitle, highlight }: CardProps) {
  const theme = useTheme();

  return (
    <ContentCard style={[styles.card, { padding: theme.space.xl, gap: theme.space.xs }]}>
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>{title}</Text>
      <Text
        style={[
          theme.typography.bodySm,
          { color: highlight ? theme.color.status.success.text : theme.color.text.muted },
        ]}
      >
        {subtitle}
      </Text>
      <Image source={icon} style={styles.icon} resizeMode="contain" />
    </ContentCard>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  card: { flex: 1, minHeight: 132 },
  icon: { position: 'absolute', right: 14, bottom: 14, width: 48, height: 48 },
});
