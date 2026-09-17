import { useMutation, useQueryClient } from '@tanstack/react-query';

import { quizApi } from '@/features/quiz/api/quizApi';
import { quizKeys } from '@/features/quiz/model/quizKeys';

/**
 * 퀴즈 답안 제출. 정답이면 그 즉시 잠기고, 오답이면 재도전 가능(최대 3회)하다 — 서버가 그
 * 상태를 판단해서 내려주므로 이 훅은 결과를 전달하고 오늘의 퀴즈 캐시를 무효화하기만 한다
 * (alreadySubmitted·공개 여부가 서버 기준으로 다시 맞춰진다).
 *
 * 포인트 잔액은 이 feature 가 모르는 슬라이스라 여기서 안 건드린다 — 호출하는 라우트가
 * 따로 처리한다 (상점 구매·집안일 승인과 같은 방식).
 */
export function useSubmitQuiz(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (params: { quizId: number; answer: string }) =>
      quizApi.submitQuiz(familyId as number, childId as number, params.quizId, params.answer),
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      queryClient.invalidateQueries({ queryKey: quizKeys.today(familyId, childId) });
    },
  });
}
