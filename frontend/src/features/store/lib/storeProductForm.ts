import type { StoreProductCategory } from '@/features/store/model/types';

export const NAME_MAX = 100;
export const DESCRIPTION_MAX = 255;

export type StoreProductFormInput = {
  name: string;
  description: string;
  category: StoreProductCategory | null;
  /** Input 이 주는 원시 문자열. 숫자 변환은 이 함수 안에서 한다 */
  quantity: string;
};

export type StoreProductFormErrors = Partial<Record<keyof StoreProductFormInput, string>>;

/** 상품 등록 폼 검증 (SHOP-03). 백엔드 StoreProductCreateRequest 제약과 맞춘다. */
export function validateStoreProductForm({
  name,
  description,
  category,
  quantity,
}: StoreProductFormInput): StoreProductFormErrors {
  const errors: StoreProductFormErrors = {};

  if (name.trim().length === 0) {
    errors.name = '상품명을 입력해주세요.';
  } else if (name.trim().length > NAME_MAX) {
    errors.name = `상품명은 ${NAME_MAX}자 이내로 입력해주세요.`;
  }

  if (description.trim().length > DESCRIPTION_MAX) {
    errors.description = `설명은 ${DESCRIPTION_MAX}자 이내로 입력해주세요.`;
  }

  if (category == null) {
    errors.category = '카테고리를 선택해주세요.';
  }

  if (parseQuantity(quantity) == null) {
    errors.quantity = '0 이상의 숫자를 입력해주세요.';
  }

  return errors;
}

/** "3" → 3. 빈 문자열·음수·소수·문자는 전부 null (= 유효하지 않음). */
export function parseQuantity(raw: string): number | null {
  if (!/^\d+$/.test(raw.trim())) return null;
  return Number(raw.trim());
}

/**
 * 수정 화면 전용 가격 검증. 백엔드 StoreProductUpdateRequest의 `@Positive`와 맞춘다 —
 * 0 이하는 안 되고, 1 이상의 정수만 허용한다.
 */
export function validatePrice(raw: string): string | undefined {
  if (!/^\d+$/.test(raw.trim()) || Number(raw.trim()) <= 0) {
    return '1 이상의 숫자를 입력해주세요.';
  }
  return undefined;
}
