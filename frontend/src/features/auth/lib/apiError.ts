import type { AxiosError } from 'axios';

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
