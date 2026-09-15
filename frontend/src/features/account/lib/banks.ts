import type { ImageSourcePropType } from 'react-native';

import type { BankKey } from '@/shared/theme';

/**
 * 계좌 연동에서 고를 수 있는 은행 목록. 서버에 은행 목록 API 가 없어 프론트에서 관리한다.
 * bankCode 는 금융결제원 표준 코드. key 는 `shared/theme`의 은행별 브랜드 컬러(`bankBrand`)와
 * 같은 값을 쓴다 — 카드 배경색을 고를 때 이 key 로 바로 조회한다.
 */
export type Bank = {
  key: BankKey;
  /** 서버로 보내는 은행 코드 */
  code: string;
  /** 서버로 보내는 은행 이름 */
  name: string;
  /** 카드·리스트에 쓰는 짧은 이름 */
  shortName: string;
  logo: ImageSourcePropType;
};

export const BANKS: Bank[] = [
  {
    key: 'kb',
    code: '004',
    name: 'KB국민은행',
    shortName: '국민',
    logo: require('@/assets/images/banks/bank-kb.png'),
  },
  {
    key: 'shinhan',
    code: '088',
    name: '신한은행',
    shortName: '신한',
    logo: require('@/assets/images/banks/bank-shinhan.png'),
  },
  {
    key: 'woori',
    code: '020',
    name: '우리은행',
    shortName: '우리',
    logo: require('@/assets/images/banks/bank-woori.png'),
  },
  {
    key: 'hana',
    code: '081',
    name: '하나은행',
    shortName: '하나',
    logo: require('@/assets/images/banks/bank-hana.png'),
  },
  {
    key: 'nh',
    code: '011',
    name: 'NH농협은행',
    shortName: '농협',
    logo: require('@/assets/images/banks/bank-nh.png'),
  },
];

export function findBank(key: string | null | undefined): Bank | undefined {
  return BANKS.find((b) => b.key === key);
}
