import { useInfiniteQuery } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { walletKeys } from '@/features/wallet/model/walletKeys';

const PAGE_SIZE = 30;

/**
 * 포인트 거래내역 (WLT-06), 커서 기반 무한 스크롤. 기간(range)은 서버가 거른다.
 * range 가 null 이면 전체 기간이다. 입금/출금 방향 필터는 서버 파라미터가 사유 단위라
 * 여기서 걸지 않고 화면이 받은 목록에서 거른다.
 */
export function usePointTransactions(
  familyId: number | undefined,
  childId: number | undefined,
  range: { startDate: string; endDate: string } | null,
) {
  return useInfiniteQuery({
    queryKey: walletKeys.transactions(familyId ?? -1, childId ?? -1, range),
    queryFn: ({ pageParam }) =>
      walletApi.getTransactions(familyId as number, childId as number, {
        ...(range ?? {}),
        cursor: pageParam ?? undefined,
        size: PAGE_SIZE,
      }),
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => (lastPage.page.hasNext ? lastPage.page.nextCursor : undefined),
    enabled: familyId != null && childId != null,
    staleTime: 30_000,
  });
}
