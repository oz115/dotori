import { router } from 'expo-router';
import { useMemo } from 'react';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import { ParentInvestmentHome } from '@/features/investment';

/**
 * 부모 투자 탭의 첫 화면. 자녀 목록은 family 슬라이스 값이라 features 끼리 직접 import 하지 않도록
 * 이 라우트가 엮어서 넘긴다 (저축 탭과 같은 방식). 하위 페이지로는 고른 자녀를 childId 로 넘긴다.
 */
export default function ParentInvestmentScreen() {
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const childList = useMemo(
    () =>
      (members ?? [])
        .filter((member) => member.role === 'CHILD')
        .map((member) => ({ childId: member.userId, name: member.name })),
    [members],
  );

  return (
    <ParentInvestmentHome
      childList={childList}
      onPressStock={(productId, childId) =>
        router.push({
          pathname: '/(parent)/(tabs)/investment/[stockId]',
          params: {
            stockId: String(productId),
            ...(childId != null ? { childId: String(childId) } : {}),
          },
        })
      }
      onPressHoldings={(childId) =>
        router.push({
          pathname: '/(parent)/(tabs)/investment/holdings',
          params: { childId: String(childId) },
        })
      }
      onPressTransactions={(childId) =>
        router.push({
          pathname: '/(parent)/(tabs)/investment/transactions',
          params: { childId: String(childId) },
        })
      }
      onPressMarket={(childId) =>
        router.push({
          pathname: '/(parent)/(tabs)/investment/market',
          params: childId != null ? { childId: String(childId) } : {},
        })
      }
      onPressNews={() => router.push('/(parent)/(tabs)/investment/news')}
      onPressNewsReview={() => router.push('/(parent)/(tabs)/investment/news-review')}
    />
  );
}
