import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, SectionList, StyleSheet, Text, View } from 'react-native';

import { groupByChosung, type WordCategoryFilter } from '@/features/word/lib/wordSearch';
import { useWords } from '@/features/word/model/useWords';
import { useWordSearch } from '@/features/word/model/useWordSearch';
import type { WordListItem } from '@/features/word/model/types';
import { CategoryFilterBar } from '@/features/word/ui/CategoryFilterBar';
import { ChosungIndexRail } from '@/features/word/ui/ChosungIndexRail';
import { WordListRow } from '@/features/word/ui/WordListRow';
import { useTheme } from '@/shared/theme';
import { Header, Input, Screen } from '@/shared/ui';

/** 입력이 멈추고 이 시간(ms)이 지나야 검색 API를 부른다. 타이핑마다 매번 부르면 낭비다. */
const SEARCH_DEBOUNCE_MS = 300;

type WordCatalogProps = {
  /**
   * 단어를 눌렀을 때의 이동. 부모·아이 라우트 그룹에 같은 `/word/[wordId]` 경로가 있어서
   * 어느 그룹의 상세로 갈지는 화면(라우트)이 정한다 — 그룹을 안 적으면 아이 쪽으로 간다.
   */
  onSelectWord: (wordId: number) => void;
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
  const listRef = useRef<SectionList<WordListItem>>(null);
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [category, setCategory] = useState<WordCategoryFilter>('ALL');

  useEffect(() => {
    const id = setTimeout(() => setDebouncedQuery(query.trim()), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(id);
  }, [query]);

  const isSearching = debouncedQuery.length > 0;

  const browse = useWords(category === 'ALL' ? undefined : category);
  const search = useWordSearch(debouncedQuery);

  const words: WordListItem[] = useMemo(
    () => (isSearching ? (search.data ?? []) : (browse.data?.pages.flatMap((p) => p.words) ?? [])),
    [isSearching, search.data, browse.data],
  );
  const isPending = isSearching ? search.isPending : browse.isPending;
  const isError = isSearching ? search.isError : browse.isError;

  const sections = useMemo(
    () => groupByChosung(words).map((group) => ({ title: group.chosung, data: group.words })),
    [words],
  );
  const presentChosung = useMemo(() => new Set(sections.map((s) => s.title)), [sections]);

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
        />
        <CategoryFilterBar selected={category} onSelect={setCategory} disabled={isSearching} />
      </View>

      <View style={styles.body}>
        <SectionList
          ref={listRef}
          style={styles.flex}
          sections={sections}
          keyExtractor={(item) => String(item.wordId)}
          renderItem={({ item }) => (
            <WordListRow word={item} onPress={() => onSelectWord(item.wordId)} />
          )}
          renderSectionHeader={({ section }) => (
            <Text
              style={[
                theme.typography.label,
                {
                  color: theme.color.brand.subtleText,
                  backgroundColor: theme.color.background.canvas,
                  paddingTop: theme.space.sm,
                  paddingBottom: theme.space.xs,
                },
              ]}
            >
              {section.title}
            </Text>
          )}
          stickySectionHeadersEnabled
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
  body: { flex: 1, flexDirection: 'row' },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 40 },
});
