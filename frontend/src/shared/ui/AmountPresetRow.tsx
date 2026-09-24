import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';

import { useTheme } from '@/shared/theme';

export type AmountPreset = {
  /** React key 겸 식별자 */
  key: string;
  label: string;
  onPress: () => void;
};

type AmountPresetRowProps = {
  presets: AmountPreset[];
};

/**
 * 금액 입력 화면(이체·충전·환전·포인트 주기)의 빠른 금액 칩 줄.
 *
 * 가로 스크롤인 이유: 칩 개수·자릿수는 화면마다 다른데(+100,000원 넷이면 폭이 꽤 넓다)
 * 좁은 기기에서는 한 줄에 안 들어가 양 끝 칩이 잘려 보였다. 스크롤이면 어떤 폭에서도
 * 전부 닿을 수 있다. 다 들어가는 기기에서는 flexGrow + center 로 예전처럼 가운데 정렬된다.
 */
export function AmountPresetRow({ presets }: AmountPresetRowProps) {
  const theme = useTheme();

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      // Screen 좌우 여백만큼 밖으로 나갔다가 콘텐츠 패딩으로 되돌린다 —
      // 칩이 화면 가장자리까지 스크롤돼야 중간에 끊긴 것처럼 안 보인다.
      style={{ marginHorizontal: -theme.layout.screenX }}
      contentContainerStyle={[
        styles.content,
        { gap: theme.space.sm, paddingHorizontal: theme.layout.screenX },
      ]}
    >
      {presets.map((preset) => (
        <Pressable
          key={preset.key}
          accessibilityRole="button"
          onPress={preset.onPress}
          style={[
            styles.chip,
            { borderColor: theme.color.border.default, borderRadius: theme.radius.sm },
          ]}
        >
          <Text style={[theme.typography.label, { color: theme.color.text.default }]}>
            {preset.label}
          </Text>
        </Pressable>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  // flexGrow 로 콘텐츠가 짧을 때만 가운데 정렬, 넘치면 그대로 스크롤된다.
  content: { flexGrow: 1, justifyContent: 'center' },
  chip: { borderWidth: 1.5, paddingHorizontal: 14, paddingVertical: 8 },
});
