import type { ImageSourcePropType } from 'react-native';

import type { SavingsGroup, SavingsProductType } from '@/features/savings/model/types';

/**
 * 예적금 화면 자산·문구 — 상품 유형별 아이콘과 세그먼트(예금/적금)별 안내 배너 문구.
 * 아이콘은 앱 공용 아이콘 시트에서 잘라낸 것들이다.
 */

const PRODUCT_IMAGES: Record<SavingsProductType, ImageSourcePropType> = {
  // 예금 3종
  ORDINARY: require('@/assets/images/savings/piggy.png'), // 보통예금 — 수시입출금 저금통
  SIMPLE_INTEREST: require('@/assets/images/invest/coin.png'), // 단리예금 — 동전
  COMPOUND_INTEREST: require('@/assets/images/invest/sprout.png'), // 복리예금 — 이자가 자라는 새싹
  // 적금 3종
  SMALL_INSTALLMENT: require('@/assets/images/acorn.png'), // 소액적금 — 도토리 한 알씩
  GOAL_INSTALLMENT: require('@/assets/images/savings/target.png'), // 목표적금 — 과녁
  LONG_TERM_INSTALLMENT: require('@/assets/images/savings/safe.png'), // 장기적금 — 금고
};

export function savingsProductImage(productType: SavingsProductType): ImageSourcePropType {
  return PRODUCT_IMAGES[productType];
}

/** 배너 캐릭터 — 도토리를 모으는 돼지저금통 */
export const SAVINGS_BANNER_IMAGE: ImageSourcePropType = require('@/assets/images/savings/piggy.png');

type BannerCopy = {
  title: string;
  /** 줄 단위 본문. [강조] 구간은 화면이 brand 색으로 칠한다 */
  body: string;
  /** body 안에서 brand 색으로 강조할 구절 */
  highlight: string;
};

export const SAVINGS_BANNER_COPY: Record<SavingsGroup, BannerCopy> = {
  DEPOSIT: {
    title: '예금이란?',
    body:
      '가진 돈을 한 번에 맡기고 목돈을 불리는 저축 방법이에요.\n' +
      '약속한 날까지 기다리면\n맡긴 돈과 이자를 함께 받아요!',
    highlight: '목돈을 불리는',
  },
  INSTALLMENT: {
    title: '적금이란?',
    body:
      '매일, 매주 또는 매달 정한 돈을 차곡차곡 모으는 저축 방법이에요.\n' +
      '약속한 날까지 꾸준히 모으면\n내가 모은 돈과 이자를 함께 받아요!',
    highlight: '차곡차곡',
  },
};
