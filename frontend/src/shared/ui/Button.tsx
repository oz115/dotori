import { LinearGradient } from 'expo-linear-gradient';
import type { ReactNode } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, type PressableProps } from 'react-native';

import { useTheme } from '@/shared/theme';

type ButtonVariant = 'primary' | 'gradient' | 'secondary' | 'outline';
type ButtonSize = 'md' | 'sm';

type ButtonProps = PressableProps & {
  label: string;
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  /** 라벨 오른쪽 아이콘 (예: 회원가입 › 의 chevron) */
  trailingIcon?: ReactNode;
};

/**
 * CTA 버튼. 색·간격·모서리·타이포는 전부 shared/theme 토큰에서.
 * gradient/primary 는 보통 풀폭, outline·size="sm" 은 인라인 액션용.
 */
export function Button({
  label,
  variant = 'primary',
  size = 'md',
  loading = false,
  trailingIcon,
  disabled,
  style,
  ...pressableProps
}: ButtonProps) {
  const theme = useTheme();
  const isDisabled = disabled || loading;
  const sm = size === 'sm';

  const surface = (pressed: boolean) => {
    if (variant === 'gradient') return 'transparent';
    if (variant === 'primary') {
      return pressed ? theme.color.brand.solidPressed : theme.color.brand.solid;
    }
    if (variant === 'outline') {
      return pressed ? theme.color.brand.subtle : theme.color.background.surface;
    }
    return pressed ? theme.color.background.surfaceSunken : theme.palette.neutral[100];
  };

  const contentColor =
    variant === 'primary' || variant === 'gradient'
      ? theme.color.text.onBrand
      : variant === 'outline'
        ? theme.color.brand.solid
        : theme.color.text.default;

  const content = loading ? (
    <ActivityIndicator color={contentColor} />
  ) : (
    <>
      <Text
        style={[sm ? theme.typography.label : theme.typography.button, { color: contentColor }]}
      >
        {label}
      </Text>
      {trailingIcon}
    </>
  );

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      disabled={isDisabled}
      {...pressableProps}
      style={(state) => [
        styles.container,
        {
          minHeight: sm ? 36 : 52,
          borderRadius: sm ? theme.radius.sm : theme.radius.md,
          paddingHorizontal: sm ? theme.space.md : theme.space.lg,
          backgroundColor: surface(state.pressed),
          borderWidth: variant === 'outline' ? 1.5 : 0,
          borderColor: theme.color.brand.solid,
          opacity: isDisabled ? 0.5 : state.pressed && variant === 'gradient' ? 0.92 : 1,
        },
        typeof style === 'function' ? style(state) : style,
      ]}
    >
      {variant === 'gradient' ? (
        <LinearGradient
          colors={theme.palette.gradient.deep}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={StyleSheet.absoluteFill}
        />
      ) : null}
      {content}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    overflow: 'hidden',
  },
});
