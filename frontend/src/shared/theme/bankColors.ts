import { palette } from '@/shared/theme/palette';

/**
 * 은행별 브랜드 컬러. 계좌 연동 화면에서 은행 배지/아이콘 배경에 사용.
 * (Figma: A505 / node 448:1471 "은행별 Key Color")
 *
 * textOnBrand: 이 색을 배경으로 깔았을 때 얹을 글자색. kb·nh는 노란 계열이라
 * 흰 글자 대비가 약해 어두운 글자를 쓴다.
 */
export const bankBrand = {
  kb: { label: 'KB국민은행', color: '#FFBC00', textOnBrand: palette.neutral[900] },
  hana: { label: '하나은행', color: '#008375', textOnBrand: palette.neutral[0] },
  woori: { label: '우리은행', color: '#1FB8E4', textOnBrand: palette.neutral[0] },
  shinhan: { label: '신한은행', color: '#0173BC', textOnBrand: palette.neutral[0] },
  nh: { label: '농협은행', color: '#FFB71B', textOnBrand: palette.neutral[900] },
} as const;

export type BankKey = keyof typeof bankBrand;
