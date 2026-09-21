import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 구매 실패(SHOP-06) 사유를 화면 문구로 바꾼다. 백엔드 ErrorCode 순서와 맞춘다. */
export function purchaseErrorMessage(error: unknown): string {
  if (isNetworkError(error)) return '서버에 연결할 수 없어요.';

  switch (apiErrorCode(error)) {
    case 'PRICE_NOT_EVALUATED':
      return '아직 가격이 산정되지 않은 상품이에요.';
    case 'DAILY_LIMIT_EXCEEDED':
      // 문장이 둘이라 줄바꿈해 한눈에 읽히게 한다 (모달 error 는 한 <Text> 라 \n 이 먹는다).
      return '오늘 이미 구매한 상품이에요.\n내일 다시 시도해주세요.';
    case 'OUT_OF_STOCK':
      return '방금 재고가 다 떨어졌어요.';
    case 'INSUFFICIENT_POINT':
      return '보유 도토리가 부족해요.';
    // 부모가 삭제한 상품을 구매하려 한 경우 — 재시도해도 소용없으니 이유를 분명히 알린다.
    case 'PRODUCT_NOT_FOUND':
      return '부모님이 삭제한 상품이라 더 이상 구매할 수 없어요.\n목록에서 다른 상품을 골라주세요.';
    default:
      return '구매에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
