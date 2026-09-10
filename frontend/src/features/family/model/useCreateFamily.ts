import { useMutation, useQueryClient } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';
import { familyKeys } from '@/features/family/model/familyKeys';
import { savedFamilyId } from '@/features/family/lib/savedFamilyId';
import type { FamilyCreateBody } from '@/features/family/model/types';

/**
 * 가족 생성(온보딩) mutation. `POST /api/v1/families`.
 * 성공하면 familyId 를 로컬에 저장하고 "가족 소속 여부" 캐시를 갱신한다.
 */
export function useCreateFamily() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: FamilyCreateBody) => familyApi.create(body),
    onSuccess: (res) => {
      void savedFamilyId.set(res.familyId);
      queryClient.setQueryData(familyKeys.status(), { hasFamily: true });
      queryClient.invalidateQueries({ queryKey: familyKeys.all });
    },
  });
}
