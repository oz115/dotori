import { companyEmoji, companyImage, companyLabel } from '@/features/investment/lib/companyAssets';
import type {
  Holding,
  HoldingResponse,
  InvestmentNews,
  InvestmentSummary,
  InvestmentTransactionItem,
  NewsSummaryResponse,
  PriceHistoryResponse,
  PricePoint,
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

/**
 * TEMP: 등락 그래프 미리보기용 목업. AI 서버가 아직 안 붙어 있어(로컬 개발 환경)
 * `investment_price_history`가 비어 있는 동안, 실제 데이터가 쌓였을 때 어떻게 보일지
 * 확인하려고 최근 `days`일치 가짜 랜덤워크를 만든다. 오늘 값은 항상 실제 currentPrice와
 * 같게 맞춘다 — 화면 다른 곳(현재가 숫자 등)이랑 어긋나 보이면 안 되니까.
 *
 * 실제 가격 이력이 쌓이면(history.length > 0) toStockDetail이 이 함수를 아예 안 부르니,
 * AI 서버 연동이 끝나면 이 함수와 호출부만 지우면 된다.
 */
function mockPriceHistory(currentPrice: number, days = 14): PricePoint[] {
  const points: PricePoint[] = [];
  let price = currentPrice;
  const today = new Date();

  for (let i = 0; i < days; i += 1) {
    const date = new Date(today);
    date.setDate(date.getDate() - i);
    points.push({ label: `${date.getMonth() + 1}/${date.getDate()}`, price: Math.round(price) });
    const drift = (Math.random() - 0.5) * currentPrice * 0.06; // 하루당 ±3% 안팎으로 걷는다
    price = Math.max(1, price - drift);
  }

  return points.reverse();
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
    // 아직 가격이 움직인 적 없는 가족은 이력이 비어 있다 — TEMP: 지금은 목업으로 채운다
    // (mockPriceHistory 주석 참고, AI 서버 연동되면 지울 것)
    history: points.length > 0 ? points : mockPriceHistory(detail.currentPrice),
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

export function toTransactionItem(transaction: TransactionResponse): InvestmentTransactionItem {
  return {
    id: transaction.investmentTransactionId,
    productName: transaction.productName,
    image: companyImage(transaction.productName),
    type: transaction.transactionType,
    totalAmount: transaction.totalAmount,
    date: formatNewsDate(transaction.createdAt),
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
