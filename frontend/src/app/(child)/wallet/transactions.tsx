import { useAuthStore, useSignupDate } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { ChildWalletHistory } from '@/features/wallet';

/**
 * 내 거래 내역 (자녀). 홈 도토리 카드의 "거래 내역"에서 들어온다.
 * 도토리(포인트) 입출금 내역과 환전 요청 내역을 탭으로 오간다.
 */
export default function ChildWalletTransactionsScreen() {
  const familyId = useFamilyId().data ?? undefined;
  const childId = useAuthStore((state) => state.user?.userId);
  const signupDate = useSignupDate();

  return <ChildWalletHistory familyId={familyId} childId={childId} minPickableDate={signupDate} />;
}
