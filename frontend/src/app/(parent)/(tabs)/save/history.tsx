import { router, useLocalSearchParams } from 'expo-router';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import { SavingsHistory } from '@/features/savings';

/**
 * 자녀 한 명의 예적금 거래 내역 (부모). "보유 현황" 의 자녀 구역에서 들어온다.
 *
 * 아이 화면과 같은 컴포넌트를 쓴다 - 조회 API 가 (familyId, childId) 만 받고 화면에
 * 아이 전용 로직이 없어서다. 제목에 이름을 넣어 누구 것인지 밝히고, 뒤로가기는
 * 부모 그룹으로 명시한다 (`/save` 는 두 그룹에 다 있어 그룹 없이는 갈 곳을 못 정한다).
 */
export default function ParentSavingsHistoryScreen() {
  const { childId } = useLocalSearchParams<{ childId: string }>();
  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);

  const numericChildId = Number(childId) || undefined;
  const childName = (members ?? []).find((member) => member.userId === numericChildId)?.name;

  return (
    <SavingsHistory
      familyId={familyId ?? undefined}
      childId={numericChildId}
      title={childName ? `${childName}의 저축 거래 내역` : '저축 거래 내역'}
      onBack={() =>
        router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/save/holdings')
      }
    />
  );
}
