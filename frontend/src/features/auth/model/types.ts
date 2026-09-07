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

/** POST /api/v1/auth/login — refreshToken도 body로 내려온다 (모바일이라 쿠키 대신 body 방식) */
export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  refreshToken: string;
  user: {
    userId: number;
    name: string;
    role: UserRole;
  };
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
};
