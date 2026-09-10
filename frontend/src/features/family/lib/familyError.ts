import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 가족 생성(온보딩) 실패 원인 → 사용자 문구. */
export function createFamilyErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'ALREADY_IN_FAMILY':
      return '이미 가족에 속해 있어요.';
    case 'FORBIDDEN':
      return '부모 계정만 가족을 만들 수 있어요.';
    case 'INVALID_REQUEST':
      return '입력한 금액을 확인해주세요. 1,000원 이상, 1,000원 단위로 입력해요.';
    default:
      return '가족 생성에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
