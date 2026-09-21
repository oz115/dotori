/**
 * 이메일 인증코드 입력 규칙. 서버(RandomCodeGenerator)가 혼동 문자를 뺀 대문자·숫자 6자리를 만들고,
 * 확인 API 도 대문자로 정규화해 비교하므로 숫자 키패드가 아니라 문자 키패드가 필요하다.
 */
export const EMAIL_CODE_LENGTH = 6;

/** 입력값에서 영문·숫자만 남기고 대문자로 바꿔 최대 길이로 자른다. */
export function normalizeEmailCode(text: string): string {
  return text
    .replace(/[^0-9A-Za-z]/g, '')
    .toUpperCase()
    .slice(0, EMAIL_CODE_LENGTH);
}

export function isCompleteEmailCode(code: string): boolean {
  return code.length === EMAIL_CODE_LENGTH;
}
