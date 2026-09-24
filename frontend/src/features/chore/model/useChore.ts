import { useQuery } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 집안일 상세. 부모 상세·수정 화면과, 완료 요청 상세에서 보상·기간을 채울 때 쓴다
 * (완료 요청 목록 한 줄에는 choreId 만 있고 보상·기간이 없다).
 */
export function useChore(
  familyId: number | undefined,
  childId: number | undefined,
  choreId: number | undefined,
) {
  return useQuery({
    queryKey: choreKeys.detail(familyId ?? -1, childId ?? -1, choreId ?? -1),
    queryFn: () => choreApi.getChore(familyId as number, childId as number, choreId as number),
    enabled: familyId != null && childId != null && choreId != null,
  });
}
