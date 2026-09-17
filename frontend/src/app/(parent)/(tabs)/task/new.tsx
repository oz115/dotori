import { useMemo } from 'react';

import { ParentChoreForm } from '@/features/chore';
import { useFamilyId, useFamilyMembers } from '@/features/family';

/** 할 일 등록. 부모 할 일 목록의 등록 배너에서 들어온다. */
export default function ParentChoreCreateScreen() {
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const children = useMemo(
    () =>
      (members ?? [])
        .filter((member) => member.role === 'CHILD')
        .map((member) => ({ childId: member.userId, name: member.name })),
    [members],
  );

  return <ParentChoreForm familyId={familyId ?? undefined} childList={children} />;
}
