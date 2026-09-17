import { useLocalSearchParams } from 'expo-router';

import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { SavingsProductDetail } from '@/features/savings';

/** 예적금 상품 상세. 저축 메인의 상품 카드에서 들어온다. */
export default function SavingsProductDetailScreen() {
  const { productId } = useLocalSearchParams<{ productId: string }>();
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);

  return (
    <SavingsProductDetail
      familyId={familyId ?? undefined}
      childId={childId}
      productId={Number(productId)}
    />
  );
}
