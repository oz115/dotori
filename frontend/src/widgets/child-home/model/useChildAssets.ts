import { summarizeHoldings, useInvestmentHoldings } from '@/features/investment';
import { summarizeSavingsAssets, useSavingsSummary } from '@/features/savings';

import {
  investmentAssetRow,
  savingsAssetRow,
  type AssetRowText,
} from '@/widgets/child-home/model/assetRows';

/**
 * 자녀 홈 "내 자산 현황"의 예적금·투자 줄. 로그인한 아이 본인의 실제 데이터다.
 *
 * - 예적금: 서버 요약 API(진행 중 + 수령 대기 건의 원금 + 이자). 캐시 키에 familyId·childId 가 있다.
 * - 투자: 보유 목록(`useInvestmentHoldings`)에서 현재 시세 기준 보유 가치를 계산한다. 투자 탭
 *   "보유 자산"이 같은 데이터·같은 계산(`summarizeHoldings`)을 쓰므로 숫자가 서로 일치한다.
 *   본인 childId 를 명시해 조회하므로 캐시 키에 사용자가 들어간다 (계정을 바꿔도 이전 사용자의 값이 남지 않는다).
 *
 * familyId·childId 는 다른 슬라이스(family·auth) 값이라 호출하는 쪽이 `useChildIdentity` 로 넘긴다.
 */
export function useChildAssets(
  familyId: number | undefined,
  childId: number | undefined,
): { savings: AssetRowText; investment: AssetRowText; refetch: () => void } {
  const savings = useSavingsSummary(familyId, childId);
  const holdings = useInvestmentHoldings(childId, { enabled: childId != null });

  return {
    savings: savingsAssetRow({
      isPending: savings.isPending,
      isError: savings.isError,
      data: savings.data != null ? summarizeSavingsAssets(savings.data) : undefined,
    }),
    investment: investmentAssetRow({
      isPending: holdings.isPending,
      isError: holdings.isError,
      data: holdings.data != null ? summarizeHoldings(holdings.data) : undefined,
    }),
    // 자산 카드로 슬라이드할 때 최신값으로 새로고침하려고 노출한다 (환전 승인 반영 지연 방지).
    refetch: () => {
      void savings.refetch();
      void holdings.refetch();
    },
  };
}
