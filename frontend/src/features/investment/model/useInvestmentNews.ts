import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { toInvestmentNews } from '@/features/investment/lib/mappers';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/** 오늘의 투자 뉴스 (하루 3건, 00:05 KST 발행). 본문은 팝업이 상세 API 로 채운다. */
export function useInvestmentNews() {
  return useQuery({
    queryKey: investmentKeys.news(),
    queryFn: () => investmentApi.getNews(),
    select: (news) => news.map(toInvestmentNews),
    staleTime: 60_000,
  });
}
