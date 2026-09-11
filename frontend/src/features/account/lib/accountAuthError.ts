import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 1원 송금 요청 실패 → 사용자 문구. */
export function accountAuthRequestErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'ACCOUNT_ALREADY_LINKED':
      return '이미 연동된 계좌가 있어요.';
    case 'NOT_FOUND':
      return '금융망 정보를 찾을 수 없어요. 다시 로그인해주세요.';
    case 'FINANCE_API_ERROR':
      return '은행 연결에 실패했어요. 계좌번호를 확인하고 다시 시도해주세요.';
    default:
      return '1원 송금에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}

/** 1원 인증 코드 검증 실패 → 사용자 문구. */
export function accountVerifyErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'AUTH_CODE_MISMATCH':
      return '인증 코드가 일치하지 않아요. 입금내역을 다시 확인해주세요.';
    case 'ACCOUNT_ALREADY_LINKED':
      return '이미 연동된 계좌가 있어요.';
    case 'FINANCE_API_ERROR':
      return '인증 확인에 실패했어요. 잠시 후 다시 시도해주세요.';
    default:
      return '인증에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
