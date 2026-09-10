import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { parseAmountInput } from '@/features/family/lib/onboarding';
import { formatWon } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { Input } from '@/shared/ui';

type Props = {
  label: string;
  /** 프리셋 금액(원) */
  presets: readonly number[];
  /** 현재 선택된 금액(원). 프리셋에 없으면 직접 입력으로 간주. */
  value: number | null;
  onChange: (won: number | null) => void;
  /** 도움말/설명 (금액 단위 안내 등) */
  hint?: string;
  error?: string;
};

/**
 * 프리셋 칩 + 직접 입력을 함께 제공하는 금액 선택 필드 (온보딩 용돈·보상 한도).
 * 프리셋을 누르면 직접 입력값은 비우고, 직접 입력하면 프리셋 선택이 풀린다.
 */
export function AmountPresetField({ label, presets, value, onChange, hint, error }: Props) {
  const theme = useTheme();
  const [custom, setCustom] = useState(() =>
    value != null && !presets.includes(value) ? String(value) : '',
  );

  const onPreset = (won: number) => {
    setCustom('');
    onChange(won);
  };

  const onCustom = (text: string) => {
    setCustom(text);
    onChange(parseAmountInput(text));
  };

  const customActive = custom.trim() !== '';

  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={[theme.typography.label, { color: theme.color.text.label }]}>{label}</Text>

      <View style={styles.chips}>
        {presets.map((won) => {
          const selected = !customActive && value === won;
          return (
            <Pressable
              key={won}
              onPress={() => onPreset(won)}
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
                {formatWon(won)}
              </Text>
            </Pressable>
          );
        })}
      </View>

      <Input
        variant="box"
        placeholder="직접 입력 (원)"
        keyboardType="number-pad"
        value={custom}
        onChangeText={onCustom}
        error={error}
      />
      {hint && !error ? (
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>{hint}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderWidth: 1.5,
  },
});
