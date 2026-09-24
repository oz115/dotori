import { router } from 'expo-router';

import { AccountManageView } from '@/widgets/mypage';

/** 연동 계좌 관리 (부모). 계좌 연동·변경 화면은 이 그룹 안에 있다 */
export default function ParentAccountManageScreen() {
  return (
    <AccountManageView
      onLinkAccount={() => router.push('/(parent)/account/link')}
      onChangeAccount={() =>
        router.push({ pathname: '/(parent)/account/link', params: { mode: 'change' } })
      }
    />
  );
}
