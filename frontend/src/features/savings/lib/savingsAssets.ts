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

/**
 * 상품 상세 배너에 들어가는 한 줄 설명.
 *
 * 서버의 `description` 을 쓰지 않는다. 그 값은 시더가 `educationGoal` 을 그대로 넣은
 * 것이고("자유로운 대신 이자가 적다는 점 학습"), 기획 문서에 쓰는 학습 목표 메모라
 * 화면에 내보낼 문장이 아니다. 초등학생이 읽을 말은 화면이 갖는다 — 그룹 안내 문구
 * (SAVINGS_BANNER_COPY)와 같은 자리에 두는 이유다.
 *
 * <p>쓰는 방식은 각 상품의 스펙에서 나온다. 무엇이 다른지를 한 문장으로 짚고, 그 대가가
 * 무엇인지(자유로운 대신 이자가 적다, 오래 기다리는 만큼 많다)를 설명하지 않고 느끼게 한다.
 * 숫자는 적지 않는다 - 이자율·기간·최대 금액은 바로 아래 정보 카드에 이미 있고, 가족
 * 설정에 따라 달라지는 값도 있어 문구에 박으면 어긋난다.
 */
const PRODUCT_DESCRIPTIONS: Record<SavingsProductType, string> = {
  ORDINARY: '돼지저금통처럼 언제든 넣고 빼는 예금이에요.\n이자는 적지만 매주 꼬박꼬박 들어와요.',
  SIMPLE_INTEREST:
    '도토리를 한 번에 맡기고 기다리는 예금이에요.\n' +
    '처음 맡긴 만큼만 이자가 붙어서 얼마를 받을지 미리 알 수 있어요.',
  COMPOUND_INTEREST:
    '이자에 또 이자가 붙는 예금이에요.\n' +
    '눈덩이처럼 불어나니, 같은 돈을 맡긴 단리예금과 얼마나 다른지 비교해 보세요.',
  SMALL_INSTALLMENT:
    '매주 같은 금액을 두 번만 넣으면 끝나는 짧은 적금이에요.\n' +
    '작게 시작해서 끝까지 해내는 맛을 먼저 느껴봐요.',
  GOAL_INSTALLMENT:
    '먼저 목표를 정하고 자유롭게 채우는 적금이에요.\n목표를 채우는 순간 이자까지 바로 받아요.',
  LONG_TERM_INSTALLMENT:
    '매주 같은 금액을 여섯 번 넣는 가장 긴 적금이에요.\n오래 기다리는 만큼 이자가 가장 커요.',
};

export function savingsProductDescription(productType: SavingsProductType): string {
  return PRODUCT_DESCRIPTIONS[productType];
}
