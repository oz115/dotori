import { useLocalSearchParams } from 'expo-router';

import { ParentChoreEditForm } from '@/features/chore';
import { useFamilyId } from '@/features/family';

/** 할 일 수정. 상세 헤더의 "수정"과 목록 더보기의 "수정하기"에서 들어온다. */
export default function ParentChoreEditScreen() {
  const { choreId, childId } = useLocalSearchParams<{ choreId: string; childId: string }>();
  const { data: familyId } = useFamilyId();

  return (
    <ParentChoreEditForm
      familyId={familyId ?? undefined}
      childId={Number(childId) || undefined}
      choreId={Number(choreId) || undefined}
    />
  );
}
