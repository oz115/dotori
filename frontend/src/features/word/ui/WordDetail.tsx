import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { wordApi } from '@/features/word/api/wordApi';
import { categoryLabel } from '@/features/word/lib/categoryLabel';
import { categoryBadgeColors } from '@/features/word/lib/categoryTheme';
import { difficultyBadgeColors, difficultyLabel } from '@/features/word/lib/difficultyTier';
import { groupByChosung } from '@/features/word/lib/wordSearch';
import { useWord } from '@/features/word/model/useWord';
import { useWords } from '@/features/word/model/useWords';
import type { WordCategory } from '@/features/word/model/types';
import { wordKeys } from '@/features/word/model/wordKeys';
import { useTheme } from '@/shared/theme';
import { Button, Header } from '@/shared/ui';

type WordDetailProps = {
  wordId: number;
  /** 목록에서 보고 있던 카테고리 필터. 이전/다음이 이 범위 안에서만 돌게 한다. 없으면 전체. */
  category?: WordCategory;
  /**
   * 뒤로가기. 부모·아이 라우트 그룹에 같은 `/word` 경로가 있어서 스택에 돌아갈 곳이 없을 때
   * 어느 그룹의 목록으로 갈지는 화면(라우트)이 정한다.
   */
  onBack: () => void;
};

/**
 * 경제 단어장 상세. `words` 테이블엔 예문 컬럼이 없어 뜻(definition)과 쉬운 설명을 보여준다.
 * 카테고리 배지는 검정+흰 글씨, 난이도 배지는 초급/중급/고급 시맨틱 톤온톤(D안).
 *
 * 쉬운 설명은 정석 뜻을 대체하지 않고 아래에 덧붙인다 — 어려운 말도 눈에 익혀야 하는
 * 단어장이라 원래 뜻을 감추면 안 되고, 아이는 아래 문단만 읽어도 이해할 수 있다.
 * 아직 채우지 못한 단어가 있어서 없으면 블록째로 뺀다.
 *
 * 이전/다음 단어는 라우터로 새 화면을 띄우지 않는다 — `router.replace`를 쓰면 스택
 * 네비게이션 전환이 일어나서 헤더까지 포함해 화면 전체가 다시 뜨는 것처럼 보인다.
 * 대신 지금 보고 있는 wordId를 로컬 state로 들고, 버튼을 누르면 그 state만 바꿔서
 * 같은 화면 안에서 내용(배지·단어·뜻)만 갈아 끼운다 — 헤더는 그대로 고정된다.
 */
export function WordDetail({ wordId: initialWordId, category, onBack: goBack }: WordDetailProps) {
  const theme = useTheme();
  const queryClient = useQueryClient();
  const [currentWordId, setCurrentWordId] = useState(initialWordId);
  const { data: word, isPending, isError } = useWord(currentWordId);

  // 이전/다음 이동은 목록에서 보던 카테고리로 지금까지 불러온 페이지 안에서만 가능하다 —
  // 목록이 무한 스크롤이라 사전 전체 순서를 한 번에 갖고 있지 않다. 아직 안 불러온
  // 이웃이면(또는 검색 결과에서 바로 들어왔으면) 버튼이 비활성화된다.
  // 순서는 목록 화면과 똑같이 초성 그룹(가나다)으로 정렬해 계산한다 — 응답 순서를 그대로
  // 쓰면 화면에 보이는 순서와 다음 버튼의 순서가 어긋난다.
  const browse = useWords(category);
  const loadedWords = useMemo(
    () => groupByChosung(browse.data?.pages.flatMap((p) => p.words) ?? []).flatMap((g) => g.words),
    [browse.data],
  );
  const index = loadedWords.findIndex((w) => w.wordId === currentWordId);
  const hasNeighbor = index !== -1 && loadedWords.length > 1;

  // 목록엔 뜻(definition)이 없어서 이전/다음을 누를 때마다 그제서야 상세를 새로 불러오면
  // 매번 네트워크 왕복만큼 느리게 느껴진다. 지금 보는 단어의 앞뒤 이웃 상세를 미리
  // 캐시에 채워 둬서, 실제로 눌렀을 땐 이미 있는 데이터를 즉시 보여준다.
  useEffect(() => {
    if (!hasNeighbor) return;
    const prevId = loadedWords[(index - 1 + loadedWords.length) % loadedWords.length].wordId;
    const nextId = loadedWords[(index + 1) % loadedWords.length].wordId;
    for (const neighborId of [prevId, nextId]) {
      queryClient.prefetchQuery({
        queryKey: wordKeys.detail(neighborId),
        queryFn: () => wordApi.getWord(neighborId),
        staleTime: 5 * 60_000,
      });
    }
  }, [hasNeighbor, index, loadedWords, queryClient]);

  if (isPending) {
    return (
      <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
        <Header title="단어 뜻" onBack={goBack} />
        <View style={styles.centered}>
          <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
            불러오는 중이에요.
          </Text>
        </View>
      </View>
    );
  }

  if (isError || !word) {
    return (
      <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
        <Header title="단어 뜻" onBack={goBack} />
        <View style={styles.centered}>
          <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
            단어를 찾을 수 없어요.
          </Text>
        </View>
      </View>
    );
  }

  const catBadge = categoryBadgeColors(theme);
  const diffBadge = difficultyBadgeColors(theme, word.difficultyLevel);

  const goToNeighbor = (offset: 1 | -1) => {
    if (!hasNeighbor) return;
    const neighbor = loadedWords[(index + offset + loadedWords.length) % loadedWords.length];
    setCurrentWordId(neighbor.wordId);
  };

  return (
    <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
      <Header title="단어 뜻" onBack={goBack} />

      <View style={{ padding: theme.layout.screenX, gap: theme.space.lg }}>
        <View style={[styles.badgeRow, { gap: theme.space.xs }]}>
          {word.category ? (
            <View
              style={[
                styles.badge,
                { backgroundColor: catBadge.bg, borderRadius: theme.radius.full },
              ]}
            >
              <Text style={[theme.typography.caption, { color: catBadge.text }]}>
                {categoryLabel(word.category)}
              </Text>
            </View>
          ) : null}
          <View
            style={[
              styles.badge,
              { backgroundColor: diffBadge.bg, borderRadius: theme.radius.full },
            ]}
          >
            <Text style={[theme.typography.caption, { color: diffBadge.text }]}>
              {difficultyLabel(word.difficultyLevel)}
            </Text>
          </View>
        </View>

        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>{word.word}</Text>

        <View style={{ gap: theme.space.xs }}>
          <Text style={[theme.typography.label, { color: theme.color.text.label }]}>뜻</Text>
          <Text style={[theme.typography.body, { color: theme.color.text.default }]}>
            {word.definition}
          </Text>
        </View>

        {word.easyDefinition ? (
          <View
            style={[
              styles.easyBox,
              {
                backgroundColor: theme.color.background.surfaceSunken,
                borderRadius: theme.radius.md,
                padding: theme.space.lg,
                gap: theme.space.xs,
              },
            ]}
          >
            <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
              쉽게 말하면
            </Text>
            {/* 아이가 실제로 읽는 본문이라 줄간격을 넉넉히 준다 (퀴즈 해설과 같은 값) */}
            <Text
              style={[theme.typography.body, styles.easyText, { color: theme.color.text.default }]}
            >
              {word.easyDefinition}
            </Text>
          </View>
        ) : null}

        <View style={[styles.navRow, { gap: theme.space.sm }]}>
          <Button
            label="← 이전 단어"
            variant="outline"
            size="sm"
            style={styles.flexItem}
            disabled={!hasNeighbor}
            onPress={() => goToNeighbor(-1)}
          />
          <Button
            label="다음 단어 →"
            variant="outline"
            size="sm"
            style={styles.flexItem}
            disabled={!hasNeighbor}
            onPress={() => goToNeighbor(1)}
          />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  flexItem: { flex: 1 },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  badgeRow: { flexDirection: 'row' },
  badge: { paddingHorizontal: 10, paddingVertical: 4 },
  easyBox: { width: '100%' },
  easyText: { lineHeight: 28 },
  navRow: { flexDirection: 'row', marginTop: 8 },
});
