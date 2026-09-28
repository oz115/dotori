import { useQuery } from '@tanstack/react-query';
import { Image, Text, View } from 'react-native';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { changeColor } from '@/features/investment/lib/change';
import { companyImage } from '@/features/investment/lib/companyAssets';
import { formatAcorns, formatNumber, signedAcorns } from '@/features/investment/lib/format';
import { formatNewsDate } from '@/features/investment/lib/mappers';
import { investmentKeys } from '@/features/investment/model/investmentKeys';
import { useTheme } from '@/shared/theme';
import { fontFamily } from '@/shared/theme/fonts';
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
      {/* 날짜는 카드마다가 아니라 여기 한 번만 — 목록 전체가 같은 날 뉴스라 카드마다
          찍으면 같은 값이 반복된다 */}
      <View
        style={{ flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between' }}
      >
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          어제 뉴스의 가격 변화
        </Text>
        {review.data != null && review.data.reviews.length > 0 ? (
          <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>
            {formatNewsDate(review.data.newsDate)}
          </Text>
        ) : null}
      </View>
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
              gap: theme.space.md,
              borderRadius: theme.radius.lg,
              backgroundColor: theme.color.brand.subtle,
            }}
          >
            <View style={{ gap: theme.space.xs }}>
              {/* 카드 안에서 회사 이름 다음으로 먼저 읽히는 값이라 굵게 — 원래 label
                  weight(medium)로는 바로 아래 본문과 무게감이 거의 같아서 안 두드러졌다 */}
              <Text
                style={[
                  theme.typography.label,
                  { color: theme.color.text.strong, fontFamily: fontFamily.bold },
                ]}
              >
                {item.title}
              </Text>
              <Text style={body}>{item.content}</Text>
            </View>

            {item.ready && item.previousPrice != null && item.currentPrice != null ? (
              <>
                {/* 가격 변화는 흰 박스로 따로 — 숫자와 해설 글이 한 덩어리로 붙으면 안 읽힌다.
                    어느 회사인지는 글자("OO 가격 변화")보다 주식 목록 카드(StockCard)와 같은
                    이미지+이름이 훨씬 눈에 띄어서, 왼쪽엔 회사를 왼쪽 정렬로 두고
                    오른쪽엔 가격 변동을 오른쪽 정렬로 둔 양쪽 정렬 한 줄로 바꾼다 */}
                <View
                  style={{
                    flexDirection: 'row',
                    alignItems: 'center',
                    gap: theme.space.sm,
                    backgroundColor: theme.color.background.surface,
                    borderRadius: theme.radius.md,
                    padding: theme.space.md,
                  }}
                >
                  <Image
                    source={companyImage(item.productName ?? '')}
                    style={{ width: 40, height: 40, borderRadius: theme.radius.md }}
                    resizeMode="cover"
                  />
                  <Text
                    numberOfLines={1}
                    style={[theme.typography.label, { flex: 1, color: theme.color.text.strong }]}
                  >
                    {item.productName}
                  </Text>
                  <View style={{ alignItems: 'flex-end', gap: theme.space.xs }}>
                    <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>
                      {/* 단위는 뒤에 한 번만 — 양쪽에 다 붙이면 좁은 화면에서 박스를 넘친다 */}
                      {formatNumber(item.previousPrice)} → {formatAcorns(item.currentPrice)}
                    </Text>
                    <Text
                      style={[
                        theme.typography.label,
                        { color: changeColor(item.currentPrice - item.previousPrice, theme.color) },
                      ]}
                    >
                      {item.currentPrice === item.previousPrice
                        ? '변동 없음'
                        : signedAcorns(item.currentPrice - item.previousPrice)}
                    </Text>
                  </View>
                </View>

                {item.explanation ? (
                  // 가격 변화(흰 박스)와 다른 색으로 — 숫자 사실과 해설(왜 그런지)이
                  // 같은 흰 박스면 성격이 다른 정보라는 게 안 읽힌다
                  <View
                    style={{
                      backgroundColor: theme.color.status.warning.subtle,
                      borderRadius: theme.radius.md,
                      padding: theme.space.md,
                      gap: theme.space.xs,
                    }}
                  >
                    <Text
                      style={[theme.typography.label, { color: theme.color.status.warning.text }]}
                    >
                      🤔 왜 그랬을까요?
                    </Text>
                    <Text style={body}>{item.explanation}</Text>
                  </View>
                ) : null}
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
