import { useMutation, useQueryClient } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';
import { familyKeys } from '@/features/family/model/familyKeys';

/**
 * 자녀가 초대 코드로 가족에 합류하는 mutation. `POST /api/v1/families/connect`.
 * 성공하면 familyId 를 저장하고 "가족 소속 여부" 캐시를 갱신한다.
 */
export function useConnectFamily() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) => familyApi.connect(code.trim().toUpperCase()),
    onSuccess: (res) => {
      queryClient.setQueryData(familyKeys.status(), {
        hasFamily: true,
        familyId: res.familyId,
      });
      queryClient.invalidateQueries({ queryKey: familyKeys.all });
    },
  });
}
