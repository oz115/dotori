import { router } from 'expo-router';

import { BottomNav, type NavIconName } from '@/shared/ui';

const NAV_ITEMS: { key: NavIconName; label: string }[] = [
  { key: 'task', label: '할 일' },
  { key: 'store', label: '상점' },
  { key: 'home', label: '홈' },
  { key: 'save', label: '저축' },
  { key: 'invest', label: '투자' },
];

type Props = {
  /** 현재 화면에 해당하는 탭 */
  active: NavIconName;
};

/**
 * 메인 하단 탭바 (자녀·부모 공용). 공용 BottomNav(shared/ui)에 라우트 연결을 붙인 것으로,
 * 홈 탭은 '/' 가 역할별 홈((child)/(parent))으로 알아서 갈라진다.
 * 할 일·상점·저축 화면이 생기면 여기 한 곳만 연결하면 되고,
 * 부모 전용 탭 구성이 필요해지면 그때 위젯을 역할별로 분리한다.
 */
export function MainTabBar({ active }: Props) {
  const onSelect = (key: NavIconName) => {
    if (key === active) {
      return;
    }
    if (key === 'home') {
      router.replace('/');
      return;
    }
    if (key === 'invest') {
      router.replace('/invest');
      return;
    }
    // TODO: 할 일·상점·저축 탭은 해당 화면 태스크에서 라우트가 생기면 연결한다
  };

  return <BottomNav items={NAV_ITEMS} activeKey={active} onSelect={onSelect} />;
}
