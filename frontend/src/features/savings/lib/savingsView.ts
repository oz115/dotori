import { savingsProductImage, savingsProductName } from '@/features/savings/lib/savingsAssets';
import type {
  SavingsCard,
  SavingsCardLine,
  SavingsGroup,
  SavingsProductResponse,
  SavingsProductType,
  SavingsSubscription,
  SavingsTransactionItem,
  SavingsTransactionResponse,
  SavingsTransactionType,
} from '@/features/savings/model/types';
import { parseServerDate } from '@/shared/lib/datetime';
import { formatPoint } from '@/shared/lib/money';

/**
 * 상품 + 내 가입 목록 → 저축 메인 카드 뷰모델.
 * 가입된 상품은 진행 정보(납입액·이자·만기까지·진행바)를, 미가입 상품은 스펙
 * (이자율·기간·가입 가능 금액)을 보여준다. 시간 의존 계산은 now 를 받아 테스트를 가능하게 한다.
 */

const DEPOSIT_TYPES: SavingsProductType[] = ['ORDINARY', 'SIMPLE_INTEREST', 'COMPOUND_INTEREST'];

export function savingsGroupOf(productType: SavingsProductType): SavingsGroup {
  return DEPOSIT_TYPES.includes(productType) ? 'DEPOSIT' : 'INSTALLMENT';
}

export function savingsGroupLabel(group: SavingsGroup): string {
  return group === 'DEPOSIT' ? '예금' : '적금';
}

/**
 * 저축 메인 상단 분류 칩 (상점·미션과 같은 FilterChips). '전체' 는 칩 쪽이 붙이므로
 * 여기 넣지 않는다. 아이 화면과 부모 화면이 같은 순서·같은 문구를 써야 해서 여기 둔다.
 */
export const SAVINGS_GROUP_FILTERS: { key: SavingsGroup; label: string }[] = [
  { key: 'DEPOSIT', label: savingsGroupLabel('DEPOSIT') },
  { key: 'INSTALLMENT', label: savingsGroupLabel('INSTALLMENT') },
];

/**
 * 만기까지 남은 날짜 (오늘 포함 올림). 이미 지났으면 0.
 *
 * 파싱할 수 없는 값도 0 이다 — 화면에 NaN 이 찍히는 것보다는 낫다.
 * 서버가 마이크로초까지 보내므로 공용 파서를 거친다 (Hermes 는 그걸 Invalid Date 로 본다).
 */
export function daysUntil(iso: string, now: Date): number {
  const target = parseServerDate(iso);
  if (target == null) {
    return 0;
  }
  return Math.max(0, Math.ceil((target.getTime() - now.getTime()) / (24 * 60 * 60 * 1000)));
}

const WEEK_MS = 7 * 24 * 60 * 60 * 1000;

/**
 * 가입 시각부터 지금까지 지난 '완전한 주' 수. 서버 규칙(ChronoUnit.WEEKS.between)과 같은 절삭.
 * 1주가 지나야 1이 된다 — 눈금 하나가 차는 기준이자, 적금 "1주 1회 납입" 판정 기준이기도 하다.
 * 시각을 못 읽으면 0 (진행 표시가 NaN 으로 깨지지 않게 한다).
 */
export function weeksSince(startedAt: string, now: Date): number {
  const start = parseServerDate(startedAt);
  if (start == null) {
    return 0;
  }
  return Math.max(0, Math.floor((now.getTime() - start.getTime()) / WEEK_MS));
}

/** 가입~만기 사이의 총 주차 수 (거치식 예금 눈금 개수). 4주 상품이면 4. */
function totalWeeksBetween(startedAt: string, maturityAt: string): number {
  const start = parseServerDate(startedAt);
  const end = parseServerDate(maturityAt);
  if (start == null || end == null || end.getTime() <= start.getTime()) {
    return 0;
  }
  return Math.max(0, Math.round((end.getTime() - start.getTime()) / WEEK_MS));
}

function maturityLine(subscription: SavingsSubscription, now: Date): SavingsCardLine {
  if (subscription.status === 'MATURED') {
    return { label: '만기', value: '달성!' };
  }
  if (subscription.maturityAt == null) {
    return { label: '만기', value: '없음' };
  }
  return { label: '만기까지', value: `${daysUntil(subscription.maturityAt, now)}일` };
}

type SavingsSegments = { total: number; filled: number };

function subscribedLines(
  subscription: SavingsSubscription,
  now: Date,
): { lines: SavingsCardLine[]; segments?: SavingsSegments } {
  switch (subscription.contributionStyle) {
    // 보통예금 — 수시입출금, 무기한이라 눈금(만기)이 없다
    case 'FREE':
      return {
        lines: [
          { label: '모은 돈', value: formatPoint(subscription.currentAmount) },
          { label: '이자', value: '매주 지급' },
        ],
      };
    // 단리·복리예금 — 가입 때 맡긴 목돈이 만기까지 이자를 불린다. 눈금은 주차(1주=1칸).
    // 가입한 날이 1주차라 첫 칸은 바로 찬다 (경과 주수 + 1). 적금이 가입 시 1회차로
    // 시작해 첫 칸이 차는 것과 같은 규칙이다.
    case 'LUMP_SUM': {
      const total =
        subscription.maturityAt != null
          ? totalWeeksBetween(subscription.startedAt, subscription.maturityAt)
          : 0;
      const filled =
        subscription.status === 'MATURED'
          ? total
          : Math.min(total, weeksSince(subscription.startedAt, now) + 1);
      return {
        lines: [
          { label: '예치 금액', value: formatPoint(subscription.principal) },
          { label: '쌓인 이자', value: formatPoint(subscription.accruedInterest) },
          maturityLine(subscription, now),
        ],
        segments: total > 0 ? { total, filled } : undefined,
      };
    }
    // 소액·중기·장기적금 — 매주 같은 금액을 약속한 회차만큼 납입. 눈금은 회차(=주차).
    case 'FIXED_INSTALLMENT': {
      const total = subscription.installmentCount ?? 0;
      return {
        lines: [
          { label: '매주', value: formatPoint(subscription.principal) },
          { label: '모은 돈', value: formatPoint(subscription.currentAmount) },
          { label: '납입', value: `${subscription.contributionCount}회 / ${total}회` },
        ],
        segments:
          total > 0
            ? { total, filled: Math.min(total, subscription.contributionCount) }
            : undefined,
      };
    }
  }
}

/** 이 상품에 넣을 수 있는 최대 금액이 무엇을 뜻하는지. 카드 한 칸에 들어가야 해서 짧게 */
export function savingsMaxAmountLabel(product: SavingsProductResponse): string {
  switch (product.contributionStyle) {
    case 'FREE':
      return '최대 저금';
    case 'LUMP_SUM':
      return '최대 저금';
    case 'FIXED_INSTALLMENT':
      return '매주 최대';
  }
}

/**
 * 미가입 카드에 싣는 줄 — "최대로 넣으면 도토리를 얼마 더 받는지".
 *
 * 이자율(%)을 쓰지 않는다. 초등학생에게 "15%" 는 읽을 수는 있어도 도토리를 몇 개 받는지
 * 알 수 없는 값이고, 상품끼리 비교도 안 된다 — 이자율 15% 인 단리예금과 10% 인 복리예금
 * 중 무엇이 많이 주는지는 퍼센트만 보면 거꾸로 읽힌다.
 *
 * 그래서 최대 금액으로 실제 받는 도토리를 계산해 보여 준다. 계산은 서버 정산과 같은 식
 * (estimateReturn)이라 화면이 약속한 값과 실제 지급액이 어긋나지 않는다.
 *
 * 최대 금액을 앞에 두는 순서가 중요하다. 받는 도토리는 "최대로 넣었을 때" 의 값이라,
 * 기준 금액이 먼저 보이지 않으면 아무 금액에나 그만큼 주는 것처럼 읽힌다.
 */
function productLines(product: SavingsProductResponse): SavingsCardLine[] {
  const preview = estimateReturn(product, product.maxAmount);
  const lines: SavingsCardLine[] = [
    { label: savingsMaxAmountLabel(product), value: formatPoint(product.maxAmount) },
  ];
  if (product.termWeeks != null) {
    lines.push({ label: '기간', value: `${product.termWeeks}주` });
  }
  // 보통예금은 만기가 없어 한 주치가 전부다 (weekly). 나머지는 만기에 한 번에 받는다.
  lines.push({
    label: preview.weekly ? '매주 도토리' : '받는 도토리',
    value: `+ ${formatPoint(preview.interest)}`,
  });
  return lines;
}

/**
 * 가입 전 미리보기 — 이 금액으로 가입하면 만기에 얼마를 받는지.
 * 서버 정산({@code SavingsProductType.weeklyInterest})과 같은 식으로 계산해야
 * 화면이 약속한 금액과 실제 지급액이 어긋나지 않는다.
 * 보통예금은 만기가 없어 "매주 받는 이자" 한 주치를 준다 (weekly: true).
 */
export function estimateReturn(
  product: SavingsProductResponse,
  amount: number,
): { principal: number; interest: number; total: number; weekly: boolean } {
  const rate = product.interestRate;
  switch (product.contributionStyle) {
    // 보통예금 — 잔액을 한 주 유지하면 받는 이자
    case 'FREE': {
      const interest = Math.round((amount * rate) / 100);
      return { principal: amount, interest, total: amount + interest, weekly: true };
    }
    // 단리는 매주 (원금 × 기간 이자율 ÷ 기간), 복리는 매주 (원금+쌓인 이자) × 주간 이자율
    case 'LUMP_SUM': {
      const term = product.termWeeks ?? 0;
      let interest = 0;
      if (product.productType === 'COMPOUND_INTEREST') {
        for (let week = 0; week < term; week += 1) {
          interest += Math.round(((amount + interest) * rate) / 100);
        }
      } else if (term > 0) {
        interest = Math.round((amount * rate) / 100 / term) * term;
      }
      return { principal: amount, interest, total: amount + interest, weekly: false };
    }
    // 정액 적립식 — 매주 납입으로 원금이 늘어나며 그 주 잔액 기준으로 이자가 쌓인다
    case 'FIXED_INSTALLMENT': {
      const term = product.termWeeks ?? 0;
      const count = product.installmentCount ?? 0;
      let balance = amount;
      let remaining = count - 1;
      let interest = 0;
      for (let week = 0; week < term; week += 1) {
        if (term > 0) {
          interest += Math.round((balance * rate) / 100 / term);
        }
        if (remaining > 0) {
          balance += amount;
          remaining -= 1;
        }
      }
      const principal = amount * count;
      return { principal, interest, total: principal + interest, weekly: false };
    }
  }
}

/** '2026-09-17T12:00:00+09:00' → '2026. 09. 17' (거래 이력 날짜, 투자 이력과 같은 표기) */
export function formatSavingsDate(iso: string): string {
  const [year, month, day] = iso.slice(0, 10).split('-');
  return `${year}. ${month}. ${day}`;
}

/** 거래 종류 → 아이 눈높이 배지 문구와 지갑 기준 방향 */
const TRANSACTION_BADGES: Record<SavingsTransactionType, { badge: string; sign: '+' | '-' }> = {
  INITIAL_DEPOSIT: { badge: '넣었어요', sign: '-' },
  CONTRIBUTION: { badge: '넣었어요', sign: '-' },
  INTEREST: { badge: '이자 받았어요', sign: '+' },
  EARLY_TERMINATION_PAYOUT: { badge: '돌려받았어요', sign: '+' },
  MATURITY_PAYOUT: { badge: '만기 수령', sign: '+' },
};

export function toSavingsTransactionItem(
  transaction: SavingsTransactionResponse,
): SavingsTransactionItem {
  const { badge, sign } = TRANSACTION_BADGES[transaction.transactionType];
  return {
    id: transaction.savingsTransactionId,
    // 서버 productName 대신 화면 이름 (목록·상세와 같은 이름이어야 한다)
    productName: savingsProductName(transaction.productType),
    image: savingsProductImage(transaction.productType),
    badge,
    sign,
    amount: transaction.amount,
    date: formatSavingsDate(transaction.createdAt),
  };
}

/**
 * 카드 목록 조립. 상품당 카드 하나 — 진행 중(ACTIVE·MATURED) 가입이 있으면
 * 그 가입의 진행 정보를 싣고, 없으면 상품 스펙을 싣는다.
 * (해지·수령 완료된 가입은 "보유"로 치지 않는다)
 */
export function buildSavingsCards(
  products: SavingsProductResponse[],
  subscriptions: SavingsSubscription[],
  now: Date,
): SavingsCard[] {
  const activeByProductId = new Map(
    subscriptions
      .filter((s) => s.status === 'ACTIVE' || s.status === 'MATURED')
      .map((s) => [s.savingsProductId, s] as const),
  );

  return products.map((product) => {
    const subscription = activeByProductId.get(product.savingsProductId);
    const base = {
      productId: product.savingsProductId,
      // 화면 이름은 앱이 갖는다 (savingsAssets 의 PRODUCT_NAMES 주석 참고)
      name: savingsProductName(product.productType),
      image: savingsProductImage(product.productType),
      group: savingsGroupOf(product.productType),
    };
    if (subscription == null) {
      return { ...base, subscribed: false, matured: false, lines: productLines(product) };
    }
    const { lines, segments } = subscribedLines(subscription, now);
    return {
      ...base,
      subscriptionId: subscription.savingsSubscriptionId,
      subscribed: true,
      matured: subscription.status === 'MATURED',
      lines,
      segments,
    };
  });
}
