import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { CheckCircleIcon } from '@/shared/ui/icons';

type CheckboxProps = {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  /**
   * square — 약관 동의 목록 (빈 사각 → 그린 채움 + 흰 체크)
   * circle — 로그인 "아이디 저장" (원형 체크 글리프, 회색 ↔ 그린)
   */
  shape?: 'square' | 'circle';
  disabled?: boolean;
};

const SIZE = 22;

/** 체크박스. 체크 시 브랜드 그린. */
export function Checkbox({
  checked,
  onChange,
  label,
  shape = 'square',
  disabled = false,
}: CheckboxProps) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={() => onChange(!checked)}
      disabled={disabled}
      accessibilityRole="checkbox"
      accessibilityState={{ checked, disabled }}
      accessibilityLabel={label}
      style={styles.row}
      hitSlop={6}
    >
      {shape === 'circle' ? (
        <CheckCircleIcon
          size={SIZE}
          color={checked ? theme.color.brand.solid : theme.color.text.muted}
        />
      ) : (
        <View
          style={[
            styles.square,
            {
              borderRadius: theme.radius.sm,
              backgroundColor: checked ? theme.color.brand.solid : 'transparent',
              borderColor: checked ? theme.color.brand.solid : theme.color.border.strong,
            },
          ]}
        >
          {checked ? (
            <Ionicons name="checkmark" size={15} color={theme.color.text.onBrand} />
          ) : null}
        </View>
      )}
      {label ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.text.default }]}>{label}</Text>
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  square: {
    width: SIZE,
    height: SIZE,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
