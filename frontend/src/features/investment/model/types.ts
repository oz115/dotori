/**
 * 투자 기능이 다루는 도메인 타입. 아직 백엔드 투자 API가 없어 화면 구현에 필요한
 * 모양을 먼저 정의한 것 — API 명세(노션)가 확정되면 응답 타입에 맞춰 조정한다.
 */

/** 투자 요약 — 넣은 돈(원금)과 번 돈(수익), 단위는 포인트(P) */
export type InvestmentSummary = {
  investedPoints: number;
  earnedPoints: number;
};

/** 오늘의 뉴스 카드 한 장 */
export type InvestmentNews = {
  id: number;
  /** 분류 배지 (환경, 기술, 생활 …) */
  tag: string;
  title: string;
  body: string;
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
  /** 업종 분류 배지 텍스트 (친환경 기업 …) */
  category: string;
  /** 상품 일러스트 자리 표시자 — Figma 에셋 export 후 이미지로 교체 */
  emoji: string;
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
  /** 업종 분류 (생활 소비 …) */
  category: string;
  /** 종목 아이콘 자리 표시자 — Figma 에셋 export 후 이미지로 교체 */
  emoji: string;
  /** 현재가, 단위는 도토리 */
  currentPrice: number;
  /** 전일 대비 등락, 단위는 도토리 (+오름 / -내림) */
  change: number;
  /** 미니 차트용 최근 가격 추이 */
  trend: number[];
};
