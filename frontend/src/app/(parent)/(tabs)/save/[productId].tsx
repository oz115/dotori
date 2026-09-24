import { router, useLocalSearchParams } from 'expo-router';
import { useMemo } from 'react';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import { ParentSavingsProductDetail } from '@/features/savings';

/**
 * 부모가 보는 예적금 상품 상세.
 *
 * 보유 자녀를 찾으려면 가족의 자녀 전체가 필요한데, 자녀 목록은 family 슬라이스 값이고
 * features 끼리는 서로 import 하지 않으므로 라우트가 엮어서 넘긴다.
 */
export default function ParentSavingsProductScreen() {
  const { productId } = useLocalSearchParams<{ productId: string }>();
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const childList = useMemo(
    () =>
      (members ?? [])
        .filter((member) => member.role === 'CHILD')
        .map((member) => ({ childId: member.userId, name: member.name })),
    [members],
  );

  return (
    <ParentSavingsProductDetail
      familyId={familyId ?? undefined}
      childId={childList[0]?.childId}
      productId={Number(productId)}
      childList={childList}
      onBack={() => (router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/save'))}
    />
  );
}
