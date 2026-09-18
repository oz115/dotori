import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { useTheme } from '@/shared/theme';
import { ContentCard, MockBadge, useTabOverlap } from '@/shared/ui';

/**
 * 부모용 "활동 보드". 자녀 홈의 것과 자리는 같지만 보여주는 내용이 다르다 — 아이는
 * "풀어볼 것"을 안내받고, 부모는 "아이가 어떻게 했는지" 결과를 본다. 퀴즈·단어장 모두
 * 아직 백엔드 도메인이 없어 완전히 예시다.
 */
export function ChildActivityBoard() {
  const theme = useTheme();
  const tabOverlap = useTabOverlap();

  return (
    <View
      style={{
        gap: theme.space.md,
        backgroundColor: theme.color.background.surfaceSunken,
        marginHorizontal: -theme.layout.screenX,
        paddingHorizontal: theme.layout.screenX,
        paddingTop: theme.space['2xl'],
        paddingBottom: theme.space['4xl'] + tabOverlap,
      }}
    >
      <View style={[styles.titleRow, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          활동 보드
        </Text>
        <MockBadge />
      </View>
      <View style={[styles.column, { gap: theme.space.md }]}>
        <ActivityRow
          icon={require('@/assets/images/icons/icon-quiz.png')}
          title="오늘의 경제 퀴즈"
          subtitle="인플레이션"
          trailing={
            <View style={[styles.resultRow, { gap: theme.space.xs }]}>
              <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
                오답
              </Text>
              <View
                style={[
                  styles.resultBadge,
                  { backgroundColor: theme.color.status.danger.solid, borderRadius: 999 },
                ]}
              >
                <Ionicons name="close" size={18} color={theme.color.text.onBrand} />
              </View>
            </View>
          }
        />
        <ActivityRow
          icon={require('@/assets/images/icons/icon-word.png')}
          title="경제 단어장"
          subtitle="오늘의 단어: 중앙은행"
          trailing={<Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />}
        />
      </View>
    </View>
  );
}

type ActivityRowProps = {
  icon: ImageSourcePropType;
  title: string;
  subtitle: string;
  trailing: ReactNode;
};

function ActivityRow({ icon, title, subtitle, trailing }: ActivityRowProps) {
  const theme = useTheme();

  return (
    <ContentCard style={[styles.card, { padding: theme.space.lg, gap: theme.space.md }]}>
      <Image source={icon} style={styles.icon} resizeMode="contain" />
      <View style={{ flex: 1, gap: 2 }}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>{title}</Text>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>{subtitle}</Text>
      </View>
      {trailing}
    </ContentCard>
  );
}

const styles = StyleSheet.create({
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  column: { flexDirection: 'column' },
  card: { flexDirection: 'row', alignItems: 'center' },
  icon: { width: 56, height: 56 },
  resultRow: { flexDirection: 'row', alignItems: 'center' },
  resultBadge: { width: 28, height: 28, alignItems: 'center', justifyContent: 'center' },
});
