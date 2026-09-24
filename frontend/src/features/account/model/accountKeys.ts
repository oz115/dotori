import type { TransactionDirection, TransactionRangeFilter } from '@/features/account/model/types';

/**
 * 계좌 관련 TanStack Query 키.
 */
export const accountKeys = {
  all: ['account'] as const,
  /** 연동 계좌 상세 (GET /accounts/me) */
  me: () => [...accountKeys.all, 'me'] as const,
  /** 거래내역 (GET /accounts/transactions). 필터(기간·유형)마다 다른 캐시를 쓴다 */
  transactions: (
    range: TransactionRangeFilter,
    transactionType: TransactionDirection | undefined,
  ) => {
    const rangeKey = range.mode === 'period' ? range.period : `${range.startDate}~${range.endDate}`;
    return [...accountKeys.all, 'transactions', rangeKey, transactionType ?? 'ALL'] as const;
  },
};
