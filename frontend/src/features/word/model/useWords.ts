import { useInfiniteQuery } from '@tanstack/react-query';

import { wordApi } from '@/features/word/api/wordApi';
import { wordKeys } from '@/features/word/model/wordKeys';
import type { WordCategory } from '@/features/word/model/types';

// 한 건이 4개 필드짜리 가벼운 행이라 페이지를 크게 잡아도 부담이 없다 — 페이지가 잘면
// 스크롤할 때마다 서버 왕복이 생겨 목록이 자주 끊긴다.
const PAGE_SIZE = 50;

/**
 * 경제 단어 목록, 카테고리별 무한 스크롤. category 가 undefined 면 전체를 본다.
 * 사전을 끝까지 미리 다 받아오지 않고, 목록 화면을 내릴 때마다 다음 페이지를 이어 받는다.
 */
export function useWords(category: WordCategory | undefined) {
  return useInfiniteQuery({
    queryKey: wordKeys.list(category),
    queryFn: ({ pageParam }) => wordApi.getWords({ category, page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined,
    staleTime: 5 * 60_000,
  });
}
