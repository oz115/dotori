import { useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';

import { ParentCompletionRequestList } from '@/features/chore';
import { useFamilyId, useFamilyMembers } from '@/features/family';
import { walletKeys } from '@/features/wallet';

/**
 * 완료 요청 목록 (부모). 할 일 목록의 "전체보기"에서 들어온다.
 *
 * 승인하면 아이 지갑에 도토리가 들어가는데, chore feature 는 지갑 슬라이스를 모른다.
 * 그 경계를 잇는 것이 라우트의 몫이라 여기서 잔액 캐시를 턴다 (상점 구매와 같은 방식).
 */
export default function ParentCompletionRequestsScreen() {
  const queryClient = useQueryClient();
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const children = useMemo(
    () =>
      (members ?? [])
        .filter((member) => member.role === 'CHILD')
        .map((member) => ({ childId: member.userId, name: member.name })),
    [members],
  );

  return (
    <ParentCompletionRequestList
      familyId={familyId ?? undefined}
      childList={children}
      onApproved={(childId) => {
        if (familyId == null) return;
        queryClient.invalidateQueries({ queryKey: walletKeys.balance(familyId, childId) });
      }}
    />
  );
}
