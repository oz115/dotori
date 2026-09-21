import { useQueryClient } from '@tanstack/react-query';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useMemo } from 'react';

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
  const { viaDeepLink } = useLocalSearchParams<{ viaDeepLink?: string }>();

  // task 탭 밖(알림)에서 곧장 이 화면으로 열리면, 이 탭의 내부 스택에 요청 목록 하나만
  // 쌓여서 뒤로가기·탭 전환이 깨진다. 상점 상세와 같은 방식으로, 할 일 목록에 "요청 목록을
  // 다시 열어달라"는 파라미터를 실어 replace하고 재-push는 언마운트되지 않는 목록 쪽이 한다.
  useEffect(() => {
    if (viaDeepLink !== '1') return;

    router.replace({ pathname: '/(parent)/(tabs)/task', params: { openRequests: '1' } });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 최초 마운트 시 1회만
  }, []);

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
