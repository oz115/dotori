import { useMutation, useQueryClient } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';
import { familyKeys } from '@/features/family/model/familyKeys';
import type { FamilyCreateBody } from '@/features/family/model/types';

/**
 * 가족 생성(온보딩) mutation. `POST /api/v1/families`.
 * 성공하면 "내 가족" 캐시를 무효화해 온보딩 게이트가 새 상태를 다시 읽게 한다.
 */
export function useCreateFamily() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: FamilyCreateBody) => familyApi.create(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: familyKeys.all });
    },
  });
}
