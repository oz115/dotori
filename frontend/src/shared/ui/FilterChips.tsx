import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

export type FilterOption<T extends string> = {
  key: T;
  label: string;
  /**
   * 선택됐을 때 배경색 (미지정 시 기본 브랜드 그린). 저축 목록의 예금/적금처럼 칩이
   * 같은 종류를 가리키는 다른 곳(상품 카드 뱃지)과 색으로 이어져야 할 때만 넘긴다.
   */
  activeColor?: string;
  /** activeColor 를 넘길 때 그 위에 올라갈 글자색. 기본은 onBrand(흰색)와 같다. */
  activeTextColor?: string;
};

type FilterChipsProps<T extends string> = {
  options: FilterOption<T>[];
  /** null 이면 '전체'가 선택된 상태 */
  selected: T | null;
  onSelect: (key: T | null) => void;
  /** '전체' 칩 문구. 기본 '전체' */
  allLabel?: string;
  /** false 면 '전체' 칩을 빼고 옵션만 보여준다 — 항상 하나가 골라져 있어야 하는 화면(자녀 한 명씩 보기)용. 기본 true */
  showAll?: boolean;
  /**
   * true 면 가로 스크롤 대신 칩이 폭을 균등하게 나눠 가져 화면 끝까지 채운다.
   * 옵션 수가 고정돼 있고(할 일 주기 5개 등) 라벨이 짧아 줄바꿈 없이 들어갈 때만 켠다 —
   * 옵션이 늘어나거나 라벨이 길어지면 칩이 좁아지며 글자가 잘릴 수 있다.
   */
  fill?: boolean;
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
  fill = false,
}: FilterChipsProps<T>) {
  const theme = useTheme();
  const items: { key: T | null; label: string; activeColor?: string; activeTextColor?: string }[] =
    showAll ? [{ key: null, label: allLabel }, ...options] : options;

  const chips = items.map((item) => {
    const active = item.key === selected;
    return (
      <Pressable
        key={item.key ?? '__all__'}
        accessibilityRole="button"
        accessibilityState={{ selected: active }}
        onPress={() => onSelect(item.key)}
        style={[
          styles.chip,
          fill && styles.chipFill,
          {
            borderRadius: theme.radius.full,
            backgroundColor: active
              ? (item.activeColor ?? theme.color.brand.solid)
              : theme.color.background.surfaceSunken,
          },
        ]}
      >
        <Text
          numberOfLines={1}
          style={[
            theme.typography.label,
            {
              color: active
                ? (item.activeTextColor ?? theme.color.text.onBrand)
                : theme.color.text.default,
            },
          ]}
        >
          {item.label}
        </Text>
      </Pressable>
    );
  });

  // fill 은 옵션 수가 고정된 화면 전용이라 스크롤이 필요 없다 — 폭을 나눠 갖는 고정 행으로 그린다.
  if (fill) {
    return <View style={[styles.fillRow, { gap: theme.space.sm }]}>{chips}</View>;
  }

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: theme.space.sm }}
    >
      {chips}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  chip: { paddingHorizontal: 16, paddingVertical: 8, alignItems: 'center' },
  // flex:1 로 칩마다 같은 폭을 가져가 좌우로 균등하게 나뉜다
  chipFill: { flex: 1, paddingHorizontal: 4 },
  fillRow: { flexDirection: 'row' },
});
