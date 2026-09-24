import { parseQuantity, validatePrice, validateStoreProductForm } from './storeProductForm';

function input(overrides: Partial<Parameters<typeof validateStoreProductForm>[0]> = {}) {
  return {
    name: '게임 30분 이용권',
    description: '정해진 시간 외에 추가로 게임을 하고 싶을 때 구매',
    category: 'GAME' as const,
    quantity: '5',
    ...overrides,
  };
}

describe('validateStoreProductForm', () => {
  it('유효한 입력이면 에러가 없다', () => {
    expect(validateStoreProductForm(input())).toEqual({});
  });

  it('이름이 비어 있으면 에러', () => {
    expect(validateStoreProductForm(input({ name: '  ' }))).toHaveProperty('name');
  });

  it('이름이 100자를 넘으면 에러', () => {
    expect(validateStoreProductForm(input({ name: 'a'.repeat(101) }))).toHaveProperty('name');
  });

  it('설명이 255자를 넘으면 에러', () => {
    expect(validateStoreProductForm(input({ description: 'a'.repeat(256) }))).toHaveProperty(
      'description',
    );
  });

  it('설명은 비어 있어도 에러가 아니다', () => {
    expect(validateStoreProductForm(input({ description: '' }))).toEqual({});
  });

  it('카테고리를 안 고르면 에러', () => {
    expect(validateStoreProductForm(input({ category: null }))).toHaveProperty('category');
  });

  it('수량이 음수·소수·빈 값이면 에러', () => {
    expect(validateStoreProductForm(input({ quantity: '-1' }))).toHaveProperty('quantity');
    expect(validateStoreProductForm(input({ quantity: '1.5' }))).toHaveProperty('quantity');
    expect(validateStoreProductForm(input({ quantity: '' }))).toHaveProperty('quantity');
  });

  it('수량 0은 유효하다', () => {
    expect(validateStoreProductForm(input({ quantity: '0' }))).toEqual({});
  });
});

describe('parseQuantity', () => {
  it('숫자 문자열을 정수로 바꾼다', () => {
    expect(parseQuantity('12')).toBe(12);
  });

  it('숫자가 아니면 null', () => {
    expect(parseQuantity('abc')).toBeNull();
    expect(parseQuantity('-1')).toBeNull();
    expect(parseQuantity('1.5')).toBeNull();
    expect(parseQuantity('')).toBeNull();
  });
});

describe('validatePrice', () => {
  it('1 이상의 정수는 유효하다', () => {
    expect(validatePrice('100')).toBeUndefined();
  });

  it('0 이하·소수·빈 값·문자는 에러', () => {
    expect(validatePrice('0')).toBeDefined();
    expect(validatePrice('-5')).toBeDefined();
    expect(validatePrice('1.5')).toBeDefined();
    expect(validatePrice('')).toBeDefined();
    expect(validatePrice('abc')).toBeDefined();
  });
});
