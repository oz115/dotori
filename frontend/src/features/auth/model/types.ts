export type UserRole = 'PARENT' | 'CHILD';

/** 로그인 세션이 들고 있는 최소 사용자 정보. 이름 등 프로필 상세는 별도 조회. */
export type AuthUser = {
  userId: number;
  role: UserRole;
  /** 로그인 응답에서 받은 이름 (앱 재시작 후 복원 시에는 없을 수 있음) */
  name?: string;
};

/**
 * loading  — 부팅 중, 저장된 토큰으로 세션 복원 시도 중
 * authenticated — 유효한 세션 있음
 * guest    — 세션 없음 (로그인 필요)
 */
export type AuthStatus = 'loading' | 'authenticated' | 'guest';

/** POST /api/v1/auth/login */
export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    userId: number;
    name: string;
    role: UserRole;
  };
};

/** GET /api/v1/users/{userId} — 마이페이지가 쓰는 회원 정보 */
export type UserProfile = {
  userId: number;
  loginId: string;
  name: string;
  email: string;
  role: UserRole;
  emailVerified: boolean;
  createdAt: string;
};

/** POST /api/v1/auth/refresh */
export type RefreshResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  refreshToken: string;
};

export type LoginBody = {
  loginId: string;
  password: string;
};

export type SignupBody = {
  loginId: string;
  password: string;
  name: string;
  email: string;
  role: UserRole;
  /** 이메일 인증 확인에서 발급받은 토큰 (인증된 이메일만 가입 가능) */
  verificationToken: string;
};

/**
 * 인증코드 발송 용도. SIGNUP 만 동작이 다르다 —
 * 이미 가입된 이메일이면 코드를 보내지 않고 409 DUPLICATE_EMAIL.
 * (FIND_ID/PASSWORD_RESET/미지정은 계정 존재 여부를 노출하지 않음)
 */
export type EmailVerificationPurpose = 'SIGNUP' | 'FIND_ID' | 'PASSWORD_RESET';

/** POST /api/v1/auth/email-verification/confirm */
export type EmailConfirmResponse = {
  verified: boolean;
  verificationToken: string;
};
