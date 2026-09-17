import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { SavingsHome } from '@/features/savings';

/**
 * 자녀 저축 탭. 예적금 API 가 (familyId, childId) 경로 스타일이라
 * family(가족)와 auth(본인 userId)를 라우트가 엮어서 넘긴다.
 * 상세 화면 태스크 전이라 카드 탭 이동(onPressCard)은 아직 연결하지 않는다.
 */
export default function ChildSaveScreen() {
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);

  return <SavingsHome familyId={familyId ?? undefined} childId={childId} />;
}
