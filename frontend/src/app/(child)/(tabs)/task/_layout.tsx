import { Stack } from 'expo-router';

/**
 * 할 일 탭 안의 자체 스택 (목록 → 완료 이력). 탭 레이아웃 안에 중첩돼 있어서
 * 이 안에서 화면을 넘겨도 하단 탭바는 그 자리에 고정된다 (상점 탭과 같은 구조).
 */
export default function TaskStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
