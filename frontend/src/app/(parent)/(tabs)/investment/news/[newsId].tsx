import { router, useLocalSearchParams } from 'expo-router';

import { NewsPager } from '@/features/investment';
import { Header, Screen } from '@/shared/ui';

/** 투자 뉴스 본문. 아이가 읽는 것과 같은 뉴스라 회사·등락 정답은 여기서도 보여주지 않는다. */
export default function ParentInvestmentNewsScreen() {
  const { newsId } = useLocalSearchParams<{ newsId: string }>();
  return (
    <Screen
      header={
        <Header
          title="투자 뉴스"
          onBack={() =>
            router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/investment')
          }
        />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      {/* 좌우로 밀어 오늘의 다른 기사로 넘어간다 — 스크롤·여백은 페이저가 페이지마다 갖는다 */}
      <NewsPager newsId={Number(newsId)} />
    </Screen>
  );
}
