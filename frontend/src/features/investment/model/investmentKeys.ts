/** 투자 관련 TanStack Query 키. 매수/매도 후에는 all 로 한꺼번에 무효화한다. */
export const investmentKeys = {
  all: ['investment'] as const,
  products: () => [...investmentKeys.all, 'products'] as const,
  product: (productId: number) => [...investmentKeys.all, 'product', productId] as const,
  priceHistory: (productId: number) => [...investmentKeys.all, 'priceHistory', productId] as const,
  holdings: (childId?: number) => [...investmentKeys.all, 'holdings', childId ?? 'me'] as const,
  news: (date?: string) => [...investmentKeys.all, 'news', date ?? 'today'] as const,
  newsReview: () => [...investmentKeys.all, 'newsReview'] as const,
  transactions: (childId?: number) =>
    [...investmentKeys.all, 'transactions', childId ?? 'me'] as const,
  newsDetail: (newsId: number) => [...investmentKeys.all, 'newsDetail', newsId] as const,
};
