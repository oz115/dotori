import { dailyPricePoints } from '@/features/investment/lib/priceHistory';
import { companyImage, companyLabel } from '@/features/investment/lib/companyAssets';
import type {
  Holding,
  HoldingResponse,
  InvestmentNews,
  InvestmentSummary,
  InvestmentTransactionItem,
  NewsSummaryResponse,
  PriceHistoryResponse,
  ProductDetailResponse,
  ProductSummaryResponse,
  StockDetail,
  StockSummary,
  TransactionResponse,
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
  };
}

/** 상세 = 상세 응답 + 가격 이력 + 보유 여부(보유 목록에서 판단) 세 조각의 조합 */
export function toStockDetail(
  detail: ProductDetailResponse,
  history: PriceHistoryResponse['history'],
  holding: boolean,
): StockDetail {
  const points = dailyPricePoints(history);
  return {
    id: detail.investmentProductId,
    name: detail.name,
    category: companyLabel(detail.name, detail.category),
    image: companyImage(detail.name),
    description: detail.description ?? '',
    holding,
    currentPrice: detail.currentPrice,
    changeFromYesterday: detail.changeAmount,
    history: points,
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
    count: holdings.length,
    investedPoints: holdings.reduce((sum, h) => sum + h.buyPrice * h.quantity, 0),
    earnedPoints: holdings.reduce((sum, h) => sum + (h.currentPrice - h.buyPrice) * h.quantity, 0),
  };
}

export function toTransactionItem(transaction: TransactionResponse): InvestmentTransactionItem {
  return {
    id: transaction.investmentTransactionId,
    productName: transaction.productName,
    image: companyImage(transaction.productName),
    type: transaction.transactionType,
    totalAmount: transaction.totalAmount,
    buyPrice: transaction.buyPrice,
    buyAmount: transaction.buyPrice == null ? null : transaction.buyPrice * transaction.quantity,
    realizedProfit: transaction.realizedProfit,
    date: formatNewsDate(transaction.createdAt),
    createdAt: transaction.createdAt,
  };
}

export function toInvestmentNews(news: NewsSummaryResponse): InvestmentNews {
  return {
    id: news.investmentNewsId,
    title: news.title,
    impact: news.impact,
    relatedProductName: '',
    date: formatNewsDate(news.publishedAt),
    emoji: '📰',
  };
}
