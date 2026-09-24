import { router } from 'expo-router';

import { AccountManageView } from '@/widgets/mypage';

/**
 * 연동 계좌 관리 (자녀).
 *
 * 연동·변경 화면은 온보딩에서 쓰는 setup 쪽 화면을 그대로 쓴다 - 1원 인증 절차가 부모와
 * 똑같고(계좌 API 는 역할 구분이 없다) 화면도 공용이라, 같은 것을 한 벌 더 둘 이유가 없다.
 * 변경으로 들어갈 때는 mode=change 를 넘겨 문구만 바꾼다.
 */
export default function ChildAccountManageScreen() {
  return (
    <AccountManageView
      onLinkAccount={() => router.push('/(child)/setup/account/link')}
      onChangeAccount={() =>
        router.push({ pathname: '/(child)/setup/account/link', params: { mode: 'change' } })
      }
    />
  );
}
