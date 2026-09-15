import { useFamilyId } from '@/features/family';
import { StoreProductForm } from '@/features/store';

export default function ParentStoreProductCreateScreen() {
  const { data: familyId } = useFamilyId();
  return <StoreProductForm familyId={familyId ?? undefined} />;
}
