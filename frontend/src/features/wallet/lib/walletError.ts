import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 충전 실패 → 사용자 문구. */
export function chargeErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INVALID_CHARGE_UNIT':
      return '10원 단위로만 충전할 수 있어요.';
    case 'INSUFFICIENT_ACCOUNT_BALANCE':
      return '계좌 잔액이 부족해요.';
    case 'BANK_TRANSFER_FAILED':
      return '송금에 실패했어요. 잠시 후 다시 시도해주세요.';
    case 'FINANCE_API_ERROR':
      return '은행 연결에 실패했어요. 잠시 후 다시 시도해주세요.';
    default:
      return '충전에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}

/** 환전 요청 실패 → 사용자 문구. */
export function cashoutErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INSUFFICIENT_POINT':
      return '보유 포인트가 부족해요.';
    case 'ACCOUNT_NOT_LINKED':
      return '연동된 계좌가 없어요.';
    default:
      return '환전 요청에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
