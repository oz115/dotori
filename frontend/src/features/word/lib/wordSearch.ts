import { CHOSUNG_ORDER, chosungGroupOf } from '@/features/word/lib/chosung';
import { difficultyTier, type DifficultyTier } from '@/features/word/lib/difficultyTier';
import type { WordCategory } from '@/features/word/model/types';

export type WordCategoryFilter = WordCategory | 'ALL';
export type WordDifficultyFilter = DifficultyTier | 'ALL';

/**
 * 난이도 필터를 건다. 목록 화면과 상세의 이전/다음이 **같은 함수**를 봐야 한다 —
 * 각자 거르면 목록에서 초급 7개를 보다가 들어갔는데 화살표는 21개를 도는 식으로 어긋난다.
 */
export function filterByDifficulty<T extends { difficultyLevel: number }>(
  words: T[],
  difficulty: WordDifficultyFilter,
): T[] {
  return difficulty === 'ALL'
    ? words
    : words.filter((word) => difficultyTier(word.difficultyLevel) === difficulty);
}

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
