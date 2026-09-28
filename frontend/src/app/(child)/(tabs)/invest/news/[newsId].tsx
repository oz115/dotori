import { router, useLocalSearchParams } from 'expo-router';

import { NewsPager } from '@/features/investment';
import { Header, Screen } from '@/shared/ui';

export default function InvestmentNewsScreen() {
  const { newsId } = useLocalSearchParams<{ newsId: string }>();
  return (
    <Screen
      header={
        <Header
          title="투자 뉴스"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/invest/index'))}
        />
      }
      edges={[]}
      padded={false}
    >
      {/* 좌우로 밀어 오늘의 다른 기사로 넘어간다 — 스크롤·여백은 페이저가 페이지마다 갖는다 */}
      <NewsPager newsId={Number(newsId)} />
    </Screen>
  );
}
