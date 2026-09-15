import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 구매 실패(SHOP-06) 사유를 화면 문구로 바꾼다. 백엔드 ErrorCode 순서와 맞춘다. */
export function purchaseErrorMessage(error: unknown): string {
  if (isNetworkError(error)) return '서버에 연결할 수 없어요.';

  switch (apiErrorCode(error)) {
    case 'PRICE_NOT_EVALUATED':
      return '아직 가격이 산정되지 않은 상품이에요.';
    case 'DAILY_LIMIT_EXCEEDED':
      return '오늘 이미 구매한 상품이에요. 내일 다시 시도해주세요.';
    case 'OUT_OF_STOCK':
      return '방금 재고가 다 떨어졌어요.';
    case 'INSUFFICIENT_POINT':
      return '보유 도토리가 부족해요.';
    default:
      return '구매에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
