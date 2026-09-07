import { create } from 'zustand';

import type { UserRole } from '@/features/auth/model/types';

/**
 * 회원가입 퍼널이 여러 화면(약관 → 이메일 → 계정정보 → 역할)에 걸쳐 모으는 값.
 * 화면 진입 시 `reset()` 으로 초기화하고, "다음" 마다 `patch()` 로 채운다.
 */
type SignupState = {
  agreedOptional: boolean; // 선택 약관 동의 여부 (필수 약관은 진행 조건이라 따로 안 둠)
  email: string;
  emailVerified: boolean;
  loginId: string;
  password: string;
  name: string;
  role: UserRole | null;

  patch: (values: Partial<Omit<SignupState, 'patch' | 'reset'>>) => void;
  reset: () => void;
};

const initial = {
  agreedOptional: false,
  email: '',
  emailVerified: false,
  loginId: '',
  password: '',
  name: '',
  role: null as UserRole | null,
};

export const useSignupStore = create<SignupState>((set) => ({
  ...initial,
  patch: (values) => set(values),
  reset: () => set(initial),
}));
