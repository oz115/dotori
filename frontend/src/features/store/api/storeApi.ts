import { httpClient } from '@/shared/api';
import type {
  StoreProductCreateBody,
  StoreProductDetail,
  StoreProductSummary,
  StoreProductUpdateBody,
  StorePurchaseResult,
  PurchaseHistoryItem,
} from '@/features/store/model/types';

/** 상점 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const storeApi = {
  getProducts: (familyId: number) =>
    httpClient
      .get<{ products: StoreProductSummary[] }>(`/api/v1/families/${familyId}/store/products`)
      .then((res) => res.data.products),

  getProduct: (familyId: number, productId: number) =>
    httpClient
      .get<StoreProductDetail>(`/api/v1/families/${familyId}/store/products/${productId}`)
      .then((res) => res.data),

  createProduct: (familyId: number, body: StoreProductCreateBody) =>
    httpClient
      .post<StoreProductDetail>(`/api/v1/families/${familyId}/store/products`, body)
      .then((res) => res.data),

  updateProduct: (familyId: number, productId: number, body: StoreProductUpdateBody) =>
    httpClient
      .patch<StoreProductDetail>(`/api/v1/families/${familyId}/store/products/${productId}`, body)
      .then((res) => res.data),

  deleteProduct: (familyId: number, productId: number) =>
    httpClient
      .delete<void>(`/api/v1/families/${familyId}/store/products/${productId}`)
      .then(() => undefined),

  purchaseProduct: (familyId: number, productId: number, quantity: number) =>
    httpClient
      .post<StorePurchaseResult>(
        `/api/v1/families/${familyId}/store/products/${productId}/purchases`,
        { quantity },
      )
      .then((res) => res.data),

  /** 구매 내역 (SHOP-07). startDate/endDate 는 'YYYY-MM-DD', 없으면 전체 기간 */
  getPurchaseHistory: (
    familyId: number,
    childId: number,
    range?: { startDate: string; endDate: string },
  ) =>
    httpClient
      .get<{ purchases: PurchaseHistoryItem[] }>(
        `/api/v1/families/${familyId}/children/${childId}/store/purchases`,
        { params: range },
      )
      .then((res) => res.data.purchases),
};
