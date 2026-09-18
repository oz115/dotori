import { useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';

import { accountKeys } from '@/features/account';
import { useAuthStore } from '@/features/auth';
import { useFamilyId, useFamilyMembers } from '@/features/family';
import { ParentCashoutRequestList } from '@/features/wallet';

/**
 * 자녀 환전 요청 목록 (부모). 부모 홈의 도토리 카드 "환전하기"에서 들어온다.
 *
 * 승인하면 내 계좌에서 아이 계좌로 실제 송금이 일어나는데, wallet feature 는 계좌
 * 슬라이스를 모른다. 그 경계를 잇는 것이 라우트의 몫이라 여기서 계좌 캐시를 턴다
 * (완료 요청 화면이 지갑 잔액을 터는 것과 같은 방식).
 */
export default function ParentCashoutRequestsScreen() {
  const queryClient = useQueryClient();
  const viewerId = useAuthStore((state) => state.user?.userId);
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
    <ParentCashoutRequestList
      familyId={familyId ?? undefined}
      childList={children}
      viewerId={viewerId}
      onApproved={() => {
        queryClient.invalidateQueries({ queryKey: accountKeys.all });
      }}
    />
  );
}
