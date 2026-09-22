import { useInfiniteQuery } from '@tanstack/react-query';
import { useEffect } from 'react';

import { wordApi } from '@/features/word/api/wordApi';
import { wordKeys } from '@/features/word/model/wordKeys';
import type { WordCategory } from '@/features/word/model/types';

// 사전 전체가 100단어 남짓이고 한 건이 4개 필드짜리 가벼운 행이라, 한 번에 다 받아도
// 10KB 안팎이다. 페이지를 잘게 나눌 이유가 없다.
const PAGE_SIZE = 200;

/**
 * 경제 단어 목록, 카테고리별 무한 스크롤. category 가 undefined 면 전체를 본다.
 *
 * <p>남은 페이지가 있으면 스크롤을 기다리지 않고 알아서 이어 받는다. 난이도 필터가
 * 클라이언트에서 도는데 목록을 일부만 들고 있으면, 아직 안 받은 페이지의 단어가 조용히
 * 빠져서 "초급 24개" 중 몇 개만 보이게 된다. 필터 결과가 짧으면 onEndReached 가 아예
 * 안 불려서 스스로 회복하지도 못한다.
 */
export function useWords(category: WordCategory | undefined) {
  const query = useInfiniteQuery({
    queryKey: wordKeys.list(category),
    queryFn: ({ pageParam }) => wordApi.getWords({ category, page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined,
    staleTime: 5 * 60_000,
  });

  const { hasNextPage, isFetchingNextPage, fetchNextPage } = query;
  useEffect(() => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  return query;
}
