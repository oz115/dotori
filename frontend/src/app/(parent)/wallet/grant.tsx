import { router } from 'expo-router';

import { GrantPointsScreen } from '@/features/wallet';
import { useParentFamily } from '@/widgets/parent-home';

/**
 * 도토리 주기 (부모). 부모 홈의 자녀 도토리 카드 "도토리 주기"에서 들어온다.
 * 지금은 자녀 한 명 기준이라 부모 홈과 같은 자녀(첫 자녀)에게 준다 — 여러 명이 되면 여기서 고른다.
 */
export default function ParentGrantScreen() {
  const { familyId, childId, childName } = useParentFamily();

  return (
    <GrantPointsScreen
      familyId={familyId}
      childId={childId}
      childName={childName}
      onBack={() => router.back()}
      onDone={() => router.back()}
    />
  );
}
