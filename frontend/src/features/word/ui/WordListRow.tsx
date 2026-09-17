import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { WordListItem } from '@/features/word/model/types';
import { useTheme } from '@/shared/theme';

type WordListRowProps = {
  word: WordListItem;
  onPress: () => void;
};

/**
 * 단어장 목록 한 행. 카테고리·난이도·뜻은 여기서 보여주지 않는다 — 목록은 가볍게,
 * 상세에서만 확인한다. 뜻은 목록 API 응답에도 없어서(definition은 상세 전용 필드) 애초에
 * 보여줄 데이터가 없다.
 *
 * `word`를 wordId·word만 있는 최소 타입(WordListItem)으로 받는다 — 무한 스크롤 목록
 * (WordSummary)과 검색 결과(WordSearchSummary) 둘 다 이 자리에 그대로 들어올 수 있어야
 * 해서, 그 둘의 공통 부분집합만 요구한다.
 */
export function WordListRow({ word, onPress }: WordListRowProps) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        {
          gap: theme.space.md,
          paddingVertical: theme.space.md,
          borderBottomColor: theme.color.border.card,
          opacity: pressed ? 0.7 : 1,
        },
      ]}
    >
      <View
        style={[
          styles.icon,
          { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.md },
        ]}
      >
        <Ionicons name="book-outline" size={18} color={theme.color.brand.solid} />
      </View>
      <View style={styles.main}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          {word.word}
        </Text>
      </View>
      <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', borderBottomWidth: StyleSheet.hairlineWidth },
  icon: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center' },
  main: { flex: 1, minWidth: 0 },
});
