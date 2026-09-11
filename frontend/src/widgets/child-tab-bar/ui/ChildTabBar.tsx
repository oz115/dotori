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
 * 자녀 화면 공통 하단 탭바. 공용 BottomNav(shared/ui)에 자녀 라우트 연결을 붙인 것으로,
 * 자녀 탭 화면들이 이 위젯 하나로 같은 탭 구성을 공유한다.
 * 할 일·상점·저축 화면이 생기면 여기 한 곳만 연결하면 된다.
 */
export function ChildTabBar({ active }: Props) {
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
