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
