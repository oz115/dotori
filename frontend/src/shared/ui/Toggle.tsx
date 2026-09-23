import { StyleSheet, Switch, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type ToggleProps = {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  disabled?: boolean;
};

/**
 * 켜고 끄는 필터용 토글 스위치 ("미완료만 보기", "보유한 것만 보기" 등 — 미션·저축·투자
 * 목록이 함께 쓴다). 체크박스(Checkbox)와 반대로 라벨이 왼쪽, 스위치가 오른쪽에 온다.
 *
 * 스위치 자체가 이미 터치 가능한 네이티브 컨트롤이라 바깥을 Pressable 로 한 번 더 감싸지
 * 않는다 — 감싸면 스위치를 직접 눌렀을 때 onValueChange 와 바깥 onPress 가 함께 불려
 * 두 번 토글될 수 있다.
 */
export function Toggle({ checked, onChange, label, disabled = false }: ToggleProps) {
  const theme = useTheme();

  return (
    <View style={styles.row}>
      {label ? (
        <Text
          style={[
            theme.typography.bodySm,
            { color: disabled ? theme.color.text.muted : theme.color.text.default },
          ]}
        >
          {label}
        </Text>
      ) : null}
      <Switch
        value={checked}
        onValueChange={onChange}
        disabled={disabled}
        accessibilityLabel={label}
        trackColor={{ false: theme.color.border.strong, true: theme.color.brand.solid }}
        thumbColor={theme.color.background.surface}
        ios_backgroundColor={theme.color.border.strong}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
});
