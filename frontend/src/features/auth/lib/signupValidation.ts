export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

/** 아이디 길이 범위. 상한은 서버(@Size(max = 50))와 같은 값이다. */
export const LOGIN_ID_MIN = 4;
export const LOGIN_ID_MAX = 50;

/** 공백이 섞인 아이디를 막는 문구. 입력하는 즉시 보여 준다. */
export const LOGIN_ID_BLANK_ERROR = '아이디에 공백이 들어가면 안 돼요.';

/**
 * 아이디 검증. 통과하면 undefined, 아니면 사용자에게 보일 문구.
 *
 * <p>공백을 먼저 본다. 예전에는 `trim()` 한 길이만 봐서 `hong gil dong` 같은 아이디가
 * 그대로 가입됐다 — 서버 제약도 길이(@Size)뿐이라 아무도 막지 않았다 (QA). 로그인할 때
 * 눈에 보이지 않는 한 칸 때문에 아이디가 틀리는 것이 가장 나쁜 결과다.
 *
 * <p>가운데 공백만이 아니라 앞뒤 공백도 막는다. 앞뒤는 잘라 주면 될 것 같지만, 입력칸에는
 * 공백이 보이는데 "사용 가능" 이라고 하면 무엇이 저장됐는지 알 수 없다.
 */
export function validateLoginId(loginId: string): string | undefined {
  if (/\s/.test(loginId)) return LOGIN_ID_BLANK_ERROR;
  const length = loginId.trim().length;
  if (length < LOGIN_ID_MIN) return `아이디는 ${LOGIN_ID_MIN}자 이상 입력해주세요.`;
  if (length > LOGIN_ID_MAX) return `아이디는 ${LOGIN_ID_MAX}자 이하로 입력해주세요.`;
  return undefined;
}

type AccountInput = {
  loginId: string;
  password: string;
  passwordConfirm: string;
  name: string;
};

/** 계정 정보 단계 검증. 필드별 에러 메시지 맵을 반환한다 (빈 맵이면 통과). */
export function validateAccountStep({
  loginId,
  password,
  passwordConfirm,
  name,
}: AccountInput): Partial<Record<keyof AccountInput, string>> {
  const errors: Partial<Record<keyof AccountInput, string>> = {};

  const loginIdError = validateLoginId(loginId);
  if (loginIdError) {
    errors.loginId = loginIdError;
  }
  if (password.length < 8) {
    errors.password = '비밀번호는 8자 이상이어야 해요.';
  }
  if (passwordConfirm !== password) {
    errors.passwordConfirm = '비밀번호가 일치하지 않아요.';
  }
  if (name.trim().length === 0) {
    errors.name = '이름을 입력해주세요.';
  }

  return errors;
}
