/**
 * 요약 화면이 쓰는 표시용 계산. 서버가 주는 값은 시각·금액 그대로라, "며칠 남았나" 같은
 * 화면 문구용 변환은 여기 모아 둔다 — 컴포넌트 안에서 날짜를 계산하면 테스트할 수가 없다.
 */

/**
 * 만기까지 남은 날. 날짜 경계로 세므로 오늘이 만기면 0이다.
 * 만기가 없거나(보통예금) 이미 지났으면 null — 둘 다 "다가올 만기"로 보여 줄 것이 없다.
 */
export function daysUntilMaturity(
  maturityAt: string | null,
  now: Date = new Date(),
): number | null {
  if (maturityAt == null) {
    return null;
  }

  const maturity = new Date(maturityAt);
  if (Number.isNaN(maturity.getTime())) {
    return null;
  }

  const days = Math.ceil((startOfDay(maturity) - startOfDay(now)) / MS_PER_DAY);
  return days < 0 ? null : days;
}

const MS_PER_DAY = 24 * 60 * 60 * 1000;

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}
