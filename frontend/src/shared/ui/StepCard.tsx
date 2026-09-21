import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type StepCardProps = {
  title: string;
  description: string;
  /** 이미 완료된 단계 (체크 표시, 액션 숨김) */
  done?: boolean;
  /** 지금 할 수 있는 단계 (브랜드색으로 강조). 아니면 흐리게 보인다 */
  enabled: boolean;
  action: ReactNode;
};

/**
 * 온보딩 허브의 단계 카드. 자녀(가족 연결·계좌 연동)와 부모(가족 만들기·아이 초대)가
 * 같은 모양의 안내 화면을 쓰도록 공통화했다. 지금 할 단계는 강조하고 잠긴 단계는 흐리게 그린다.
 */
export function StepCard({ title, description, done = false, enabled, action }: StepCardProps) {
  const theme = useTheme();
  const highlighted = enabled && !done;

  return (
    <View
      style={[
        styles.card,
        {
          borderColor: highlighted ? theme.color.brand.solid : theme.color.border.default,
          backgroundColor: highlighted ? theme.color.brand.tint : theme.color.background.surface,
          borderRadius: theme.radius.lg,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: done || enabled ? 1 : 0.6,
        },
      ]}
    >
      <View style={styles.cardHead}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>{title}</Text>
        {done ? (
          <Ionicons name="checkmark-circle" size={20} color={theme.color.brand.solid} />
        ) : null}
      </View>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>{description}</Text>
      {done ? null : action}
    </View>
  );
}

const styles = StyleSheet.create({
  card: { borderWidth: 1.5 },
  cardHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
