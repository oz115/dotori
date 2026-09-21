import { router } from 'expo-router';
import { ScrollView } from 'react-native';

import { NewsReview } from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

export default function InvestmentNewsReviewScreen() {
  const theme = useTheme();
  return (
    <Screen
      header={
        <Header
          title="지난 뉴스 해설"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/invest/index'))}
        />
      }
      edges={[]}
      padded={false}
    >
      <ScrollView
        contentContainerStyle={{ padding: theme.layout.screenX, paddingBottom: theme.space['4xl'] }}
      >
        <NewsReview />
      </ScrollView>
    </Screen>
  );
}
