import { Pressable, StyleSheet, Text } from 'react-native';

import { useTheme } from '@/shared/theme';

type HeaderChipButtonProps = {
  label: string;
  onPress: () => void;
  /** 스크린 리더용 라벨. 생략하면 label을 그대로 쓴다 */
  accessibilityLabel?: string;
};

/**
 * 헤더 우측에 붙는 알약(pill) 모양 보조 버튼 (Figma: "구매 내역"/"완료 이력"/"거래 내역").
 * `Header`의 `right` 슬롯에 넣어 쓴다 — 밑줄 없는 민짜 텍스트인 `action` prop과 달리
 * 배경이 있는 칩이라 화면들 사이에서 톤이 같아야 한다.
 */
export function HeaderChipButton({ label, onPress, accessibilityLabel }: HeaderChipButtonProps) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? label}
      onPress={onPress}
      hitSlop={8}
      style={[
        styles.chip,
        {
          borderRadius: theme.radius.full,
          backgroundColor: theme.color.background.surfaceSunken,
        },
      ]}
    >
      <Text
        numberOfLines={1}
        style={[theme.typography.caption, { color: theme.color.text.default }]}
      >
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  chip: { paddingHorizontal: 12, paddingVertical: 6 },
});
