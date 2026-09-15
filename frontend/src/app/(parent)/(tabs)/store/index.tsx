import { router } from 'expo-router';

import { useFamilyId } from '@/features/family';
import { StoreCatalog } from '@/features/store';

export default function ParentStoreScreen() {
  const { data: familyId } = useFamilyId();

  return (
    <StoreCatalog
      familyId={familyId ?? undefined}
      onRegisterPress={() => router.push('/store/new')}
    />
  );
}
