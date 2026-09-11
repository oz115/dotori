import type { CashoutRequest } from '@/features/wallet/model/types';

/**
 * 화면 확인용 예시 지갑 데이터.
 *
 * 아직 아이를 연결하지 않았거나 지갑이 비어 있으면 부모 홈이 전부 "—" 로 남아
 * 배치를 볼 수 없어서, 그럴 때만 자리에 채워 넣는다. 실제 값이 있으면 쓰지 않는다.
 */
const NOW = '2026-09-11T18:40:00+09:00';

/** 자녀 보유 도토리 예시 */
export const mockChildBalance = 12_400;

export const mockCashoutRequests: CashoutRequest[] = [
  {
    cashoutRequestId: -1,
    childId: -1,
    parentId: -1,
    pointAmount: 5_000,
    status: 'PENDING',
    requestedAt: NOW,
    processedAt: null,
  },
];
