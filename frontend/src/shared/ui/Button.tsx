import { Pressable, StyleSheet, Text, type PressableProps, useColorScheme } from 'react-native';

type ButtonVariant = 'primary' | 'secondary';

type ButtonProps = PressableProps & {
  label: string;
  variant?: ButtonVariant;
};

/**
 * shared/ui 의 가장 기본적인 버튼 예시.
 * 모든 레이어(entities/features/widgets/pages)에서 공용으로 가져다 씁니다.
 */
export function Button({ label, variant = 'primary', style, ...pressableProps }: ButtonProps) {
  const isDark = useColorScheme() === 'dark';

  return (
    <Pressable
      {...pressableProps}
      style={(state) => [
        styles.container,
        variant === 'primary' ? styles.primary : isDark ? styles.secondaryDark : styles.secondary,
        state.pressed &&
          (variant === 'primary'
            ? styles.primaryPressed
            : isDark
              ? styles.secondaryPressedDark
              : styles.secondaryPressed),
        typeof style === 'function' ? style(state) : style,
      ]}
    >
      <Text
        style={[
          styles.label,
          variant === 'primary' || isDark ? styles.labelLight : styles.labelDark,
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
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  primary: {
    backgroundColor: '#2563eb',
  },
  primaryPressed: {
    backgroundColor: '#1d4ed8',
  },
  secondary: {
    backgroundColor: '#e5e7eb',
  },
  secondaryPressed: {
    backgroundColor: '#d1d5db',
  },
  secondaryDark: {
    backgroundColor: '#374151',
  },
  secondaryPressedDark: {
    backgroundColor: '#4b5563',
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
  },
  labelLight: {
    color: '#ffffff',
  },
  labelDark: {
    color: '#111827',
  },
});
