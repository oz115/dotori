import { router } from 'expo-router';
import { Alert } from 'react-native';

import { useMyAccount } from '@/features/account';
import { MyPage, type MyPageMenuItem } from '@/widgets/mypage';

/**
 * 부모 마이페이지. 자녀와 달리 가족(이름·삭제)을 관리해야 해서 메뉴가 하나 더 있다.
 */
export default function ParentMyPageScreen() {
  const account = useMyAccount();

  const notReady = () => Alert.alert('준비 중이에요', '곧 만나볼 수 있어요.');

  const menu: MyPageMenuItem[] = [
    { label: '내 정보 수정', onPress: () => router.push('/mypage-edit') },
    { label: '가족 관리', onPress: () => router.push('/family') },
    {
      label: '충전계좌관리',
      // 연동 전이면 연동 화면으로, 이미 연동돼 있으면 관리 화면이 생길 때까지 보류
      onPress: account.data ? notReady : () => router.push('/account/link'),
    },
  ];

  return <MyPage menu={menu} character={require('@/assets/images/faces/parent-mypage.png')} />;
}
