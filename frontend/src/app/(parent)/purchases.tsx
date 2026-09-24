import { useFamilyId, useFamilyMembers } from '@/features/family';
import { PurchaseHistoryScreen } from '@/features/store';
import { fromDateParam } from '@/shared/lib/dateRange';

/**
 * 부모가 자녀 구매 내역을 본다 (SHOP-07). 이 앱은 가족당 자녀 1명을 가정하므로,
 * 구성원 목록에서 role이 CHILD인 사람을 그대로 대상으로 삼는다.
 */
export default function ParentPurchaseHistoryScreen() {
  const { data: familyId } = useFamilyId();
  const members = useFamilyMembers(familyId ?? undefined);
  const child = members.data?.find((m) => m.role === 'CHILD');
  // 달력 하한은 보는 사람(부모)이 아니라 내역의 주인(아이)의 합류일이어야 한다 —
  // 구매는 아이가 가족에 합류한 뒤에만 생긴다
  const childJoinedDate = child ? fromDateParam(child.joinedAt.slice(0, 10)) : undefined;

  return (
    <PurchaseHistoryScreen
      familyId={familyId ?? undefined}
      childId={child?.userId}
      minPickableDate={childJoinedDate}
    />
  );
}
