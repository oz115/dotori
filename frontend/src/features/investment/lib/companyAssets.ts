import type { ImageSourcePropType } from 'react-native';

import type { InvestmentCategory } from '@/features/investment/model/types';

/**
 * 회사(종목)별 화면 자산 — 아이콘·업종 라벨·뉴스 이모지. 서버는 이름·카테고리(enum)만
 * 주므로, 시드 10개 회사의 표시용 자산을 이름으로 매핑한다. 시드에 없는 회사가
 * 추가되면 기본값(도토리 아이콘, enum 라벨)으로 안전하게 표시된다.
 */

type CompanyAsset = {
  image: ImageSourcePropType;
  /** 업종 배지 텍스트 (아이스크림, 항공 …) */
  label: string;
  /** 뉴스 카드 일러스트 자리의 이모지 */
  emoji: string;
};

const COMPANY_ASSETS: Record<string, CompanyAsset> = {
  시원아이스: {
    image: require('@/assets/images/invest/companies/ice.png'),
    label: '아이스크림',
    emoji: '🍦',
  },
  푸른들농업: {
    image: require('@/assets/images/invest/companies/farm.png'),
    label: '농업',
    emoji: '🌾',
  },
  두근여행: {
    image: require('@/assets/images/invest/companies/travel.png'),
    label: '여행',
    emoji: '🧳',
  },
  신나는월드: {
    image: require('@/assets/images/invest/companies/world.png'),
    label: '놀이공원',
    emoji: '🎡',
  },
  하늘항공: {
    image: require('@/assets/images/invest/companies/air.png'),
    label: '항공',
    emoji: '✈️',
  },
  번개택배: {
    image: require('@/assets/images/invest/companies/delivery.png'),
    label: '택배',
    emoji: '📦',
  },
  튼튼건설: {
    image: require('@/assets/images/invest/companies/construction.png'),
    label: '건설',
    emoji: '🏗️',
  },
  고소한빵: {
    image: require('@/assets/images/invest/companies/bread.png'),
    label: '제빵',
    emoji: '🥐',
  },
  씽씽자동차: {
    image: require('@/assets/images/invest/companies/car.png'),
    label: '자동차',
    emoji: '🚗',
  },
  파워배터리: {
    image: require('@/assets/images/invest/companies/battery.png'),
    label: '배터리',
    emoji: '🔋',
  },
};

const FALLBACK_IMAGE = require('@/assets/images/acorn.png');

const CATEGORY_LABELS: Record<InvestmentCategory, string> = {
  STOCK: '주식',
  BOND: '채권',
  ETF: 'ETF',
  FUND: '펀드',
  OTHER: '기타',
};

export function companyImage(name: string): ImageSourcePropType {
  return COMPANY_ASSETS[name]?.image ?? FALLBACK_IMAGE;
}

/** 업종 배지 텍스트 — 시드 회사면 고유 라벨, 아니면 서버 카테고리의 한글 라벨 */
export function companyLabel(name: string, category: InvestmentCategory | null): string {
  return COMPANY_ASSETS[name]?.label ?? (category != null ? CATEGORY_LABELS[category] : '기타');
}

export function companyEmoji(name: string): string {
  return COMPANY_ASSETS[name]?.emoji ?? '📰';
}
