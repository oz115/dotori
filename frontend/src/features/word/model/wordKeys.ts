import type { WordCategory } from '@/features/word/model/types';

/** 단어장 관련 TanStack Query 키. */
export const wordKeys = {
  all: ['word'] as const,
  list: (category?: WordCategory) => [...wordKeys.all, 'list', category ?? 'ALL'] as const,
  search: (keyword: string) => [...wordKeys.all, 'search', keyword] as const,
  detail: (wordId: number) => [...wordKeys.all, 'detail', wordId] as const,
};
