import { router, useLocalSearchParams } from 'expo-router';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import { ParentInvestmentTransactions } from '@/features/investment';

/** 자녀의 투자 거래 내역 (부모). 조회 대상 자녀는 메인에서 넘어온 childId 로 정한다. */
export default function ParentInvestmentTransactionsScreen() {
  const { childId: childIdParam } = useLocalSearchParams<{ childId?: string }>();
  const childId = childIdParam != null ? Number(childIdParam) : undefined;

  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);
  const childName = members?.find((member) => member.userId === childId)?.name;

  return (
    <ParentInvestmentTransactions
      childId={childId}
      childName={childName}
      onBack={() =>
        router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/investment')
      }
    />
  );
}
