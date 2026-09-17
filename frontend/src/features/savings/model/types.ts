import type { ImageSourcePropType } from 'react-native';

/**
 * 예적금 서버 계약. 경로가 `/families/{familyId}/children/{childId}/savings/...` 라서
 * familyId·childId 를 함께 넘겨야 한다 — 조회는 아이 본인과 그 가족의 부모 모두 할 수 있다.
 * 화면용 카드 뷰모델(SavingsCard)로의 변환은 lib/savingsView 가 담당한다.
 */

export type SavingsProductType =
  | 'ORDINARY'
  | 'SIMPLE_INTEREST'
  | 'COMPOUND_INTEREST'
  | 'SMALL_INSTALLMENT'
  | 'GOAL_INSTALLMENT'
  | 'LONG_TERM_INSTALLMENT';

/** 납입 방식. 화면이 진행률을 무엇으로 그릴지 정하는 값이다 */
export type ContributionStyle = 'FREE' | 'LUMP_SUM' | 'FIXED_INSTALLMENT' | 'FLEXIBLE_GOAL';

export type PayoutTiming = 'WEEKLY_IMMEDIATE' | 'MATURITY_LUMP_SUM' | 'ON_GOAL_ACHIEVED';

export type SavingsSubscriptionStatus = 'ACTIVE' | 'MATURED' | 'CANCELLED' | 'COMPLETED';

/** 저축 메인 상단 세그먼트 — 예금 | 적금 */
export type SavingsGroup = 'DEPOSIT' | 'INSTALLMENT';

/**
 * GET .../savings/summary 응답 — 부모 홈 카드 한 장을 채우는 집계.
 * 진행 중(ACTIVE)·수령 대기(MATURED) 건만 세므로, 이미 수령·해지한 건은 잡히지 않는다.
 */
export type SavingsSummary = {
  /** 납입·정산이 돌고 있는 가입 건수 */
  activeCount: number;
  /** 만기에 닿았지만 아이가 아직 수령하지 않은 건수 */
  maturedCount: number;
  /** 납입된 원금 누계 (이자 제외) */
  totalCurrentAmount: number;
  /** 아직 받지 않은 이자 누계. 보통예금 이자는 매주 지갑으로 바로 나가 여기 잡히지 않는다 */
  totalAccruedInterest: number;
  /** 원금 + 이자 */
  totalAmount: number;
  /** 진행 중인 건 가운데 가장 빨리 오는 만기. 만기가 있는 건이 없으면 null */
  nearestMaturityAt: string | null;
};

/** GET .../savings/products 응답 한 건. maxAmount 는 이 가족 기준 가입 가능 최대 금액 */
export type SavingsProductResponse = {
  savingsProductId: number;
  name: string;
  productType: SavingsProductType;
  description: string | null;
  interestRate: number;
  rewardRatio: number;
  contributionStyle: ContributionStyle;
  payoutTiming: PayoutTiming;
  termWeeks: number | null;
  installmentCount: number | null;
  educationGoal: string | null;
  preSignupMessage: string | null;
  maxAmount: number;
};

/** GET .../savings/subscriptions 응답 한 건 */
export type SavingsSubscription = {
  savingsSubscriptionId: number;
  savingsProductId: number;
  productName: string;
  productType: SavingsProductType;
  contributionStyle: ContributionStyle;
  /** 정액 적립식이 약속한 총 납입 회차. 해당 없는 상품은 null */
  installmentCount: number | null;
  /** 가입 때 확정한 금액 (거치식=최초원금, 정액 적립식=회당 약정액, 목표적금=목표 금액) */
  principal: number;
  currentAmount: number;
  accruedInterest: number;
  totalAmount: number;
  contributionCount: number;
  status: SavingsSubscriptionStatus;
  startedAt: string;
  /** 만기 시각. 보통예금(무기한)은 null */
  maturityAt: string | null;
  nextSettlementAt: string | null;
  receivedAt: string | null;
};

/** 카드 안의 정보 한 줄 (예치 금액 3,000P 처럼 라벨·값 쌍) */
export type SavingsCardLine = {
  label: string;
  value: string;
};

/** 저축 메인의 상품 카드 하나 — 가입 여부에 따라 줄 내용과 진행바가 달라진다 */
export type SavingsCard = {
  productId: number;
  subscriptionId?: number;
  name: string;
  image: ImageSourcePropType;
  group: SavingsGroup;
  subscribed: boolean;
  /** 만기 도달로 수령 대기 중 (배지 표시용) */
  matured: boolean;
  lines: SavingsCardLine[];
  /** 0~1. undefined 면 진행바를 그리지 않는다 (보통예금·미가입) */
  progress?: number;
};
