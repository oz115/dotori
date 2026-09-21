import { router } from 'expo-router';

import { WordCatalog } from '@/features/word';

/** 부모용 경제 단어장. 아이와 똑같은 사전 화면이고, 단어 사전은 조회만 있어 읽기 전용이다. */
export default function ParentWordScreen() {
  return (
    <WordCatalog
      onSelectWord={(wordId, category) =>
        router.push({
          pathname: '/(parent)/word/[wordId]',
          params: { wordId: String(wordId), ...(category ? { category } : {}) },
        })
      }
    />
  );
}
