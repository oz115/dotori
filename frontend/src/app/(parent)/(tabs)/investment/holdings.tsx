import { router, useLocalSearchParams } from 'expo-router';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import { ParentInvestmentHoldings } from '@/features/investment';

/** 자녀의 보유 주식 전체 (부모). 조회 대상 자녀는 메인에서 넘어온 childId 로 정한다. */
export default function ParentInvestmentHoldingsScreen() {
  const { childId: childIdParam } = useLocalSearchParams<{ childId?: string }>();
  const childId = childIdParam != null ? Number(childIdParam) : undefined;

  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);
  const childName = members?.find((member) => member.userId === childId)?.name;

  return (
    <ParentInvestmentHoldings
      childId={childId}
      childName={childName}
      onBack={() =>
        router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/investment')
      }
      onPressStock={(productId) =>
        router.push({
          pathname: '/(parent)/(tabs)/investment/[stockId]',
          params: {
            stockId: String(productId),
            ...(childId != null ? { childId: String(childId) } : {}),
          },
        })
      }
    />
  );
}
