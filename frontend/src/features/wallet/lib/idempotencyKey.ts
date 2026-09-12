/**
 * 충전·환전처럼 실제 돈이 움직이는 요청에 붙이는 멱등키.
 * 네트워크 재시도로 같은 요청이 두 번 가도 서버가 한 번만 반영하게 한다
 * (`Idempotency-Key` 헤더 — PointWalletController 참고).
 *
 * 암호학적으로 안전할 필요는 없다 — 화면 한 번 열렸을 때 겹치지만 않으면 된다.
 */
export function newIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
