import {
  companyEmoji,
  companyImage,
  companyLabel,
} from '@/features/investment/lib/companyAssets';
import type {
  Holding,
  HoldingResponse,
  InvestmentNews,
  InvestmentSummary,
  NewsSummaryResponse,
  PriceHistoryResponse,
  PricePoint,
  ProductDetailResponse,
  ProductSummaryResponse,
  StockDetail,
  StockSummary,
} from '@/features/investment/model/types';

/**
 * 서버 응답 → 화면용 타입 변환. 서버는 이름·숫자·ISO 일시만 주고,
 * 아이콘·업종 라벨·이모지·날짜 라벨 같은 표시용 값은 여기서 붙인다.
 * UI 컴포넌트는 화면용 타입만 알게 해서, 명세가 바뀌어도 이 파일만 고치면 된다.
 */

/** 서버 일시(ISO 8601, KST 오프셋)의 날짜 부분만 떼어낸다 — Date 파싱(타임존) 이슈 회피 */
function datePart(iso: string): { year: string; month: string; day: string } {
  const [year, month, day] = iso.slice(0, 10).split('-');
  return { year, month, day };
}

/** '2026-09-14T00:05:00+09:00' → '2026. 09. 14' (뉴스 카드 날짜) */
export function formatNewsDate(iso: string): string {
  const { year, month, day } = datePart(iso);
  return `${year}. ${month}. ${day}`;
}

/** '2026-09-14T00:05:00+09:00' → '9/14' (차트 x축 라벨) */
export function formatDayLabel(iso: string): string {
  const { month, day } = datePart(iso);
  return `${Number(month)}/${day}`;
}

export function toStockSummary(product: ProductSummaryResponse): StockSummary {
  return {
    id: product.investmentProductId,
    name: product.name,
    category: companyLabel(product.name, product.category),
    image: companyImage(product.name),
    currentPrice: product.currentPrice,
    change: product.changeAmount,
    // 목록 응답에는 가격 추이가 없다 — 비워 두면 StockCard 가 미니 차트를 숨긴다
    trend: [],
  };
}

/** 상세 = 상세 응답 + 가격 이력 + 보유 여부(보유 목록에서 판단) 세 조각의 조합 */
export function toStockDetail(
  detail: ProductDetailResponse,
  history: PriceHistoryResponse['history'],
  holding: boolean,
): StockDetail {
  const points: PricePoint[] = history.map((row) => ({
    label: formatDayLabel(row.recordedAt),
    price: row.price,
  }));
  return {
    id: detail.investmentProductId,
    name: detail.name,
    category: companyLabel(detail.name, detail.category),
    image: companyImage(detail.name),
    description: detail.description ?? '',
    holding,
    currentPrice: detail.currentPrice,
    changeFromYesterday: detail.changeAmount,
    // 아직 가격이 움직인 적 없는 가족은 이력이 비어 있다 — 현재가 한 점으로 차트를 그린다
    history: points.length > 0 ? points : [{ label: '오늘', price: detail.currentPrice }],
  };
}

export function toHolding(holding: HoldingResponse): Holding {
  return {
    id: holding.investmentProductId,
    name: holding.productName,
    image: companyImage(holding.productName),
    quantity: holding.quantity,
    buyPrice: holding.buyPrice,
    currentPrice: holding.currentPrice,
  };
}

/** 투자 요약(넣은 돈/번 돈) — 별도 API 없이 보유 목록에서 계산한다 (팀 결정) */
export function summarizeInvestment(holdings: Holding[]): InvestmentSummary {
  return {
    investedPoints: holdings.reduce((sum, h) => sum + h.buyPrice * h.quantity, 0),
    earnedPoints: holdings.reduce((sum, h) => sum + (h.currentPrice - h.buyPrice) * h.quantity, 0),
  };
}

export function toInvestmentNews(news: NewsSummaryResponse): InvestmentNews {
  const productName = news.relatedProducts[0]?.productName ?? '';
  const category = news.relatedProducts[0]?.category ?? null;
  return {
    id: news.investmentNewsId,
    tag: productName !== '' ? companyLabel(productName, category) : '오늘의 소식',
    title: news.title,
    body:
      news.impact === 'POSITIVE'
        ? '가격이 오를 수 있는 소식이에요.'
        : '가격이 내릴 수 있는 소식이에요.',
    impact: news.impact,
    relatedProductName: productName,
    date: formatNewsDate(news.publishedAt),
    emoji: productName !== '' ? companyEmoji(productName) : '📰',
  };
}
