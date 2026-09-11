import { useMutation, useQueryClient } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';
import { familyKeys } from '@/features/family/model/familyKeys';
import { savedFamilyId } from '@/features/family/lib/savedFamilyId';

/**
 * 가족 삭제 mutation. `DELETE /api/v1/families/{familyId}`.
 * 성공하면 저장된 familyId 를 지우고 "가족 없음" 으로 캐시를 되돌린다.
 * (현재는 온보딩 재진행 테스트용 임시 기능)
 */
export function useDeleteFamily() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (familyId: number) => familyApi.remove(familyId),
    onSuccess: () => {
      void savedFamilyId.clear();
      queryClient.setQueryData(familyKeys.status(), { hasFamily: false });
      queryClient.invalidateQueries({ queryKey: familyKeys.all });
    },
  });
}
