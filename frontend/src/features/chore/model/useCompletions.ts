import { useQuery } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 승인·지급까지 끝난 완료 내역. 기간을 안 넘기면 서버가 최근 30일로 잡는다.
 *
 * 이 응답은 완료 요청이 아니라 포인트 원장에서 온다 - 금액이 그때 실제로 지급된 값이라
 * 부모가 나중에 보상을 고쳐도 과거 이력이 흔들리지 않는다. 대신 요청 ID 와 사진이 없어서
 * 여기서 상세로는 넘어갈 수 없다.
 */
export function useCompletions(
  familyId: number | undefined,
  childId: number | undefined,
  startDate?: string,
  endDate?: string,
) {
  return useQuery({
    queryKey: choreKeys.completions(familyId ?? -1, childId ?? -1, startDate ?? '', endDate ?? ''),
    queryFn: () =>
      choreApi.getCompletions(familyId as number, childId as number, startDate, endDate),
    enabled: familyId != null && childId != null,
  });
}
