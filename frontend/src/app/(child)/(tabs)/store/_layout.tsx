import { Stack } from 'expo-router';

/**
 * 상점 탭 안의 자체 스택 (목록 → 상품 상세). 탭 레이아웃 안에 중첩돼 있어서
 * 이 안에서 화면을 넘겨도 하단 탭바는 계속 그 자리에 고정돼 있다.
 *
 * initialRouteName을 index(목록)로 명시해야, 탭 밖(알림함/푸시)에서 store/[productId]로
 * 바로 딥링크했을 때 expo-router가 그 아래에 목록 화면을 자동으로 깔아준다 — 이게 없으면
 * 상세 화면 하나만 스택에 쌓여서 뒤로가기가 엉뚱한 탭으로 새고, 하단 탭을 다시 눌러도
 * 목록 없이 상세만 보인다. (parent)/(tabs)/_layout.tsx가 탭 순서 문제를 같은 방식으로
 * 푼 것과 동일한 메커니즘이다.
 */
export const unstable_settings = { initialRouteName: 'index' };

export default function StoreStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
