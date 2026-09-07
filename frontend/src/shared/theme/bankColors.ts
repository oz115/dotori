/**
 * 은행별 브랜드 컬러. 계좌 연동 화면에서 은행 배지/아이콘 배경에 사용.
 * (Figma: A505 / node 448:1471 "은행별 Key Color")
 */
export const bankBrand = {
  kb: { label: 'KB국민은행', color: '#FFBC00' },
  hana: { label: '하나은행', color: '#008375' },
  woori: { label: '우리은행', color: '#1FB8E4' },
  shinhan: { label: '신한은행', color: '#0173BC' },
  nh: { label: '농협은행', color: '#FFB71B' },
} as const;

export type BankKey = keyof typeof bankBrand;
