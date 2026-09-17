import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';

import { useTheme } from '@/shared/theme';

export type FilterOption<T extends string> = { key: T; label: string };

type ChoreFilterChipsProps<T extends string> = {
  options: FilterOption<T>[];
  /** null 이면 '전체'가 선택된 상태 */
  selected: T | null;
  onSelect: (key: T | null) => void;
  /** '전체' 칩 문구. 기본 '전체' */
  allLabel?: string;
};

/**
 * 가로 스크롤 필터 칩. 할 일 목록의 반복 주기(한번·매일·매주·매월)와 부모 화면의
 * 자녀(민준·서윤) 필터가 같은 모양이라 하나로 쓴다.
 *
 * '전체'는 옵션 배열에 넣지 않고 여기서 붙인다 — 부르는 쪽이 매번 sentinel 값을
 * 만들지 않아도 되고, 선택 없음을 null 하나로 표현할 수 있다.
 *
 * 상점에도 비슷한 칩(CategoryFilterBar)이 있지만 색이 다르다(연회색 배경 + 검은 글씨).
 * 시안이 갈라져 있어 합치지 않았다 — 디자인이 정리되면 그때 shared 로 올린다.
 */
export function ChoreFilterChips<T extends string>({
  options,
  selected,
  onSelect,
  allLabel = '전체',
}: ChoreFilterChipsProps<T>) {
  const theme = useTheme();
  const items: { key: T | null; label: string }[] = [{ key: null, label: allLabel }, ...options];

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: theme.space.sm }}
    >
      {items.map((item) => {
        const active = item.key === selected;
        return (
          <Pressable
            key={item.key ?? '__all__'}
            accessibilityRole="button"
            accessibilityState={{ selected: active }}
            onPress={() => onSelect(item.key)}
            style={[
              styles.chip,
              {
                borderRadius: theme.radius.full,
                backgroundColor: active ? theme.color.brand.solid : theme.color.chip.idle,
              },
            ]}
          >
            <Text
              style={[
                theme.typography.label,
                { color: active ? theme.color.text.onBrand : theme.color.chip.idleText },
              ]}
            >
              {item.label}
            </Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  chip: { paddingHorizontal: 18, paddingVertical: 9 },
});
