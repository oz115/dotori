import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';
import { QuizScreen } from '@/features/quiz';
import { useWalletBalance } from '@/features/wallet';

/**
 * quiz·family·auth·wallet 슬라이스를 엮는 지점. features 는 서로 직접 import 하지 않으므로
 * (fsd-lint 참고), 퀴즈 화면이 필요로 하는 familyId·childId는 라우트가 조합해서 넘긴다.
 */
export default function ChildQuizScreen() {
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);
  const balance = useWalletBalance(familyId ?? undefined, childId);

  return (
    <QuizScreen
      familyId={familyId ?? undefined}
      childId={childId}
      onSubmitted={() => balance.refetch()}
    />
  );
}
