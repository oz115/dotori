import { Text, View } from 'react-native';

import { formatNewsDate } from '@/features/investment/lib/mappers';
import { useInvestmentNewsDetail } from '@/features/investment/model/useInvestmentNewsDetail';
import { InvestmentQueryError } from '@/features/investment/ui/InvestmentQueryError';
import { useTheme } from '@/shared/theme';

/** 뉴스 본문 전용 페이지 내용. 회사·업종·등락 정답은 노출하지 않는다. */
export function NewsArticle({ newsId }: { newsId: number }) {
  const theme = useTheme();
  const valid = Number.isInteger(newsId) && newsId > 0;
  const news = useInvestmentNewsDetail(valid ? newsId : undefined);
  if (!valid) return <Text>뉴스를 찾을 수 없어요.</Text>;
  if (news.isError)
    return (
      <InvestmentQueryError
        error={news.error}
        fetching={news.isFetching}
        onRetry={() => news.refetch()}
      />
    );
  if (news.isPending) return <Text>뉴스를 불러오는 중이에요.</Text>;
  return (
    <View style={{ gap: theme.space.xl }}>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
        {formatNewsDate(news.data.publishedAt)} · 경제 소식
      </Text>
      <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>
        {news.data.title}
      </Text>
      <Text style={[theme.typography.body, { color: theme.color.text.default, lineHeight: 30 }]}>
        {news.data.content}
      </Text>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
        이 소식은 어떤 회사에 영향을 줄까요?
      </Text>
    </View>
  );
}
