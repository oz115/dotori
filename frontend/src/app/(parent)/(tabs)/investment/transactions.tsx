import { router, useLocalSearchParams } from 'expo-router';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import { ParentInvestmentTransactions } from '@/features/investment';
import { fromDateParam } from '@/shared/lib/dateRange';

/** 자녀의 투자 거래 내역 (부모). 조회 대상 자녀는 메인에서 넘어온 childId 로 정한다. */
export default function ParentInvestmentTransactionsScreen() {
  const { childId: childIdParam } = useLocalSearchParams<{ childId?: string }>();
  const childId = childIdParam != null ? Number(childIdParam) : undefined;

  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);
  const child = members?.find((member) => member.userId === childId);
  // 달력 하한은 보는 사람(부모)이 아니라 내역의 주인(아이)의 합류일이어야 한다 —
  // 거래는 아이가 가족에 합류한 뒤에만 생긴다
  const childJoinedDate = child ? fromDateParam(child.joinedAt.slice(0, 10)) : undefined;

  return (
    <ParentInvestmentTransactions
      childId={childId}
      childName={child?.name}
      minPickableDate={childJoinedDate}
      onBack={() =>
        router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/investment')
      }
    />
  );
}
