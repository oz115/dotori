import { useMutation, useQueryClient } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { walletKeys } from '@/features/wallet/model/walletKeys';
import { newIdempotencyKey } from '@/shared/lib/idempotencyKey';

/**
 * 환전 승인 (부모). 부모 계좌에서 자녀 계좌로 실제 송금이 일어나는, 되돌릴 수 없는 요청이라
 * 멱등키를 붙인다 — 재시도로 두 번 가도 서버가 한 번만 보낸다.
 *
 * 승인하면 요청 상태가 바뀌고(PENDING → SUCCEED) 자녀 포인트는 요청 시점에 이미 빠져
 * 있으므로, 목록과 잔액 캐시를 함께 턴다. 부모 계좌 잔액은 이 feature 가 모르는
 * 슬라이스라 여기서 안 건드린다 — 호출하는 라우트가 따로 처리한다.
 */
export function useApproveCashout(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (cashoutRequestId: number) => {
      if (familyId == null || childId == null) {
        return Promise.reject(new Error('familyId/childId 가 없습니다'));
      }
      return walletApi.approveCashout(familyId, childId, cashoutRequestId, newIdempotencyKey());
    },
    // 성공이든 실패든 목록을 다시 받는다. 승인 도중 송금이 실패하면 서버가 포인트를
    // 환급하고 FAILED 로 끝내는데, 그것도 화면에 반영되어야 한다.
    onSettled: () => {
      if (familyId == null || childId == null) return;
      void queryClient.invalidateQueries({
        queryKey: walletKeys.cashoutRequestsOf(familyId, childId),
      });
      void queryClient.invalidateQueries({ queryKey: walletKeys.balance(familyId, childId) });
    },
  });
}
