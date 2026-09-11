import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';
import { familyKeys } from '@/features/family/model/familyKeys';
import type { FamilyUpdateBody } from '@/features/family/model/types';

/** 가족 정보(이름·온보딩 설정) 조회. */
export function useFamily(familyId: number | undefined) {
  return useQuery({
    queryKey: familyKeys.detail(familyId ?? -1),
    queryFn: () => familyApi.get(familyId as number),
    enabled: familyId != null,
    staleTime: 60_000,
  });
}

/** 가족 이름·설정 수정. 성공하면 가족 관련 캐시를 다시 읽는다. */
export function useUpdateFamily(familyId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: FamilyUpdateBody) => {
      if (familyId == null) return Promise.reject(new Error('familyId 가 없습니다'));
      return familyApi.update(familyId, body);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: familyKeys.all });
    },
  });
}
