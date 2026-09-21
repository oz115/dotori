import { useMutation, useQueryClient } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';
import { familyKeys } from '@/features/family/model/familyKeys';

/**
 * 자녀를 가족에서 제외한다 (부모 전용).
 *
 * 성공하면 가족 관련 캐시를 통째로 턴다 - 구성원 목록뿐 아니라 부모 홈의 자녀별 미션·자산
 * 조회가 모두 이 목록에서 childId 를 받아 가므로, 목록만 갱신하면 나간 아이의 화면이 남는다.
 */
export function useRemoveChild(familyId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (childId: number) => familyApi.removeChild(familyId as number, childId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: familyKeys.all });
    },
  });
}
