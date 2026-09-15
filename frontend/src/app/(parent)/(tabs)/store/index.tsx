import { router } from 'expo-router';

import { useFamilyId } from '@/features/family';
import { StoreCatalog } from '@/features/store';

export default function ParentStoreScreen() {
  const { data: familyId } = useFamilyId();

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
    />
  );
}
