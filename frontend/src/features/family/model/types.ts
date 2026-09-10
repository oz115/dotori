/**
 * 온보딩에서 부모가 고르는 아이의 금융 지식 수준. 서버 enum 과 1:1.
 * 퀴즈 난이도·설명 문구 조정에 쓰인다.
 */
export type FinancialKnowledgeLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

/**
 * POST /api/v1/families 요청 본문.
 * 금액 두 개는 화면 단위인 원(₩)이다 — 서버가 10원=1포인트로 환산해 저장한다.
 * 최소 1000원, 1000원 단위만 허용 (아니면 400 INVALID_REQUEST).
 */
export type FamilyCreateBody = {
  name: string;
  /** 아이가 매주 받는 기본 용돈(원) */
  weeklyAllowance: number;
  /** 도토리 활동으로 한 주 동안 받을 수 있는 추가 보상 한도(원) */
  weeklyRewardLimit: number;
  financialKnowledgeLevel: FinancialKnowledgeLevel;
};

/** POST /api/v1/families 응답 (201) */
export type FamilyCreateResponse = {
  familyId: number;
  name: string;
  createdBy: number;
  createdAt: string;
};

/** GET /api/v1/families/status */
export type FamilyStatus = {
  hasFamily: boolean;
};

/** POST /api/v1/families/{familyId}/invitations 응답 (201). code 는 6자리, 10분 TTL. */
export type InvitationResponse = {
  familyId: number;
  code: string;
  /** ISO 8601 만료 시각 */
  expiresAt: string;
};
