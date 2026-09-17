import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';

import { categoryLabel } from '@/features/word/lib/categoryLabel';
import type { WordCategory } from '@/features/word/model/types';
import { useTheme } from '@/shared/theme';

export type WordCategoryFilterKey = WordCategory | 'ALL';

const FILTERS: { key: WordCategoryFilterKey; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'INCOME', label: categoryLabel('INCOME') },
  { key: 'CONSUMPTION', label: categoryLabel('CONSUMPTION') },
  { key: 'SAVINGS', label: categoryLabel('SAVINGS') },
  { key: 'INVESTMENT', label: categoryLabel('INVESTMENT') },
  { key: 'BANKING', label: categoryLabel('BANKING') },
  { key: 'ECONOMY', label: categoryLabel('ECONOMY') },
  { key: 'OTHER', label: categoryLabel('OTHER') },
];

type CategoryFilterBarProps = {
  selected: WordCategoryFilterKey;
  onSelect: (key: WordCategoryFilterKey) => void;
  /**
   * 검색 중일 때 넘긴다. 검색 API는 카테고리를 모르므로(파라미터도 응답도 없음) 검색어가
   * 있는 동안엔 필터를 눌러도 반영할 방법이 없다 — 대신 흐리게 보여주고 눌리지 않게 한다.
   */
  disabled?: boolean;
};

/** 단어장 카테고리 필터 칩. `features/store`의 CategoryFilterBar와 같은 모양을 그대로 따랐다. */
export function CategoryFilterBar({
  selected,
  onSelect,
  disabled = false,
}: CategoryFilterBarProps) {
  const theme = useTheme();

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: theme.space.sm }}
      style={{ opacity: disabled ? 0.4 : 1 }}
      pointerEvents={disabled ? 'none' : 'auto'}
    >
      {FILTERS.map((filter) => {
        const active = filter.key === selected;
        return (
          <Pressable
            key={filter.key}
            disabled={disabled}
            accessibilityRole="button"
            accessibilityState={{ selected: active, disabled }}
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
