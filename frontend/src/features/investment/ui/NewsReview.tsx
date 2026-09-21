import { useQuery } from '@tanstack/react-query';
import { Text, View } from 'react-native';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { changeColor } from '@/features/investment/lib/change';
import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import { investmentKeys } from '@/features/investment/model/investmentKeys';
import { useTheme } from '@/shared/theme';
import { InvestmentQueryError } from '@/features/investment/ui/InvestmentQueryError';

/** 가격 반영이 끝난 어제 뉴스의 결과. 대기 중에는 회사와 해설을 노출하지 않는다. */
export function NewsReview() {
  const theme = useTheme();
  const review = useQuery({
    queryKey: investmentKeys.newsReview(),
    queryFn: investmentApi.getNewsReview,
    retry: false,
    refetchInterval: (query) => (query.state.data === null ? false : 60_000),
  });
  const body = [theme.typography.bodySm, { color: theme.color.text.sub }];
  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={body}>
        어제 발행된 뉴스와 오늘 반영된 가격을 함께 확인해요. 오늘 뉴스의 결과는 내일 확인할 수
        있어요.
      </Text>
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
        어제 뉴스의 가격 변화
      </Text>
      {review.isPending ? (
        <Text style={body}>뉴스 결과를 불러오는 중이에요.</Text>
      ) : review.isError ? (
        <InvestmentQueryError
          error={review.error}
          fetching={review.isFetching}
          onRetry={() => review.refetch()}
        />
      ) : review.data == null ? (
        <Text style={body}>지난 뉴스 해설은 아직 이용할 수 없어요.</Text>
      ) : review.data.reviews.length === 0 ? (
        <Text style={body}>아직 돌아볼 어제 뉴스가 없어요.</Text>
      ) : (
        review.data.reviews.map((item) => (
          <View
            key={item.investmentNewsId}
            style={{
              padding: theme.space.lg,
              gap: theme.space.sm,
              borderRadius: theme.radius.lg,
              backgroundColor: theme.color.brand.subtle,
            }}
          >
            <Text style={body}>{review.data?.newsDate} 뉴스</Text>
            <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
              {item.title}
            </Text>
            <Text style={body}>{item.content}</Text>
            {item.ready && item.previousPrice != null && item.currentPrice != null ? (
              <>
                <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                  {item.productName}
                </Text>
                <Text
                  style={[
                    theme.typography.label,
                    { color: changeColor(item.currentPrice - item.previousPrice, theme.color) },
                  ]}
                >
                  {formatAcorns(item.previousPrice)} → {formatAcorns(item.currentPrice)}
                  {'\n'}
                  {item.currentPrice === item.previousPrice
                    ? '변동 없음'
                    : signedAcorns(item.currentPrice - item.previousPrice)}
                </Text>
                <Text style={body}>{item.explanation}</Text>
              </>
            ) : (
              <Text style={body}>오늘 가격에 반영되면 결과를 보여줄게요.</Text>
            )}
          </View>
        ))
      )}
      <Text style={body}>
        도토리에서는 뉴스의 영향을 다음 날 가격에 반영해요. 실제 시장에서는 여러 요인이 함께
        작용해요.
      </Text>
    </View>
  );
}
