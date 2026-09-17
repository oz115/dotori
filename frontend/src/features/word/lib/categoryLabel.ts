import type { WordCategory } from '@/features/word/model/types';

const CATEGORY_LABELS: Record<WordCategory, string> = {
  INCOME: '소득',
  CONSUMPTION: '소비',
  SAVINGS: '저축',
  INVESTMENT: '투자',
  BANKING: '금융',
  ECONOMY: '경제',
  OTHER: '기타',
};

export function categoryLabel(category: WordCategory): string {
  return CATEGORY_LABELS[category];
}
