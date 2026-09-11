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
