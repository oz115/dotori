/** 상점 관련 TanStack Query 키. */
export const storeKeys = {
  all: ['store'] as const,
  products: (familyId: number) => [...storeKeys.all, 'products', familyId] as const,
};
