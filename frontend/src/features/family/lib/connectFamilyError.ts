import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 자녀 가족 연동(초대 코드 입력) 실패 원인 → 사용자 문구. */
export function connectFamilyErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INVALID_FAMILY_CODE':
      return '코드가 올바르지 않거나 만료됐어요. 부모님께 다시 받아주세요.';
    case 'ALREADY_IN_FAMILY':
      return '이미 가족에 연결돼 있어요.';
    case 'FORBIDDEN':
      return '자녀 계정만 코드로 가족에 연결할 수 있어요.';
    default:
      return '가족 연결에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
