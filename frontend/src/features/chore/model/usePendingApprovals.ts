import { useQuery } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 부모가 처리해야 할 집안일 완료 요청(대기함). 부모 홈의 "확인해주세요"가 쓴다.
 *
 * 사진 URL 이 몇 분이면 만료되는 presigned 링크라 오래 캐시하지 않는다
 * (ChoreCompletionSummary 주석 참고).
 */
export function usePendingApprovals(familyId: number | undefined, childId: number | undefined) {
  return useQuery({
    queryKey: choreKeys.pendingRequests(familyId ?? -1, childId ?? -1),
    queryFn: () => choreApi.getCompletionRequests(familyId as number, childId as number, 'PENDING'),
    enabled: familyId != null && childId != null,
    staleTime: 10_000,
  });
}
