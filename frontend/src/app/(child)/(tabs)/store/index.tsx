import { router } from 'expo-router';

import { useFamilyId } from '@/features/family';
import { StoreCatalog } from '@/features/store';

export default function ChildStoreScreen() {
  const { data: familyId } = useFamilyId();

  return (
    <StoreCatalog
      familyId={familyId ?? undefined}
      character="child"
      onPressProduct={(productId) =>
        router.push({
          pathname: '/(child)/(tabs)/store/[productId]',
          params: { productId: String(productId) },
        })
      }
    />
  );
}
