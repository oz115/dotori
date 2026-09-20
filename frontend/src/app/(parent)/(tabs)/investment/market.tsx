import { router, useLocalSearchParams } from 'expo-router';

import { ParentInvestmentMarket } from '@/features/investment';

/** 주식 시세 (부모). 종목 상세로 넘어갈 때 보고 있던 자녀(childId)를 이어서 넘긴다. */
export default function ParentInvestmentMarketScreen() {
  const { childId } = useLocalSearchParams<{ childId?: string }>();

  return (
    <ParentInvestmentMarket
      onBack={() =>
        router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/investment')
      }
      onPressStock={(productId) =>
        router.push({
          pathname: '/(parent)/(tabs)/investment/[stockId]',
          params: { stockId: String(productId), ...(childId != null ? { childId } : {}) },
        })
      }
    />
  );
}
