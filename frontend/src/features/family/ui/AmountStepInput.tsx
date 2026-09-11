import { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { parseAmountInput } from '@/features/family/lib/onboarding';
import { formatWonNumber, formatWonShort } from '@/shared/lib/money';
import { fontFamily, useTheme } from '@/shared/theme';

type Props = {
  /** 현재 금액(원). null 이면 미입력 */
  value: number | null;
  onChange: (won: number | null) => void;
  /** 빠른 선택 프리셋(원) */
  presets: readonly number[];
  /** 큰 숫자 아래 안내 (단위 규칙 등) */
  hint?: string;
  error?: string;
};

/**
 * 큰 숫자 표시 자체가 입력란인 금액 필드 (온보딩 용돈·보상 한도).
 * 테두리 박스를 탭하면 숫자 키패드가 뜨고, 아래 프리셋 칩으로도 채울 수 있다.
 */
export function AmountStepInput({ value, onChange, presets, hint, error }: Props) {
  const theme = useTheme();
  const inputRef = useRef<TextInput>(null);
  const [focused, setFocused] = useState(false);

  const display = value == null ? '' : formatWonNumber(value);
  const accent = error
    ? theme.color.status.danger.solid
    : focused
      ? theme.color.border.focus
      : theme.color.border.default;

  return (
    <View style={{ gap: theme.space['2xl'] }}>
      <Pressable onPress={() => inputRef.current?.focus()}>
        <View
          style={[
            styles.box,
            {
              borderColor: accent,
              borderRadius: theme.radius.lg,
              backgroundColor: theme.color.background.surface,
            },
          ]}
        >
          <TextInput
            ref={inputRef}
            value={display}
            onChangeText={(t) => onChange(parseAmountInput(t))}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            placeholder="0"
            placeholderTextColor={theme.color.text.muted}
            keyboardType="number-pad"
            style={[styles.amount, { color: theme.color.text.strong }]}
          />
          <Text style={[styles.won, { color: theme.color.text.sub }]}>원</Text>
        </View>
      </Pressable>

      <View style={styles.chips}>
        {presets.map((won) => {
          const selected = value === won;
          return (
            <Pressable
              key={won}
              onPress={() => onChange(won)}
              accessibilityRole="button"
              accessibilityState={{ selected }}
              style={[
                styles.chip,
                {
                  borderRadius: theme.radius.md,
                  borderColor: selected ? theme.color.brand.solid : theme.color.border.default,
                  backgroundColor: selected
                    ? theme.color.brand.tint
                    : theme.color.background.surface,
                },
              ]}
            >
              <Text
                style={[
                  theme.typography.body,
                  { color: selected ? theme.color.brand.solid : theme.color.text.default },
                ]}
              >
                {formatWonShort(won)}
              </Text>
            </Pressable>
          );
        })}
      </View>

      {error ? (
        <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
          {error}
        </Text>
      ) : hint ? (
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>{hint}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  box: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    minHeight: 66,
    paddingHorizontal: 18,
    gap: 6,
  },
  amount: {
    flex: 1,
    textAlign: 'right',
    fontFamily: fontFamily.bold,
    fontSize: 28,
    paddingVertical: 0,
  },
  won: {
    fontFamily: fontFamily.medium,
    fontSize: 18,
  },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderWidth: 1.5,
  },
});
