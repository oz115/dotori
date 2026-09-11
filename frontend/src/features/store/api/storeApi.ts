import { httpClient } from '@/shared/api';
import type { StoreProductSummary } from '@/features/store/model/types';

/** 상점 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const storeApi = {
  getProducts: (familyId: number) =>
    httpClient
      .get<{ products: StoreProductSummary[] }>(`/api/v1/families/${familyId}/store/products`)
      .then((res) => res.data.products),
};
