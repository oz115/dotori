import { useQuery } from '@tanstack/react-query';

import { wordApi } from '@/features/word/api/wordApi';
import { wordKeys } from '@/features/word/model/wordKeys';

/**
 * 단어명 검색 (서버 검색 API). 목록은 무한 스크롤이라 한 번에 일부만 로드돼 있어서,
 * 검색은 클라이언트에 이미 있는 데이터가 아니라 서버 전체에서 찾아야 한다 —
 * 그래야 아직 안 불러온 페이지의 단어도 검색 결과에 나온다.
 */
export function useWordSearch(keyword: string) {
  return useQuery({
    queryKey: wordKeys.search(keyword),
    queryFn: () => wordApi.searchWords(keyword),
    enabled: keyword.length > 0,
    staleTime: 60_000,
  });
}
