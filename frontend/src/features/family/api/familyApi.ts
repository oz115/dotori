import { httpClient } from '@/shared/api';
import type { FamilyCreateBody, FamilyCreateResponse } from '@/features/family/model/types';

/**
 * 가족 관련 원시 HTTP 호출. 토큰·세션 처리는 httpClient 인터셉터가 맡고,
 * 성공 후 캐시 갱신 등 오케스트레이션은 features/family/model 의 훅에서 담당한다.
 */
export const familyApi = {
  /** 가족 생성(온보딩) — 부모만, 이미 가족이 있으면 409 ALREADY_IN_FAMILY */
  create: (body: FamilyCreateBody) =>
    httpClient.post<FamilyCreateResponse>('/api/v1/families', body).then((res) => res.data),
};
