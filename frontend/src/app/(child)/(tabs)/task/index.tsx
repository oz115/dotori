import { router, useLocalSearchParams } from 'expo-router';

import { useAuthStore } from '@/features/auth';
import { ChildChoreList } from '@/features/chore';
import { useFamilyId } from '@/features/family';

/**
 * 아이 할 일 탭. familyId(family)·childId(auth 본인)는 서로 다른 슬라이스 값이라
 * 라우트가 엮어서 넘긴다.
 *
 * 알림에서 들어오면 openChoreId 를 넘긴다 - 아직 할 미션이면 상세 모달, 이미 받은
 * 미션이면 완료 이력이 열린다.
 */
export default function ChildTaskScreen() {
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);
  const { openChoreId } = useLocalSearchParams<{ openChoreId?: string }>();

  return (
    <ChildChoreList
      familyId={familyId ?? undefined}
      childId={childId}
      requestedChoreId={openChoreId ? Number(openChoreId) || undefined : undefined}
      onRequestedChoreConsumed={() => router.setParams({ openChoreId: undefined })}
    />
  );
}
