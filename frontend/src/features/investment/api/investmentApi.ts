import { httpClient } from '@/shared/api';
import type {
  HoldingResponse,
  NewsDetailResponse,
  NewsSummaryResponse,
  OrderResponse,
  PriceHistoryResponse,
  ProductDetailResponse,
  ProductSummaryResponse,
  TransactionResponse,
} from '@/features/investment/model/types';

/**
 * 투자 원시 HTTP 호출. 가족은 서버가 토큰에서 알아내므로 familyId 를 넘기지 않는다.
 * 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당.
 */
export const investmentApi = {
  getProducts: () =>
    httpClient
      .get<{ products: ProductSummaryResponse[] }>('/api/v1/investments/products')
      .then((res) => res.data.products),

  getProduct: (productId: number) =>
    httpClient
      .get<ProductDetailResponse>(`/api/v1/investments/products/${productId}`)
      .then((res) => res.data),

  /** startDate/endDate 는 'YYYY-MM-DD'. 안 주면 서버 기본(최근 30일) */
  getPriceHistory: (productId: number, range?: { startDate: string; endDate: string }) =>
    httpClient
      .get<PriceHistoryResponse>(`/api/v1/investments/products/${productId}/price-history`, {
        params: range,
      })
      .then((res) => res.data.history),

  /** childId 없으면 본인 보유 (자녀용). 부모가 자녀 것을 볼 때만 childId 를 넘긴다 */
  getHoldings: (childId?: number) =>
    httpClient
      .get<{ holdings: HoldingResponse[] }>('/api/v1/investments/holdings', {
        params: childId != null ? { childId } : undefined,
      })
      .then((res) => res.data.holdings),

  /** date 없으면 오늘(KST) 뉴스 */
  getNews: (date?: string) =>
    httpClient
      .get<{ news: NewsSummaryResponse[] }>('/api/v1/investments/news', {
        params: date != null ? { date } : undefined,
      })
      .then((res) => res.data.news),

  getNewsDetail: (newsId: number) =>
    httpClient
      .get<NewsDetailResponse>(`/api/v1/investments/news/${newsId}`)
      .then((res) => res.data),

  /** 거래 이력 (최신순). 필터 없이 전체 — 기간·유형 필터가 필요해지면 params 를 확장한다 */
  getTransactions: () =>
    httpClient
      .get<{ transactions: TransactionResponse[] }>('/api/v1/investments/transactions')
      .then((res) => res.data.transactions),

  /** 매수 — 종목당 1주 규칙이라 수량 없이 종목만 보낸다 */
  buy: (productId: number) =>
    httpClient
      .post<OrderResponse>('/api/v1/investments/orders/buy', { investmentProductId: productId })
      .then((res) => res.data),

  /** 매도 — 들고 있는 1주 전량 */
  sell: (productId: number) =>
    httpClient
      .post<OrderResponse>('/api/v1/investments/orders/sell', { investmentProductId: productId })
      .then((res) => res.data),
};
