import { useMutation } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';

/**
 * 아이 초대 코드 발급 mutation. `POST /api/v1/families/{familyId}/invitations`.
 * 재호출하면 서버가 이전 코드를 무효화하고 새 코드를 준다 (재발급 겸용).
 */
export function useCreateInvitation() {
  return useMutation({
    mutationFn: (familyId: number) => familyApi.createInvitation(familyId),
  });
}

/**
 * 발급했던 초대 코드 폐기 mutation. `DELETE /api/v1/families/{familyId}/invitations`.
 * "나중에 초대하기"처럼 코드를 쓰지 않고 넘어갈 때, 공유되지 않은 코드가 10분간 살아 있지 않게 한다.
 */
export function useRevokeInvitation() {
  return useMutation({
    mutationFn: (familyId: number) => familyApi.revokeInvitation(familyId),
  });
}
