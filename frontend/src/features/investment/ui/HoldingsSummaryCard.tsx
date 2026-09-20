import type { ImageSourcePropType } from 'react-native';

import { summarizeInvestment } from '@/features/investment/lib/mappers';
import type { Holding } from '@/features/investment/model/types';
import { InvestmentSummaryCards } from '@/features/investment/ui/InvestmentSummaryCards';

// 마스코트 시트의 다람쥐 — 보유 화면의 안내 캐릭터 (메인의 여우와 짝)
const SQUIRREL: ImageSourcePropType = require('@/assets/images/invest/squirrel.png');

/** 메인과 보유 화면은 같은 요약 기준을 사용하고, 캐릭터만 다르다. */
export function HoldingsSummaryCard({ holdings }: { holdings: Holding[] }) {
  return <InvestmentSummaryCards summary={summarizeInvestment(holdings)} mascot={SQUIRREL} />;
}
