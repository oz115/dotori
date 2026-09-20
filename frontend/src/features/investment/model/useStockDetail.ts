import { useMemo } from 'react';

import { toStockDetail } from '@/features/investment/lib/mappers';
import { useInvestmentHoldings } from '@/features/investment/model/useInvestmentHoldings';
import { useInvestmentProduct } from '@/features/investment/model/useInvestmentProduct';
import { usePriceHistory } from '@/features/investment/model/usePriceHistory';

/**
 * 상세 화면용 StockDetail 조합 — 상세 응답 + 가격 이력(차트) + 보유 여부(배지)
 * 세 조회를 묶는다. 상세·차트의 로딩과 오류를 따로 표시하고 임시 가격을 만들지 않는다.
 * 보유 여부는 childId 없으면 본인 기준이고, 부모가 특정 자녀의 보유를 볼 때만 childId 를 넘긴다.
 */
export function useStockDetail(productId: number | undefined, childId?: number) {
  const product = useInvestmentProduct(productId);
  const history = usePriceHistory(productId);
  const holdings = useInvestmentHoldings(childId);

  const detail = useMemo(() => {
    if (product.data == null) {
      return undefined;
    }
    const holding = (holdings.data ?? []).find((h) => h.id === product.data.investmentProductId);
    return {
      ...toStockDetail(product.data, history.data ?? [], holding != null),
      buyPrice: holding?.buyPrice,
    };
  }, [product.data, history.data, holdings.data]);

  return {
    detail,
    holding: detail?.holding ?? false,
    historyFetching: history.isFetching,
    historyPending: history.isPending,
    historyError: history.isError,
    refetchHistory: history.refetch,
    isPending: product.isPending,
    isError: product.isError,
    refetch: product.refetch,
  };
}
