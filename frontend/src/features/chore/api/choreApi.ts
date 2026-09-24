import { httpClient } from '@/shared/api';
import type {
  ChoreApprovalResult,
  ChoreCompletionCreateBody,
  ChoreCompletionHistoryItem,
  ChoreCompletionResult,
  ChoreCompletionSummary,
  ChoreCreateBody,
  ChoreDetail,
  ChorePhotoFile,
  ChorePhotoUploadResult,
  ChoreRejectionResult,
  ChoreSummary,
  ChoreUpdateBody,
} from '@/features/chore/model/types';

const base = (familyId: number, childId: number) =>
  `/api/v1/families/${familyId}/children/${childId}/chores`;

/** 집안일 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const choreApi = {
  /** 활성 집안일 목록 */
  getChores: (familyId: number, childId: number) =>
    httpClient
      .get<{ chores: ChoreSummary[] }>(base(familyId, childId))
      .then((res) => res.data.chores),

  /** 집안일 상세. 지워진 것도 조회된다 (deletedAt 으로 구분) */
  getChore: (familyId: number, childId: number, choreId: number) =>
    httpClient.get<ChoreDetail>(`${base(familyId, childId)}/${choreId}`).then((res) => res.data),

  /** 집안일 등록 (부모). 보상은 응답 시점에 아직 null 이고 AI 가 곧 채운다 */
  createChore: (familyId: number, childId: number, body: ChoreCreateBody) =>
    httpClient.post<ChoreDetail>(base(familyId, childId), body).then((res) => res.data),

  /** 집안일 수정 (부모). 보낸 필드만 바뀐다 */
  updateChore: (familyId: number, childId: number, choreId: number, body: ChoreUpdateBody) =>
    httpClient
      .patch<ChoreDetail>(`${base(familyId, childId)}/${choreId}`, body)
      .then((res) => res.data),

  /** 집안일 삭제 (부모). 소프트 삭제라 이미 올라온 완료 요청은 계속 처리할 수 있다 */
  deleteChore: (familyId: number, childId: number, choreId: number) =>
    httpClient.delete<void>(`${base(familyId, childId)}/${choreId}`).then(() => undefined),

  /**
   * 완료 요청 목록. 부모는 승인 대기함으로, 아이는 "내가 보낸 요청"으로 쓴다.
   *
   * 사진 URL 이 몇 분이면 만료되는 presigned 링크라 오래 캐시하면 안 된다 (model 훅 참고).
   */
  getCompletionRequests: (
    familyId: number,
    childId: number,
    status: ChoreCompletionSummary['status'] = 'PENDING',
  ) =>
    httpClient
      .get<{ requests: ChoreCompletionSummary[] }>(
        `${base(familyId, childId)}/completion-requests`,
        {
          params: { status },
        },
      )
      .then((res) => res.data.requests),

  /** 기간 내 승인·지급까지 끝난 완료 내역. 안 넘기면 서버가 최근 30일로 잡는다 */
  getCompletions: (familyId: number, childId: number, startDate?: string, endDate?: string) =>
    httpClient
      .get<{ completions: ChoreCompletionHistoryItem[] }>(
        `${base(familyId, childId)}/completions`,
        {
          params: { startDate, endDate },
        },
      )
      .then((res) => res.data.completions),

  /**
   * 인증 사진 업로드 (아이). 돌려받은 photoKey 를 완료 요청 본문에 실어 보낸다.
   *
   * 경로가 chores 가 아니라 uploads 라서 base() 를 쓰지 않는다. Content-Type 은 직접 넣지
   * 않는다 — RN 의 FormData 는 boundary 를 런타임이 붙이므로, 손으로 지정하면 깨진다.
   */
  uploadChorePhoto: (familyId: number, childId: number, photo: ChorePhotoFile) => {
    const form = new FormData();
    // RN 의 FormData 는 Blob 대신 이 모양의 객체를 파일로 받는다 (웹 표준과 다르다).
    form.append('photo', photo as unknown as Blob);

    return httpClient
      .post<ChorePhotoUploadResult>(
        `/api/v1/families/${familyId}/children/${childId}/uploads/chore-photos`,
        form,
      )
      .then((res) => res.data.photoKey);
  },

  /** 완료 요청 보내기 (아이) */
  requestCompletion: (
    familyId: number,
    childId: number,
    choreId: number,
    body: ChoreCompletionCreateBody,
  ) =>
    httpClient
      .post<ChoreCompletionResult>(
        `${base(familyId, childId)}/${choreId}/completion-requests`,
        body,
      )
      .then((res) => res.data),

  /** 완료 요청 취소 (아이). 행은 CANCELLED 로 남고, 같은 주기에 다시 요청할 수 있다 */
  cancelCompletion: (familyId: number, childId: number, requestId: number) =>
    httpClient
      .delete<void>(`${base(familyId, childId)}/completion-requests/${requestId}`)
      .then(() => undefined),

  /** 완료 승인 + 보상 지급 (부모). 응답의 rewardPoints 가 실제 지급액이다 */
  approveCompletion: (familyId: number, childId: number, requestId: number) =>
    httpClient
      .post<ChoreApprovalResult>(
        `${base(familyId, childId)}/completion-requests/${requestId}/approve`,
      )
      .then((res) => res.data),

  /** 완료 거절 (부모). 사유는 보내지 않는다 — 서버가 받지 않는다 */
  rejectCompletion: (familyId: number, childId: number, requestId: number) =>
    httpClient
      .post<ChoreRejectionResult>(
        `${base(familyId, childId)}/completion-requests/${requestId}/reject`,
      )
      .then((res) => res.data),
};
