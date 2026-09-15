/** 상점 상품 관련 서버 계약. 경로가 `/families/{familyId}/store/products` 라서 familyId 만 있으면 된다. */

export type StoreProductCategory = 'GAME' | 'SNACK' | 'TOY' | 'OUTING' | 'PRIVILEGE' | 'ETC';

/** GET .../store/products 목록의 한 건 */
export type StoreProductSummary = {
  storeProductId: number;
  name: string;
  description: string | null;
  category: StoreProductCategory;
  /** AI 가치 산정 전이면 null (= 가격 산정 중) */
  price: number | null;
  quantity: number;
  createdAt: string;
};

/**
 * POST .../store/products 요청 바디 (SHOP-03).
 * price 는 없다 — AI 가 카테고리·설명을 근거로 비동기 산정한다.
 */
export type StoreProductCreateBody = {
  name: string;
  description: string | null;
  category: StoreProductCategory;
  quantity: number;
};

/**
 * PATCH .../store/products/{productId} 요청 바디 (SHOP-04). 부분 수정이라 다 선택적이지만,
 * 이 앱의 수정 화면은 항상 전체 필드를 채워서 보낸다(현재 값 프리필 방식이라 실질적으로 전체 수정).
 * price 는 AI 산정값이지만 부모가 직접 덮어쓸 수 있다 — 산정 전(null)이면 화면에서 아예 막는다.
 */
export type StoreProductUpdateBody = {
  name: string;
  description: string | null;
  category: StoreProductCategory;
  price?: number;
  quantity: number;
};

/**
 * GET .../store/purchases 목록의 한 건 (SHOP-07).
 * category 는 지금 상품의 카테고리다 — 상품이 삭제됐으면 null (화면에서 "기타"로 대신 표시).
 */
export type PurchaseHistoryItem = {
  purchaseId: number;
  storeProductId: number;
  productName: string;
  category: StoreProductCategory | null;
  quantity: number;
  totalPrice: number;
  purchasedAt: string;
};

/** POST .../store/products/{productId}/purchases 응답 (SHOP-06) */
export type StorePurchaseResult = {
  purchaseId: number;
  storeProductId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  pointBalanceAfter: number;
  purchasedAt: string;
};

/** GET .../store/products/{productId} 상세 (SHOP-02) */
export type StoreProductDetail = {
  storeProductId: number;
  name: string;
  description: string | null;
  category: StoreProductCategory;
  /** AI 가 산정하는 가치 가중치. 화면엔 안 쓰고 price 만 보여준다 */
  value: number | null;
  price: number | null;
  quantity: number;
  createdBy: number;
  createdAt: string;
};
