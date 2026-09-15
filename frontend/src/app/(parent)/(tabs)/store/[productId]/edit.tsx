import { useLocalSearchParams } from 'expo-router';

import { useFamilyId } from '@/features/family';
import { StoreProductEditForm } from '@/features/store';

export default function ParentStoreProductEditScreen() {
  const { productId } = useLocalSearchParams<{ productId: string }>();
  const { data: familyId } = useFamilyId();

  return <StoreProductEditForm familyId={familyId ?? undefined} productId={Number(productId)} />;
}
