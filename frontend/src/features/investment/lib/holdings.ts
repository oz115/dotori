import type { Holding, HoldingsSummary } from '@/features/investment/model/types';

/**
 * 보유 화면 상단 합계 계산. "번 돈"은 (현재가 - 산 가격) × 수량의 합 —
 * 종목당 1주 규칙이라 사실상 현재가와 산 가격의 차이 합이다.
 */
export function summarizeHoldings(holdings: Holding[]): HoldingsSummary {
  return {
    count: holdings.length,
    totalValue: holdings.reduce((sum, h) => sum + h.currentPrice * h.quantity, 0),
    totalEarned: holdings.reduce((sum, h) => sum + (h.currentPrice - h.buyPrice) * h.quantity, 0),
  };
}

/** 요약 카드에 종목을 몇 줄까지 늘어놓을지 — 카드가 화면 절반 폭이라 이 이상은 안 들어간다 */
const PREVIEW_LIMIT = 3;

/** 미리보기 한 줄 — 아이콘과 이름만. 가격·손익은 좁은 카드에 넣지 않는다 */
export type HoldingPreviewItem = Pick<Holding, 'id' | 'name' | 'image'>;

/** 좁은 요약 카드용 보유 종목 미리보기 (앞 몇 개 + 나머지 개수) */
export type HoldingsPreview = {
  items: HoldingPreviewItem[];
  /** 보여주지 못한 나머지 종목 수 (0이면 전부 보여준 것) */
  restCount: number;
};

/**
 * 부모 홈 "투자" 카드처럼 좁은 자리에 쓰는 보유 종목 미리보기.
 * 종목이 늘어나도 카드 높이가 밀리지 않게 앞 몇 개만 두고 나머지는 "외 N개"로 접는다.
 *
 * 이름만이 아니라 아이콘까지 함께 넘긴다. 화면이 `companyImage(name)` 를 다시 부르면
 * 이름 → 아이콘 매핑이 두 곳에 생기는데, 매핑은 이미 응답을 {@link Holding} 으로 바꾸는
 * 단계에서 한 번 끝나 있다.
 */
export function previewHoldings(
  holdings: Holding[],
  limit: number = PREVIEW_LIMIT,
): HoldingsPreview {
  return {
    items: holdings.slice(0, limit).map(({ id, name, image }) => ({ id, name, image })),
    restCount: Math.max(holdings.length - limit, 0),
  };
}
