import { ParentQuizScreen } from '@/features/quiz';
import { useParentFamily } from '@/widgets/parent-home';

/**
 * 부모용 오늘의 경제 퀴즈. 부모 홈의 진입 카드에서 push 로 들어오는 독립 화면이라 탭 밖에 있다.
 * quiz·family 슬라이스를 엮는 지점이라, 조회에 쓸 familyId·childId 는 부모 홈과 같은 방식으로
 * 라우트가 조합해서 넘긴다.
 */
export default function ParentQuizRoute() {
  const { familyId, childId, isPending } = useParentFamily();

  return <ParentQuizScreen familyId={familyId} childId={childId} familyPending={isPending} />;
}
