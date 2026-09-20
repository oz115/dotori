import { ScrollView, Text } from 'react-native';

import { useInvestmentNews } from '@/features/investment/model/useInvestmentNews';
import { InvestmentQueryError } from '@/features/investment/ui/InvestmentQueryError';
import { NewsList } from '@/features/investment/ui/NewsList';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type Props = {
  onBack: () => void;
  onPressNews: (newsId: number) => void;
};

/**
 * 오늘의 뉴스 목록 (부모). 아이가 보는 뉴스와 같은 것이라 회사·등락 정답은 보여주지 않는다.
 * 부모는 이 뉴스를 함께 읽고 아이와 이야기하는 자리라 그 안내를 위에 적었다.
 */
export function ParentInvestmentNews({ onBack, onPressNews }: Props) {
  const theme = useTheme();
  const news = useInvestmentNews();
  const muted = [theme.typography.bodySm, { color: theme.color.text.sub }];

  return (
    <Screen
      header={<Header title="오늘의 뉴스" onBack={onBack} />}
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingTop: theme.layout.screenY,
          // 탭바가 스크롤 영역 아래를 덮으므로 마지막 항목이 가려지지 않게 하단 여백을 넉넉히 둔다
          paddingBottom: theme.space['4xl'],
          gap: theme.space.lg,
        }}
      >
        <Text style={muted}>
          아이가 오늘 읽는 뉴스와 같아요. 어떤 회사에 영향을 줄지 함께 이야기해 보세요.
        </Text>
        {news.isPending ? (
          <Text style={muted}>오늘의 뉴스를 불러오는 중이에요.</Text>
        ) : news.isError ? (
          <InvestmentQueryError
            error={news.error}
            fetching={news.isFetching}
            onRetry={() => news.refetch()}
          />
        ) : news.data.length === 0 ? (
          <Text style={muted}>오늘의 뉴스가 아직 없어요.</Text>
        ) : (
          <NewsList items={news.data} onPressItem={(item) => onPressNews(item.id)} />
        )}
      </ScrollView>
    </Screen>
  );
}
