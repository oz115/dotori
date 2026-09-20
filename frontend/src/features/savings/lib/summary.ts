import type {
  SavingsSubscription,
  SavingsSubscriptionStatus,
  SavingsSummary,
} from '@/features/savings/model/types';
import { parseServerDate } from '@/shared/lib/datetime';

/**
 * 요약 화면이 쓰는 표시용 계산. 서버가 주는 값은 시각·금액 그대로라, "며칠 남았나" 같은
 * 화면 문구용 변환은 여기 모아 둔다 — 컴포넌트 안에서 날짜를 계산하면 테스트할 수가 없다.
 */

/**
 * 만기까지 남은 날. 날짜 경계로 세므로 오늘이 만기면 0이다.
 * 만기가 없거나(보통예금) 이미 지났으면 null — 둘 다 "다가올 만기"로 보여 줄 것이 없다.
 */
export function daysUntilMaturity(
  maturityAt: string | null,
  now: Date = new Date(),
): number | null {
  if (maturityAt == null) {
    return null;
  }

  // 서버가 마이크로초까지 보내므로 공용 파서를 거친다 — Hermes 는 그걸 Invalid Date 로 본다
  const maturity = parseServerDate(maturityAt);
  if (maturity == null) {
    return null;
  }

  const days = Math.ceil((startOfDay(maturity) - startOfDay(now)) / MS_PER_DAY);
  return days < 0 ? null : days;
}

const MS_PER_DAY = 24 * 60 * 60 * 1000;

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

/**
 * 부모 홈 카드가 "납입 중"으로 셀 가입 건.
 *
 * <p>가입 목록 API 는 끝난 건(COMPLETED·CANCELLED)까지 전부 내려준다. 그 건들은 이미
 * 지갑으로 빠져나가 아이의 예적금이 아니므로, 서버 요약(SavingsSummaryResponse)이 쓰는
 * 집합과 같게 ACTIVE·MATURED 만 남긴다. 두 곳이 다른 집합을 세면 "3계좌"라고 적어 놓고
 * 금액은 2계좌 몫만 나온다.
 */
const ONGOING_STATUSES: SavingsSubscriptionStatus[] = ['ACTIVE', 'MATURED'];

export function ongoingSavingsAccounts(
  subscriptions: SavingsSubscription[],
): SavingsSubscription[] {
  return subscriptions.filter((subscription) => ONGOING_STATUSES.includes(subscription.status));
}

/** "내 자산" 한 줄에 쓰는 예적금 자산 — 몇 건에 얼마가 들어 있는지 */
export type SavingsAssets = {
  /** 진행 중(ACTIVE) + 수령 대기(MATURED) 가입 건수 */
  count: number;
  /** 원금 + 아직 받지 않은 이자, 단위는 포인트 */
  totalAmount: number;
};

/**
 * 서버 요약(SavingsSummary)에서 "지금 내 예적금 자산"을 뽑는다.
 *
 * <p>ACTIVE·MATURED 만 센다 — 이미 수령(COMPLETED)하거나 해지(CANCELLED)한 건은 지갑으로
 * 빠져나가 더는 예적금 자산이 아니다. 서버 요약이 이 집합만 집계하므로 그 값을 그대로 쓴다.
 *
 * <p>금액은 서버가 원금(currentAmount)과 이자(accruedInterest)를 나눠 더한 totalAmount 다.
 * 클라이언트가 가입 목록을 다시 더하지 않는다 — 원금·이자를 섞어 계산하다 어긋나는 것을
 * 막고, 저축 탭(부모 "보유 현황"의 `개수 · 합계`)이 쓰는 원금 + 이자와 같은 값을 유지한다.
 * 이미 지갑으로 나간 보통예금 이자는 애초에 여기 없다.
 *
 * <p>포인트 단위 그대로라 원↔포인트 환산이 없다.
 */
export function summarizeSavingsAssets(
  summary: Pick<SavingsSummary, 'activeCount' | 'maturedCount' | 'totalAmount'>,
): SavingsAssets {
  return {
    count: summary.activeCount + summary.maturedCount,
    totalAmount: summary.totalAmount,
  };
}

/** 요약 카드에 계좌를 몇 줄까지 늘어놓을지 — 카드가 화면 절반 폭이라 이 이상은 안 들어간다 */
const PREVIEW_LIMIT = 3;

/** 미리보기 한 줄 — 아이콘을 고를 상품 유형과 이름만. 금액·진행률은 좁은 카드에 넣지 않는다 */
export type SavingsAccountPreviewItem = Pick<
  SavingsSubscription,
  'savingsSubscriptionId' | 'productName' | 'productType' | 'status' | 'maturityAt'
>;

/** 좁은 요약 카드용 계좌 미리보기 (앞 몇 개 + 나머지 개수) */
export type SavingsAccountsPreview = {
  items: SavingsAccountPreviewItem[];
  /** 보여주지 못한 나머지 계좌 수 (0이면 전부 보여준 것) */
  restCount: number;
};

/**
 * 부모 홈 "예적금" 카드처럼 좁은 자리에 쓰는 계좌 미리보기.
 * 계좌가 늘어나도 카드 높이가 밀리지 않게 앞 몇 개만 두고 나머지는 "외 N개"로 접는다.
 *
 * <p>이미 걸러 둔 목록을 받는다 — 여기서 상태까지 보면 호출부가 센 건수와 목록이 어긋날 수 있다.
 */
export function previewSavingsAccounts(
  accounts: SavingsSubscription[],
  limit: number = PREVIEW_LIMIT,
): SavingsAccountsPreview {
  return {
    items: accounts
      .slice(0, limit)
      .map(({ savingsSubscriptionId, productName, productType, status, maturityAt }) => ({
        savingsSubscriptionId,
        productName,
        productType,
        status,
        maturityAt,
      })),
    restCount: Math.max(accounts.length - limit, 0),
  };
}

/**
 * 계좌 줄 오른쪽에 붙일 만기 표시. 색은 화면이 정하므로 종류만 돌려준다 —
 * 라이브러리가 테마 색을 알면 색 토큰이 바뀔 때 여기까지 같이 고쳐야 한다.
 */
export type SavingsMaturityMarker = { kind: 'MATURED' } | { kind: 'COUNTDOWN'; days: number };

/**
 * 가입 건 하나의 만기 표시를 정한다. 보여 줄 것이 없으면 null 이다.
 *
 * 표시가 없는 경우가 둘 있다.
 *
 * - **보통예금** — 만기가 없는 수시입출금이라 셀 날짜가 없다 (maturityAt 이 null)
 * - **만기는 지났는데 상태가 아직 ACTIVE** — 정산 배치가 매일 00:20 에 돌아 상태를
 *   MATURED 로 바꾸므로 그 사이에 창이 생긴다. 이때 "만기"로 적으면 부모는 수령을
 *   기대하는데 아이 화면에서는 아직 누를 수 없어, 차라리 비워 둔다.
 */
export function savingsMaturityMarker(
  account: SavingsAccountPreviewItem,
  now: Date = new Date(),
): SavingsMaturityMarker | null {
  if (account.status === 'MATURED') {
    return { kind: 'MATURED' };
  }
  if (account.maturityAt == null) {
    return null;
  }

  const days = daysUntilMaturity(account.maturityAt, now);
  return days == null ? null : { kind: 'COUNTDOWN', days };
}
