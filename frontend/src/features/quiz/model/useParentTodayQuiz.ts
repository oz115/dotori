import { useQuery } from '@tanstack/react-query';

import { quizApi } from '@/features/quiz/api/quizApi';
import { quizKeys } from '@/features/quiz/model/quizKeys';
import { apiErrorCode } from '@/shared/api';

/**
 * 부모가 보는 자녀의 오늘의 퀴즈(문제·정답·아이가 낸 답). 부모 전용 API 다.
 *
 * 아직 안 만들어졌으면(QUIZ_NOT_READY) 재시도해도 같고, 권한이 없으면(FORBIDDEN) 더 두드려 봐야
 * 소용없다. 이 조회는 퀴즈 생성을 일으키지 않는다 — 생성은 서버 배치 몫이다.
 *
 * familyId·childId 는 다른 슬라이스(family) 값이라 호출하는 쪽(widget)이 넘겨준다.
 * 둘 중 하나라도 없으면 조회하지 않는다.
 */
export function useParentTodayQuiz(familyId: number | undefined, childId: number | undefined) {
  return useQuery({
    queryKey: quizKeys.parentToday(familyId ?? -1, childId ?? -1),
    queryFn: () => quizApi.getParentTodayQuiz(familyId as number, childId as number),
    enabled: familyId != null && childId != null,
    retry: (count, error) => {
      const code = apiErrorCode(error);
      return code === 'QUIZ_NOT_READY' || code === 'FORBIDDEN' ? false : count < 1;
    },
  });
}
