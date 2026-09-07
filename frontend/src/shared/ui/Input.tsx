import { Ionicons } from '@expo/vector-icons';
import { useState, type ReactNode } from 'react';
import { Pressable, StyleSheet, Text, TextInput, type TextInputProps, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type InputProps = TextInputProps & {
  /** 입력란 위에 표시되는 라벨 */
  label?: string;
  /** 에러 메시지. 있으면 강조색과 메시지가 danger 로 바뀐다 */
  error?: string;
  /**
   * line — 밑줄형 (Figma: 회원가입/계좌). 포커스 시 밑줄·글자가 브랜드 그린.
   * box  — 박스형 (Figma: 로그인). 아이콘 슬롯, 포커스 시 테두리 그린.
   */
  variant?: 'line' | 'box';
  /** box 변형에서 왼쪽 아이콘 슬롯 */
  leftIcon?: ReactNode;
};

export function Input({
  label,
  error,
  variant = 'line',
  leftIcon,
  onFocus,
  onBlur,
  style,
  secureTextEntry,
  ...textInputProps
}: InputProps) {
  const theme = useTheme();
  const [focused, setFocused] = useState(false);
  const [revealed, setRevealed] = useState(false);

  const idle = variant === 'box' ? theme.color.border.default : theme.color.border.strong;
  const accent = error
    ? theme.color.status.danger.solid
    : focused
      ? theme.color.border.focus
      : idle;
  // line 변형: 포커스 시 입력 텍스트도 브랜드 그린 (Figma)
  const lineActive = variant === 'line' && focused && !error;

  const field = (
    <TextInput
      placeholderTextColor={lineActive ? theme.color.brand.solid : theme.color.text.muted}
      {...textInputProps}
      secureTextEntry={secureTextEntry && !revealed}
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
        {
          color: lineActive ? theme.color.brand.solid : theme.color.text.strong,
          paddingVertical: theme.space.sm,
        },
        variant === 'line' && { borderBottomWidth: 2, borderBottomColor: accent },
        variant === 'box' && styles.boxField,
        style,
      ]}
    />
  );

  return (
    <View style={styles.container}>
      {label ? (
        <Text style={[theme.typography.label, { color: theme.color.text.muted }]}>{label}</Text>
      ) : null}

      {variant === 'box' ? (
        <View
          style={[
            styles.box,
            theme.shadow.card,
            {
              minHeight: theme.layout.inputHeight,
              gap: theme.space.sm,
              borderColor: accent,
              borderRadius: theme.radius.lg,
              backgroundColor: theme.color.background.surface,
              paddingHorizontal: theme.space.lg,
            },
          ]}
        >
          {leftIcon ? <View style={styles.leftIcon}>{leftIcon}</View> : null}
          {field}
          {secureTextEntry ? (
            <Pressable
              onPress={() => setRevealed((v) => !v)}
              hitSlop={8}
              accessibilityRole="button"
              accessibilityLabel={revealed ? '비밀번호 숨기기' : '비밀번호 표시'}
            >
              <Ionicons
                name={revealed ? 'eye-outline' : 'eye-off-outline'}
                size={20}
                color={theme.color.text.muted}
              />
            </Pressable>
          ) : null}
        </View>
      ) : (
        field
      )}

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
  box: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
  },
  boxField: {
    flex: 1,
  },
  leftIcon: {
    width: 22,
    alignItems: 'center',
  },
});
