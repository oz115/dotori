import { router } from 'expo-router';
import { Alert } from 'react-native';

import { MyPage, type MyPageMenuItem } from '@/widgets/mypage';

/**
 * 자녀 마이페이지. 가족 관리는 부모 몫이라 빠져 있고,
 * 계좌는 이미 연동을 마쳐야 홈에 들어올 수 있어 관리 화면이 생길 때까지 보류다.
 */
export default function ChildMyPageScreen() {
  const notReady = () => Alert.alert('준비 중이에요', '곧 만나볼 수 있어요.');

  const menu: MyPageMenuItem[] = [
    { label: '내 정보 수정', onPress: () => router.push('/mypage-edit') },
    { label: '충전계좌관리', onPress: notReady },
  ];

  return <MyPage menu={menu} character={require('@/assets/images/faces/child-mypage.png')} />;
}
