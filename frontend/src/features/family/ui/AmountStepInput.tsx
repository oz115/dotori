import { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import {
  addAmount,
  AMOUNT_ADD_STEPS_WON,
  MAX_AMOUNT_WON,
  parseAmountInput,
} from '@/features/family/lib/onboarding';
import { formatWon, formatWonNumber, formatWonShort } from '@/shared/lib/money';
import { fontFamily, useTheme } from '@/shared/theme';

type Props = {
  /** 현재 금액(원). null 이면 미입력 */
  value: number | null;
  onChange: (won: number | null) => void;
  /** 큰 숫자 아래 안내 (단위 규칙 등) */
  hint?: string;
  error?: string;
};

/**
 * 큰 숫자 표시 자체가 입력란인 금액 필드 (온보딩 용돈·보상 한도).
 * 테두리 박스를 탭하면 숫자 키패드가 뜨고, 아래 "+5천 원" 같은 버튼을 누르면 그만큼 더해진다.
 * 프리셋을 "선택"하는 방식이 아니라 "더하는" 방식이라 선택 표시가 없다.
 * 상한(MAX_AMOUNT_WON)을 넘는 입력은 상한으로 자르고 안내한다.
 */
export function AmountStepInput({ value, onChange, hint, error }: Props) {
  const theme = useTheme();
  const inputRef = useRef<TextInput>(null);
  const [focused, setFocused] = useState(false);
  const [capped, setCapped] = useState(false);

  const display = value == null ? '' : formatWonNumber(value);
  const capMessage = `최대 ${formatWon(MAX_AMOUNT_WON)}까지 입력할 수 있어요.`;
  const shownError = error ?? (capped ? capMessage : undefined);
  const accent = shownError
    ? theme.color.status.danger.solid
    : focused
      ? theme.color.border.focus
      : theme.color.border.default;

  const onChangeText = (text: string) => {
    const parsed = parseAmountInput(text);
    // 상한으로 잘린 입력이면 그 사실을 알린다
    setCapped(Number(text.replace(/[^0-9]/g, '')) > MAX_AMOUNT_WON);
    onChange(parsed);
  };

  const add = (step: number) => {
    setCapped(false);
    onChange(addAmount(value, step));
  };

  const clear = () => {
    setCapped(false);
    onChange(null);
  };

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
            onChangeText={onChangeText}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            placeholder="0"
            placeholderTextColor={theme.color.text.muted}
            keyboardType="number-pad"
            style={[
              styles.amount,
              { color: theme.color.text.strong, fontSize: 28 * theme.textScale },
            ]}
          />
          <Text
            style={[styles.won, { color: theme.color.text.sub, fontSize: 18 * theme.textScale }]}
          >
            원
          </Text>
        </View>
      </Pressable>

      <View style={styles.chips}>
        {AMOUNT_ADD_STEPS_WON.map((step) => (
          <Pressable
            key={step}
            onPress={() => add(step)}
            accessibilityRole="button"
            accessibilityLabel={`${formatWon(step)} 더하기`}
            style={[
              styles.chip,
              {
                borderRadius: theme.radius.md,
                borderColor: theme.color.border.default,
                backgroundColor: theme.color.background.surface,
              },
            ]}
          >
            <Text
              numberOfLines={1}
              style={[theme.typography.body, { color: theme.color.text.default }]}
            >
              +{formatWonShort(step)}
            </Text>
          </Pressable>
        ))}
        <Pressable
          onPress={clear}
          disabled={value == null}
          accessibilityRole="button"
          accessibilityLabel="금액 지우기"
          style={[
            styles.chip,
            {
              borderRadius: theme.radius.md,
              borderColor: theme.color.border.default,
              backgroundColor: theme.color.background.surface,
              opacity: value == null ? 0.4 : 1,
            },
          ]}
        >
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>지우기</Text>
        </Pressable>
      </View>

      {shownError ? (
        <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
          {shownError}
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
    // 크기는 쓰는 쪽에서 기기 글꼴 배율을 상쇄해 넣는다 (shared/theme/fontScale.ts)
    fontFamily: fontFamily.bold,
    paddingVertical: 0,
  },
  won: {
    fontFamily: fontFamily.medium,
  },
  // 버튼은 한 줄에 균등하게 나눠 담는다 (줄바꿈 없음)
  chips: { flexDirection: 'row', gap: 8 },
  chip: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 4,
    borderWidth: 1.5,
  },
});
