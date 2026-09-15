import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';

import type { StoreProductCategory } from '@/features/store/model/types';
import { useTheme } from '@/shared/theme';

export type CategoryFilterKey = StoreProductCategory | 'ALL';

const FILTERS: { key: CategoryFilterKey; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'GAME', label: 'GAME' },
  { key: 'SNACK', label: 'SNACK' },
  { key: 'TOY', label: 'TOY' },
  { key: 'OUTING', label: 'OUTING' },
  { key: 'PRIVILEGE', label: 'PRIVILEGE' },
  { key: 'ETC', label: 'ETC' },
];

type CategoryFilterBarProps = {
  selected: CategoryFilterKey;
  onSelect: (key: CategoryFilterKey) => void;
};

/** 상점 카테고리 필터 칩. 목록은 이미 다 받아온 상태라 선택은 클라이언트 사이드 필터링이다. */
export function CategoryFilterBar({ selected, onSelect }: CategoryFilterBarProps) {
  const theme = useTheme();

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: theme.space.sm }}
    >
      {FILTERS.map((filter) => {
        const active = filter.key === selected;
        return (
          <Pressable
            key={filter.key}
            accessibilityRole="button"
            accessibilityState={{ selected: active }}
            onPress={() => onSelect(filter.key)}
            style={[
              styles.chip,
              {
                borderRadius: theme.radius.full,
                backgroundColor: active
                  ? theme.color.brand.solid
                  : theme.color.background.surfaceSunken,
              },
            ]}
          >
            <Text
              style={[
                theme.typography.label,
                { color: active ? theme.color.text.onBrand : theme.color.text.default },
              ]}
            >
              {filter.label}
            </Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  chip: { paddingHorizontal: 16, paddingVertical: 8 },
});
