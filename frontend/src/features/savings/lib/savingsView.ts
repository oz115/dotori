import { savingsProductImage } from '@/features/savings/lib/savingsAssets';
import type {
  SavingsCard,
  SavingsCardLine,
  SavingsGroup,
  SavingsProductResponse,
  SavingsProductType,
  SavingsSubscription,
} from '@/features/savings/model/types';
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

/** 만기까지 남은 날짜 (오늘 포함 올림). 이미 지났으면 0 */
export function daysUntil(iso: string, now: Date): number {
  const diff = new Date(iso).getTime() - now.getTime();
  return Math.max(0, Math.ceil(diff / (24 * 60 * 60 * 1000)));
}

/** 기간형 상품의 경과 진행률 (0~1) */
function periodProgress(startedAt: string, maturityAt: string, now: Date): number {
  const start = new Date(startedAt).getTime();
  const end = new Date(maturityAt).getTime();
  if (end <= start) {
    return 1;
  }
  return Math.min(1, Math.max(0, (now.getTime() - start) / (end - start)));
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

function subscribedLines(
  subscription: SavingsSubscription,
  now: Date,
): { lines: SavingsCardLine[]; progress?: number } {
  switch (subscription.contributionStyle) {
    // 보통예금 — 수시입출금, 이자는 매주 지갑으로 바로 들어와 쌓이지 않는다
    case 'FREE':
      return {
        lines: [
          { label: '모은 돈', value: formatPoint(subscription.currentAmount) },
          { label: '이자', value: '매주 지급' },
        ],
      };
    // 단리·복리예금 — 가입 때 맡긴 목돈이 만기까지 이자를 불린다
    case 'LUMP_SUM':
      return {
        lines: [
          { label: '예치 금액', value: formatPoint(subscription.principal) },
          { label: '쌓인 이자', value: formatPoint(subscription.accruedInterest) },
          maturityLine(subscription, now),
        ],
        progress:
          subscription.maturityAt != null
            ? periodProgress(subscription.startedAt, subscription.maturityAt, now)
            : undefined,
      };
    // 소액·장기적금 — 매주 같은 금액을 약속한 회차만큼 납입
    case 'FIXED_INSTALLMENT': {
      const total = subscription.installmentCount ?? 0;
      return {
        lines: [
          { label: '매주', value: formatPoint(subscription.principal) },
          { label: '모은 돈', value: formatPoint(subscription.currentAmount) },
          { label: '납입', value: `${subscription.contributionCount}회 / ${total}회` },
        ],
        progress: total > 0 ? Math.min(1, subscription.contributionCount / total) : undefined,
      };
    }
    // 목표적금 — 목표 금액을 채우면 즉시 지급
    case 'FLEXIBLE_GOAL':
      return {
        lines: [
          { label: '목표', value: formatPoint(subscription.principal) },
          { label: '모은 돈', value: formatPoint(subscription.currentAmount) },
          maturityLine(subscription, now),
        ],
        progress:
          subscription.principal > 0
            ? Math.min(1, subscription.currentAmount / subscription.principal)
            : undefined,
      };
  }
}

function productLines(product: SavingsProductResponse): SavingsCardLine[] {
  const lines: SavingsCardLine[] = [{ label: '이자율', value: `${product.interestRate}%` }];
  if (product.termWeeks != null) {
    lines.push({ label: '기간', value: `${product.termWeeks}주` });
  }
  lines.push({ label: '최대', value: formatPoint(product.maxAmount) });
  return lines;
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
      name: product.name,
      image: savingsProductImage(product.productType),
      group: savingsGroupOf(product.productType),
    };
    if (subscription == null) {
      return { ...base, subscribed: false, matured: false, lines: productLines(product) };
    }
    const { lines, progress } = subscribedLines(subscription, now);
    return {
      ...base,
      subscriptionId: subscription.savingsSubscriptionId,
      subscribed: true,
      matured: subscription.status === 'MATURED',
      lines,
      progress,
    };
  });
}
