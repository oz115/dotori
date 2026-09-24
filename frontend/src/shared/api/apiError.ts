import type { AxiosError } from 'axios';

/**
 * 백엔드 공통 에러 응답(`ErrorResponse { code, message, timestamp }`) 판별 유틸.
 * 화면·feature 는 이 두 함수로 "무슨 에러인지"를 읽어 사용자 문구로 매핑한다.
 */

type ApiErrorBody = { code?: string; message?: string };

/** BE 공통 에러 응답의 `code` (ErrorCode 상수명). 없으면 undefined. */
export function apiErrorCode(error: unknown): string | undefined {
  return (error as AxiosError<ApiErrorBody>)?.response?.data?.code;
}

/** 서버에 아예 못 닿은 경우 (응답 없음). */
export function isNetworkError(error: unknown): boolean {
  const e = error as AxiosError;
  return !!e && !e.response;
}
