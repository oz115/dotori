// Created: 2026-09-19 13:19:13
import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 마이페이지 - 이름 변경 실패 원인 → 사용자 문구. */
export function updateProfileErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INVALID_REQUEST':
      return '이름을 확인해주세요.';
    default:
      return '정보 수정에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}

/** 마이페이지 - 비밀번호 변경 실패 원인 → 사용자 문구. */
export function changePasswordErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'PASSWORD_MISMATCH':
      return '현재 비밀번호가 일치하지 않아요.';
    case 'INVALID_PASSWORD_FORMAT':
      return '새 비밀번호는 공백 없이 8자 이상 64자 이하여야 해요.';
    default:
      return '비밀번호 변경에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
