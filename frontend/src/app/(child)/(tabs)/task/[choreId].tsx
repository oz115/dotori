import { router, useLocalSearchParams } from 'expo-router';

import { useAuthStore } from '@/features/auth';
import { ChildChoreDetail } from '@/features/chore';
import { useFamilyId } from '@/features/family';

/**
 * 미션 상세 (아이). 미션 목록·홈·알림에서 들어온다.
 *
 * 부모 상세와 같은 자리(탭 안의 스택)다. 모달이었을 때는 팝업 바깥을 잘못 누르거나
 * 뒤로가기를 누르면 쓰던 내용이 날아갔다 (QA).
 */
export default function ChildChoreDetailScreen() {
  const { choreId, from } = useLocalSearchParams<{
    choreId: string;
    /** 'home' 이면 홈에서 곧장 들어온 것이다 */
    from?: string;
  }>();
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);

  return (
    <ChildChoreDetail
      familyId={familyId ?? undefined}
      childId={childId}
      choreId={Number(choreId) || undefined}
      // 홈에서 들어왔으면 되돌릴 자리가 미션 목록이 아니라 홈이다 (부모 상세와 같은 규칙).
      onBack={
        from === 'home'
          ? () => router.replace('/(child)/(tabs)')
          : () => (router.canGoBack() ? router.back() : router.replace('/task'))
      }
    />
  );
}
