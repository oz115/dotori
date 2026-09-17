import { useQuery } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 이 할 일에 아이가 보내 놓은 대기 중 요청. 상세 모달이 "확인 중" 화면을 그릴 때 쓴다.
 *
 * 목록과 같은 쿼리 키를 쓰고 select 로 한 건만 고른다 - 별도 키를 만들면 같은 데이터를
 * 두 번 받아 오고, 취소 후 무효화도 두 곳을 챙겨야 한다.
 *
 * 사진 URL 이 몇 분이면 만료되는 presigned 링크라 오래 캐시하지 않는다.
 */
export function usePendingCompletionRequest(
  familyId: number | undefined,
  childId: number | undefined,
  choreId: number | undefined,
) {
  return useQuery({
    queryKey: choreKeys.completionRequests(familyId ?? -1, childId ?? -1, 'PENDING'),
    queryFn: () => choreApi.getCompletionRequests(familyId as number, childId as number, 'PENDING'),
    enabled: familyId != null && childId != null,
    staleTime: 10_000,
    select: (requests) => requests.find((request) => request.choreId === choreId) ?? null,
  });
}
