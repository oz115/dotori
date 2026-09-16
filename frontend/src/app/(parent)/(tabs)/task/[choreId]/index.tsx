import { router, useLocalSearchParams } from 'expo-router';
import { useMemo } from 'react';

import { ParentChoreDetail } from '@/features/chore';
import { useFamilyId, useFamilyMembers } from '@/features/family';

/**
 * 할 일 상세 (부모). 목록에서 행을 누르면 들어온다.
 *
 * childId 를 쿼리로 함께 받는다. 조회 경로가 /children/{childId}/chores/{choreId} 라
 * choreId 만으로는 부를 수 없고, 목록이 이미 아는 값이라 다시 찾을 필요가 없다.
 *
 * 수정·삭제 화면은 아직 없어서 콜백을 넘기지 않는다 - 그동안 헤더에 버튼이 뜨지 않는다.
 */
export default function ParentChoreDetailScreen() {
  const { choreId, childId } = useLocalSearchParams<{ choreId: string; childId: string }>();
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const childName = useMemo(
    () => (members ?? []).find((member) => String(member.userId) === childId)?.name,
    [members, childId],
  );

  return (
    <ParentChoreDetail
      familyId={familyId ?? undefined}
      childId={Number(childId) || undefined}
      choreId={Number(choreId) || undefined}
      childName={childName}
      onEdit={() =>
        router.push({
          pathname: '/task/[choreId]/edit',
          params: { choreId: String(choreId), childId: String(childId) },
        })
      }
    />
  );
}
