import { router } from 'expo-router';

import { ReportScreen } from '@/features/report';
import { useParentFamily } from '@/widgets/parent-home';

/**
 * 부모용 리포트 화면. 자녀의 수입·소비 통계(RPT-02·03)와 자산 현황(RPT-04)을 모은다.
 *
 * <p>탭이 아니라 푸시 라우트다 — 부모 탭이 다섯 개로 차 있어서, 홈의 "자녀 자산" 섹션
 * 링크로 들어온다. 탭으로 올리게 되면 진입점이 사라지므로 화면이 스스로 자녀를 고르게
 * 해야 한다 (지금은 홈과 같은 방식으로 가족에서 첫 자녀를 찾는다).
 */
export default function ParentReportScreen() {
  const { familyId, childId, childName, isPending } = useParentFamily();

  return (
    <ReportScreen
      familyId={familyId}
      childId={childId}
      childName={childName}
      familyPending={isPending}
      onBack={() => (router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)'))}
    />
  );
}
