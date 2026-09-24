import { useQueryClient } from '@tanstack/react-query';
import { useLocalSearchParams } from 'expo-router';

import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { SavingsProductDetail } from '@/features/savings';
import { walletKeys } from '@/features/wallet';

/**
 * 예적금 상품 상세. 저축 메인의 상품 카드에서 들어온다.
 *
 * 가입·납입·수령·해지는 모두 지갑을 거치는데, savings feature 는 wallet 슬라이스를 모른다.
 * 그 경계를 잇는 것이 라우트의 몫이라 여기서 잔액 캐시를 턴다 (상점 구매와 같은 방식).
 */
export default function SavingsProductDetailScreen() {
  const { productId } = useLocalSearchParams<{ productId: string }>();
  const queryClient = useQueryClient();
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);

  return (
    <SavingsProductDetail
      familyId={familyId ?? undefined}
      childId={childId}
      productId={Number(productId)}
      onWalletChanged={() => {
        if (familyId == null || childId == null) return;
        queryClient.invalidateQueries({ queryKey: walletKeys.balance(familyId, childId) });
      }}
    />
  );
}
