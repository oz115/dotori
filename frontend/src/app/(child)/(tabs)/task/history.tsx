import { useAuthStore } from '@/features/auth';
import { ChildChoreHistory } from '@/features/chore';
import { useFamilyId } from '@/features/family';

/** 아이 완료 이력. 할 일 목록 헤더의 "완료 이력"에서 들어온다. */
export default function ChildTaskHistoryScreen() {
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);

  return <ChildChoreHistory familyId={familyId ?? undefined} childId={childId} />;
}
