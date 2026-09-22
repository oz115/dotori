import { useSignupDate } from '@/features/auth';
import { useFamilyId, useFamilyMembers } from '@/features/family';
import { PurchaseHistoryScreen } from '@/features/store';

/**
 * 부모가 자녀 구매 내역을 본다 (SHOP-07). 이 앱은 가족당 자녀 1명을 가정하므로,
 * 구성원 목록에서 role이 CHILD인 사람을 그대로 대상으로 삼는다.
 */
export default function ParentPurchaseHistoryScreen() {
  const { data: familyId } = useFamilyId();
  const members = useFamilyMembers(familyId ?? undefined);
  const childId = members.data?.find((m) => m.role === 'CHILD')?.userId;
  const signupDate = useSignupDate();

  return (
    <PurchaseHistoryScreen
      familyId={familyId ?? undefined}
      childId={childId}
      minPickableDate={signupDate}
    />
  );
}
