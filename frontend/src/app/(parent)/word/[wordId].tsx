import { router, useLocalSearchParams } from 'expo-router';

import { WordDetail } from '@/features/word';

export default function ParentWordDetailScreen() {
  const { wordId } = useLocalSearchParams<{ wordId: string }>();
  return (
    <WordDetail
      wordId={Number(wordId)}
      onBack={() => (router.canGoBack() ? router.back() : router.replace('/(parent)/word'))}
    />
  );
}
