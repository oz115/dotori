import { useQuery } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { walletKeys } from '@/features/wallet/model/walletKeys';

/**
 * 도토리 포인트 잔액. 자녀 본인도, 같은 가족의 부모도 같은 API 로 조회한다.
 *
 * familyId·childId 는 다른 슬라이스(family·auth) 값이라 이 feature 가 직접 읽지 않고
 * 호출하는 쪽(widget)이 넘겨준다 — features 는 서로 직접 import 하지 않는다.
 * 둘 다 있어야 조회할 수 있어, 그 전엔 요청을 보내지 않는다.
 */
export function useWalletBalance(familyId: number | undefined, childId: number | undefined) {
  const enabled = familyId != null && childId != null;

  return useQuery({
    queryKey: walletKeys.balance(familyId ?? -1, childId ?? -1),
    queryFn: () => walletApi.getBalance(familyId as number, childId as number),
    enabled,
    staleTime: 10_000,
  });
}
