export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

/** 아이디 길이 범위. 상한은 서버(@Size(max = 50))와 같은 값이다. */
export const LOGIN_ID_MIN = 4;
export const LOGIN_ID_MAX = 50;

/** 아이디 길이 검증. 통과하면 undefined, 아니면 사용자에게 보일 문구. */
export function validateLoginId(loginId: string): string | undefined {
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
