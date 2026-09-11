import { useAuthStore } from '@/features/auth';
import { useFamilyId } from '@/features/family';

/**
 * wallet·chore·store 조회에 필요한 (familyId, childId) 쌍.
 *
 * features 는 서로 직접 import 하지 않으므로, family(familyId)·auth(childId=본인 userId)를
 * 엮는 건 이 widget 이 한다. 자녀 홈은 가족·계좌가 모두 연결된 뒤에만 그려지므로
 * (app/(child)/index.tsx 가드) 정상 흐름에서는 곧 둘 다 채워진다.
 */
export function useChildIdentity() {
  const { data: familyId } = useFamilyId();
  const childId = useAuthStore((s) => s.user?.userId);
  return { familyId: familyId ?? undefined, childId };
}
