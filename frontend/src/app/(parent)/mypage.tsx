import { router } from 'expo-router';

import { MyPage, type MyPageMenuItem } from '@/widgets/mypage';

/**
 * 부모 마이페이지. 자녀와 달리 가족(이름·삭제)을 관리해야 해서 메뉴가 하나 더 있다.
 */
export default function ParentMyPageScreen() {
  const menu: MyPageMenuItem[] = [
    { label: '내 정보 수정', onPress: () => router.push('/mypage-edit') },
    { label: '가족 관리', onPress: () => router.push('/family') },
    {
      label: '연동 계좌 관리',
      onPress: () => router.push('/account-manage'),
    },
  ];

  return <MyPage menu={menu} character={require('@/assets/images/faces/parent-mypage.png')} />;
}
