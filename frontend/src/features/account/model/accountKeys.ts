/**
 * 계좌 관련 TanStack Query 키.
 */
export const accountKeys = {
  all: ['account'] as const,
  /** 연동 계좌 상세 (GET /accounts/me) */
  me: () => [...accountKeys.all, 'me'] as const,
};
