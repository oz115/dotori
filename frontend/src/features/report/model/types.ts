/**
 * 리포트 API(RPT-02·03·04) 응답 타입.
 *
 * 유형 값은 서버의 `point_transactions.transaction_type` ENUM 라벨을 그대로 쓴다.
 * 지갑 거래내역(WLT-06)의 `transactionType` 과 같은 값이라, 화면은 사유 표기를 한 번만
 * 만들면 두 곳에 같이 쓸 수 있다.
 */

/** 포인트가 늘어나는 사유 8개 */
export type IncomeType =
  | 'CHARGE'
  | 'GRANT'
  | 'ALLOWANCE'
  | 'CHORE_REWARD'
  | 'QUIZ_REWARD'
  | 'SAVINGS_WITHDRAWAL'
  | 'INVESTMENT_SELL'
  | 'REFUND';

/** 포인트가 줄어드는 사유 4개 */
export type SpendingType = 'PURCHASE' | 'CASHOUT' | 'SAVINGS_DEPOSIT' | 'INVESTMENT_BUY';

/** 수입 유형별 한 줄. 서버가 금액 내림차순으로 정렬해서 준다 */
export type IncomeTypeAmount = {
  incomeType: IncomeType;
  amount: number;
  count: number;
};

/** 소비 유형별 한 줄 */
export type SpendingTypeAmount = {
  spendingType: SpendingType;
  amount: number;
  count: number;
};

/**
 * 일자별 집계 한 줄.
 *
 * **거래가 없는 날은 행이 아예 없다.** 주 단위 화면은 축이 월~일 7칸으로 고정이라
 * 빈 날을 화면에서 채워야 한다 (`lib/weeklyFlow`).
 */
export type ReportDailyAmount = {
  /** KST 기준 일자 (YYYY-MM-DD) */
  date: string;
  amount: number;
};

/** GET .../reports/income */
export type IncomeReport = {
  total: number;
  byType: IncomeTypeAmount[];
  byPeriod: ReportDailyAmount[];
};

/** GET .../reports/spending */
export type SpendingReport = {
  total: number;
  byType: SpendingTypeAmount[];
  byPeriod: ReportDailyAmount[];
};

/**
 * GET .../reports/assets — 기간 파라미터가 없는 현재 시점 스냅샷.
 *
 * 서버가 `totalAssets` 를 세 자산의 합으로 채워서 준다. 평가 손익은 이미
 * `investment.valuation` 에 반영되어 있어 총액에 다시 더하지 않는다.
 */
export type AssetReport = {
  childId: number;
  totalAssets: number;
  point: { balance: number };
  savings: { totalAmount: number; activeCount: number };
  investment: { valuation: number; profitLoss: number };
};

/** 조회 기간 (KST 날짜, 양끝 포함) */
export type ReportPeriod = {
  startDate: string;
  endDate: string;
};
