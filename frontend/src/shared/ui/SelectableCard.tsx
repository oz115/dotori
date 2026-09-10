import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, View, type ViewStyle } from 'react-native';

import { useTheme } from '@/shared/theme';

type SelectableCardProps = {
  selected: boolean;
  onPress: () => void;
  children: ReactNode;
  /** 선택 시 우상단 체크 배지 (Figma: 은행 선택). 기본 false */
  showCheck?: boolean;
  disabled?: boolean;
  style?: ViewStyle;
};

/**
 * 선택 가능한 카드 (Figma: 역할 선택, 은행 선택).
 * 선택 시 브랜드 그린 테두리 + 연한 그린 배경.
 */
export function SelectableCard({
  selected,
  onPress,
  children,
  showCheck = false,
  disabled = false,
  style,
}: SelectableCardProps) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="radio"
      accessibilityState={{ selected, disabled }}
      style={[
        styles.card,
        style,
        {
          borderRadius: theme.radius.lg,
          padding: theme.space.lg,
          borderColor: selected ? theme.color.brand.solid : theme.color.border.default,
          backgroundColor: selected ? theme.color.brand.tint : theme.color.background.surfaceSunken,
        },
      ]}
    >
      {children}
      {showCheck && selected ? (
        <View style={[styles.badge, { backgroundColor: theme.color.brand.solid }]}>
          <Ionicons name="checkmark" size={14} color={theme.color.text.onBrand} />
        </View>
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1.5,
  },
  badge: {
    position: 'absolute',
    top: -8,
    right: -8,
    width: 24,
    height: 24,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
