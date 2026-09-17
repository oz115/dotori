import { useMemo } from 'react';

import { toStockDetail } from '@/features/investment/lib/mappers';
import { useInvestmentHoldings } from '@/features/investment/model/useInvestmentHoldings';
import { useInvestmentProduct } from '@/features/investment/model/useInvestmentProduct';
import { usePriceHistory } from '@/features/investment/model/usePriceHistory';

/**
 * 상세 화면용 StockDetail 조합 — 상세 응답 + 가격 이력(차트) + 보유 여부(배지)
 * 세 조회를 묶는다. 상세가 있으면 이력이 늦어도 현재가 한 점으로 먼저 그린다.
 */
export function useStockDetail(productId: number | undefined) {
  const product = useInvestmentProduct(productId);
  const history = usePriceHistory(productId);
  const holdings = useInvestmentHoldings();

  const detail = useMemo(() => {
    if (product.data == null) {
      return undefined;
    }
    const holding = (holdings.data ?? []).some((h) => h.id === product.data.investmentProductId);
    return toStockDetail(product.data, history.data ?? [], holding);
  }, [product.data, history.data, holdings.data]);

  return {
    detail,
    holding: detail?.holding ?? false,
    isPending: product.isPending,
    isError: product.isError,
    refetch: product.refetch,
  };
}
