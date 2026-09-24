import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/** 종목 가격 이력 (차트용, 서버 기본 최근 30일·시간 오름차순). */
export function usePriceHistory(productId: number | undefined) {
  return useQuery({
    queryKey: investmentKeys.priceHistory(productId ?? -1),
    queryFn: () => investmentApi.getPriceHistory(productId as number),
    enabled: productId != null,
    retry: false,
    staleTime: 60_000,
  });
}
