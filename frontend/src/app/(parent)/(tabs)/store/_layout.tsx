import { Stack } from 'expo-router';

/**
 * 상점 탭 안의 자체 스택 (목록 → 상세 / 등록). 탭 레이아웃 안에 중첩돼 있어서
 * 이 안에서 화면을 넘겨도 하단 탭바는 계속 그 자리에 고정돼 있다.
 */
export default function StoreStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
