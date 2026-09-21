import { httpClient } from '@/shared/api';
import type {
  Family,
  FamilyConnectResponse,
  FamilyCreateBody,
  FamilyCreateResponse,
  FamilyMember,
  FamilyStatus,
  FamilyUpdateBody,
  InvitationResponse,
} from '@/features/family/model/types';

/**
 * 가족 관련 원시 HTTP 호출. 토큰·세션 처리는 httpClient 인터셉터가 맡고,
 * 성공 후 캐시 갱신 등 오케스트레이션은 features/family/model 의 훅에서 담당한다.
 */
export const familyApi = {
  /** 가족 생성(온보딩) — 부모만, 이미 가족이 있으면 409 ALREADY_IN_FAMILY */
  create: (body: FamilyCreateBody) =>
    httpClient.post<FamilyCreateResponse>('/api/v1/families', body).then((res) => res.data),

  /** 현재 사용자의 가족 소속 여부 — 온보딩 진입 분기에 쓴다 */
  getStatus: () => httpClient.get<FamilyStatus>('/api/v1/families/status').then((res) => res.data),

  /** 가족 정보 조회 (이름·온보딩 설정) */
  get: (familyId: number) =>
    httpClient.get<Family>(`/api/v1/families/${familyId}`).then((res) => res.data),

  /** 가족 이름·설정 부분 수정 — 보낸 필드만 바뀐다 */
  update: (familyId: number, body: FamilyUpdateBody) =>
    httpClient.patch<Family>(`/api/v1/families/${familyId}`, body).then((res) => res.data),

  /** 가족 구성원 목록 — 부모 홈이 자녀(childId·이름)를 찾는 데 쓴다 */
  getMembers: (familyId: number) =>
    httpClient
      .get<{ members: FamilyMember[] }>(`/api/v1/families/${familyId}/members`)
      .then((res) => res.data.members),

  /** 아이 초대 코드 발급 — 부모(가족 생성자)만. 6자리, 10분 TTL. 재호출하면 새 코드 */
  createInvitation: (familyId: number) =>
    httpClient
      .post<InvitationResponse>(`/api/v1/families/${familyId}/invitations`, undefined)
      .then((res) => res.data),

  /** 발급된 초대 코드 폐기(만료) — 부모만, 성공 시 204. 발급된 코드가 없어도 204 */
  revokeInvitation: (familyId: number) =>
    httpClient.delete(`/api/v1/families/${familyId}/invitations`),

  /** 가족 삭제 — 생성자만, 성공 시 204 */
  remove: (familyId: number) => httpClient.delete(`/api/v1/families/${familyId}`),

  /** 자녀가 초대 코드로 가족에 합류 — 자녀만, 잘못된/만료된 코드면 INVALID_FAMILY_CODE */
  connect: (code: string) =>
    httpClient
      .post<FamilyConnectResponse>('/api/v1/families/connect', { code })
      .then((res) => res.data),
};
