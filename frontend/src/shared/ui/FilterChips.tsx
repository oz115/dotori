import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';

import { useTheme } from '@/shared/theme';

export type FilterOption<T extends string> = { key: T; label: string };

type FilterChipsProps<T extends string> = {
  options: FilterOption<T>[];
  /** null 이면 '전체'가 선택된 상태 */
  selected: T | null;
  onSelect: (key: T | null) => void;
  /** '전체' 칩 문구. 기본 '전체' */
  allLabel?: string;
  /** false 면 '전체' 칩을 빼고 옵션만 보여준다 — 항상 하나가 골라져 있어야 하는 화면(자녀 한 명씩 보기)용. 기본 true */
  showAll?: boolean;
};

/**
 * 가로 스크롤 필터 칩. 할 일 목록의 반복 주기(한번·매일·매주·매월), 부모 화면의
 * 자녀(민준·서윤) 필터, 환전 요청의 상태(대기 중·지난 내역) 필터가 같은 모양이라 하나로 쓴다.
 *
 * '전체'는 옵션 배열에 넣지 않고 여기서 붙인다 — 부르는 쪽이 매번 sentinel 값을
 * 만들지 않아도 되고, 선택 없음을 null 하나로 표현할 수 있다.
 *
 * 색과 크기는 상점(CategoryFilterBar)에 맞춘다. 원래 미선택 칩이 진회색 채움 + 흰 글씨라
 * 선택된 칩보다 오히려 눈에 띄었고, 여백도 2px 씩 더 커서(18/9) 탭을 옮기면 같은 역할의
 * 칩이 다른 것처럼 보였다. 두 칩이 같아졌으니 다음에 손댈 때 shared 로 합친다.
 */
export function FilterChips<T extends string>({
  options,
  selected,
  onSelect,
  allLabel = '전체',
  showAll = true,
}: FilterChipsProps<T>) {
  const theme = useTheme();
  const items: { key: T | null; label: string }[] = showAll
    ? [{ key: null, label: allLabel }, ...options]
    : options;

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
              {item.label}
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
