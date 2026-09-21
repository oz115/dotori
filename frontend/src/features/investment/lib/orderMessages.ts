import { formatAcorns } from '@/features/investment/lib/format';
import { apiErrorCode, isNetworkError } from '@/shared/api';

/**
 * 매수/매도 결과·실패를 아이 눈높이 문구로 바꾼다. 상세 화면(TradeButtons)과
 * 여러 종목 한꺼번에 사고팔기(TradeSelectionBar)가 같은 말을 쓰게 한 곳에 모았다.
 */

/**
 * 매도 결과 한 줄 — "얼마를 벌었어요/잃었어요"만 말한다.
 * 산 값·판 값·잔액까지 다 늘어놓으면 아이가 결과를 못 읽는다는 피드백으로 줄였다.
 */
export function sellOutcomeMessage(profit: number | null | undefined): string {
  if (profit == null) {
    return '판 돈이 지갑으로 들어왔어요.';
  }
  if (profit > 0) {
    return `${formatAcorns(profit)}를 벌었어요!`;
  }
  if (profit < 0) {
    return `${formatAcorns(-profit)}를 잃었어요.`;
  }
  return '산 값 그대로 팔았어요.';
}

/** 주문 실패 문구. 네트워크 단절은 결과를 모르는 상태라 따로 안내한다. */
export function orderErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '거래가 완료됐는지 확인하지 못했어요. 거래 내역과 잔액을 먼저 확인해 주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INSUFFICIENT_POINT':
      return '도토리가 부족해요. 미션을 해내고 도토리를 모아 보세요!';
    case 'ALREADY_HOLDING':
      return '이미 가지고 있는 주식이에요. 한 회사는 1주만 살 수 있어요.';
    case 'PRODUCT_INACTIVE':
      return '지금은 거래가 잠시 멈춘 회사예요.';
    case 'INSUFFICIENT_QUANTITY':
      return '가지고 있지 않은 주식이에요.';
    default:
      return '잠시 후 다시 시도해 주세요.';
  }
}
