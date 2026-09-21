import { useMutation, useQueryClient } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import type { GrantBody } from '@/features/wallet/model/types';
import { walletKeys } from '@/features/wallet/model/walletKeys';

type GrantVars = {
  body: GrantBody;
  /** 같은 지급 의도(같은 금액)로 다시 보낼 때는 같은 키를 쓴다 — 서버가 한 번만 반영한다 */
  idempotencyKey: string;
};

/**
 * 부모가 자녀에게 도토리를 준다. 성공하면 그 자녀의 잔액 캐시를 무효화해 다음 조회에서 서버 값을
 * 다시 받는다 — 금융 mutation 은 낙관적으로 미리 반영하지 않는다.
 * 지급 전용 화면은 캐시된 잔액 위에 새로 그려지므로, 부모 홈의 "자녀 보유 도토리"도 이 무효화로 갱신된다.
 */
export function useGrantPoints(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ body, idempotencyKey }: GrantVars) => {
      if (familyId == null || childId == null) {
        return Promise.reject(new Error('familyId/childId 가 없습니다'));
      }
      return walletApi.grant(familyId, childId, body, idempotencyKey);
    },
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      void queryClient.invalidateQueries({ queryKey: walletKeys.balance(familyId, childId) });
    },
  });
}
