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
