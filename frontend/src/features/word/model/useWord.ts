import { useQuery } from '@tanstack/react-query';

import { wordApi } from '@/features/word/api/wordApi';
import { wordKeys } from '@/features/word/model/wordKeys';

/** 경제 단어 상세 (뜻 포함). */
export function useWord(wordId: number | undefined) {
  return useQuery({
    queryKey: wordKeys.detail(wordId ?? -1),
    queryFn: () => wordApi.getWord(wordId as number),
    enabled: wordId != null,
    staleTime: 5 * 60_000,
  });
}
