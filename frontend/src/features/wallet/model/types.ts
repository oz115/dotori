/**
 * 도토리 포인트 지갑 관련 서버 계약.
 * 경로가 `/families/{familyId}/children/{childId}/wallet/...` 라서 familyId·childId 를
 * 함께 넘겨야 한다 — familyId 는 `useFamilyId`, childId 는 본인 userId 를 쓴다.
 */

/** GET .../wallet/balance 응답 */
export type PointBalance = {
  childId: number;
  balance: number;
  /** 잔액이 마지막으로 바뀐 시각. 지갑이 아직 없으면 null */
  updatedAt: string | null;
};
