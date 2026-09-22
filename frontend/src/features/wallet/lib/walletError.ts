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

/**
 * 도토리 지급 실패 → 사용자 문구 (부모).
 * 지급은 돈을 새로 만드는 일이라 "됐는지 모르겠다"를 실패로 단정하지 않는다 — 네트워크가 끊기면
 * 아이 잔액을 확인하라고 안내하고, 같은 금액으로 다시 누르면 멱등키 덕에 두 번 지급되지 않는다.
 */
export function grantErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '지급 결과를 확인할 수 없어요. 아이의 도토리를 확인한 뒤 다시 시도해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'NOT_FAMILY_CHILD':
      return '우리 가족의 아이에게만 줄 수 있어요.';
    case 'FORBIDDEN':
      return '도토리는 부모만 줄 수 있어요.';
    case 'INVALID_REQUEST':
      return '금액을 다시 확인해주세요.';
    default:
      return '도토리를 주지 못했어요. 잠시 후 다시 시도해주세요.';
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

/**
 * 환전 승인·거절 실패 → 사용자 문구 (부모).
 *
 * 승인은 실제 송금이라 "실패했다"와 "결과를 모른다"를 갈라야 한다. 네트워크가 끊기거나
 * 응답이 오지 않은 경우 송금이 됐는지 안 됐는지 화면은 알 수 없으므로, 실패라고 단정하지
 * 않고 목록을 다시 확인하라고 안내한다.
 */
export function reviewCashoutErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '처리 결과를 확인할 수 없어요. 목록을 새로고침해 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'CASHOUT_REQUEST_ALREADY_PROCESSED':
      return '이미 처리된 요청이에요. 목록을 새로고침해주세요.';
    case 'CASHOUT_REQUEST_NOT_FOUND':
      return '요청을 찾을 수 없어요. 목록을 새로고침해주세요.';
    case 'ACCOUNT_NOT_LINKED':
      return '연결된 계좌가 없어 송금할 수 없어요. 계좌를 먼저 연결해주세요.';
    case 'INSUFFICIENT_ACCOUNT_BALANCE':
      return '계좌 잔액이 부족해요.';
    case 'BANK_TRANSFER_FAILED':
      return '송금에 실패해 도토리를 아이에게 돌려줬어요.';
    case 'FORBIDDEN':
      return '이 요청을 처리할 권한이 없어요.';
    default:
      return '처리에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
