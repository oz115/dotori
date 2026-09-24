import { router } from 'expo-router';
import { useMemo } from 'react';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import { ChildrenSavingsStatus } from '@/features/savings';

/**
 * 자녀 예적금 보유 현황 (부모). 저축 메인 헤더의 버튼으로 들어온다.
 *
 * 자녀 목록은 family 슬라이스 값이라 라우트가 엮어서 넘긴다 — 상품 조회에 쓸 자녀
 * (첫째)도 여기서 고른다. 상품은 가족 단위 값이라 누구로 물어도 같다.
 */
export default function ParentSavingsHoldingsScreen() {
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
    <ChildrenSavingsStatus
      familyId={familyId ?? undefined}
      childList={childList}
      productChildId={childList[0]?.childId}
      onPressProduct={(productId) =>
        router.push({
          pathname: '/(parent)/(tabs)/save/[productId]',
          params: { productId: String(productId) },
        })
      }
      onPressHistory={(childId) =>
        router.push({
          pathname: '/(parent)/(tabs)/save/history',
          params: { childId: String(childId) },
        })
      }
      onBack={() => (router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/save'))}
    />
  );
}
