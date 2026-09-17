import { useQuery } from '@tanstack/react-query';

import { quizApi } from '@/features/quiz/api/quizApi';
import { quizKeys } from '@/features/quiz/model/quizKeys';
import { apiErrorCode } from '@/shared/api';

/**
 * 오늘의 퀴즈. 아직 안 만들어졌으면(QUIZ_NOT_READY) 재시도해도 똑같으니 다시 두드리지 않는다
 * (AI 배치가 매일 00:10에 도니, 그 전에 열면 늘 이 상태다).
 */
export function useTodayQuiz(familyId: number | undefined, childId: number | undefined) {
  return useQuery({
    queryKey: quizKeys.today(familyId ?? -1, childId ?? -1),
    queryFn: () => quizApi.getTodayQuiz(familyId as number, childId as number),
    enabled: familyId != null && childId != null,
    retry: (count, error) => (apiErrorCode(error) === 'QUIZ_NOT_READY' ? false : count < 1),
  });
}
