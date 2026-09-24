import { hasChildMember } from '@/features/family/lib/familyReady';
import { useFamilyMembers } from '@/features/family/model/useFamilyMembers';
import { useFamilyStatus } from '@/features/family/model/useFamilyStatus';

/**
 * 부모가 서비스를 쓸 준비가 됐는지. 가족을 만들고 **아이가 한 명이라도 합류**해야 준비 완료다.
 * 부모 탭 진입 게이트와 온보딩 허브가 같은 기준을 쓴다.
 *
 * 구성원 조회가 실패하면 막지 않고 통과시킨다 — 네트워크 오류 때문에 이미 아이가 있는 부모가
 * 온보딩에 갇히는 것보다, 통과 후 각 화면이 오류를 보여 주는 편이 낫다.
 *
 * @param pollMs 아이의 합류를 기다리는 화면(온보딩 허브)에서 구성원을 다시 조회할 주기(ms)
 */
export function useFamilyReady(pollMs?: number) {
  const status = useFamilyStatus();
  const familyId = status.data?.familyId ?? undefined;
  const hasFamily = status.data?.hasFamily === true;
  const members = useFamilyMembers(hasFamily ? familyId : undefined, pollMs);

  const hasChild = members.isError || hasChildMember(members.data);

  return {
    /** 가족 상태·구성원 조회가 끝나기 전 (구성원은 가족이 있을 때만 기다린다) */
    isPending: status.isPending || (hasFamily && members.isPending),
    hasFamily,
    hasChild,
    /** 가족이 있고 아이가 합류한 상태 */
    ready: hasFamily && hasChild,
  };
}
