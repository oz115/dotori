import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 계좌 이체 실패 → 사용자 문구. */
export function transferErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'ACCOUNT_NOT_LINKED':
      return '연동된 계좌가 없어요.';
    case 'SELF_TRANSFER_NOT_ALLOWED':
      return '내 계좌로는 이체할 수 없어요.';
    case 'TRANSFER_LIMIT_EXCEEDED':
      return '1회 이체 한도를 넘었어요.';
    case 'INSUFFICIENT_ACCOUNT_BALANCE':
      return '계좌 잔액이 부족해요.';
    case 'BANK_TRANSFER_FAILED':
    case 'FINANCE_API_ERROR':
      return '계좌번호를 확인하고 다시 시도해주세요.';
    default:
      return '이체에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
