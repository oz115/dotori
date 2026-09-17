import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { SavingsHistory } from '@/features/savings';

/** 예적금 거래 이력. 저축 메인 헤더의 "거래 내역"에서 들어온다. */
export default function SavingsHistoryScreen() {
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);

  return <SavingsHistory familyId={familyId ?? undefined} childId={childId} />;
}
