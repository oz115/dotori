import { ActivityIndicator, Pressable, StyleSheet, Text, type PressableProps } from 'react-native';

import { useTheme } from '@/shared/theme';

type ButtonVariant = 'primary' | 'secondary';

type ButtonProps = PressableProps & {
  label: string;
  variant?: ButtonVariant;
  loading?: boolean;
};

/**
 * 기본 CTA 버튼. 색·간격·모서리·타이포는 전부 shared/theme 토큰에서 가져온다.
 * 화면에서는 부모 컨테이너 너비를 채우도록 배치한다 (Figma: 풀폭 버튼).
 */
export function Button({
  label,
  variant = 'primary',
  loading = false,
  disabled,
  style,
  ...pressableProps
}: ButtonProps) {
  const theme = useTheme();
  const isPrimary = variant === 'primary';
  const isDisabled = disabled || loading;

  const background = (pressed: boolean) => {
    if (isDisabled) {
      return isPrimary ? theme.color.text.muted : theme.color.background.surfaceSunken;
    }
    if (isPrimary) {
      return pressed ? theme.color.brand.solidPressed : theme.color.brand.solid;
    }
    return pressed ? theme.color.background.surfaceSunken : theme.palette.neutral[100];
  };

  const labelColor = isPrimary ? theme.color.text.onBrand : theme.color.text.default;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      disabled={isDisabled}
      {...pressableProps}
      style={(state) => [
        styles.container,
        {
          borderRadius: theme.radius.md,
          paddingHorizontal: theme.space.lg,
          backgroundColor: background(state.pressed),
        },
        typeof style === 'function' ? style(state) : style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={labelColor} />
      ) : (
        <Text style={[theme.typography.button, { color: labelColor }]}>{label}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    minHeight: 52,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
