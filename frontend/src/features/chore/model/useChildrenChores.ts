import { useMemo } from 'react';
import { useQueries } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { earliestStartDate, isPaidThisPeriod, todayDate } from '@/features/chore/lib/choreStatus';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChoreSummary } from '@/features/chore/model/types';

/** 조회 대상 자녀. 이름은 가족 슬라이스에서 오므로 부르는 쪽(라우트)이 넘겨준다 */
export type ChoreChild = { childId: number; name: string };

/** 여러 자녀의 목록을 한 화면에 섞어 놓으므로 누구 것인지 함께 들고 다닌다 */
export type ParentChore = ChoreSummary & {
  childId: number;
  childName: string;
  /**
   * 이번 주기의 보상 지급이 끝났는지. 화면은 이 값이 true 인 건을 목록에서 뺀다.
   *
   * 아이 화면의 status === 'DONE' 과 같은 판정이다. 'PENDING'(확인 대기)은 구분하지
   * 않는다 - 부모 화면은 대기 중인 요청을 목록 위의 "완료 요청" 카드에서 따로 보여 주고,
   * 행마다 표시하지 않기 때문이다.
   */
  done: boolean;
};

/**
 * 부모가 보는 가족 전체의 할 일.
 *
 * 경로가 `/children/{childId}/chores` 라서 가족 단위로 한 번에 받을 수 없다. 자녀 수만큼
 * 병렬로 부르고 합친다 - 가족당 자녀가 몇 명 수준이라 이 정도면 충분하고, 서버에 가족
 * 단위 엔드포인트를 새로 파는 것보다 낫다.
 *
 * 목록에는 완료 여부가 없어서 자녀별 완료 내역을 함께 받아 겹친다. 이것이 없으면 이미
 * 승인·지급까지 끝난 미션이 계속 목록에 남아, 부모 화면과 아이 화면이 서로 다른 것을
 * 보여 준다.
 *
 * 합친 뒤에는 순서가 사라지므로 등록 시각 내림차순으로 다시 세운다. 그냥 이어 붙이면
 * 첫째 아이 것이 전부 위에 몰려 한 아이의 목록처럼 보인다.
 */
export function useChildrenChores(familyId: number | undefined, children: ChoreChild[]) {
  const chores = useQueries({
    queries: children.map((child) => ({
      queryKey: choreKeys.list(familyId ?? -1, child.childId),
      queryFn: () => choreApi.getChores(familyId as number, child.childId),
      enabled: familyId != null,
    })),
    combine: (results) => ({
      byChild: results.map((result) => result.data ?? []),
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
    }),
  });

  const today = todayDate();

  // 조회 기간은 자녀별로 다르다. 그 아이 목록에서 가장 이른 주기 시작일부터 봐야
  // 매주·매월 반복이 며칠 전에 승인된 경우가 빠지지 않는다 (아이 화면과 같은 규칙).
  const completions = useQueries({
    queries: children.map((child, index) => {
      const childChores = chores.byChild[index] ?? [];
      const from = earliestStartDate(childChores) ?? today;
      return {
        queryKey: choreKeys.completions(familyId ?? -1, child.childId, from, today),
        queryFn: () => choreApi.getCompletions(familyId as number, child.childId, from, today),
        // 할 일이 없으면 물어볼 것도 없다. 물으면 이 쿼리가 영영 pending 으로 남는다.
        enabled: familyId != null && childChores.length > 0,
      };
    }),
    combine: (results) => ({
      byChild: results.map((result) => result.data ?? []),
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
    }),
  });

  const items = useMemo(
    () =>
      children
        .flatMap((child, index) =>
          (chores.byChild[index] ?? []).map<ParentChore>((chore) => ({
            ...chore,
            childId: child.childId,
            childName: child.name,
            done: isPaidThisPeriod(chore, completions.byChild[index] ?? []),
          })),
        )
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    [children, chores.byChild, completions.byChild],
  );

  const needsCompletions = chores.byChild.some((list) => list.length > 0);

  return {
    items,
    isPending: chores.isPending || (needsCompletions && completions.isPending),
    isError: chores.isError || completions.isError,
  };
}
