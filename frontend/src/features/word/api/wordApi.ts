import { httpClient } from '@/shared/api';
import type {
  WordCategory,
  WordDetailInfo,
  WordSearchSummary,
  WordSummary,
} from '@/features/word/model/types';

export type WordListPage = {
  words: WordSummary[];
  page: number;
  totalPages: number;
};

/** 단어장 원시 HTTP 호출. 캐시 갱신은 model 훅이 담당. */
export const wordApi = {
  getWords: (params: { category?: WordCategory; page: number; size: number }) =>
    httpClient.get<WordListPage>('/api/v1/words', { params }).then((res) => res.data),

  getWord: (wordId: number) =>
    httpClient.get<WordDetailInfo>(`/api/v1/words/${wordId}`).then((res) => res.data),

  /** 단어명 부분 일치 검색. 카테고리 파라미터가 없다 — 검색 API 자체가 카테고리를 모른다. */
  searchWords: (keyword: string) =>
    httpClient
      .get<{ words: WordSearchSummary[] }>('/api/v1/words/search', { params: { keyword } })
      .then((res) => res.data.words),
};
