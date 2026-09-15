import { router, useLocalSearchParams } from 'expo-router';

import { useAuthStore } from '@/features/auth';
import { useFamilyId, useFamilyMembers } from '@/features/family';
import { StoreProductDetailScreen, useStoreProduct } from '@/features/store';

/**
 * store·family·auth 슬라이스를 엮는 지점. features 는 서로 직접 import 하지 않으므로
 * (fsd-lint 참고), 상세 화면이 필요로 하는 role·등록자 이름은 라우트가 조합해서 넘긴다.
 */
export default function ParentStoreProductDetailScreen() {
  const { productId } = useLocalSearchParams<{ productId: string }>();
  const numericProductId = Number(productId);

  const { data: familyId } = useFamilyId();
  const role = useAuthStore((s) => s.user?.role);
  const { data: product } = useStoreProduct(familyId ?? undefined, numericProductId);
  const members = useFamilyMembers(familyId ?? undefined);
  const registrantName = members.data?.find((m) => m.userId === product?.createdBy)?.name;

  return (
    <StoreProductDetailScreen
      familyId={familyId ?? undefined}
      productId={numericProductId}
      role={role}
      registrantName={registrantName}
      balance={undefined}
      onNavigateToList={() => router.replace('/(parent)/(tabs)/store')}
    />
  );
}
