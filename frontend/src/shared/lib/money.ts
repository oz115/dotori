/**
 * 금액(원) 표시·검증 공용 유틸.
 *
 * 화면이 다루는 금액은 원(₩), 서버 내부 저장은 포인트(10원 = 1포인트)다.
 * 원↔포인트 환산이 필요해지면(지갑·상점) 그 함수도 여기에 추가한다 —
 * 환산 규칙이 화면마다 흩어지면 서로 다른 비율로 계산하는 사고가 난다.
 */

/** 1234567 → "1,234,567원" */
export function formatWon(won: number): string {
  return `${won.toLocaleString('ko-KR')}원`;
}

/** 1234567 → "1,234,567" (단위 없이 천 단위 구분) */
export function formatWonNumber(won: number): string {
  return won.toLocaleString('ko-KR');
}

/**
 * 5000 → "5천 원", 10000 → "1만 원", 30000 → "3만 원", 15000 → "1만 5천 원".
 * 천 단위로 안 떨어지면 전체 표기로 폴백한다 (프리셋 칩 라벨용).
 */
export function formatWonShort(won: number): string {
  if (won % 1000 !== 0 || won <= 0) return formatWon(won);
  const man = Math.floor(won / 10000);
  const cheon = Math.floor((won % 10000) / 1000);
  const parts: string[] = [];
  if (man > 0) parts.push(`${man}만`);
  if (cheon > 0) parts.push(`${cheon}천`);
  return `${parts.join(' ')} 원`;
}
