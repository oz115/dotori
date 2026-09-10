import { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { fontFamily, useTheme } from '@/shared/theme';

type Props = {
  value: string;
  onChangeText: (v: string) => void;
  /** 칸 수 (기본 6) */
  length?: number;
  /** 마지막 칸까지 채워지면 호출 */
  onFilled?: () => void;
  autoFocus?: boolean;
  error?: boolean;
};

/**
 * OTP 스타일 코드 입력 — 칸을 나눠 보여주지만 실제 입력은 투명 TextInput 하나가 받는다.
 * 초대 코드(영문 대문자 + 숫자)를 받는다.
 */
export function CodeInput({ value, onChangeText, length = 6, onFilled, autoFocus, error }: Props) {
  const theme = useTheme();
  const inputRef = useRef<TextInput>(null);
  const [focused, setFocused] = useState(false);

  const cells = Array.from({ length }, (_, i) => value[i] ?? '');

  const handleChange = (raw: string) => {
    const next = raw
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, '')
      .slice(0, length);
    onChangeText(next);
    if (next.length === length) {
      inputRef.current?.blur();
      onFilled?.();
    }
  };

  return (
    <Pressable style={styles.row} onPress={() => inputRef.current?.focus()}>
      {cells.map((ch, i) => {
        const active = focused && i === Math.min(value.length, length - 1);
        const on = error
          ? theme.color.status.danger.solid
          : ch !== '' || active
            ? theme.color.brand.solid
            : theme.color.border.default;
        return (
          <View
            key={i}
            style={[
              styles.cell,
              {
                borderRadius: theme.radius.md,
                borderColor: on,
                borderWidth: active ? 2 : 1.5,
                backgroundColor: theme.color.background.surface,
              },
            ]}
          >
            <Text style={[styles.char, { color: theme.color.text.strong }]}>{ch}</Text>
          </View>
        );
      })}
      <TextInput
        ref={inputRef}
        value={value}
        onChangeText={handleChange}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        autoFocus={autoFocus}
        keyboardType="visible-password"
        autoCapitalize="characters"
        autoCorrect={false}
        maxLength={length}
        caretHidden
        contextMenuHidden
        style={styles.hiddenInput}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 8 },
  cell: {
    flex: 1,
    maxWidth: 56,
    height: 58,
    alignItems: 'center',
    justifyContent: 'center',
  },
  char: { fontFamily: fontFamily.bold, fontSize: 24 },
  hiddenInput: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    opacity: 0,
  },
});
