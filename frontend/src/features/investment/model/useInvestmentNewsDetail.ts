import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/** 뉴스 상세 — 목록에 없는 본문(content)을 팝업이 열릴 때 가져온다. */
export function useInvestmentNewsDetail(newsId: number | undefined) {
  return useQuery({
    queryKey: investmentKeys.newsDetail(newsId ?? -1),
    queryFn: () => investmentApi.getNewsDetail(newsId as number),
    enabled: newsId != null,
    retry: false,
    staleTime: 5 * 60_000,
  });
}
