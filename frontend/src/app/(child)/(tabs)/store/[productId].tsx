import { router, useLocalSearchParams } from 'expo-router';
import { useEffect } from 'react';

import { useAuthStore } from '@/features/auth';
import { useFamilyId, useFamilyMembers } from '@/features/family';
import { StoreProductDetailScreen, useStoreProduct } from '@/features/store';
import { useWalletBalance } from '@/features/wallet';

/**
 * store·family·auth·wallet 슬라이스를 엮는 지점. features 는 서로 직접 import 하지 않으므로
 * (fsd-lint 참고), 상세 화면이 필요로 하는 role·등록자 이름·잔액은 라우트가 조합해서 넘긴다.
 */
export default function ChildStoreProductDetailScreen() {
  const { productId, viaDeepLink } = useLocalSearchParams<{
    productId: string;
    viaDeepLink?: string;
  }>();
  const numericProductId = Number(productId);

  // store 탭 밖(알림/홈)에서 곧장 이 화면으로 push되면, 이 탭의 내부 스택에 상세 하나만
  // 쌓여서 뒤로가기·탭 전환이 깨진다. 목록 화면에 "이 상품을 다시 열어달라"는 파라미터를
  // 실어 replace한다 — 이 화면(상세) 스스로 재-push를 예약하면, replace가 이 화면을
  // 언마운트시키는 순간 effect cleanup이 그 예약을 취소해버린다. 그래서 재-push는
  // 언마운트되지 않는 목록 화면 쪽이 하도록 넘긴다.
  useEffect(() => {
    if (viaDeepLink !== '1') return;

    router.replace({
      pathname: '/(child)/(tabs)/store',
      params: { openProductId: String(numericProductId) },
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 최초 마운트 시 1회만, productId 바뀌어도 재실행 안 함
  }, []);

  const { data: familyId } = useFamilyId();
  const role = useAuthStore((s) => s.user?.role);
  const childId = useAuthStore((s) => s.user?.userId);
  const { data: product } = useStoreProduct(familyId ?? undefined, numericProductId);
  const members = useFamilyMembers(familyId ?? undefined);
  const registrantName = members.data?.find((m) => m.userId === product?.createdBy)?.name;
  const balance = useWalletBalance(familyId ?? undefined, childId);

  return (
    <StoreProductDetailScreen
      familyId={familyId ?? undefined}
      productId={numericProductId}
      role={role}
      registrantName={registrantName}
      balance={balance.data?.balance}
      onPurchased={() => balance.refetch()}
      onNavigateToList={() => router.replace('/(child)/(tabs)/store')}
    />
  );
}
