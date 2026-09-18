import { Stack } from 'expo-router';

/**
 * 부모 저축 탭 안의 자체 스택 (메인 → 상품 상세 / 자녀 보유 현황). 탭 레이아웃 안에
 * 중첩돼 있어서 이 안에서 화면을 넘겨도 하단 탭바는 그 자리에 고정된다.
 */
export default function ParentSaveStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
