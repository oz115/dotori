import { useQuery } from '@tanstack/react-query';

import { savedFamilyId } from '@/features/family/lib/savedFamilyId';
import { familyKeys } from '@/features/family/model/familyKeys';

/**
 * 로컬(SecureStore)에 저장된 내 가족 id. 서버에 "내 가족 id" 조회 API가 없어서
 * (savedFamilyId 참고) 기기에 저장된 값을 읽어온다.
 *
 * `familyKeys.all` 아래에 있어 가족 생성·연동·삭제 mutation 이 캐시를 무효화할 때
 * 같이 다시 읽힌다. 지갑·집안일·상점처럼 familyId 가 필요한 화면에서 쓴다.
 */
export function useFamilyId() {
  return useQuery({
    queryKey: familyKeys.id(),
    queryFn: () => savedFamilyId.get(),
    staleTime: Infinity,
  });
}
