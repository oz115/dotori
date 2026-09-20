import { router, useLocalSearchParams } from 'expo-router';
import { ScrollView } from 'react-native';

import { NewsArticle } from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

export default function InvestmentNewsScreen() {
  const { newsId } = useLocalSearchParams<{ newsId: string }>();
  const theme = useTheme();
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
      <ScrollView
        contentContainerStyle={{ padding: theme.layout.screenX, paddingBottom: theme.space['4xl'] }}
      >
        <NewsArticle newsId={Number(newsId)} />
      </ScrollView>
    </Screen>
  );
}
