import { useMutation, useQueryClient } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChorePhotoFile } from '@/features/chore/model/types';

type RequestCompletionInput = {
  choreId: number;
  photo: ChorePhotoFile;
  description: string | null;
};

/**
 * 완료 요청 보내기 (아이). 사진 업로드와 요청 생성을 한 번에 처리한다.
 *
 * 서버는 두 단계다 — 사진을 올려 키를 받고, 그 키를 요청 본문에 실어 보낸다. 화면에서는
 * "완료 요청 보내기" 버튼 하나이므로 여기서 묶었다. 중간에 실패해 키만 남아도 사용자에게
 * 보이는 것은 없고(요청이 안 만들어졌으니 목록에도 안 뜬다) 버킷에 고아 파일 하나가 남을
 * 뿐이라, 단계를 갈라 보여 줄 만한 실익이 없다.
 *
 * 성공하면 대기 중 요청 목록을 무효화한다 — 목록의 "확인 중" 배지가 이 쿼리를 본다.
 */
export function useRequestCompletion(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ choreId, photo, description }: RequestCompletionInput) => {
      const photoKey = await choreApi.uploadChorePhoto(
        familyId as number,
        childId as number,
        photo,
      );
      return choreApi.requestCompletion(familyId as number, childId as number, choreId, {
        photoKey,
        description,
      });
    },
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      queryClient.invalidateQueries({ queryKey: choreKeys.completionRequests(familyId, childId) });
    },
  });
}
