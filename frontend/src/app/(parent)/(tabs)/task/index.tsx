import { router } from 'expo-router';
import { useMemo } from 'react';

import { ParentChoreList } from '@/features/chore';
import { useFamilyId, useFamilyMembers } from '@/features/family';

/**
 * 부모 할 일 탭.
 *
 * 집안일 조회 경로가 전부 childId 를 요구하는데 그 값과 이름은 가족 슬라이스에 있다.
 * features 끼리는 서로 import 하지 않으므로 라우트가 엮어서 넘긴다.
 *
 *  * 상세·수정·삭제·완료 요청 화면은 아직 없어서 콜백을 넘기지 않는다. 각 화면이 생길 때
 * 여기에 한 줄씩 붙는다.
 */
export default function ParentTaskScreen() {
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const children = useMemo(
    () =>
      (members ?? [])
        .filter((member) => member.role === 'CHILD')
        .map((member) => ({ childId: member.userId, name: member.name })),
    [members],
  );

  return (
    <ParentChoreList
      familyId={familyId ?? undefined}
      childList={children}
      onRegisterPress={() => router.push('/task/new')}
      onChorePress={(chore) =>
        router.push({
          pathname: '/task/[choreId]',
          params: { choreId: String(chore.choreId), childId: String(chore.childId) },
        })
      }
      onEditChore={(chore) =>
        router.push({
          pathname: '/task/[choreId]/edit',
          params: { choreId: String(chore.choreId), childId: String(chore.childId) },
        })
      }
      onRequestPress={() => router.push('/task/requests')}
      onSeeAllRequests={() => router.push('/task/requests')}
      onHistoryPress={() => router.push('/task/history')}
    />
  );
}
