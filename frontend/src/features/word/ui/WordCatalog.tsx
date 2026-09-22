import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, SectionList, StyleSheet, Text, View } from 'react-native';

import { difficultyTier } from '@/features/word/lib/difficultyTier';
import {
  filterByDifficulty,
  groupByChosung,
  type WordCategoryFilter,
} from '@/features/word/lib/wordSearch';
import { useWords } from '@/features/word/model/useWords';
import { useWordSearch } from '@/features/word/model/useWordSearch';
import type { DifficultyTier } from '@/features/word/lib/difficultyTier';
import type { WordCategory, WordListItem } from '@/features/word/model/types';
import { CategoryFilterBar } from '@/features/word/ui/CategoryFilterBar';
import {
  DifficultyFilterBar,
  type WordDifficultyFilterKey,
} from '@/features/word/ui/DifficultyFilterBar';
import { ChosungIndexRail } from '@/features/word/ui/ChosungIndexRail';
import { WordListRow, WORD_ROW_HEIGHT } from '@/features/word/ui/WordListRow';
import { useTheme } from '@/shared/theme';
import { Header, Input, Screen, usePullToRefresh } from '@/shared/ui';

/** 입력이 멈추고 이 시간(ms)이 지나야 검색 API를 부른다. 타이핑마다 매번 부르면 낭비다. */
const SEARCH_DEBOUNCE_MS = 300;

/** 초성 헤더 높이(px). 아래 renderSectionHeader 가 이 값을 그대로 height 로 쓴다. */
const SECTION_HEADER_HEIGHT = 30;

type WordCatalogProps = {
  /**
   * 단어를 눌렀을 때의 이동. 부모·아이 라우트 그룹에 같은 `/word/[wordId]` 경로가 있어서
   * 어느 그룹의 상세로 갈지는 화면(라우트)이 정한다 — 그룹을 안 적으면 아이 쪽으로 간다.
   * category·difficulty 는 지금 걸려 있는 필터 — 상세의 이전/다음이 목록에서 보던 그
   * 범위와 순서를 그대로 돌게 라우트 파라미터로 넘긴다 (전체면 undefined).
   * 검색 중이면 카테고리를 넘기지 않는다 — 검색 API 가 카테고리를 모른다.
   */
  onSelectWord: (wordId: number, category?: WordCategory, difficulty?: DifficultyTier) => void;
};

/**
 * 경제 단어장 목록. 완료해야 할 목록이 아니라 필요할 때 찾아보는 사전이라, 검색·초성
 * 인덱스로 훑어보는 걸 우선한다 — 진도·완료 표시는 없다.
 *
 * 전역 사전이라 조회만 있고 사용자별 데이터가 없다 — 부모와 아이가 똑같은 화면을 쓴다.
 *
 * 두 데이터 소스를 상황에 따라 갈아 낀다.
 * - 검색어가 없으면: 카테고리별 무한 스크롤 목록(`useWords`, GET .../words).
 * - 검색어가 있으면: 서버 검색(`useWordSearch`, GET .../words/search) — 목록은 일부만
 *   로드돼 있어서 클라이언트에서 걸러내면 아직 안 불러온 페이지의 단어를 놓친다.
 *   검색 API는 카테고리를 모르므로, 검색 중엔 카테고리 필터를 비활성화한다.
 */
export function WordCatalog({ onSelectWord }: WordCatalogProps) {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  const listRef = useRef<SectionList<WordListItem>>(null);
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [category, setCategory] = useState<WordCategoryFilter>('ALL');
  const [difficulty, setDifficulty] = useState<WordDifficultyFilterKey>('ALL');

  useEffect(() => {
    const id = setTimeout(() => setDebouncedQuery(query.trim()), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(id);
  }, [query]);

  const isSearching = debouncedQuery.length > 0;

  const browse = useWords(category === 'ALL' ? undefined : category);
  const search = useWordSearch(debouncedQuery);

  const loadedBrowseWords = useMemo(
    () => browse.data?.pages.flatMap((p) => p.words) ?? [],
    [browse.data],
  );
  // 서버 검색 응답을 기다리는 동안엔 이미 불러온 목록에서 먼저 걸러 보여준다 — 방금까지
  // 화면에 보이던 단어를 검색했는데 스피너부터 뜨면 느리게 느껴진다.
  const interimMatches = useMemo(
    () => loadedBrowseWords.filter((w) => w.word.includes(debouncedQuery)),
    [loadedBrowseWords, debouncedQuery],
  );
  // 난이도 필터를 걸기 전의 목록. 칩에 보여줄 건수를 이걸로 센다 — 이미 걸러진 것을 세면
  // 지금 고른 칩만 0이 아닌 숫자가 되어 다른 칩이 전부 비어 보인다.
  const unfiltered: WordListItem[] = useMemo(
    () => (isSearching ? (search.data ?? interimMatches) : loadedBrowseWords),
    [isSearching, search.data, interimMatches, loadedBrowseWords],
  );
  const difficultyCounts = useMemo(() => {
    const counts: Record<WordDifficultyFilterKey, number> = {
      ALL: unfiltered.length,
      BEGINNER: 0,
      INTERMEDIATE: 0,
      ADVANCED: 0,
    };
    for (const word of unfiltered) {
      counts[difficultyTier(word.difficultyLevel)] += 1;
    }
    return counts;
  }, [unfiltered]);

  // 카테고리를 바꿨더니 고른 난이도가 0건이 되는 조합이 있다(경제×초급 등). 그때는 고른 값을
  // 되돌리는 대신 이 파생값만 전체로 본다 — 카테고리를 되돌리면 원래 고르던 난이도가 살아난다.
  const effectiveDifficulty: WordDifficultyFilterKey =
    difficulty !== 'ALL' && difficultyCounts[difficulty] === 0 ? 'ALL' : difficulty;

  // 검색 응답에도 난이도는 있어서(WordSearchSummary) 이 필터는 검색 중에도 그대로 돈다 —
  // 카테고리 필터와 달리 비활성화하지 않는다.
  const words: WordListItem[] = useMemo(
    () => filterByDifficulty(unfiltered, effectiveDifficulty),
    [unfiltered, effectiveDifficulty],
  );
  const isPending = isSearching
    ? search.isPending && interimMatches.length === 0
    : browse.isPending;
  const isError = isSearching ? search.isError : browse.isError;

  const sections = useMemo(
    () => groupByChosung(words).map((group) => ({ title: group.chosung, data: group.words })),
    [words],
  );
  const presentChosung = useMemo(() => new Set(sections.map((s) => s.title)), [sections]);

  /**
   * `scrollToLocation` 이 쓸 평탄화 인덱스별 위치표.
   *
   * <p>이게 없으면 아직 화면에 그려진 적 없는 구간으로는 점프가 조용히 실패한다 —
   * VirtualizedList 는 재어 본 적 있는 행까지만 위치를 알고, 모르는 곳으로 가라고 하면
   * onScrollToIndexFailed 로 빠진다. 위에서 몇 개는 되고 아래쪽 초성은 안 먹히던 증상이
   * 이것이다.
   *
   * <p>SectionList 의 평탄화 순서는 섹션마다 [헤더, 항목…, 푸터] 다. 푸터를 그리지 않아도
   * 자리는 세므로 길이 0으로 같이 넣는다.
   */
  const itemLayouts = useMemo(() => {
    const layouts: { length: number; offset: number; index: number }[] = [];
    let offset = 0;
    let index = 0;
    const push = (length: number) => {
      layouts.push({ length, offset, index });
      offset += length;
      index += 1;
    };
    for (const section of sections) {
      push(SECTION_HEADER_HEIGHT);
      for (let i = 0; i < section.data.length; i += 1) {
        push(WORD_ROW_HEIGHT);
      }
      push(0);
    }
    return layouts;
  }, [sections]);

  const jumpToChosung = (chosung: string) => {
    const sectionIndex = sections.findIndex((s) => s.title === chosung);
    if (sectionIndex === -1) return;
    listRef.current?.scrollToLocation({ sectionIndex, itemIndex: 0, viewPosition: 0 });
  };

  const loadMore = () => {
    if (isSearching || !browse.hasNextPage || browse.isFetchingNextPage) return;
    browse.fetchNextPage();
  };

  return (
    <Screen
      header={
        <Header
          title="경제 단어장"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/'))}
        />
      }
      padded={false}
    >
      <View
        style={{
          paddingHorizontal: theme.layout.screenX,
          gap: theme.space.md,
          paddingBottom: theme.space.sm,
        }}
      >
        <Input
          variant="box"
          leftIcon={<Ionicons name="search" size={18} color={theme.color.text.muted} />}
          placeholder="궁금한 경제 용어를 검색해보세요"
          value={query}
          onChangeText={setQuery}
          returnKeyType="search"
          rightSlot={
            query.length > 0 ? (
              <Pressable
                // 디바운스를 기다리지 않고 바로 목록으로 돌아간다 — 지우기는 타이핑이 아니다
                onPress={() => {
                  setQuery('');
                  setDebouncedQuery('');
                }}
                hitSlop={8}
                accessibilityRole="button"
                accessibilityLabel="검색어 지우기"
              >
                <Ionicons name="close-circle" size={20} color={theme.color.text.muted} />
              </Pressable>
            ) : undefined
          }
        />
        <CategoryFilterBar selected={category} onSelect={setCategory} disabled={isSearching} />
        <DifficultyFilterBar
          selected={effectiveDifficulty}
          onSelect={setDifficulty}
          counts={difficultyCounts}
        />
      </View>

      <View style={styles.body}>
        <SectionList
          refreshControl={refreshControl}
          ref={listRef}
          style={styles.flex}
          sections={sections}
          keyExtractor={(item) => String(item.wordId)}
          renderItem={({ item }) => (
            <WordListRow
              word={item}
              onPress={() =>
                onSelectWord(
                  item.wordId,
                  isSearching || category === 'ALL' ? undefined : category,
                  effectiveDifficulty === 'ALL' ? undefined : effectiveDifficulty,
                )
              }
            />
          )}
          renderSectionHeader={({ section }) => (
            <Text
              style={[
                theme.typography.label,
                styles.sectionHeader,
                {
                  color: theme.color.brand.subtleText,
                  backgroundColor: theme.color.background.canvas,
                },
              ]}
            >
              {section.title}
            </Text>
          )}
          stickySectionHeadersEnabled
          getItemLayout={(_data, index) => itemLayouts[index] ?? { length: 0, offset: 0, index }}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingBottom: theme.space['3xl'],
            flexGrow: 1,
          }}
          onEndReached={loadMore}
          onEndReachedThreshold={0.5}
          ListFooterComponent={
            !isSearching && browse.isFetchingNextPage ? (
              <ActivityIndicator
                color={theme.color.brand.solid}
                style={{ paddingVertical: theme.space.md }}
              />
            ) : null
          }
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
                {isPending
                  ? '단어를 불러오는 중이에요.'
                  : isError
                    ? '단어를 불러오지 못했어요.'
                    : query
                      ? `"${query}"에 맞는 단어가 없어요.`
                      : effectiveDifficulty !== 'ALL'
                        ? '이 난이도에 맞는 단어가 없어요.'
                        : '표시할 단어가 없어요.'}
              </Text>
            </View>
          }
          showsVerticalScrollIndicator={false}
        />
        <ChosungIndexRail present={presentChosung} onJump={jumpToChosung} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  // getItemLayout 이 이 높이를 그대로 쓴다 — 여기를 고치면 SECTION_HEADER_HEIGHT 도 같이 고친다
  sectionHeader: { height: SECTION_HEADER_HEIGHT, textAlignVertical: 'center' },
  body: { flex: 1, flexDirection: 'row' },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 40 },
});
