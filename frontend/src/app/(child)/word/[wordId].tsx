import { useLocalSearchParams } from 'expo-router';

import { WordDetail } from '@/features/word';

export default function ChildWordDetailScreen() {
  const { wordId } = useLocalSearchParams<{ wordId: string }>();
  return <WordDetail wordId={Number(wordId)} />;
}
