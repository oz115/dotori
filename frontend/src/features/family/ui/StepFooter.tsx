import { router } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type Props = {
  /** 주 버튼 (다음 / 설정 완료 등) */
  primaryLabel: string;
  onPrimary: () => void;
  primaryDisabled?: boolean;
  primaryLoading?: boolean;
  /** true 면 왼쪽에 "이전" 버튼을 붙인다 (기본 router.back) */
  showBack?: boolean;
  onBack?: () => void;
};

/**
 * 온보딩 스텝 하단 버튼 영역. "이전"(선택) + 주 버튼을 한 줄에 놓는다.
 * 주 버튼이 항상 더 넓다.
 */
export function StepFooter({
  primaryLabel,
  onPrimary,
  primaryDisabled,
  primaryLoading,
  showBack = false,
  onBack,
}: Props) {
  const theme = useTheme();

  if (!showBack) {
    return (
      <Button
        label={primaryLabel}
        variant="primary"
        onPress={onPrimary}
        disabled={primaryDisabled}
        loading={primaryLoading}
      />
    );
  }

  return (
    <View style={[styles.row, { gap: theme.space.sm }]}>
      <Button
        label="이전"
        variant="ghost"
        onPress={onBack ?? (() => router.back())}
        style={styles.back}
      />
      <Button
        label={primaryLabel}
        variant="primary"
        onPress={onPrimary}
        disabled={primaryDisabled}
        loading={primaryLoading}
        style={styles.primary}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  back: { flex: 1 },
  primary: { flex: 2 },
});
