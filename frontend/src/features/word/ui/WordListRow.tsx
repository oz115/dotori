import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { difficultyBadgeColors } from '@/features/word/lib/difficultyTier';
import type { WordListItem } from '@/features/word/model/types';
import { useTheme } from '@/shared/theme';

/**
 * 행 높이(px). 목록이 `getItemLayout` 으로 초성 점프 위치를 계산하는 데 쓴다.
 *
 * <p>추정값이 아니라 이 행이 실제로 갖는 높이다 — 아래 `styles.row` 가 이 값을 그대로
 * height 로 쓴다. 재는 대신 못 박아 두는 편이 안전하다: 높이가 조금이라도 어긋나면
 * 초성 점프가 아래로 갈수록 점점 밀린다.
 */
export const WORD_ROW_HEIGHT = 58;

type WordListRowProps = {
  word: WordListItem;
  onPress: () => void;
};

/**
 * 단어장 목록 한 행. 카테고리·뜻은 여기서 보여주지 않는다 — 목록은 가볍게, 상세에서만
 * 확인한다. 뜻은 목록 API 응답에도 없어서(definition은 상세 전용 필드) 애초에 보여줄
 * 데이터가 없다.
 *
 * <p>난이도만은 왼쪽 아이콘의 색으로 드러낸다. 글자를 더 얹지 않고도 목록을 훑을 때
 * 초록·노랑·빨강이 눈에 들어와서, 지금 건 필터가 뭔지도 같이 읽힌다. 색은 상세의 난이도
 * 배지·목록의 난이도 필터 칩과 같은 것을 쓴다 — 세 곳이 어긋나면 신호가 아니라 장식이 된다.
 *
 * `word`를 wordId·word만 있는 최소 타입(WordListItem)으로 받는다 — 무한 스크롤 목록
 * (WordSummary)과 검색 결과(WordSearchSummary) 둘 다 이 자리에 그대로 들어올 수 있어야
 * 해서, 그 둘의 공통 부분집합만 요구한다.
 */
export function WordListRow({ word, onPress }: WordListRowProps) {
  const theme = useTheme();
  const tone = difficultyBadgeColors(theme, word.difficultyLevel);

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        {
          gap: theme.space.md,
          borderBottomColor: theme.color.border.card,
          opacity: pressed ? 0.7 : 1,
        },
      ]}
    >
      <View style={[styles.icon, { backgroundColor: tone.bg, borderRadius: theme.radius.md }]}>
        <Ionicons name="book-outline" size={18} color={tone.text} />
      </View>
      <View style={styles.main}>
        {/* 높이를 못 박아 두었으므로 줄바꿈을 막는다 — 가장 긴 단어가 10자라 잘릴 일은 없다 */}
        <Text
          numberOfLines={1}
          style={[theme.typography.titleMd, { color: theme.color.text.strong }]}
        >
          {word.word}
        </Text>
      </View>
      <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    height: WORD_ROW_HEIGHT,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  icon: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center' },
  main: { flex: 1, minWidth: 0 },
});
