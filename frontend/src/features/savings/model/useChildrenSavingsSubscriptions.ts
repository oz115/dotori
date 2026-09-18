import { useQueries } from '@tanstack/react-query';

import { savingsApi } from '@/features/savings/api/savingsApi';
import { savingsKeys } from '@/features/savings/model/savingsKeys';
import type { SavingsSubscription } from '@/features/savings/model/types';

/** 조회 대상 자녀. 이름은 가족 슬라이스에서 오므로 부르는 쪽(라우트)이 넘겨준다 */
export type SavingsChild = { childId: number; name: string };

/** 자녀 한 명의 가입 목록 (이름을 함께 들고 다닌다) */
export type ChildSavings = {
  childId: number;
  name: string;
  subscriptions: SavingsSubscription[];
};

/**
 * 가족 전체 자녀의 예적금 가입 현황.
 *
 * 경로가 `/children/{childId}/savings/subscriptions` 라서 가족 단위로 한 번에 받을 수 없다.
 * 자녀 수만큼 병렬로 부르고 합친다 - 집안일의 useChildrenChores 와 같은 방식이고, 가족당
 * 자녀가 몇 명 수준이라 이 정도면 충분하다.
 *
 * 부모 화면 두 곳이 쓴다. "자녀 예적금 보유 현황" 은 자녀별로 나눠 보여 주고, 상품 상세는
 * 이 상품을 든 자녀가 누구인지 찾는다.
 */
export function useChildrenSavingsSubscriptions(
  familyId: number | undefined,
  children: SavingsChild[],
) {
  return useQueries({
    queries: children.map((child) => ({
      queryKey: savingsKeys.subscriptions(familyId ?? -1, child.childId),
      queryFn: () => savingsApi.getSubscriptions(familyId as number, child.childId),
      enabled: familyId != null,
    })),
    combine: (results) => ({
      items: results.map<ChildSavings>((result, index) => ({
        childId: children[index].childId,
        name: children[index].name,
        subscriptions: result.data ?? [],
      })),
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
      refetch: () => results.forEach((result) => result.refetch()),
    }),
  });
}

/**
 * 지금 "보유 중"으로 볼 가입만 남긴다.
 *
 * 해지(CANCELLED)·수령 완료(COMPLETED)는 빼고 진행 중(ACTIVE)과 수령 대기(MATURED)만 센다 -
 * 아이 화면의 카드 조립(buildSavingsCards)과 같은 기준이라, 부모와 아이가 서로 다른
 * 개수를 보지 않는다.
 */
export function heldSubscriptions(subscriptions: SavingsSubscription[]): SavingsSubscription[] {
  return subscriptions.filter((s) => s.status === 'ACTIVE' || s.status === 'MATURED');
}
