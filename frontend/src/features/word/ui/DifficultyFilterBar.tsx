import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';

import {
  difficultyTierColors,
  TIER_ORDER,
  tierLabel,
  type DifficultyTier,
} from '@/features/word/lib/difficultyTier';
import type { WordDifficultyFilter } from '@/features/word/lib/wordSearch';
import { useTheme } from '@/shared/theme';

export type WordDifficultyFilterKey = WordDifficultyFilter;

/** '전체'는 난이도가 아니라 해제 버튼이라 tier 가 없다 — 색도 그래서 갈린다 */
const FILTERS: { key: WordDifficultyFilterKey; label: string; tier: DifficultyTier | null }[] = [
  { key: 'ALL', label: '전체', tier: null },
  ...TIER_ORDER.map((tier) => ({ key: tier, label: tierLabel(tier), tier })),
];

type DifficultyFilterBarProps = {
  selected: WordDifficultyFilterKey;
  onSelect: (key: WordDifficultyFilterKey) => void;
  /** 칩마다 지금 걸린 다른 조건 기준의 건수. 0이면 눌러도 빈 화면이라 미리 막는다 */
  counts: Record<WordDifficultyFilterKey, number>;
};

/**
 * 단어장 난이도 필터 칩. 카테고리 칩과 같은 모양이되 한 줄 아래에 둔다 — 두 축을 한 줄에
 * 섞으면 서로 배타가 되어 "경제 + 초급"을 고를 수 없다.
 *
 * <p>건수를 같이 보여주고 0건이면 누르지 못하게 한다. 카테고리와 난이도를 곱하면 21칸 중
 * 6칸이 실제로 비어 있어서(경제×초급, 저축×고급 등), 그냥 두면 아이가 빈 화면을 본다.
 *
 * <p>칩 색은 상세 화면의 난이도 배지와 같은 톤온톤(초록·노랑·빨강)이다. 같은 난이도가
 * 화면마다 다른 색이면 아이가 둘을 잇지 못한다. 고른 칩만 같은 색의 진한 쪽으로 채워
 * 색 하나로 "무슨 난이도인지"와 "골랐는지"를 같이 읽게 한다. '전체'는 난이도가 아니라
 * 해제 버튼이라 브랜드색을 그대로 쓴다.
 */
export function DifficultyFilterBar({ selected, onSelect, counts }: DifficultyFilterBarProps) {
  const theme = useTheme();

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: theme.space.sm }}
    >
      {FILTERS.map((filter) => {
        const active = filter.key === selected;
        const count = counts[filter.key];
        const empty = count === 0 && !active;
        const tone = filter.tier ? difficultyTierColors(theme, filter.tier) : null;
        const background = active
          ? (tone?.solid ?? theme.color.brand.solid)
          : (tone?.bg ?? theme.color.background.surfaceSunken);
        const foreground = active
          ? theme.color.text.onBrand
          : (tone?.text ?? theme.color.text.default);
        return (
          <Pressable
            key={filter.key}
            disabled={empty}
            accessibilityRole="button"
            accessibilityState={{ selected: active, disabled: empty }}
            onPress={() => onSelect(filter.key)}
            style={[
              styles.chip,
              {
                borderRadius: theme.radius.full,
                opacity: empty ? 0.35 : 1,
                backgroundColor: background,
              },
            ]}
          >
            <Text style={[theme.typography.label, { color: foreground }]}>
              {filter.label} {count}
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
