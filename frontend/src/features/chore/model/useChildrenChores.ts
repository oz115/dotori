import { useQueries } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChoreSummary } from '@/features/chore/model/types';

/** 조회 대상 자녀. 이름은 가족 슬라이스에서 오므로 부르는 쪽(라우트)이 넘겨준다 */
export type ChoreChild = { childId: number; name: string };

/** 여러 자녀의 목록을 한 화면에 섞어 놓으므로 누구 것인지 함께 들고 다닌다 */
export type ParentChore = ChoreSummary & { childId: number; childName: string };

/**
 * 부모가 보는 가족 전체의 할 일.
 *
 * 경로가 `/children/{childId}/chores` 라서 가족 단위로 한 번에 받을 수 없다. 자녀 수만큼
 * 병렬로 부르고 합친다 - 가족당 자녀가 몇 명 수준이라 이 정도면 충분하고, 서버에 가족
 * 단위 엔드포인트를 새로 파는 것보다 낫다.
 *
 * 합친 뒤에는 순서가 사라지므로 등록 시각 내림차순으로 다시 세운다. 그냥 이어 붙이면
 * 첫째 아이 것이 전부 위에 몰려 한 아이의 목록처럼 보인다.
 */
export function useChildrenChores(familyId: number | undefined, children: ChoreChild[]) {
  return useQueries({
    queries: children.map((child) => ({
      queryKey: choreKeys.list(familyId ?? -1, child.childId),
      queryFn: () => choreApi.getChores(familyId as number, child.childId),
      enabled: familyId != null,
    })),
    combine: (results) => ({
      items: results
        .flatMap((result, index) =>
          (result.data ?? []).map<ParentChore>((chore) => ({
            ...chore,
            childId: children[index].childId,
            childName: children[index].name,
          })),
        )
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
    }),
  });
}
