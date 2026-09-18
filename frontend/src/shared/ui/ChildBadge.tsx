import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

/**
 * 아이 이름 배지. 부모 화면에서 여러 자녀의 항목이 한 목록에 섞여 나오므로 누구 것인지 표시한다.
 *
 * 색은 이름에서 뽑는다 — 서버가 자녀별 색을 주지 않고, 순번으로 정하면 목록 정렬이 바뀔 때
 * 같은 아이의 색이 달라진다. 이름이 같으면 화면이 달라도 늘 같은 색이 나온다.
 */
export function ChildBadge({ name }: { name: string }) {
  const theme = useTheme();
  const tone = [
    { bg: theme.color.brand.subtle, text: theme.color.brand.subtleText },
    { bg: theme.color.status.danger.subtle, text: theme.color.status.danger.text },
    { bg: theme.color.status.info.subtle, text: theme.color.status.info.text },
    { bg: theme.color.status.warning.subtle, text: theme.color.status.warning.text },
  ][hash(name) % 4];

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: tone.bg,
          borderRadius: theme.radius.full,
          paddingHorizontal: theme.space.sm,
        },
      ]}
    >
      <Text numberOfLines={1} style={[theme.typography.caption, { color: tone.text }]}>
        {name}
      </Text>
    </View>
  );
}

function hash(value: string): number {
  let acc = 0;
  for (let i = 0; i < value.length; i += 1) {
    acc = (acc * 31 + value.charCodeAt(i)) % 997;
  }
  return acc;
}

const styles = StyleSheet.create({
  // 로그인 아이디를 이름으로 쓰는 계정이 있어 "insonggim1010" 처럼 길어질 수 있다.
  // 줄이지 않으면 배지가 제목을 밀어내므로, 폭에 상한을 두고 넘치면 말줄임한다.
  badge: { paddingVertical: 2, maxWidth: 110, flexShrink: 1 },
});
