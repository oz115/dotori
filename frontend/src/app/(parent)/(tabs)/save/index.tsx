import { router } from 'expo-router';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import { ParentSavingsHome } from '@/features/savings';

/**
 * 부모 저축 탭. 예적금 API 가 (familyId, childId) 경로 스타일이라 상품을 물어볼 자녀가
 * 한 명 필요한데, 상품 6종과 가입 가능 최대 금액은 가족 단위 값이라 누구로 물어도 같다.
 * 그래서 첫째를 넘긴다 — 자녀가 없으면 undefined 이고 화면이 안내만 띄운다.
 */
export default function ParentSaveScreen() {
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const firstChildId = (members ?? []).find((member) => member.role === 'CHILD')?.userId;

  return (
    <ParentSavingsHome
      familyId={familyId ?? undefined}
      childId={firstChildId}
      onPressProduct={(productId) =>
        router.push({
          pathname: '/(parent)/(tabs)/save/[productId]',
          params: { productId: String(productId) },
        })
      }
      onPressChildrenStatus={() => router.push('/(parent)/(tabs)/save/holdings')}
    />
  );
}
