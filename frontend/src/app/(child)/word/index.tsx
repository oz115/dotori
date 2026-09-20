import { router } from 'expo-router';

import { WordCatalog } from '@/features/word';

export default function ChildWordScreen() {
  return (
    <WordCatalog
      // 부모 그룹에도 같은 경로가 있어 그룹까지 적는다
      onSelectWord={(wordId) =>
        router.push({ pathname: '/(child)/word/[wordId]', params: { wordId: String(wordId) } })
      }
    />
  );
}
