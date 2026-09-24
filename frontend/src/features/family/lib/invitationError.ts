import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 초대 코드 발급 실패 원인 → 사용자 문구. */
export function invitationErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'FAMILY_NOT_FOUND':
      return '가족 정보를 찾을 수 없어요. 다시 로그인해주세요.';
    case 'FORBIDDEN':
      return '가족을 만든 부모만 코드를 발급할 수 있어요.';
    default:
      return '코드 발급에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
