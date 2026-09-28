/**
 * 아이디 찾기 → 비밀번호 재설정으로 이메일 인증 결과를 넘기는 일회용 전달 슬롯.
 *
 * 서버는 아이디 찾기에서 verificationToken 을 소모하지 않고(비밀번호 재설정 성공 때 소모),
 * 같은 이메일의 토큰을 TTL(10분) 안에서 이어 쓰게 해 둔다. 프론트가 이 결과를 넘기지 않으면
 * 사용자가 같은 이메일로 인증을 두 번 하게 된다.
 *
 * 토큰이라 화면 전환 파라미터(URL)에 싣지 않고 메모리에만 잠깐 둔다. 받는 화면은 `peek` 으로
 * 읽어 자기 state 에 옮긴 뒤 `clear` 로 비운다. 서버 TTL 보다 조금 짧게 잡은 유효시간이 지나면 버린다.
 * (읽으면서 비우는 방식은 state 초기화 함수가 두 번 불리는 개발 모드에서 값을 잃는다)
 */
export type RecoveryVerification = { email: string; verificationToken: string };

/** 서버 토큰 TTL(10분)보다 짧게 — 화면 이동 중에 만료되는 일이 없게 */
const HANDOFF_TTL_MS = 9 * 60 * 1000;

let pending: { value: RecoveryVerification; expiresAt: number } | null = null;

export const recoveryHandoff = {
  put(value: RecoveryVerification, now: number = Date.now()): void {
    pending = { value, expiresAt: now + HANDOFF_TTL_MS };
  },

  /** 전달된 인증 결과를 읽는다(비우지 않는다). 없거나 만료됐으면 null. */
  peek(now: number = Date.now()): RecoveryVerification | null {
    return pending && pending.expiresAt > now ? pending.value : null;
  },

  clear(): void {
    pending = null;
  },
};
