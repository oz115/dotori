import { router, useLocalSearchParams } from 'expo-router';
import { ScrollView } from 'react-native';

import { NewsArticle } from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

/** 투자 뉴스 본문. 아이가 읽는 것과 같은 뉴스라 회사·등락 정답은 여기서도 보여주지 않는다. */
export default function ParentInvestmentNewsScreen() {
  const { newsId } = useLocalSearchParams<{ newsId: string }>();
  const theme = useTheme();
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
      <ScrollView
        contentContainerStyle={{ padding: theme.layout.screenX, paddingBottom: theme.space['4xl'] }}
      >
        <NewsArticle newsId={Number(newsId)} />
      </ScrollView>
    </Screen>
  );
}
