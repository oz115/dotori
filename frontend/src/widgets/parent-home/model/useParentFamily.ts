import { useFamilyId, useFamilyMembers } from '@/features/family';

/**
 * 부모 홈이 필요로 하는 (familyId, 자녀) 한 쌍.
 *
 * 지갑·집안일 조회 경로가 전부 childId 를 요구하는데 부모는 자기 id 밖에 모르므로,
 * 가족 구성원 목록에서 자녀를 찾아 쓴다. 지금은 자녀 한 명 기준이라 첫 자녀만 본다 —
 * 여러 명이 되면 여기서 선택 상태를 갖게 된다.
 */
export function useParentFamily() {
  const { data: familyId } = useFamilyId();
  const members = useFamilyMembers(familyId ?? undefined);

  const child = members.data?.find((m) => m.role === 'CHILD');

  return {
    familyId: familyId ?? undefined,
    childId: child?.userId,
    childName: child?.name,
    /** 가족·구성원 조회가 끝나기 전 */
    isPending: familyId == null || members.isPending,
    isError: members.isError,
  };
}
