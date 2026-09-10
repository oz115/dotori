/**
 * 가족 관련 TanStack Query 키. 기능별로 한 곳에 모아 무효화 대상을 명확히 한다.
 */
export const familyKeys = {
  all: ['family'] as const,
  /** 현재 로그인 사용자의 가족 소속 여부 (GET /families/status) */
  status: () => [...familyKeys.all, 'status'] as const,
};
