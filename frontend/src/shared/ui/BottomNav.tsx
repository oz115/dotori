import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useTheme } from '@/shared/theme';
import { navIcons, type NavIconName } from '@/shared/ui/icons';

type NavItem = {
  key: NavIconName;
  label: string;
};

type BottomNavProps = {
  items: NavItem[];
  activeKey: NavIconName;
  onSelect: (key: NavIconName) => void;
  /** false 면 아이콘만 (Figma 네비게이션2). 기본 true (네비게이션1). */
  showLabels?: boolean;
};

const ICON_SIZE = 26;

/**
 * 하단 탭 네비게이션 (Figma: 네비게이션1/2). 흰 배경 + 위쪽만 둥근 모서리,
 * 활성 탭은 진한 아이콘, showLabels 일 때 라벨은 브랜드 그린.
 * expo-router 연결은 app 레이어에서 <Tabs tabBar={...}> 로 감싼다.
 */
export function BottomNav({ items, activeKey, onSelect, showLabels = true }: BottomNavProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <View
      style={[
        styles.container,
        {
          paddingBottom: insets.bottom || theme.space.sm,
          backgroundColor: theme.color.background.surface,
          borderTopLeftRadius: theme.radius['2xl'],
          borderTopRightRadius: theme.radius['2xl'],
        },
      ]}
    >
      {items.map((item) => {
        const active = item.key === activeKey;
        const Icon = navIcons[item.key];
        const tint = active ? theme.color.text.strong : theme.color.border.default;

        return (
          <Pressable
            key={item.key}
            onPress={() => onSelect(item.key)}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            accessibilityLabel={item.label}
            style={styles.item}
          >
            <Icon size={ICON_SIZE} color={tint} />
            {showLabels ? (
              <Text
                style={[
                  theme.typography.caption,
                  { color: active ? theme.color.brand.solid : theme.color.text.muted },
                ]}
              >
                {item.label}
              </Text>
            ) : null}
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    paddingTop: 12,
    paddingHorizontal: 8,
  },
  item: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    paddingVertical: 4,
  },
});
