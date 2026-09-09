export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
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

  if (loginId.trim().length < 4) {
    errors.loginId = '아이디는 4자 이상 입력해주세요.';
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
