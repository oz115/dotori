import { router, useLocalSearchParams } from 'expo-router';

import { WordDetail } from '@/features/word';

export default function ChildWordDetailScreen() {
  const { wordId } = useLocalSearchParams<{ wordId: string }>();
  return (
    <WordDetail
      wordId={Number(wordId)}
      // 부모 그룹에도 같은 경로가 있어 그룹까지 적는다
      onBack={() => (router.canGoBack() ? router.back() : router.replace('/(child)/word'))}
    />
  );
}
