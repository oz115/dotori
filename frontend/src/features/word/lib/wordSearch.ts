import { CHOSUNG_ORDER, chosungGroupOf } from '@/features/word/lib/chosung';
import type { WordCategory } from '@/features/word/model/types';

export type WordCategoryFilter = WordCategory | 'ALL';

export type WordChosungGroup<T> = { chosung: string; words: T[] };

/**
 * 초성별로 묶는다. 무한 스크롤 목록(WordSummary)·검색 결과(WordSearchSummary) 둘 다
 * `word` 필드만 있으면 되므로 제네릭으로 둔다 — 그래야 두 출처를 이 함수 하나로 다룬다.
 */
export function groupByChosung<T extends { word: string }>(words: T[]): WordChosungGroup<T>[] {
  const byChosung = new Map<string, T[]>();
  for (const word of words) {
    const key = chosungGroupOf(word.word);
    const group = byChosung.get(key);
    if (group) {
      group.push(word);
    } else {
      byChosung.set(key, [word]);
    }
  }
  return CHOSUNG_ORDER.filter((c) => byChosung.has(c)).map((chosung) => ({
    chosung,
    words: byChosung.get(chosung)!,
  }));
}
