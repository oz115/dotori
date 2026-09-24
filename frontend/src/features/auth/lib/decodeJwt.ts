/**
 * JWT payload 를 검증 없이 디코드한다. (서명 검증은 서버 몫이고, 여기서 읽는 값은
 * 우리 SecureStore/HTTPS 경로로 받은 것이라 신뢰한다.)
 *
 * access token 에는 `sub`(userId), `role`, `exp` 가 들어 있다.
 */
export type AccessTokenClaims = {
  sub: string;
  role: 'PARENT' | 'CHILD';
  exp: number;
};

function base64UrlDecode(input: string): string {
  const base64 = input.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  // RN(Hermes)/브라우저 공통 전역
  return atob(padded);
}

export function decodeJwt<T = AccessTokenClaims>(token: string): T {
  const payload = token.split('.')[1];
  if (!payload) {
    throw new Error('malformed jwt');
  }
  return JSON.parse(base64UrlDecode(payload)) as T;
}
