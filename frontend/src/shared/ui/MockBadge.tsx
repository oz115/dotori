import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

/**
 * 지금 보이는 값이 실제 데이터가 아니라 자리를 채운 예시라는 표시.
 *
 * 서버에 아직 아무것도 없을 때(집안일 미등록, 상점 비어있음 등) 화면이 통째로 비면
 * 배치를 볼 수 없어 예시로 채우는데, 실제 값과 구분되지 않으면 곤란해서 함께 붙인다.
 * 실데이터가 들어오면 사라진다.
 */
export function MockBadge() {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: theme.color.background.surfaceSunken,
          borderRadius: theme.radius.full,
        },
      ]}
    >
      <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>예시</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { paddingHorizontal: 8, paddingVertical: 2 },
});
