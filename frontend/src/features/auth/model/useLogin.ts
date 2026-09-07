import { useMutation } from '@tanstack/react-query';

import { savedLoginId } from '@/features/auth/lib/savedLoginId';
import { signIn } from '@/features/auth/model/session';
import type { LoginBody } from '@/features/auth/model/types';

type LoginInput = LoginBody & { rememberId: boolean };

/** 로그인 + 아이디 저장/삭제. mutation 밖에서도 테스트할 수 있게 분리. */
export async function performLogin({ loginId, password, rememberId }: LoginInput): Promise<void> {
  await signIn({ loginId, password });
  await (rememberId ? savedLoginId.set(loginId) : savedLoginId.clear());
}

/**
 * 로그인 mutation. 성공 시 세션이 authenticated 로 바뀌고
 * RootLayout 의 Stack.Protected 가 알아서 홈으로 이동한다.
 */
export function useLogin() {
  return useMutation({ mutationFn: performLogin });
}
