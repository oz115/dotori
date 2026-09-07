import { useState } from 'react';
import { StyleSheet, Text, TextInput, type TextInputProps, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type InputProps = TextInputProps & {
  /** 입력란 위에 표시되는 라벨 */
  label?: string;
  /** 에러 메시지. 있으면 밑줄과 메시지가 danger 색으로 바뀐다 */
  error?: string;
};

/**
 * 밑줄형 텍스트 입력 (Figma: border-bottom 2px, 포커스 시 브랜드 그린).
 * 상태: 기본(muted 밑줄) → 포커스(brand 밑줄) → 에러(danger 밑줄 + 메시지).
 */
export function Input({ label, error, onFocus, onBlur, style, ...textInputProps }: InputProps) {
  const theme = useTheme();
  const [focused, setFocused] = useState(false);

  const underline = error
    ? theme.color.status.danger.solid
    : focused
      ? theme.color.border.focus
      : theme.color.border.strong;

  return (
    <View style={styles.container}>
      {label ? (
        <Text style={[theme.typography.label, { color: theme.color.text.muted }]}>{label}</Text>
      ) : null}
      <TextInput
        placeholderTextColor={theme.color.text.muted}
        {...textInputProps}
        onFocus={(e) => {
          setFocused(true);
          onFocus?.(e);
        }}
        onBlur={(e) => {
          setFocused(false);
          onBlur?.(e);
        }}
        style={[
          theme.typography.body,
          styles.input,
          {
            color: theme.color.text.strong,
            borderBottomColor: underline,
            paddingVertical: theme.space.sm,
          },
          style,
        ]}
      />
      {error ? (
        <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
          {error}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 6,
  },
  input: {
    borderBottomWidth: 2,
  },
});
