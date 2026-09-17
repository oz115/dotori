import { useMutation, useQueryClient } from '@tanstack/react-query';

import { savingsApi } from '@/features/savings/api/savingsApi';
import { savingsKeys } from '@/features/savings/model/savingsKeys';
import { newIdempotencyKey } from '@/shared/lib/idempotencyKey';

/**
 * 포인트가 움직이는 예적금 mutation 4종 (가입·납입·수령·해지).
 * 성공하면 savings 캐시 전체를 무효화한다 — 가입 목록·거래 이력·요약이 전부 바뀌기 때문.
 * 금융 mutation 이라 낙관적 업데이트는 하지 않고, 요청마다 새 멱등키를 만들어 재시도 이중 처리를 막는다.
 */

function useInvalidateSavings() {
  const queryClient = useQueryClient();
  return () => void queryClient.invalidateQueries({ queryKey: savingsKeys.all });
}

function requireIds(familyId: number | undefined, childId: number | undefined) {
  if (familyId == null || childId == null) {
    throw new Error('familyId/childId 가 없습니다');
  }
  return { familyId, childId };
}

export function useSubscribeSavings(familyId: number | undefined, childId: number | undefined) {
  const invalidate = useInvalidateSavings();
  return useMutation({
    mutationFn: (body: { savingsProductId: number; amount: number }) => {
      const ids = requireIds(familyId, childId);
      return savingsApi.subscribe(ids.familyId, ids.childId, body, newIdempotencyKey());
    },
    onSuccess: invalidate,
  });
}

export function useContributeSavings(familyId: number | undefined, childId: number | undefined) {
  const invalidate = useInvalidateSavings();
  return useMutation({
    mutationFn: (input: { savingsSubscriptionId: number; amount: number }) => {
      const ids = requireIds(familyId, childId);
      return savingsApi.contribute(
        ids.familyId,
        ids.childId,
        input.savingsSubscriptionId,
        { amount: input.amount },
        newIdempotencyKey(),
      );
    },
    onSuccess: invalidate,
  });
}

export function useReceiveSavings(familyId: number | undefined, childId: number | undefined) {
  const invalidate = useInvalidateSavings();
  return useMutation({
    mutationFn: (savingsSubscriptionId: number) => {
      const ids = requireIds(familyId, childId);
      return savingsApi.receive(
        ids.familyId,
        ids.childId,
        savingsSubscriptionId,
        newIdempotencyKey(),
      );
    },
    onSuccess: invalidate,
  });
}

export function useTerminateSavings(familyId: number | undefined, childId: number | undefined) {
  const invalidate = useInvalidateSavings();
  return useMutation({
    mutationFn: (savingsSubscriptionId: number) => {
      const ids = requireIds(familyId, childId);
      return savingsApi.terminate(
        ids.familyId,
        ids.childId,
        savingsSubscriptionId,
        newIdempotencyKey(),
      );
    },
    onSuccess: invalidate,
  });
}
