import { router, useLocalSearchParams } from 'expo-router';

import { WordDetail } from '@/features/word';
import type { DifficultyTier, WordCategory } from '@/features/word';

export default function ParentWordDetailScreen() {
  const { wordId, category, difficulty } = useLocalSearchParams<{
    wordId: string;
    category?: string;
    difficulty?: string;
  }>();
  return (
    <WordDetail
      wordId={Number(wordId)}
      // 목록에서 넘겨준 값만 오는 파라미터라 목록의 필터 키 그대로다
      category={category as WordCategory | undefined}
      difficulty={difficulty as DifficultyTier | undefined}
      onBack={() => (router.canGoBack() ? router.back() : router.replace('/(parent)/word'))}
    />
  );
}
