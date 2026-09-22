import { Ionicons } from '@expo/vector-icons';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { wordApi } from '@/features/word/api/wordApi';
import { categoryLabel } from '@/features/word/lib/categoryLabel';
import { categoryBadgeColors } from '@/features/word/lib/categoryTheme';
import {
  difficultyBadgeColors,
  difficultyLabel,
  type DifficultyTier,
} from '@/features/word/lib/difficultyTier';
import { EasyDefinitionBubble } from '@/features/word/ui/EasyDefinitionBubble';
import { filterByDifficulty, groupByChosung } from '@/features/word/lib/wordSearch';
import { useWord } from '@/features/word/model/useWord';
import { useWords } from '@/features/word/model/useWords';
import type { WordCategory } from '@/features/word/model/types';
import { wordKeys } from '@/features/word/model/wordKeys';
import { useTheme } from '@/shared/theme';
import { Header } from '@/shared/ui';

type WordDetailProps = {
  wordId: number;
  /** 목록에서 보고 있던 카테고리 필터. 이전/다음이 이 범위 안에서만 돌게 한다. 없으면 전체. */
  category?: WordCategory;
  /** 목록에서 보고 있던 난이도 필터. 카테고리와 같은 이유로 받는다. 없으면 전 난이도. */
  difficulty?: DifficultyTier;
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
 * 그 블록은 다람쥐가 건네는 말풍선으로 그린다. 같은 화면에 글이 두 덩이 있으면 어느 쪽을
 * 읽어야 할지 아이가 헷갈리는데, 화자를 세워 두면 "사전이 적은 뜻"과 "누가 풀어 준 말"로
 * 역할이 갈린다. 마스코트는 미션·퀴즈에서 쓰던 다람쥐 그대로다.
 *
 * 이전/다음 단어는 라우터로 새 화면을 띄우지 않는다 — `router.replace`를 쓰면 스택
 * 네비게이션 전환이 일어나서 헤더까지 포함해 화면 전체가 다시 뜨는 것처럼 보인다.
 * 대신 지금 보고 있는 wordId를 로컬 state로 들고, 버튼을 누르면 그 state만 바꿔서
 * 같은 화면 안에서 내용(배지·단어·뜻)만 갈아 끼운다 — 헤더는 그대로 고정된다.
 */
export function WordDetail({
  wordId: initialWordId,
  category,
  difficulty,
  onBack: goBack,
}: WordDetailProps) {
  const theme = useTheme();
  const queryClient = useQueryClient();
  const [currentWordId, setCurrentWordId] = useState(initialWordId);
  const { data: word, isPending, isError } = useWord(currentWordId);

  // 이전/다음 이동은 목록에서 보던 카테고리로 지금까지 불러온 페이지 안에서만 가능하다 —
  // 이웃의 범위와 순서는 **목록 화면에서 보던 그대로**여야 한다. 그래서 목록이 쓰는 것과
  // 같은 필터(filterByDifficulty)와 같은 정렬(groupByChosung)을 여기서도 거친다 — 한쪽만
  // 고치면 초급 7개를 보다 들어왔는데 화살표는 카테고리 전체 21개를 도는 식으로 어긋난다.
  // 검색 결과에서 바로 들어왔으면 넘어온 필터가 없어 사전 전체를 돈다.
  const browse = useWords(category);
  const loadedWords = useMemo(() => {
    const all = browse.data?.pages.flatMap((p) => p.words) ?? [];
    return groupByChosung(filterByDifficulty(all, difficulty ?? 'ALL')).flatMap((g) => g.words);
  }, [browse.data, difficulty]);
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

        <EasyDefinitionBubble easyDefinition={word.easyDefinition} />
      </View>

      {/* 글자 없이 화살표만 둔다. 이동 방향은 화살표로 이미 분명하고, 이 화면의 주인공은
          단어와 뜻이라 버튼이 가로폭을 다 먹을 이유가 없다. 라벨은 스크린리더에만 준다.
          단어마다 뜻·쉬운 설명 길이가 제각각인데 버튼이 그때그때 다른 높이에 있으면 연달아
          넘겨볼 때 손가락이 따라다녀야 한다. 화면 높이의 65% 지점에 못 박아 같은 자리에 둔다. */}
      <View style={styles.navRow}>
        <NeighborButton direction="prev" disabled={!hasNeighbor} onPress={() => goToNeighbor(-1)} />
        <NeighborButton direction="next" disabled={!hasNeighbor} onPress={() => goToNeighbor(1)} />
      </View>
    </View>
  );
}

/** 이전/다음 단어로 가는 원형 버튼. 이웃이 없으면 흐리게 두고 누르지 못하게 한다 */
function NeighborButton({
  direction,
  disabled,
  onPress,
}: {
  direction: 'prev' | 'next';
  disabled: boolean;
  onPress: () => void;
}) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={direction === 'prev' ? '이전 단어' : '다음 단어'}
      accessibilityState={{ disabled }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.navButton,
        {
          backgroundColor: disabled
            ? theme.color.background.surfaceSunken
            : theme.color.brand.solid,
          opacity: pressed ? 0.7 : 1,
        },
      ]}
    >
      <Ionicons
        name={direction === 'prev' ? 'arrow-back' : 'arrow-forward'}
        size={28}
        color={disabled ? theme.color.text.muted : theme.color.text.onBrand}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  badgeRow: { flexDirection: 'row' },
  badge: { paddingHorizontal: 10, paddingVertical: 4 },
  navRow: {
    position: 'absolute',
    top: '65%',
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'center',
    // space 스케일(…40, 48)에 없는 값이라 여기만 직접 적는다. 두 버튼이 실수로 눌리지 않을
    // 만큼 떨어뜨리려고 눈으로 맞춘 간격이다 — 토큰을 늘리면 다른 화면까지 영향을 받는다.
    gap: 55,
  },
  // radius 는 지름의 절반이라야 정원이 된다. 48이 손가락이 닿는 최소치이고 여기서는
  // 화면 아래에 홀로 있는 버튼이라 그보다 넉넉히 준다
  navButton: {
    width: 70,
    height: 70,
    borderRadius: 35,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
