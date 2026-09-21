import { ParentHome } from '@/widgets/parent-home';

/**
 * 부모 홈. 가족이 없거나 아이가 아직 합류하지 않은 부모는 탭 레이아웃 가드가
 * 온보딩 허브로 보내므로 여기서는 준비가 끝난 경우만 그린다.
 */
export default function ParentHomeScreen() {
  return <ParentHome />;
}
