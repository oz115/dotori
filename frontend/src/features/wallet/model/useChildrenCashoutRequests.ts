import { useQueries } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { walletKeys } from '@/features/wallet/model/walletKeys';
import type { CashoutRequest } from '@/features/wallet/model/types';

/** 목록 화면이 자녀를 가려내는 데 쓰는 최소 정보. 가족 구성원 목록에서 만들어 넘긴다 */
export type CashoutChild = { childId: number; name: string };

/** 응답에 자녀 이름이 없어서(childId 만 온다) 화면이 쓸 이름을 붙여 둔다 */
export type ParentCashoutRequest = CashoutRequest & { childName: string };

/**
 * 부모가 보는 환전 요청 목록. 조회 경로가 `/children/{childId}/...` 라 자녀별로 부르고
 * 합친다 (`useChildrenPendingRequests` 와 같은 이유).
 *
 * 상태 필터를 서버에 넘기지 않고 전체를 받아 화면에서 가른다 — 대기 중과 지난 내역을
 * 한 화면에 같이 보여주므로 상태마다 따로 부르면 자녀 수 × 상태 수만큼 요청이 늘어난다.
 */
export function useChildrenCashoutRequests(familyId: number | undefined, children: CashoutChild[]) {
  return useQueries({
    queries: children.map((child) => ({
      queryKey: walletKeys.cashoutRequests(familyId ?? -1, child.childId),
      queryFn: () => walletApi.getCashoutRequests(familyId as number, child.childId),
      enabled: familyId != null,
      staleTime: 10_000,
    })),
    combine: (results) => ({
      items: results.flatMap((result, index) =>
        (result.data ?? []).map<ParentCashoutRequest>((request) => ({
          ...request,
          childName: children[index].name,
        })),
      ),
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
      refetch: () => Promise.all(results.map((result) => result.refetch())),
    }),
  });
}
