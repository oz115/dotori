import { router } from 'expo-router';

import { ParentInvestmentNews } from '@/features/investment';

/** 오늘의 뉴스 목록 (부모). 뉴스를 누르면 본문 페이지로 넘어간다. */
export default function ParentInvestmentNewsListScreen() {
  return (
    <ParentInvestmentNews
      onBack={() =>
        router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/investment')
      }
      onPressNews={(newsId) =>
        router.push({
          pathname: '/(parent)/(tabs)/investment/news/[newsId]',
          params: { newsId: String(newsId) },
        })
      }
    />
  );
}
