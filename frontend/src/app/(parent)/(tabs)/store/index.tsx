import { router, useLocalSearchParams } from 'expo-router';
import { useEffect } from 'react';

import { useFamilyId } from '@/features/family';
import { StoreCatalog } from '@/features/store';

export default function ParentStoreScreen() {
  const { data: familyId } = useFamilyId();
  const { openProductId } = useLocalSearchParams<{ openProductId?: string }>();

  // 알림/홈에서 상품 상세로 곧장 들어왔다가 이 목록 화면으로 replace된 경우, 상세 화면이
  // 넘겨준 productId를 받아 바로 위에 다시 push한다 — "목록 → 상세" 스택을 만드는 두 번째
  // 단계. 이 화면(목록)은 언마운트되지 않으므로 replace와 타이밍이 겹칠 일이 없다.
  useEffect(() => {
    if (!openProductId) return;

    router.push({
      pathname: '/(parent)/(tabs)/store/[productId]',
      params: { productId: openProductId },
    });
  }, [openProductId]);

  return (
    <StoreCatalog
      familyId={familyId ?? undefined}
      character="parent"
      onRegisterPress={() => router.push('/store/new')}
      onPressProduct={(productId) =>
        router.push({
          pathname: '/(parent)/(tabs)/store/[productId]',
          params: { productId: String(productId) },
        })
      }
      onHistoryPress={() => router.push('/(parent)/purchases')}
    />
  );
}
