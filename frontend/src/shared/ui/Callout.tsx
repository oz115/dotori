import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type CalloutProps = {
  /** 왼쪽 아이콘/이모지 슬롯 */
  icon?: ReactNode;
  title: string;
  description?: string;
  /** 오른쪽 액션 슬롯 (버튼 등) */
  action?: ReactNode;
};

/**
 * 안내 배너 (Figma: 로그인 하단 "처음이신가요?", 약관 상단, 메인 알림).
 * 연한 브랜드 배경 + 아이콘 + 문구 (+ 선택적 우측 액션).
 */
export function Callout({ icon, title, description, action }: CalloutProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: theme.color.brand.tint,
          borderRadius: theme.radius.lg,
          paddingVertical: theme.space.md,
          paddingHorizontal: theme.space.lg,
          gap: theme.space.sm,
        },
      ]}
    >
      {icon ? <View style={styles.icon}>{icon}</View> : null}
      <View style={styles.body}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>{title}</Text>
        {description ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            {description}
          </Text>
        ) : null}
      </View>
      {action ? <View style={styles.action}>{action}</View> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  icon: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  body: {
    flex: 1,
    gap: 2,
  },
  action: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});
