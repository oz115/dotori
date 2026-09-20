import type { HoldingsSummary } from '@/features/investment';
import type { SavingsAssets } from '@/features/savings';
import { formatPoint } from '@/shared/lib/money';

/**
 * 자녀 홈 "내 자산 현황"의 예적금·투자 줄에 실을 문구. 조회 상태(로딩·오류·빈 상태·값)를
 * 한 곳에서 문구로 바꿔 두어, 화면은 받은 문구를 그대로 그리기만 한다.
 *
 * 금액은 전부 포인트 단위 그대로다 — 원↔포인트 환산이 끼지 않는다.
 */
export type AssetRowText = {
  subtitle: string;
  amount: string;
};

/** 값이 아직 없을 때의 금액 자리 */
const NO_AMOUNT = '-';

type QueryState<T> = {
  isPending: boolean;
  isError: boolean;
  data: T | undefined;
};

/** 예적금 줄. 가입 0건이면 0P 와 "가입한 상품 없음" — 가짜 예시 값을 넣지 않는다. */
export function savingsAssetRow({
  isPending,
  isError,
  data,
}: QueryState<SavingsAssets>): AssetRowText {
  if (isError) {
    return { subtitle: '불러오지 못했어요', amount: NO_AMOUNT };
  }
  if (isPending || data == null) {
    return { subtitle: '불러오는 중이에요', amount: NO_AMOUNT };
  }
  if (data.count === 0) {
    return { subtitle: '가입한 상품 없음', amount: formatPoint(0) };
  }
  return { subtitle: `${data.count}개 상품에 저축 중`, amount: formatPoint(data.totalAmount) };
}

/**
 * 투자 줄. 금액은 산 값이 아니라 지금 시세로 본 보유 가치(totalValue)다 — 투자 탭 "보유 자산"과
 * 같은 값이다. 종목당 1주 규칙이라 개수는 곧 회사 수다.
 */
export function investmentAssetRow({
  isPending,
  isError,
  data,
}: QueryState<HoldingsSummary>): AssetRowText {
  if (isError) {
    return { subtitle: '불러오지 못했어요', amount: NO_AMOUNT };
  }
  if (isPending || data == null) {
    return { subtitle: '불러오는 중이에요', amount: NO_AMOUNT };
  }
  if (data.count === 0) {
    return { subtitle: '보유 종목 없음', amount: formatPoint(0) };
  }
  return { subtitle: `${data.count}개 회사 보유 중`, amount: formatPoint(data.totalValue) };
}
