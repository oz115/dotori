import { toTransactionItem } from '@/features/investment/lib/mappers';
import type {
  InvestmentTransactionItem,
  TransactionResponse,
} from '@/features/investment/model/types';
import { parseServerDate } from '@/shared/lib/datetime';

/**
 * 전체 거래 응답을 표시한다. 새 서버의 손익(0 포함)은 그대로 사용한다.
 * 손익 필드가 없는 구버전 서버에 한해 동일 회사의 실제 매수·매도를 시간순으로 연결한다.
 * 현재 고정 회사 카탈로그의 이름은 서로 다르다. 필터링·페이지 분할된 목록에는 사용하지 않는다.
 */
export function toTransactionItems(rows: TransactionResponse[]): InvestmentTransactionItem[] {
  const result = new Map(rows.map((row) => [row.investmentTransactionId, toTransactionItem(row)]));
  const lots = new Map<string, { quantity: number; price: number }[]>();
  const ordered = rows.map((row) => ({ row, time: parseServerDate(row.createdAt)?.getTime() }));
  // 순서를 알 수 없으면 추정 손익으로 메우지 않는다.
  if (ordered.some(({ time }) => time == null))
    return rows.map((row) => result.get(row.investmentTransactionId)!);
  ordered.sort(
    (a, b) => a.time! - b.time! || a.row.investmentTransactionId - b.row.investmentTransactionId,
  );

  for (const { row } of ordered) {
    const inventory = lots.get(row.productName) ?? [];
    lots.set(row.productName, inventory);
    if (
      !Number.isInteger(row.quantity) ||
      row.quantity <= 0 ||
      row.totalAmount !== row.unitPrice * row.quantity
    ) {
      inventory.length = 0;
      continue;
    }
    if (row.transactionType === 'BUY') {
      inventory.push({ quantity: row.quantity, price: row.unitPrice });
      continue;
    }
    let remaining = row.quantity;
    let cost = 0;
    while (remaining > 0 && inventory.length > 0) {
      const lot = inventory[0];
      const quantity = Math.min(remaining, lot.quantity);
      cost += quantity * lot.price;
      remaining -= quantity;
      lot.quantity -= quantity;
      if (lot.quantity === 0) inventory.shift();
    }
    if (row.realizedProfit === undefined && row.buyPrice === undefined && remaining === 0) {
      const item = result.get(row.investmentTransactionId)!;
      item.buyPrice = cost / row.quantity;
      item.buyAmount = cost;
      item.realizedProfit = row.totalAmount - cost;
    }
  }
  return rows.map((row) => result.get(row.investmentTransactionId)!);
}

const DAY_MS = 86_400_000;
const KST_OFFSET_MS = 9 * 3_600_000;

/** 거래를 자르는 기간. 시작은 포함, 끝은 미포함이며 둘 다 KST 자정 기준 시각(ms)이다. */
export type TransactionPeriod = {
  start: number;
  end: number;
  /** 화면에 적을 날짜 범위 — 끝날은 마지막 날 (예: '2026.09.15 ~ 2026.09.21') */
  label: string;
};

const pad2 = (value: number) => String(value).padStart(2, '0');

/** KST 날짜를 `2026.09.15` 형식으로. */
function kstDate(ms: number): string {
  const date = new Date(ms + KST_OFFSET_MS);
  return `${date.getUTCFullYear()}.${pad2(date.getUTCMonth() + 1)}.${pad2(date.getUTCDate())}`;
}

/**
 * 오늘(KST)을 끝으로 하는 `days`일 단위 기간을 `offset`칸 뒤로 민다.
 * offset 0 은 오늘을 포함한 최근 `days`일, 1 은 그 바로 앞 `days`일, 2 는 그 앞…이다.
 * "7일 전 이 시각"이 아니라 날짜 단위로 자르므로, 7일 전 아침 거래도 오늘 밤에 같은 날로 함께 보인다.
 * 달력 주(월~일)가 아니라 오늘부터 세는 이유는 투자 주기 기준이 아직 정해지지 않았기 때문이다.
 */
export function transactionPeriod(
  days: number,
  offset: number,
  now: Date = new Date(),
): TransactionPeriod {
  const todayStart = Math.floor((now.getTime() + KST_OFFSET_MS) / DAY_MS) * DAY_MS - KST_OFFSET_MS;
  const end = todayStart + DAY_MS - offset * days * DAY_MS;
  const start = end - days * DAY_MS;
  return { start, end, label: `${kstDate(start)} ~ ${kstDate(end - DAY_MS)}` };
}

/** 거래의 시각(ms). 읽을 수 없으면 null — 언제 일어났는지 알 수 없는 거래는 어느 기간에도 넣지 않는다. */
function timeOf(item: InvestmentTransactionItem): number | null {
  return parseServerDate(item.createdAt)?.getTime() ?? null;
}

/** 기간 안의 거래만 남긴다. 순서는 그대로 둔다. */
export function transactionsInPeriod(
  items: InvestmentTransactionItem[],
  period: TransactionPeriod,
): InvestmentTransactionItem[] {
  return items.filter((item) => {
    const time = timeOf(item);
    return time != null && time >= period.start && time < period.end;
  });
}

/** 이 기간보다 더 오래된 거래가 있는지 — 이전 기간으로 넘어갈 수 있는지 판단한다. */
export function hasTransactionsBefore(
  items: InvestmentTransactionItem[],
  period: TransactionPeriod,
): boolean {
  return items.some((item) => {
    const time = timeOf(item);
    return time != null && time < period.start;
  });
}

/** 오늘(KST)을 포함한 최근 `days`일 안의 거래만 남긴다 (`transactionPeriod` 의 offset 0). */
export function recentTransactions(
  items: InvestmentTransactionItem[],
  days: number,
  now: Date = new Date(),
): InvestmentTransactionItem[] {
  return transactionsInPeriod(items, transactionPeriod(days, 0, now));
}

/** 판매 수익 합계. 원가를 모르는 판매가 하나라도 있으면 불완전한 합계를 확정값처럼 쓰지 않는다. */
export type RealizedProfitSummary = {
  /** 판매 건수 */
  count: number;
  total: number;
  incomplete: boolean;
};

/** 매도 대금이 아닌, 매수 원금을 뺀 확정 손익만 합산한다. */
export function summarizeRealizedProfit(items: InvestmentTransactionItem[]): RealizedProfitSummary {
  const sales = items.filter((item) => item.type === 'SELL');
  return {
    count: sales.length,
    total: sales.reduce((sum, item) => sum + (item.realizedProfit ?? 0), 0),
    incomplete: sales.some((item) => item.realizedProfit == null),
  };
}
