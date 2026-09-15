import { useMemo } from 'react';

import { ParentChoreHistory } from '@/features/chore';
import { useFamilyId, useFamilyMembers } from '@/features/family';

/** 완료 이력 (부모). 할 일 목록 헤더의 "완료 이력"에서 들어온다. */
export default function ParentChoreHistoryScreen() {
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const children = useMemo(
    () =>
      (members ?? [])
        .filter((member) => member.role === 'CHILD')
        .map((member) => ({ childId: member.userId, name: member.name })),
    [members],
  );

  return <ParentChoreHistory familyId={familyId ?? undefined} childList={children} />;
}
