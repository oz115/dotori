/** 상점 관련 TanStack Query 키. */
export const storeKeys = {
  all: ['store'] as const,
  products: (familyId: number) => [...storeKeys.all, 'products', familyId] as const,
  product: (familyId: number, productId: number) =>
    [...storeKeys.all, 'product', familyId, productId] as const,
  purchases: (familyId: number, childId: number, range?: { startDate: string; endDate: string }) =>
    [...storeKeys.all, 'purchases', familyId, childId, range ?? 'all'] as const,
};
