import { httpClient } from '@/shared/api';
import type { PointBalance } from '@/features/wallet/model/types';

/** 포인트 지갑 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const walletApi = {
  /** 보유 포인트 조회. 지갑이 아직 없어도 오류가 아니라 잔액 0으로 온다 */
  getBalance: (familyId: number, childId: number) =>
    httpClient
      .get<PointBalance>(`/api/v1/families/${familyId}/children/${childId}/wallet/balance`)
      .then((res) => res.data),
};
