import { Pressable, StyleSheet, Text, type PressableProps } from 'react-native';

import { useTheme } from '@/shared/theme';

type ButtonVariant = 'primary' | 'secondary';

type ButtonProps = PressableProps & {
  label: string;
  variant?: ButtonVariant;
};

/**
 * shared/ui 의 가장 기본적인 버튼.
 * 색·간격·모서리·타이포는 전부 shared/theme 토큰에서 가져온다.
 * 모든 레이어(features/widgets)에서 공용으로 가져다 쓴다.
 */
export function Button({ label, variant = 'primary', style, ...pressableProps }: ButtonProps) {
  const theme = useTheme();
  const isPrimary = variant === 'primary';

  return (
    <Pressable
      {...pressableProps}
      style={(state) => [
        styles.container,
        {
          borderRadius: theme.radius.md,
          paddingHorizontal: theme.space.lg,
          paddingVertical: theme.space.md,
        },
        {
          backgroundColor: isPrimary
            ? state.pressed
              ? theme.color.brand.solidPressed
              : theme.color.brand.solid
            : state.pressed
              ? theme.color.background.surfaceSunken
              : theme.palette.neutral[100],
        },
        typeof style === 'function' ? style(state) : style,
      ]}
    >
      <Text
        style={[
          theme.typography.button,
          { color: isPrimary ? theme.color.text.onBrand : theme.color.text.default },
        ]}
      >
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});
