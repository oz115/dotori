import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { PurchaseHistoryScreen } from '@/features/store';

export default function ChildPurchaseHistoryScreen() {
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);

  return <PurchaseHistoryScreen familyId={familyId ?? undefined} childId={childId} />;
}
