import { router } from 'expo-router';
import { ScrollView } from 'react-native';

import { NewsReview } from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

/** 지난 뉴스 해설. 아이가 보는 것과 같은 내용이라 부모도 함께 이야기할 수 있다. */
export default function ParentInvestmentNewsReviewScreen() {
  const theme = useTheme();
  return (
    <Screen
      header={
        <Header
          title="지난 뉴스 해설"
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
        <NewsReview />
      </ScrollView>
    </Screen>
  );
}
