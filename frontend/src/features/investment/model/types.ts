import type { ImageSourcePropType } from 'react-native';

/**
 * 투자 기능이 다루는 도메인 타입. 아직 백엔드 투자 API가 없어 화면 구현에 필요한
 * 모양을 먼저 정의한 것 — API 명세(노션)가 확정되면 응답 타입에 맞춰 조정한다.
 */

/** 투자 요약 — 넣은 돈(원금)과 번 돈(수익), 단위는 포인트(P) */
export type InvestmentSummary = {
  investedPoints: number;
  earnedPoints: number;
};

/** 오늘의 뉴스 카드 한 장 + 상세 팝업 내용 */
export type InvestmentNews = {
  id: number;
  /** 분류 배지 (환경, 기술, 생활 …) */
  tag: string;
  title: string;
  /** 카드에 보이는 요약 한 줄 */
  body: string;
  /** 상세 팝업 본문 (API 의 content). 목록 응답에는 없어 팝업이 상세 API 로 채운다 */
  content?: string;
  /** 종목 가격에 주는 영향 방향 (API 의 impact) */
  impact: 'POSITIVE' | 'NEGATIVE';
  /** 관련 회사 이름 (API 의 relatedProducts[0].productName — 설계상 뉴스당 1개) */
  relatedProductName: string;
  /** 표시용 날짜 문자열 (예: 2025. 09. 07) */
  date: string;
  /** 일러스트 자리 표시자 — Figma 에셋 export 후 이미지로 교체 */
  emoji: string;
};

/** 가격 추이의 한 점 (상세 차트용) */
export type PricePoint = {
  /** x축 표시용 날짜 라벨 (예: 9/01) */
  label: string;
  price: number;
};

/** 투자 상품 상세 */
export type StockDetail = {
  id: number;
  name: string;
  /** 업종 분류 배지 텍스트 (아이스크림, 항공 …) */
  category: string;
  /** 종목 아이콘 (Figma 투자 종목 시트에서 추출) */
  image: ImageSourcePropType;
  description: string;
  /** 아이가 이 상품을 보유 중인지 (보유중 배지) */
  holding: boolean;
  /** 현재가, 단위는 도토리 */
  currentPrice: number;
  /** 어제 대비 등락, 단위는 도토리 */
  changeFromYesterday: number;
  /** 일자별 가격 추이 (차트) */
  history: PricePoint[];
};

/** 보유 화면 상단 합계 (가지고 있는 상품·지금 가치·번 돈) */
export type HoldingsSummary = {
  count: number;
  /** 보유 종목 현재가 합, 단위는 도토리 */
  totalValue: number;
  /** 번 돈, 단위는 도토리 */
  totalEarned: number;
};

/** 주식 리스트의 종목 한 줄 */
export type StockSummary = {
  id: number;
  name: string;
  /** 업종 분류 (아이스크림, 항공 …) */
  category: string;
  /** 종목 아이콘 (Figma 투자 종목 시트에서 추출) */
  image: ImageSourcePropType;
  /** 현재가, 단위는 도토리 */
  currentPrice: number;
  /** 전일 대비 등락, 단위는 도토리 (+오름 / -내림) */
  change: number;
  /** 미니 차트용 최근 가격 추이 */
  trend: number[];
};

/** 보유 종목 한 줄 (보유 화면). 서버 investment_holdings 행의 화면 표현 */
export type Holding = {
  id: number;
  name: string;
  image: ImageSourcePropType;
  /** 보유 수량 — 종목당 1주 규칙이라 지금은 항상 1 */
  quantity: number;
  /** 샀을 때 가격, 단위는 도토리 */
  buyPrice: number;
  /** 현재가, 단위는 도토리 */
  currentPrice: number;
};

/** 거래 내역 화면의 한 줄 */
export type InvestmentTransactionItem = {
  id: number;
  productName: string;
  image: ImageSourcePropType;
  type: 'BUY' | 'SELL';
  /** 총 거래 금액, 단위는 도토리 (1주 규칙이라 체결 단가와 같음) */
  totalAmount: number;
  /** 표시용 날짜 문자열 (예: 2026. 09. 16) */
  date: string;
};

/* =========================================================
   서버 응답 타입 — 노션 API 명세(투자)와 1:1. 화면용 타입으로의
   변환은 lib/mappers 가 담당한다.
   ========================================================= */

export type InvestmentCategory = 'STOCK' | 'BOND' | 'ETF' | 'FUND' | 'OTHER';
export type InvestmentImpact = 'POSITIVE' | 'NEGATIVE';
export type InvestmentTransactionType = 'BUY' | 'SELL';

/** GET /investments/products 의 한 건 */
export type ProductSummaryResponse = {
  investmentProductId: number;
  name: string;
  category: InvestmentCategory | null;
  currentPrice: number;
  /** 전일 대비 변동 포인트 (+오름 / -내림) */
  changeAmount: number;
  isActive: boolean;
};

/** GET /investments/products/{id} */
export type ProductDetailResponse = {
  investmentProductId: number;
  name: string;
  description: string | null;
  category: InvestmentCategory | null;
  currentPrice: number;
  changeAmount: number;
  previousPrice: number;
  isActive: boolean;
  /** 가격 갱신 일시 (ISO 8601, KST) */
  updatedAt: string;
};

/** GET /investments/products/{id}/price-history */
export type PriceHistoryResponse = {
  investmentProductId: number;
  history: { price: number; recordedAt: string }[];
};

/** GET /investments/holdings 의 한 건 */
export type HoldingResponse = {
  investmentProductId: number;
  productName: string;
  quantity: number;
  buyPrice: number;
  currentPrice: number;
};

/** 뉴스와 연결된 종목 (설계상 뉴스당 1개) */
export type RelatedProductResponse = {
  investmentProductId: number;
  productName: string;
  category: InvestmentCategory | null;
};

/** GET /investments/news 의 한 건 — 본문(content)은 상세에만 있다 */
export type NewsSummaryResponse = {
  investmentNewsId: number;
  title: string;
  impact: InvestmentImpact;
  relatedProducts: RelatedProductResponse[];
  publishedAt: string;
};

/** GET /investments/news/{id} */
export type NewsDetailResponse = NewsSummaryResponse & {
  content: string;
};

/** GET /investments/transactions 의 한 건 */
export type TransactionResponse = {
  investmentTransactionId: number;
  productName: string;
  transactionType: InvestmentTransactionType;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  createdAt: string;
};

/** POST /investments/orders/buy·sell 응답 */
export type OrderResponse = {
  investmentTransactionId: number;
  transactionType: InvestmentTransactionType;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  pointBalanceAfter: number;
  createdAt: string;
};
