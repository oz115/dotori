import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { ChildCashoutRequestList } from '@/features/wallet';

/**
 * 내 환전 요청 내역 (자녀). 홈 도토리 카드의 "환전 내역"에서 들어온다.
 *
 * 조회 경로가 familyId·childId 를 요구하는데 둘 다 다른 슬라이스(family·auth) 값이라
 * 라우트가 모아서 넘긴다 (환전 요청 화면과 같은 방식).
 */
export default function ChildCashoutRequestsScreen() {
  const familyId = useFamilyId().data ?? undefined;
  const childId = useAuthStore((state) => state.user?.userId);

  return <ChildCashoutRequestList familyId={familyId} childId={childId} />;
}
