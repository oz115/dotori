import { useInfiniteQuery } from '@tanstack/react-query';

import { accountApi } from '@/features/account/api/accountApi';
import { accountKeys } from '@/features/account/model/accountKeys';
import type { TransactionDirection, TransactionRangeFilter } from '@/features/account/model/types';

/**
 * 연동 계좌 거래내역, 커서 기반 무한 스크롤. `GET /api/v1/accounts/transactions`.
 *
 * 기간(상대·직접 지정)·유형 필터가 바뀌면 서버가 완전히 다른 페이지 집합을 돌려주므로
 * 쿼리 키에 포함해 TanStack Query가 필터마다 별도로 캐시·페이지네이션하게 한다.
 */
export function useAccountTransactions(
  range: TransactionRangeFilter,
  transactionType: TransactionDirection | undefined,
) {
  return useInfiniteQuery({
    queryKey: accountKeys.transactions(range, transactionType),
    queryFn: ({ pageParam }) =>
      accountApi.getTransactions({
        ...(range.mode === 'period'
          ? { period: range.period }
          : { startDate: range.startDate, endDate: range.endDate }),
        transactionType,
        cursor: pageParam,
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined,
  });
}
