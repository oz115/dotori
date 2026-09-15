import { useAuthStore } from '@/features/auth';
import { ChildChoreList } from '@/features/chore';
import { useFamilyId } from '@/features/family';

/**
 * 아이 할 일 탭. familyId(family)·childId(auth 본인)는 서로 다른 슬라이스 값이라
 * 라우트가 엮어서 넘긴다.
 *
 * 상세 모달(S15P21A505-216)이 붙기 전까지 행은 눌러도 반응하지 않는다.
 */
export default function ChildTaskScreen() {
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);

  return <ChildChoreList familyId={familyId ?? undefined} childId={childId} />;
}
